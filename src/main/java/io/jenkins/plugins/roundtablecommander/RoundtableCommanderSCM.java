package io.jenkins.plugins.roundtablecommander;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.security.ACL;
import hudson.security.Permission;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import ro.acorn.roundtable.model.ICommit;
import ro.acorn.roundtable.rtbclient.IRoundtableClient;
import ro.acorn.roundtable.rtbclient.RoundtableException;
import ro.acorn.roundtable.rtbclient.UserPasswordCredentials;
import ro.acorn.roundtable.rtbclient.command.impl.BranchCopyCommand;
import ro.acorn.roundtable.rtbclient.command.impl.CheckoutCommand;
import ro.acorn.roundtable.rtbclient.command.impl.FetchCommand;
import ro.acorn.roundtable.rtbclient.impl.RoundtableClient;

public class RoundtableCommanderSCM extends SCM implements Serializable {

	private static final long serialVersionUID = -7960908689871794824L;

	@CheckForNull
	private final RoundtableRepositoryBrowser browser;
	/**
	 * All the remote repositories that we know about.
	 */
	private final List<RemoteConfig> remoteConfigs;

	private final String workingDirectory;

	private final String initCheckout;

	public static final CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers
			.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));

	@DataBoundConstructor
	public RoundtableCommanderSCM(List<RemoteConfig> remoteConfigs, String workingDirectory, String initCheckout,
			@CheckForNull RoundtableRepositoryBrowser browser) {
		this.remoteConfigs = new ArrayList<>(remoteConfigs);
		this.workingDirectory = workingDirectory;
		this.initCheckout = initCheckout;
		this.browser = new RoundtableRepositoryBrowser(this);
	}

	@Override
	public void checkout(@Nonnull Run<?, ?> build, @Nonnull Launcher launcher, @Nonnull FilePath workspace,
			@Nonnull TaskListener listener, @CheckForNull File changelogFile, @CheckForNull SCMRevisionState baseline)
			throws IOException, InterruptedException {

		try {
			IRoundtableClient client = workspace.act(new RoundtableClientMasterToSlaveFileCallable(getRegData()));

			client.addLogger(new RTBTaskListener(listener));

			final Job<?, ?> job = build.getParent();
			final EnvVars envs = build.getEnvironment(listener);
			String initWorkspace = getInitCheckout();
			LinkedHashMap<String, Integer> matchingBranches = new LinkedHashMap<>();
			List<ICommit> commits = new ArrayList<>();

			for (RemoteConfig remoteConfig : getRemoteConfigs()) {
				UserPasswordCredentials credentials = getCredentials(job, remoteConfig);
				String name = remoteConfig.getName() != null ? remoteConfig.getName() : "origin";

				// check if remote already exists, else add it here
				if (!client.getRemotes().stream().anyMatch(r -> r.namesMatch(name, r.getName()))) {
					client.addRemote(name, remoteConfig.getUrl());
				}

				Collection<String> remoteBranches = client.getRemoteBranches(name, credentials);

				if (remoteConfig.getWorkspaces() != null && !remoteConfig.getWorkspaces().isEmpty()) {
					remoteConfig.getWorkspaces().forEach(spec -> {
						spec.filterMatching(remoteBranches, envs).forEach(b -> {
							matchingBranches.put(b, spec.getShallow());
						});
					});
				} else {
					remoteBranches.forEach(b -> {
						matchingBranches.put(b, getShallowDepth());
					});
				}

				matchingBranches.entrySet().forEach(b -> {
					commits.addAll(fetchBranch(client, b.getKey(), name, b.getValue(), credentials));
				});
			}

			// check out the first branch that matched by default
			if ((initWorkspace == null || initWorkspace.isBlank())) {
				initWorkspace = matchingBranches.keySet().stream().findFirst().orElse(null);
			}

			if (initWorkspace != null) {
				client.checkout(new CheckoutCommand(initWorkspace, null, null, true, true, false));
			}

			writeChangeLog(changelogFile, commits, listener);

		} catch (RoundtableException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		return new RoundtableChangeLogParser();
	}

	@Override
	public boolean requiresWorkspaceForPolling() {
		return false;
	}

	@Override
	@Whitelisted
	public RoundtableRepositoryBrowser getBrowser() {
		return browser;
	}

	@Exported
	public List<RemoteConfig> getRemoteConfigs() {
		return Collections.unmodifiableList(remoteConfigs);
	}

	@Exported
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	@Exported
	public String getInitCheckout() {
		return initCheckout;
	}

	public boolean isAddTagAction() {
		DescriptorImpl descriptor = (DescriptorImpl) getDescriptor();
		return (descriptor != null && descriptor.isAddTagAction());
	}

	public String getRegData() {
		DescriptorImpl descriptor = (DescriptorImpl) getDescriptor();
		return descriptor != null ? descriptor.getRegData() : null;
	}

	public int getShallowDepth() {
		DescriptorImpl descriptor = (DescriptorImpl) getDescriptor();
		return descriptor != null ? descriptor.getShallowDepth() : 0;
	}

	@Extension
	public static final class DescriptorImpl extends SCMDescriptor<RoundtableCommanderSCM> {

		private boolean addTagAction;
		private int shallowDepth;
		private String regData;

		public DescriptorImpl() {
			super(RoundtableCommanderSCM.class, RoundtableRepositoryBrowser.class);
			load();
		}

		@NonNull
		@Override
		public Permission getRequiredGlobalConfigPagePermission() {
			return Jenkins.ADMINISTER;
		}

		Permission getJenkinsManageOrAdmin() {
			return Jenkins.ADMINISTER;
		}

		public String getDisplayName() {
			return "Roundtable Commander";
		}

		public boolean isAddTagAction() {
			return addTagAction;
		}

		public void setAddTagAction(boolean addTagAction) {
			this.addTagAction = addTagAction;
		}

		public int getShallowDepth() {
			return shallowDepth;
		}

		public void setShallowDepth(int shallowDepth) {
			this.shallowDepth = shallowDepth;
		}

		public String getRegData() {
			return regData;
		}

		public void setRegData(String regData) {
			this.regData = regData;
		}

		@Override
		public boolean isApplicable(Job project) {
			return true;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			req.bindJSON(this, formData);
			save();
			return true;
		}

	}

	private static class RoundtableClientMasterToSlaveFileCallable extends MasterToSlaveFileCallable<RoundtableClient> {

		private static final long serialVersionUID = 3994075442604771557L;
		private final String regData;

		public RoundtableClientMasterToSlaveFileCallable(String regData) {
			this.regData = regData;
		}

		@Override
		public RoundtableClient invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
			return RoundtableClient.in(f, regData);
		}

	}

	@Override
	public SCMRevisionState calcRevisionsFromBuild(Run<?, ?> build, FilePath workspace, Launcher launcher,
			TaskListener listener) throws IOException, InterruptedException {
		return SCMRevisionState.NONE;
	}

	private Collection<ICommit> fetchBranch(IRoundtableClient client, String branch, String remote, int shallowDepth,
			UserPasswordCredentials credentials) {
		boolean existing = client.getBranches().stream().anyMatch(b -> branch.equals(b.getName()));
		String remoteBranch = String.format("remotes/%s/%s", remote, branch);

		Collection<ICommit> commits = client.fetch(new FetchCommand(existing ? branch : remoteBranch, credentials,
				false, false, true, shallowDepth, true, false));

		if (!existing) {
			client.copy(new BranchCopyCommand(remoteBranch, branch, null, true));
		}

		return commits;
	}

	private void writeChangeLog(File changelogFile, Collection<ICommit> commits, TaskListener listener) {

		if (commits != null && !commits.isEmpty()) {

			try {
				ObjectMapper mapper = new ObjectMapper();
				mapper.writeValue(changelogFile, commits);
			} catch (Exception e) {
				listener.error("Error writing the change log file: \"%s\".", e.getMessage());
			}
		}
	}

	private UserPasswordCredentials getCredentials(Job<?, ?> job, RemoteConfig remote) {
		if (remote != null && remote.getCredentialsId() != null) {
			List<StandardUsernamePasswordCredentials> urlCredentials = CredentialsProvider.lookupCredentials(
					StandardUsernamePasswordCredentials.class, job,
					job instanceof Queue.Task ? ((Queue.Task) job).getDefaultAuthentication() : ACL.SYSTEM,
					URIRequirementBuilder.fromUri(remote.getUrl()).build());
			CredentialsMatcher ucMatcher = CredentialsMatchers.withId(remote.getCredentialsId());
			CredentialsMatcher idMatcher = CredentialsMatchers.allOf(ucMatcher,
					RoundtableCommanderSCM.CREDENTIALS_MATCHER);
			StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(urlCredentials,
					idMatcher);

			if (credentials != null) {
				return new UserPasswordCredentials(credentials.getUsername(), credentials.getPassword().getPlainText());
			}
		}

		return null;
	}

}

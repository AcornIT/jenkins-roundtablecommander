package jenkins.plugins.roundtablecommander;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.jenkinsci.remoting.RoleChecker;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import hudson.EnvVars;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.scm.SCMRevisionState;
import hudson.security.ACL;
import ro.acorn.roundtable.model.ICommit;
import ro.acorn.roundtable.rtbclient.IRoundtableClient;
import ro.acorn.roundtable.rtbclient.RoundtableException;
import ro.acorn.roundtable.rtbclient.UserPasswordCredentials;
import ro.acorn.roundtable.rtbclient.command.impl.BranchCopyCommand;
import ro.acorn.roundtable.rtbclient.command.impl.CheckoutCommand;
import ro.acorn.roundtable.rtbclient.command.impl.FetchCommand;
import ro.acorn.roundtable.rtbclient.impl.RoundtableClient;

public class CheckoutCallable implements FileCallable<Void> {

	private static final long serialVersionUID = 1L;

	private final transient Run<?, ?> build;
	private final TaskListener listener;
	private final File changelogFile;
	private final RoundtableCommanderSCM roundtableSCM;

	public CheckoutCallable(Run<?, ?> build, Launcher launcher, TaskListener listener, File changelogFile,
			SCMRevisionState baseline, RoundtableCommanderSCM roundtableSCM) {
		this.build = build;
		this.listener = listener;
		this.changelogFile = changelogFile;
		this.roundtableSCM = roundtableSCM;
	}

	@Override
	public void checkRoles(RoleChecker checker) throws SecurityException {

	}

	@Override
	public Void invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {

		try {
			IRoundtableClient client = RoundtableClient.in(workspace, roundtableSCM.getRegData());
			client.addLogger(new RTBTaskListener(listener));

			final Job<?, ?> job = build.getParent();
			final EnvVars envs = build.getEnvironment(listener);
			String initWorkspace = roundtableSCM.getInitCheckout();
			List<String> workspaces = new ArrayList<>();
			List<ICommit> commits = new ArrayList<>();

			for (RemoteConfig remoteConfig : roundtableSCM.getRemoteConfigs()) {
				UserPasswordCredentials credentials = getCredentials(job, remoteConfig);
				String name = remoteConfig.getName() != null ? remoteConfig.getName() : "origin";

				// check if remote already exists, else add it here
				if (!client.getRemotes().stream().anyMatch(r -> r.namesMatch(name, r.getName()))) {
					client.addRemote(name, remoteConfig.getUrl());
				}

				Collection<String> remoteBranches = client.getRemoteBranches(name, credentials);
				HashMap<String, Integer> matchingBranches = new HashMap<>();

				remoteConfig.getWorkspaces().forEach(spec -> {
					spec.filterMatching(remoteBranches, envs).forEach(b -> {
						workspaces.add(b);
						matchingBranches.put(b, spec.getShallow());
					});
				});

				matchingBranches.entrySet().forEach(b -> {
					commits.addAll(fetchBranch(client, b.getKey(), name, b.getValue(), credentials));
				});
			}

			// check out the first branch that matched by default
			if ((initWorkspace == null || initWorkspace.isBlank()) && !workspaces.isEmpty()) {
				initWorkspace = workspaces.get(0);
			}

			if (initWorkspace != null && !initWorkspace.isBlank()) {
				client.checkout(new CheckoutCommand(initWorkspace, null, null, true, true, false));
			}

			writeChangeLog(changelogFile, commits);

		} catch (RoundtableException e) {
			throw new IOException(e.getMessage());
		}
		return null;

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

	private void writeChangeLog(File changelogFile, Collection<ICommit> commits) {

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
			CredentialsMatcher idMatcher = CredentialsMatchers.allOf(ucMatcher, RoundtableCommanderSCM.CREDENTIALS_MATCHER);
			StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(urlCredentials,
					idMatcher);

			if (credentials != null) {
				return new UserPasswordCredentials(credentials.getUsername(), credentials.getPassword().getPlainText());
			}
		}

		return null;
	}
}

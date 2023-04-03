package jenkins.plugins.roundtablecommander;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

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
		this.remoteConfigs = remoteConfigs;
		this.workingDirectory = workingDirectory;
		this.initCheckout = initCheckout;
		this.browser = new RoundtableRepositoryBrowser(this);
	}

	@Override
	public void checkout(@Nonnull Run<?, ?> build, @Nonnull Launcher launcher, @Nonnull FilePath workspace,
			@Nonnull TaskListener listener, @CheckForNull File changelogFile, @CheckForNull SCMRevisionState baseline)
			throws IOException, InterruptedException {

		workspace.act(new CheckoutCallable(build, launcher, listener, changelogFile, baseline, this));
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
		return remoteConfigs;
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
			return Jenkins.MANAGE;
		}

		Permission getJenkinsManageOrAdmin() {
			return Jenkins.MANAGE;
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

	@Override
	public SCMRevisionState calcRevisionsFromBuild(Run<?, ?> build, FilePath workspace, Launcher launcher,
			TaskListener listener) throws IOException, InterruptedException {
		return super.calcRevisionsFromBuild(build, workspace, launcher, listener);
	}

}

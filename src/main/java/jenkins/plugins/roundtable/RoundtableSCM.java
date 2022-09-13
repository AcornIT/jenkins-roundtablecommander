package jenkins.plugins.roundtable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

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
import net.sf.json.JSONObject;

public class RoundtableSCM extends SCM implements Serializable {

	private static final long serialVersionUID = -7960908689871794824L;
	private final RoundtableRepositoryBrowser browser;
	private final UserRemoteConfig userRemoteConfig;
	private final String workingDirectory;

	private static final Logger logger = Logger.getLogger(RoundtableSCM.class.getName());

	public static final CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers
			.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));

	public RoundtableSCM(String repositoryUrl) {
		this(repositoryUrl, null);
	}

	public RoundtableSCM(String repositoryUrl, String credentialsId) {
		this(new UserRemoteConfig(repositoryUrl, credentialsId), null);
	}

	@DataBoundConstructor
	public RoundtableSCM(UserRemoteConfig userRemoteConfig, String workingDirectory) {
		this.userRemoteConfig = userRemoteConfig;
		this.workingDirectory = workingDirectory;
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
	@Exported
	public RoundtableRepositoryBrowser getBrowser() {
		return browser;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return DescriptorImpl.DESCRIPTOR;
	}

	public UserRemoteConfig getUserRemoteConfig() {
		return userRemoteConfig;
	}
	
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	@Extension
	public static final class DescriptorImpl extends SCMDescriptor<RoundtableSCM> {

		public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

		public DescriptorImpl() {
			super(RoundtableSCM.class, RoundtableRepositoryBrowser.class);
			load();
		}

		public String getDisplayName() {
			return "Roundtable";
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

}

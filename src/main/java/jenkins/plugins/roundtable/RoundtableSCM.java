package jenkins.plugins.roundtable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

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

public class RoundtableSCM extends SCM implements Serializable {

	private static final long serialVersionUID = -7960908689871794824L;

	@CheckForNull
	private final RoundtableRepositoryBrowser browser;
	/**
	 * All the remote repositories that we know about.
	 */
	private final List<UserRemoteConfig> userRemoteConfigs;

	/**
	 * All the branches that we wish to care about building.
	 */
	private final List<BranchSpec> branches;

	private final String workingDirectory;

	private static final Logger logger = Logger.getLogger(RoundtableSCM.class.getName());

	public static final CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers
			.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));

	@DataBoundConstructor
	public RoundtableSCM(List<UserRemoteConfig> userRemoteConfigs, List<BranchSpec> branches, String workingDirectory,
			@CheckForNull RoundtableRepositoryBrowser browser) {
		this.userRemoteConfigs = userRemoteConfigs;
		this.branches = branches;
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
	@Whitelisted
	public RoundtableRepositoryBrowser getBrowser() {
		return browser;
	}

	@Exported
	public List<UserRemoteConfig> getUserRemoteConfigs() {
		return userRemoteConfigs;
	}

	@Exported
	public List<BranchSpec> getBranches() {
		return branches;
	}

	@Exported
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public boolean isAddTagAction() {
        DescriptorImpl gitDescriptor = (DescriptorImpl) getDescriptor();
        return (gitDescriptor != null && gitDescriptor.isAddTagAction());
    }
	
	@Extension
	public static final class DescriptorImpl extends SCMDescriptor<RoundtableSCM> {

		private boolean addTagAction;

		public DescriptorImpl() {
			super(RoundtableSCM.class, RoundtableRepositoryBrowser.class);
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
			return "Roundtable";
		}

		public boolean isAddTagAction() {
			return addTagAction;
		}

		public void setAddTagAction(boolean addTagAction) {
			this.addTagAction = addTagAction;
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

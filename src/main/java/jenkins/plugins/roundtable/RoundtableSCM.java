package jenkins.plugins.roundtable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import net.sf.json.JSONObject;

public class RoundtableSCM extends SCM implements Serializable {

	private final RoundtableRepositoryBrowser browser;
	private final List<UserRemoteConfig> userRemoteConfigs;
	private final String workingDirectory;

	private static final Logger logger = Logger.getLogger(RoundtableSCM.class.getName());
	private static final long serialVersionUID = 1L;

	@DataBoundConstructor
	public RoundtableSCM(List<UserRemoteConfig> userRemoteConfigs, String workingDirectory) {
		this.workingDirectory = workingDirectory;
		this.userRemoteConfigs = userRemoteConfigs;
		this.browser = new RoundtableRepositoryBrowser(this);
	}

	@Override
	public void checkout(Run<?, ?> build, Launcher launcher, FilePath workspace, TaskListener listener,
			File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException {
		if (this.userRemoteConfigs != null) {
			for (UserRemoteConfig remote : userRemoteConfigs) {
				System.out.println("checkout - " + remote.getUrl() + " " + remote.getCredentialsId());
			}
		}
		
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		System.out.println("createChangeLogParser()");
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

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public List<UserRemoteConfig> getUserRemoteConfigs() {
		return userRemoteConfigs;
	}

	public static final class DescriptorImpl extends SCMDescriptor<RoundtableSCM> {
		@Extension
		public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

		private String globalConfigName;
		private boolean showEntireCommitSummaryInChanges;

		public DescriptorImpl() {
			super(RoundtableSCM.class, RoundtableRepositoryBrowser.class);
			load();
		}

		@Override
		public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			System.out.println("new instance: " + formData.toString());
			RoundtableSCM scm = req.bindJSON(RoundtableSCM.class, formData);

			return scm;
		}

		public boolean isShowEntireCommitSummaryInChanges() {
			return showEntireCommitSummaryInChanges;
		}

		public void setShowEntireCommitSummaryInChanges(boolean showEntireCommitSummaryInChanges) {
			this.showEntireCommitSummaryInChanges = showEntireCommitSummaryInChanges;
		}

		public String getDisplayName() {
			return "Roundtable";
		}

		@Override
		public boolean isApplicable(Job project) {
			return true;
		}

		/**
		 * Global setting to be used in call to "git config user.name".
		 * 
		 * @return user.name value
		 */
		public String getGlobalConfigName() {
			return Util.fixEmptyAndTrim(globalConfigName);
		}

		/**
		 * Global setting to be used in call to "git config user.name".
		 * 
		 * @param globalConfigName user.name value to be assigned
		 */
		public void setGlobalConfigName(String globalConfigName) {
			this.globalConfigName = globalConfigName;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			System.out.println("config: " + formData.toString());

			save();
			return true;
		}

		/**
		 * Fill in the environment variables for launching git
		 * 
		 * @param env base environment variables
		 */
		public void populateEnvironmentVariables(Map<String, String> env) {
			String name = getGlobalConfigName();
			if (name != null) {
				env.put("RTB_COMMITTER_NAME", name);
				env.put("RTB_AUTHOR_NAME", name);
			}
		}

	}

}

package jenkins.plugins.roundtable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

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

public class RoundtableSCM extends SCM {

	String repositoryUrl;

	@DataBoundConstructor
	public RoundtableSCM(String repositoryUrl) {
		this.repositoryUrl = repositoryUrl;
	}

	@Override
	public void checkout(Run<?, ?> build, Launcher launcher, FilePath workspace, TaskListener listener,
			File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException {
		System.out.println("checkout()");
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		System.out.println("createChangeLogParser()");
		return new RoundtableChangeLogParser();
	}

	@Extension
	public static final class DescriptorImpl extends SCMDescriptor<RoundtableSCM> {

		private String globalConfigName;
		private boolean showEntireCommitSummaryInChanges;

		public DescriptorImpl() {
			super(RoundtableSCM.class, RoundtableRepositoryBrowser.class);
			load();
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
			req.bindJSON(this, formData);
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

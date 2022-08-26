package jenkins.plugins.roundtable;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;

import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import org.jenkinsci.plugins.workflow.steps.scm.SCMStep;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Runs Roundtable using {@link RtbSCM}.
 */
public final class RoundtableStep extends SCMStep {

	private static final long serialVersionUID = -9096219579613060249L;
	private final String url;
	private String branch = "master";
	private String credentialsId;

	@DataBoundConstructor
	public RoundtableStep(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public String getBranch() {
		return branch;
	}

	public String getCredentialsId() {
		return credentialsId;
	}

	@DataBoundSetter
	public void setBranch(String branch) {
		this.branch = branch;
	}

	@DataBoundSetter
	public void setCredentialsId(String credentialsId) {
		this.credentialsId = Util.fixEmpty(credentialsId);
	}

	@Override
	public SCM createSCM() {
		return new RoundtableSCM(new UserRemoteConfig(url, null, credentialsId));
	}

	@Extension
	public static final class DescriptorImpl extends SCMStepDescriptor {

		@Inject
		private UserRemoteConfig.DescriptorImpl delegate;

		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project, @QueryParameter String url,
				@QueryParameter String credentialsId) {
			return delegate.doFillCredentialsIdItems(project, url, credentialsId);
		}

		@RequirePOST
		public FormValidation doCheckUrl(@AncestorInPath Item item, @QueryParameter String credentialsId,
				@QueryParameter String value) throws IOException, InterruptedException {
			return delegate.doCheckUrl(item, credentialsId, value);
		}

		@Override
		public String getFunctionName() {
			return "roundtable";
		}

		@Override
		public String getDisplayName() {
			return "Roundtable SCM Step";
		}

	}

}

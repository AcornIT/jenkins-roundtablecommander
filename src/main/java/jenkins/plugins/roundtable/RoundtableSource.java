package jenkins.plugins.roundtable;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceDescriptor;

public class RoundtableSource extends SCMSource {

	private final String remote;

	@CheckForNull
	private String credentialsId;

	@DataBoundConstructor
	public RoundtableSource(String remote) {
		super(remote);
		this.remote = remote;
	}

	@DataBoundSetter
	public void setCredentialsId(@CheckForNull String credentialsId) {
		this.credentialsId = credentialsId;
	}

	public String getCredentialsId() {
		return credentialsId;
	}

	public String getRemote() {
		return remote;
	}

	@Override
	public SCM build(SCMHead head, SCMRevision revision) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void retrieve(SCMSourceCriteria arg0, SCMHeadObserver arg1, SCMHeadEvent<?> arg2, TaskListener arg3)
			throws IOException, InterruptedException {

	}

	@Symbol("roundtable")
	@Extension
	public static class DescriptorImpl extends SCMSourceDescriptor {

		@Override
		public String getDisplayName() {
			return "Roundtable SCM Source";
		}

		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String remote,
				@QueryParameter String credentialsId) {
			if (context == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)
					|| context != null && !context.hasPermission(Item.EXTENDED_READ)) {
				return new StandardListBoxModel().includeCurrentValue(credentialsId);
			}
			return new StandardListBoxModel().includeEmptyValue()
					.includeMatchingAs(
							context instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) context)
									: ACL.SYSTEM,
							context, StandardUsernameCredentials.class, URIRequirementBuilder.fromUri(remote).build(),
							null)
					.includeCurrentValue(credentialsId);
		}

		public FormValidation doCheckCredentialsId(@AncestorInPath Item context, @QueryParameter String remote,
				@QueryParameter String value) {
			if (context == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)
					|| context != null && !context.hasPermission(Item.EXTENDED_READ)) {
				return FormValidation.ok();
			}

			value = Util.fixEmptyAndTrim(value);
			if (value == null) {
				return FormValidation.ok();
			}

			remote = Util.fixEmptyAndTrim(remote);
			if (remote == null)
			// not set, can't check
			{
				return FormValidation.ok();
			}

			for (ListBoxModel.Option o : CredentialsProvider.listCredentials(StandardUsernameCredentials.class, context,
					context instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) context) : ACL.SYSTEM,
					URIRequirementBuilder.fromUri(remote).build(), null)) {
				if (StringUtils.equals(value, o.value)) {
					// TODO check if this type of credential is acceptable to the Git client or does
					// it merit warning
					// NOTE: we would need to actually lookup the credential to do the check, which
					// may require
					// fetching the actual credential instance from a remote credentials store.
					// Perhaps this is
					// not required
					return FormValidation.ok();
				}
			}
			// no credentials available, can't check
			return FormValidation.warning("Cannot find any credentials with id " + value);
		}

	}

}

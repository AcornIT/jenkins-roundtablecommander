package jenkins.plugins.roundtablecommander;

import static hudson.Util.fixEmpty;
import static hudson.Util.fixEmptyAndTrim;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.interceptor.RequirePOST;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import ro.acorn.roundtable.rtbclient.RoundtableException;
import ro.acorn.roundtable.rtbclient.UserPasswordCredentials;
import ro.acorn.roundtable.rtbclient.impl.RoundtableClient;

@ExportedBean
public class RemoteConfig extends AbstractDescribableImpl<RemoteConfig> implements Serializable {

	public static final CredentialsMatcher CREDENTIAL_MATCHER = CredentialsMatchers
			.instanceOf(StandardUsernamePasswordCredentials.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -9202593335571506891L;
	private String url;
	private String name;
	private String credentialsId;
	
	/**
	 * All the workspaces that we wish to care about building.
	 */
	private final List<WorkspaceSpec> workspaces;

	@DataBoundConstructor
	public RemoteConfig(String url, String name, @CheckForNull String credentialsId, List<WorkspaceSpec> workspaces) {
		this.url = fixEmptyAndTrim(url);
		this.name = fixEmpty(name);
		this.credentialsId = fixEmpty(credentialsId);
		this.workspaces = workspaces;
	}

	@Exported
	public String getUrl() {
		return url;
	}

	@Exported
	public String getCredentialsId() {
		return credentialsId;
	}

	@Exported
	public String getName() {
		return name;
	}
	
	@Exported
	public List<WorkspaceSpec> getWorkspaces() {
		return workspaces;
	}
	
	public String toString() {
		return getUrl() + " (" + getName() + ")";
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<RemoteConfig> {

		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project, @QueryParameter String url,
				@QueryParameter String credentialsId) {
			if (project == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)
					|| project != null && !project.hasPermission(Item.EXTENDED_READ)) {
				return new StandardListBoxModel().includeCurrentValue(credentialsId);
			}
			if (project == null) {
				/* Construct a fake project */
				project = new FreeStyleProject(Jenkins.get(), "fake-" + UUID.randomUUID().toString());
			}
			return new StandardListBoxModel().includeEmptyValue()
					.includeMatchingAs(
							project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project)
									: ACL.SYSTEM,
							project, StandardUsernameCredentials.class,
							RoundtableURIRequirementBuilder.fromUri(url).build(), CREDENTIAL_MATCHER)
					.includeCurrentValue(credentialsId);
		}

		public FormValidation doCheckCredentialsId(@AncestorInPath Item project, @QueryParameter String url,
				@QueryParameter String value) {
			if (project == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)
					|| project != null && !project.hasPermission(Item.EXTENDED_READ)) {
				return FormValidation.ok();
			}

			value = Util.fixEmptyAndTrim(value);
			if (value == null) {
				return FormValidation.ok();
			}

			url = Util.fixEmptyAndTrim(url);
			if (url == null)
			// not set, can't check
			{
				return FormValidation.ok();
			}

			if (url.indexOf('$') >= 0)
			// set by variable, can't check
			{
				return FormValidation.ok();
			}
			for (ListBoxModel.Option o : CredentialsProvider.listCredentials(StandardUsernameCredentials.class, project,
					project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
					RoundtableURIRequirementBuilder.fromUri(url).build(), CREDENTIAL_MATCHER)) {
				if (StringUtils.equals(value, o.value)) {
					return FormValidation.ok();
				}
			}
			// no credentials available, can't check
			return FormValidation.warning("Cannot find any credentials with id " + value);
		}

		@RequirePOST
		public FormValidation doCheckUrl(@AncestorInPath Item item, @QueryParameter String credentialsId,
				@QueryParameter String value) throws IOException, InterruptedException {

			if (item == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)
					|| item != null && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
				return FormValidation.ok();
			}

			String url = Util.fixEmptyAndTrim(value);
			if (url == null)
				return FormValidation.error("Repository URL is mandatory.");

			if (url.indexOf('$') >= 0)
				// set by variable, can't validate
				return FormValidation.ok();

			RoundtableClient client = RoundtableClient.instance();
			StandardCredentials credentials = lookupCredentials(item, credentialsId, url);

			// attempt to connect the provided URL
			try {
				UserPasswordCredentials rtbCredentials = null;
				
				if (credentials != null && credentials instanceof StandardUsernamePasswordCredentials) {
					StandardUsernamePasswordCredentials userPasswd = (StandardUsernamePasswordCredentials) credentials;

					rtbCredentials = new UserPasswordCredentials(userPasswd.getUsername(), userPasswd.getPassword().getPlainText());
				}
				client.checkRemote(url, rtbCredentials);
			} catch (RoundtableException e) {
				return FormValidation.error(e.getMessage());
			}

			return FormValidation.ok();
		}

		private static StandardCredentials lookupCredentials(Item project, String credentialId, String uri) {
			return (credentialId == null) ? null
					: CredentialsMatchers.firstOrNull(
							CredentialsProvider.lookupCredentials(StandardCredentials.class, project, ACL.SYSTEM,
									RoundtableURIRequirementBuilder.fromUri(uri).build()),
							CredentialsMatchers.withId(credentialId));
		}

		@Override
		public String getDisplayName() {
			return "";
		}
	}
}

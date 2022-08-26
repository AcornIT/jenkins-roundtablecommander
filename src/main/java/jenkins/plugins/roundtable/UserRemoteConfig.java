package jenkins.plugins.roundtable;

import static hudson.Util.fixEmpty;
import static hudson.Util.fixEmptyAndTrim;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import ro.acorn.roundtable.rtbclient.Roundtable;
import ro.acorn.roundtable.rtbclient.RoundtableClient;
import ro.acorn.roundtable.rtbclient.RoundtableException;

@ExportedBean
public class UserRemoteConfig extends AbstractDescribableImpl<UserRemoteConfig> implements Serializable {

	public static final CredentialsMatcher CREDENTIAL_MATCHER = CredentialsMatchers
			.instanceOf(StandardUsernamePasswordCredentials.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -9202593335571506891L;
	private String name;
	private String url;
	private String credentialsId;

	@DataBoundConstructor
	public UserRemoteConfig(String url, String name, String credentialsId) {
		this.url = fixEmptyAndTrim(url);
		this.name = fixEmpty(name);
		this.credentialsId = fixEmpty(credentialsId);
	}

	@Exported
	public String getName() {
		return name;
	}

	@Exported
	public String getUrl() {
		return url;
	}

	@Exported
	public String getCredentialsId() {
		return credentialsId;
	}

	public String toString() {
		return getUrl() + " (" + getName() + ")";
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<UserRemoteConfig> {

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

			// Normally this permission is hidden and implied by Item.CONFIGURE, so from a
			// view-only form you will not be able to use this check.
			// (TODO under certain circumstances being granted only USE_OWN might suffice,
			// though this presumes a fix of JENKINS-31870.)
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

			// get git executable on master
			File workspace;
			Jenkins jenkins = Jenkins.get();
			if (item instanceof Job) {
				workspace = ((Job) item).getBuildDir();
			} else {
				workspace = jenkins.getRootDir();
			}

			RoundtableClient client = Roundtable.in(workspace).with(RTBTaskListener.NULL).getClient();
			StandardCredentials credential = lookupCredentials(item, credentialsId, url);

			// attempt to connect the provided URL
			try {
				client.setRemoteUrl(url);

				if (credential != null && credential instanceof StandardUsernamePasswordCredentials) {
					StandardUsernamePasswordCredentials userPasswd = (StandardUsernamePasswordCredentials) credential;

					client.setCredentials(userPasswd.getUsername(), userPasswd.getPassword().getPlainText());
				}
				client.getBranches();
			} catch (RoundtableException e) {
				return FormValidation.error(e.getMessage());
			}

//			// Should not track credentials use in any checkURL method, rather should track
//			// credentials use at the point where the credential is used to perform an
//			// action (like poll the repository, clone the repository, publish a change
//			// to the repository).
//
//			// attempt to connect the provided URL
//			try {
//				git.getHeadRev(url, "HEAD");
//			} catch (Exception e) {
//				
//			}

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

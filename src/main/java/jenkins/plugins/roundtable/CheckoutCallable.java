package jenkins.plugins.roundtable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jenkinsci.remoting.RoleChecker;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.scm.SCMRevisionState;
import hudson.security.ACL;
import ro.acorn.roundtable.rtbclient.Roundtable;
import ro.acorn.roundtable.rtbclient.RoundtableClient;
import ro.acorn.roundtable.rtbclient.RoundtableException;

public class CheckoutCallable implements FileCallable<Void> {

	private static final long serialVersionUID = 1L;

	private final Run<?, ?> build;
	private final Launcher launcher;
	private final TaskListener listener;
	private final File changelogFile;
	private final SCMRevisionState baseline;
	private final RoundtableSCM roundtableSCM;

	public CheckoutCallable(Run<?, ?> build, Launcher launcher, TaskListener listener, File changelogFile,
			SCMRevisionState baseline, RoundtableSCM roundtableSCM) {
		this.build = build;
		this.launcher = launcher;
		this.listener = listener;
		this.changelogFile = changelogFile;
		this.baseline = baseline;
		this.roundtableSCM = roundtableSCM;
	}

	@Override
	public void checkRoles(RoleChecker checker) throws SecurityException {

	}

	@Override
	public Void invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {

		try {
			RoundtableClient client = Roundtable.in(workspace).getClient();
			Job project = build.getParent();
			
			for (UserRemoteConfig remote : roundtableSCM.getUserRemoteConfigs()) {

				client.addRemoteUrl(remote.getName(), remote.getUrl());
				String credentialsId = remote.getCredentialsId();

				if (credentialsId != null) {
					List<StandardUsernamePasswordCredentials> urlCredentials = CredentialsProvider
							.lookupCredentials(StandardUsernamePasswordCredentials.class, project,
									project instanceof Queue.Task
											? ((Queue.Task) project).getDefaultAuthentication()
											: ACL.SYSTEM,
									URIRequirementBuilder.fromUri(remote.getUrl()).build());
					CredentialsMatcher ucMatcher = CredentialsMatchers.withId(credentialsId);
					CredentialsMatcher idMatcher = CredentialsMatchers.allOf(ucMatcher,
							RoundtableSCM.CREDENTIALS_MATCHER);
					StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(urlCredentials,
							idMatcher);
					if (credentials != null)
						client.setCredentials(remote.getName(), credentials.getUsername(),
								credentials.getPassword().getPlainText());
				}
				
				client.checkout().execute();
			}

		} catch (RoundtableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

}

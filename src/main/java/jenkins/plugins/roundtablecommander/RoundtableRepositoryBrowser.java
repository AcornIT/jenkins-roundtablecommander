package jenkins.plugins.roundtablecommander;

import java.io.IOException;
import java.net.URL;

import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.scm.ChangeLogSet.Entry;

public class RoundtableRepositoryBrowser extends RepositoryBrowser<ChangeLogSet.Entry> {

	private static final long serialVersionUID = -2222383136396264274L;
	private final RoundtableCommanderSCM scm;

	public RoundtableRepositoryBrowser(RoundtableCommanderSCM scm) {
		this.scm = scm;
	}

	@Override
	public URL getChangeSetLink(Entry changeSet) throws IOException {
		return null;
	}

}

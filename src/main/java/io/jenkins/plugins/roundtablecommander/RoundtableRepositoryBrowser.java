package io.jenkins.plugins.roundtablecommander;

import java.io.IOException;
import java.net.URL;

import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.scm.ChangeLogSet.Entry;

public class RoundtableRepositoryBrowser extends RepositoryBrowser<ChangeLogSet.Entry> {

	private static final long serialVersionUID = -2222383136396264274L;

	public RoundtableRepositoryBrowser(RoundtableCommanderSCM scm) {
	}

	@Override
	public URL getChangeSetLink(Entry changeSet) throws IOException {
		return null;
	}

}

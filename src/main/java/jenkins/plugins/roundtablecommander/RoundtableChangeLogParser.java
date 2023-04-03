package jenkins.plugins.roundtablecommander;

import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXException;

import hudson.model.Run;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.scm.ChangeLogSet.Entry;

public class RoundtableChangeLogParser extends ChangeLogParser {

	public RoundtableChangeLogParser() {
	}

	@Override
	public ChangeLogSet<? extends Entry> parse(@SuppressWarnings("rawtypes") Run build, RepositoryBrowser<?> browser, File changelogFile)
			throws IOException, SAXException {

		return new RoundtableChangeLogSet(build, browser, changelogFile);
	}

}

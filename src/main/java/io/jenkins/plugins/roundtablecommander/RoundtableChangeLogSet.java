package io.jenkins.plugins.roundtablecommander;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.stapler.export.Exported;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;

public class RoundtableChangeLogSet extends ChangeLogSet<RoundtableChangeLogEntry> {

	private final List<RoundtableChangeLogEntry> entries;

	public RoundtableChangeLogSet(Run<?, ?> run, RepositoryBrowser<?> browser, File changelogFile) throws IOException {
		super(run, browser);
		this.entries = parse(changelogFile);
	}

	@Override
	public Iterator<RoundtableChangeLogEntry> iterator() {
		return Collections.unmodifiableList(entries).iterator();
	}

	@Override
	public boolean isEmptySet() {
		return entries.isEmpty();
	}

	@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Low risk")
	public List<RoundtableChangeLogEntry> getEntries() {
		return entries;
	}

	@Exported
	public String getKind() {
		return "roundtable";
	}

	private List<RoundtableChangeLogEntry> parse(File changelogFile) throws IOException {
		if (changelogFile != null && changelogFile.exists() && changelogFile.length() > 0) {
			ObjectMapper mapper = new ObjectMapper();

			return mapper.readValue(changelogFile, new TypeReference<List<RoundtableChangeLogEntry>>() {
			});
		}

		return Collections.emptyList();

	}
}

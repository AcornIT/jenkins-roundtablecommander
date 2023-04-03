package jenkins.plugins.roundtablecommander;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import hudson.scm.ChangeLogSet.AffectedFile;
import ro.acorn.roundtable.model.ICommit;
import ro.acorn.roundtable.model.ICommitEntry;
import ro.acorn.roundtable.model.remote.RTBAction;

public class RoundtableChangeLogEntry extends ChangeLogSet.Entry implements ICommit {
	public String comment;
	public int revision;
	public Date date;
	public String user;
	public Collection<Entry> entries;
	private Collection<Path> paths;

	public RoundtableChangeLogEntry() {
	}

	@Override
	public String getMsg() {
		return comment;
	}

	@Override
	public User getAuthor() {
		return User.getOrCreateByIdOrFullName(user);
	}

	@Override
	@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Low risk")
	public Collection<Path> getAffectedFiles() {
		if (this.paths == null) {
			this.paths = new ArrayList<>();

			if (this.entries != null) {
				this.entries.forEach(e -> {
					e.files.forEach(f -> this.paths.add(new Path(f, e)));
				});
			}
		}
		return this.paths;
	}

	@Override
	public Collection<String> getAffectedPaths() {
		return this.paths.stream().map(Path::getPath).collect(Collectors.toList());
	}

	@Override
	public int getRevision() {
		return revision;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public String getUser() {
		return user;
	}

	@Override
	public Collection<ICommitEntry> getEntries() {
		return Collections.unmodifiableCollection(entries);
	}

	@Override
	public ICommitEntry addEntry(String versionId, RTBAction action, String notes) {
		return null;
	}

	@Override
	public ICommitEntry addEntry(String versionId, int action, String notes) {
		return null;
	}

	public static class Entry implements ICommitEntry {

		public String version;
		public int revision;
		public RTBAction action;
		public String notes;
		public Collection<String> files;

		@Override
		public int getRevision() {
			return revision;
		}

		@Override
		public String getVersion() {
			return version;
		}

		@Override
		public RTBAction getAction() {
			return action;
		}

		@Override
		public String getNotes() {
			return notes;
		}

		@Override
		public Collection<String> getFiles() {
			return files;
		}

		@Override
		public void addFile(String file) {
		}

	}

	@ExportedBean(defaultVisibility = 999)
	public static class Path implements AffectedFile {

		private String path;
		private Entry changeSet;

		private Path(String filePath, Entry changeSet) {
			this.path = filePath;
			this.changeSet = changeSet;
		}

		@Exported(name = "file")
		public String getPath() {
			return path;
		}

		@Exported
		public EditType getEditType() {
			switch (changeSet.action) {
			case ADD:
			case ASSIGN:
				return EditType.ADD;
			case DELETE:
				return EditType.DELETE;
			default:
				return EditType.EDIT;
			}
		}
	}
}

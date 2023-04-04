package io.jenkins.plugins.roundtablecommander;

import hudson.model.TaskListener;
import ro.acorn.roundtable.logging.RoundtableLogger;

public class RTBTaskListener implements RoundtableLogger {

	private final TaskListener listener;
	private Level level = Level.INFO;

	public RTBTaskListener(TaskListener listener) {
		this.listener = listener;
	}

	@Override
	public Level getLevel() {
		return level;
	}

	@Override
	public void setLevel(Level level) {
		this.level = level;
	}

	@Override
	public void doLog(Level level, String msg, Throwable thrown) {
		switch (level) {
		case WARNING:
			listener.error(msg);
			break;
		case ERROR:
			listener.fatalError(msg);
			break;
		default:
			listener.getLogger().println(msg);
			break;
		}
	}

}

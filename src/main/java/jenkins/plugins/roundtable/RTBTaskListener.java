package jenkins.plugins.roundtable;

import hudson.model.TaskListener;
import ro.acorn.roundtable.rtbclient.RoundtableTaskListener;

public class RTBTaskListener implements RoundtableTaskListener {

	private final TaskListener listener;
	
	public RTBTaskListener(TaskListener listener) {
		this.listener = listener;
	}

	@Override
	public void info(String msg) {
		listener.getLogger().println(msg);
	}

	@Override
	public void info(String format, Object... args) {
		info(String.format(format, args));
	}

	@Override
	public void error(String msg) {
		info("[error] " + msg);
	}

	@Override
	public void error(String format, Object... args) {
		error(String.format(format, args));
	}

	@Override
	public void fatalError(String msg) {
		error(msg);
	}

	@Override
	public void fatalError(String format, Object... args) {
		error(format, args);
	}

}

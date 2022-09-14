package jenkins.plugins.roundtable;

import java.util.List;
import java.util.Map;

import hudson.model.Run;
import hudson.scm.AbstractScmTagAction;
import hudson.util.CopyOnWriteMap;

public class RTBTagAction extends AbstractScmTagAction {
	
    private final Map<String, List<String>> tags = new CopyOnWriteMap.Tree<>();



	protected RTBTagAction(Run<?, ?> run) {
		super(run);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getDisplayName() {
        int nonNullTag = 0;
        for (List<String> v : tags.values()) {
            if (!v.isEmpty()) {
                nonNullTag += v.size();
                if (nonNullTag > 1)
                    break;
            }
        }
        if (nonNullTag == 0)
            return "No Tags";
        if (nonNullTag == 1)
            return "One tag";
        else
            return "Multiple tags";
	}

	@Override
	public boolean isTagged() {
        for (List<String> t : tags.values()) {
            if (!t.isEmpty()) return true;
        }
        return false;
	}

	@Override
	public String getIconFileName() {
	       if (!isTagged() && !getACL().hasPermission(getPermission()))
	            return null;
	       return "save.gif";
	}

}

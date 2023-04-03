package jenkins.plugins.roundtablecommander;

import java.io.Serializable;

import org.kohsuke.stapler.export.ExportedBean;

import hudson.model.Action;

@ExportedBean(defaultVisibility = 999)
public class BuildData implements Action, Serializable {

	private static final long serialVersionUID = 7120461448340342233L;

	@Override
	public String getIconFileName() {
        return jenkins.model.Jenkins.RESOURCE_PATH+"/plugin/roundtablecommander/icons/rtb-commander-icon.png";
	}

	@Override
	public String getDisplayName() {
		return "Roundtable Commander Build Data";
	}

	@Override
	public String getUrlName() {
		// TODO Auto-generated method stub
		return null;
	}

}

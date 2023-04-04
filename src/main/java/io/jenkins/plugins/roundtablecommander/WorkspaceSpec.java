package io.jenkins.plugins.roundtablecommander;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

/**
 * A specification of workspaces to build.
 *
 */
@ExportedBean
public class WorkspaceSpec extends AbstractDescribableImpl<WorkspaceSpec> implements Serializable {
	private static final long serialVersionUID = -6177158367915899356L;

	private String name;
	private int shallow;

	@Exported
	@Whitelisted
	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException();
		else if (name.length() == 0)
			this.name = "*";
		else
			this.name = name.trim();
	}

	@Exported
	@Whitelisted
	public int getShallow() {
		return shallow;
	}

	public void setShallow(int shallow) {
		this.shallow = shallow;
	}

	@DataBoundConstructor
	public WorkspaceSpec(String name) {
		setName(name);
	}

	public String toString() {
		return name;
	}

	public boolean matches(String item) {
		EnvVars env = new EnvVars();
		return matches(item, env);
	}

	/**
	 * Compare a workspace reference to configured pattern.
	 * 
	 * @param ref workspace reference to compare
	 * @param env environment variables to use in comparison
	 * @return true if workspace matches configured pattern
	 */
	public boolean matches(String ref, EnvVars env) {
		return getPattern(env).matcher(ref).matches();
	}

	public List<String> filterMatching(Collection<String> workspaces, EnvVars env) {
		List<String> items = new ArrayList<>();

		for (String b : workspaces) {
			if (matches(b, env))
				items.add(b);
		}

		return items;
	}

	private String getExpandedName(EnvVars env) {
		String expandedName = env.expand(name);
		if (expandedName.length() == 0) {
			return "*";
		}
		return expandedName;
	}

	private Pattern getPattern(EnvVars env) {
		return getPattern(env, null);
	}

	private Pattern getPattern(EnvVars env, String repositoryName) {
		String expandedName = getExpandedName(env);
		// use regex syntax directly if name starts with colon
		if (expandedName.startsWith(":") && expandedName.length() > 1) {
			String regexSubstring = expandedName.substring(1, expandedName.length());
			return Pattern.compile(regexSubstring);
		}

		return Pattern.compile(convertWildcardStringToRegex(expandedName));
	}

	private String convertWildcardStringToRegex(String expandedName) {
		StringBuilder builder = new StringBuilder();

		// was the last token a wildcard?
		boolean foundWildcard = false;

		// split the string at the wildcards
		StringTokenizer tokenizer = new StringTokenizer(expandedName, "*", true);
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();

			// is this token is a wildcard?
			if (token.equals("*")) {
				// yes, was the previous token a wildcard?
				if (foundWildcard) {
					// yes, we found "**"
					// match over any number of characters
					builder.append(".*");
					foundWildcard = false;
				} else {
					// no, set foundWildcard to true and go on
					foundWildcard = true;
				}
			} else {
				// no, was the previous token a wildcard?
				if (foundWildcard) {
					// yes, we found "*" followed by a non-wildcard
					// match any number of characters other than a "/"
					builder.append("[^/]*");
					foundWildcard = false;
				}
				// quote the non-wildcard token before adding it to the phrase
				builder.append(Pattern.quote(token));
			}
		}

		// if the string ended with a wildcard add it now
		if (foundWildcard) {
			builder.append("[^/]*");
		}
		return builder.toString();
	}

	@Extension
	public static class DescriptorImpl extends Descriptor<WorkspaceSpec> {
		@Override
		public String getDisplayName() {
			return "Workspace Spec";
		}
	}
}

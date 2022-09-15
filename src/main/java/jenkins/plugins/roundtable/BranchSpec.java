package jenkins.plugins.roundtable;

import java.io.Serializable;
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
 * A specification of branches to build.
 *
 * eg:
 * 
 * <pre>
 * master
 * origin/master
 * </pre>
 */
@ExportedBean
public class BranchSpec extends AbstractDescribableImpl<BranchSpec> implements Serializable {
	private static final long serialVersionUID = -6177158367915899356L;

	private String name;

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

	@DataBoundConstructor
	public BranchSpec(String name) {
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
	 * Compare a branch reference to configured pattern.
	 * <p>
	 * branch uses normalized format `(remotes|tags)/xx` pattern do support
	 * <ul>
	 * <li>(remote-name)?/branch</li>
	 * <li>remotes/branch</li>
	 * <li>tag</li>
	 * </ul>
	 * 
	 * @param ref branch reference to compare
	 * @param env environment variables to use in comparison
	 * @return true if branch matches configured pattern
	 */
	public boolean matches(String ref, EnvVars env) {
		return getPattern(env).matcher(ref).matches();
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

		// build a pattern into this builder
		StringBuilder builder = new StringBuilder("(");

		// if an unqualified branch was given, consider all remotes (with various
		// possible syntaxes)
		// so it will match branches from any remote repositories as the user probably
		// intended
		if (!expandedName.contains("**") && !expandedName.contains("/")) {
			builder.append("|remotes/[^/]+/|[^/]+/");
		} else {
			builder.append("|remotes/");
		}
		builder.append(")?");
		builder.append(convertWildcardStringToRegex(expandedName));
		return Pattern.compile(builder.toString());
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
	public static class DescriptorImpl extends Descriptor<BranchSpec> {
		@Override
		public String getDisplayName() {
			return "Branch Spec";
		}
	}
}

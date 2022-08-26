package jenkins.plugins.roundtable;

import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnamePortRequirement;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import com.cloudbees.plugins.credentials.domains.PathRequirement;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class RoundtableURIRequirementBuilder {

	/**
	 * Part of a pattern which matches the scheme part (appsrv, http, ...) of an
	 * URI. Defines one capturing group containing the scheme without the trailing
	 * colon and slashes
	 */
	private static final String SCHEME_P = "([a-z][a-z0-9+-]+)://"; //$NON-NLS-1$

	/**
	 * Part of a pattern which matches the host part of URIs. Defines one capturing
	 * group containing the host name.
	 */
	private static final String HOST_P = "((?:[^\\\\/:]+)|(?:\\[[0-9a-f:]+\\]))";

	/**
	 * Part of a pattern which matches the optional port part of URIs. Defines one
	 * capturing group containing the port without the preceding colon.
	 */
	private static final String OPT_PORT_P = "(?::(\\d+))?"; //$NON-NLS-1$

	/**
	 * Part of a pattern which matches a application server name. Relative paths
	 * don't start with slash or drive letters. Defines no capturing group.
	 */
	private static final String OPT_NAME_P = "(?:(?:[^\\\\/]+[\\\\/]+)*[^\\\\/]+[\\\\/]*)"; //$NON-NLS-1$

	/**
	 * A pattern matching standard URI: </br>
	 * <code>scheme "://" user_password? hostname? portnumber? path</code>
	 */
	private static final Pattern FULL_URI = Pattern.compile("^" // //$NON-NLS-1$
			+ SCHEME_P //
			+ "(?:" // start a group containing hostname and all options only //$NON-NLS-1$
			// availabe when a hostname is there
			+ HOST_P //
			+ OPT_PORT_P //
			+ "([\\\\/]?" + OPT_NAME_P + ")?" // optional appsrv name/path
			+ ")?" // close the optional group containing hostname //$NON-NLS-1$
			+ "(.+)?" // //$NON-NLS-1$
			+ "$"); //$NON-NLS-1$

	/**
	 * The list of requirements.
	 */
	@Nonnull
	private final List<DomainRequirement> requirements;

	/**
	 * Private constructor.
	 *
	 * @param requirements the list of requirements.
	 */
	private RoundtableURIRequirementBuilder(@Nonnull List<DomainRequirement> requirements) {
		this.requirements = new ArrayList<>(requirements);
	}

	/**
	 * Creates an empty builder.
	 *
	 * @return a new empty builder.
	 */
	@Nonnull
	public static RoundtableURIRequirementBuilder create() {
		return new RoundtableURIRequirementBuilder(Collections.<DomainRequirement>emptyList());
	}

	/**
	 * Creates a new builder with the same requirements as this builder.
	 *
	 * @return a new builder with the same requirements as this builder.
	 */
	@Nonnull
	public RoundtableURIRequirementBuilder duplicate() {
		return new RoundtableURIRequirementBuilder(requirements);
	}

	/**
	 * Creates a new builder using the supplied URI.
	 *
	 * @param uri the URI to create the requirements of.
	 * @return a new builder with the requirements of the supplied URI.
	 */
	@Nonnull
	public static RoundtableURIRequirementBuilder fromUri(@CheckForNull String uri) {
		return create().withUri(uri);
	}

	/**
	 * Replaces the requirements with those of the supplied URI.
	 *
	 * @param uri the URI.
	 * @return {@code this}.
	 */
	@Nonnull
	public RoundtableURIRequirementBuilder withUri(@CheckForNull String uri) {
		if (uri != null) {
			Matcher matcher = FULL_URI.matcher(uri);
			if (matcher.matches()) {
				withScheme(matcher.group(1));
				if (matcher.group(3) != null) {
					withHostnamePort(matcher.group(2), Integer.parseInt(matcher.group(3)));
				} else {
					withHostname(matcher.group(2)).withoutHostnamePort();
				}
				
				if (matcher.group(4) != null) 
					withPath(matcher.group(4));
				
				return this;
			}
		}
		return withoutScheme().withoutPath().withoutHostname().withoutHostnamePort();
	}

	/**
	 * Removes any scheme requirements.
	 *
	 * @return {@code this}.
	 */
	@Nonnull
	public RoundtableURIRequirementBuilder withoutScheme() {
		for (Iterator<DomainRequirement> iterator = requirements.iterator(); iterator.hasNext();) {
			DomainRequirement r = iterator.next();
			if (r instanceof SchemeRequirement) {
				iterator.remove();
			}
		}
		return this;
	}

	/**
	 * Removes any path requirements.
	 *
	 * @return {@code this}.
	 */
	@Nonnull
	public RoundtableURIRequirementBuilder withoutPath() {
		for (Iterator<DomainRequirement> iterator = requirements.iterator(); iterator.hasNext();) {
			DomainRequirement r = iterator.next();
			if (r instanceof PathRequirement) {
				iterator.remove();
			}
		}
		return this;
	}

	/**
	 * Removes any hostname or hostname:port requirements.
	 *
	 * @return {@code this}.
	 */
	@Nonnull
	public RoundtableURIRequirementBuilder withoutHostname() {
		for (Iterator<DomainRequirement> iterator = requirements.iterator(); iterator.hasNext();) {
			DomainRequirement r = iterator.next();
			if (r instanceof HostnameRequirement) {
				iterator.remove();
			}
		}
		return this;
	}

	/**
	 * Removes any hostname:port requirements.
	 *
	 * @return {@code this}.
	 */
	@Nonnull
	public RoundtableURIRequirementBuilder withoutHostnamePort() {
		for (Iterator<DomainRequirement> iterator = requirements.iterator(); iterator.hasNext();) {
			DomainRequirement r = iterator.next();
			if (r instanceof HostnamePortRequirement) {
				iterator.remove();
			}
		}
		return this;
	}

	/**
	 * Replace any scheme requirements with the supplied scheme.
	 *
	 * @param scheme the scheme to use as a requirement
	 * @return {@code this}.
	 */
	@Nonnull
	public RoundtableURIRequirementBuilder withScheme(@CheckForNull String scheme) {
		withoutScheme();
		if (scheme != null) {
			requirements.add(new SchemeRequirement(scheme));
		}
		return this;
	}

	/**
	 * Replace any path requirements with the supplied path.
	 *
	 * @param path to use as a requirement
	 * @return {@code this}.
	 */
	@Nonnull
	public RoundtableURIRequirementBuilder withPath(@CheckForNull String path) {
		withoutPath();
		if (path != null) {
			requirements.add(new PathRequirement(path));
		}
		return this;
	}

	/**
	 * Replace any hostname requirements with the supplied hostname.
	 *
	 * @param hostname the hostname to use as a requirement
	 * @return {@code this}.
	 */
	@Nonnull
	public RoundtableURIRequirementBuilder withHostname(@CheckForNull String hostname) {
		return withHostnamePort(hostname, -1);
	}

	/**
	 * Replace any hostname or hostname:port requirements with the supplied hostname
	 * and port.
	 *
	 * @param hostname the hostname to use as a requirement or (@code null} to not
	 *                 add any requirement
	 * @param port     the port or {@code -1} to not add
	 *                 {@link com.cloudbees.plugins.credentials.domains.HostnamePortRequirement}s
	 * @return {@code this}.
	 */
	@Nonnull
	public RoundtableURIRequirementBuilder withHostnamePort(@CheckForNull String hostname, int port) {
		withoutHostname();
		withoutHostnamePort();
		if (hostname != null) {
			requirements.add(new HostnameRequirement(hostname));
			if (port != -1) {
				requirements.add(new HostnamePortRequirement(hostname, port));
			}
		}
		return this;
	}

	/**
	 * Builds the list of requirements.
	 *
	 * @return the list of requirements.
	 */
	@Nonnull
	public List<DomainRequirement> build() {
		return new ArrayList<>(requirements);
	}

}

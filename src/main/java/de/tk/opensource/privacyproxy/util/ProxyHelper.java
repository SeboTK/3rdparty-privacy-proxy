/*--- (C) 1999-2019 Techniker Krankenkasse ---*/

package de.tk.opensource.privacyproxy.util;

import java.net.Proxy;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProxyHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHelper.class);

	@Autowired
	private Proxy proxy;

	@Value("${http.nonProxyHosts:''}")
	private String nonProxyHosts;

	public ProxyHelper() {
	}

	// Package protected constructor for unit tests
	ProxyHelper(Proxy proxy, String nonProxyHosts) {
		this.proxy = proxy;
		this.nonProxyHosts = nonProxyHosts;
	}

	/**
	 * Evaluates a list of hosts that should be reached directly, bypassing the proxy. This list is
	 * configured using the system property 'http.nonProxyHosts'. The list of patterns is separated
	 * by '|'. Any host matching one of these patterns will be reached through a direct connection
	 * instead of through a proxy.
	 *
	 * @param   url
	 *
	 * @return  the configured {@link Proxy} instance or {@link Proxy#NO_PROXY} when the hostname is
	 *          excluded.
	 */
	public Proxy selectProxy(URL url) {

		// Skip evaluation if no proxy is configured at all
		if (Proxy.NO_PROXY.equals(proxy) || StringUtils.isEmpty(nonProxyHosts)) {
			return proxy;
		}

		// The list of excluded host patterns is separated by '|'.
		Stream<String> excluded = Pattern.compile(Pattern.quote("|")).splitAsStream(nonProxyHosts);

		String hostname = url.getHost();
		boolean isExcluded = excluded.anyMatch(pattern -> matches(hostname, pattern.trim()));

		Proxy selection = isExcluded ? Proxy.NO_PROXY : proxy;
		LOGGER.debug("Using {} for {} ({})", selection, hostname, nonProxyHosts);
		return selection;
	}

	/**
	 * Evaluates if a host should be reached directly, bypassing the proxy. The pattern may start or
	 * end with a '*' for wildcards.
	 *
	 * @param   hostname
	 * @param   pattern
	 *
	 * @return  true if hostname matches the given pattern.
	 */
	public static boolean matches(String hostname, String pattern) {

		if (pattern.isEmpty()) {
			return false;
		}

		if (pattern.equals("*")) {
			return true;
		}

		if (pattern.startsWith("*")) {

			if (pattern.endsWith("*") && pattern.length() > 1) {
				return hostname.contains(pattern.substring(1, pattern.length() - 1));
			}

			return hostname.endsWith(pattern.substring(1));
		}

		if (pattern.endsWith("*")) {
			return hostname.startsWith(pattern.substring(0, pattern.length() - 1));
		}

		return pattern.equals(hostname);
	}
}

/*--- Formatiert nach TK Code Konventionen vom 05.03.2002 ---*/

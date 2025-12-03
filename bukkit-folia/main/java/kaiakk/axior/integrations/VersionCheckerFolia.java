package kaiakk.axior.integrations;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionCheckerFolia {
	private static final Pattern VERSION_PATTERN = Pattern.compile("\\b(\\d+(?:\\.\\d+)+)\\b");
	private static final String MODRINTH_VERSIONS_URL = "https://modrinth.com/plugin/axior/versions";

	public static void check(JavaPlugin plugin, String currentVersion) {
		if (plugin == null) return;
		try {
			Bukkit.getAsyncScheduler().runNow(plugin, task -> {
				try {
					String latest = fetchLatestVersion();
					if (latest == null) {
						plugin.getLogger().fine("Version check: couldn't find any version on Modrinth page.");
						return;
					}
					if (isNewer(latest, currentVersion)) {
						plugin.getLogger().info("A new Axior version is available: " + latest + " (you have " + currentVersion + "). See: " + MODRINTH_VERSIONS_URL);
					} else {
						plugin.getLogger().fine("Axior is up to date (" + currentVersion + ").");
					}
				} catch (IOException e) {
					plugin.getLogger().severe("Version check failed: " + e.getMessage());
				} catch (Throwable t) {
					plugin.getLogger().log(java.util.logging.Level.SEVERE, "Version check failed", t);
				}
			});
		} catch (Throwable t) {
			plugin.getLogger().log(java.util.logging.Level.SEVERE, "VersionCheckerFolia failed to schedule async task", t);
		}
	}

	private static String fetchLatestVersion() throws Exception {
		HttpURLConnection conn = null;
		try {
			URL url = new URL(MODRINTH_VERSIONS_URL);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);

			int code = conn.getResponseCode();
			java.io.InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
			StringBuilder body = new StringBuilder();
			if (stream != null) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
					String line;
					while ((line = in.readLine()) != null) {
						body.append(line).append('\n');
					}
				}
			}
			if (code < 200 || code >= 300) {
				String bodyStr = body.toString().trim();
				throw new IOException("Modrinth responded with HTTP " + code + ": " + truncate(bodyStr, 500));
			}

			String content = body.toString();
			String line;
			List<String> versions = new ArrayList<>();
			try (BufferedReader in = new BufferedReader(new java.io.StringReader(content))) {
				while ((line = in.readLine()) != null) {
					Matcher m = VERSION_PATTERN.matcher(line);
					while (m.find()) {
						String v = m.group(1);
						if (v.chars().filter(ch -> ch == '.').count() >= 2) {
							versions.add(v);
						}
					}
				}
			}
			if (versions.isEmpty()) return null;
			Collections.sort(versions, VersionCheckerFolia::compareVersions);
			return versions.get(versions.size() - 1);
		} finally {
			if (conn != null) conn.disconnect();
		}
	}

	private static String truncate(String s, int max) {
		if (s == null) return "";
		if (s.length() <= max) return s;
		return s.substring(0, max) + "... (truncated " + (s.length() - max) + " chars)";
	}

	private static int compareVersions(String a, String b) {
		String[] pa = a.split("\\.");
		String[] pb = b.split("\\.");
		int n = Math.max(pa.length, pb.length);
		for (int i = 0; i < n; i++) {
			int ia = i < pa.length ? parseIntSafe(pa[i]) : 0;
			int ib = i < pb.length ? parseIntSafe(pb[i]) : 0;
			if (ia != ib) return Integer.compare(ia, ib);
		}
		return 0;
	}

	private static int parseIntSafe(String s) {
		try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
	}

	private static boolean isNewer(String latest, String current) {
		if (current == null || current.isEmpty()) return true;
		return compareVersions(latest, current) > 0;
	}
}

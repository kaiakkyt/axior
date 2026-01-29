package kaiakk.axior.integrations;

import org.bukkit.plugin.java.JavaPlugin;
import kaiakk.multimedia.classes.ConsoleLog;
import kaiakk.multimedia.classes.SchedulerHelper;

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

public class VersionCheckerBukkit {
	private static final Pattern VERSION_PATTERN = Pattern.compile("\\b(\\d+(?:\\.\\d+)+)\\b");
	private static final String MODRINTH_VERSIONS_URL = "https://api.modrinth.com/v2/project/axior/version";
	private static final Pattern VERSION_NUM_PATTERN = Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");
	private static final Pattern VERSION_FORMAT_PATTERN = Pattern.compile("^[vV]?(\\d+)\\.(\\d{2})\\.(\\d{2})(?:[.-].*)?$");

	public static void check(JavaPlugin plugin, String currentVersion) {
		if (plugin == null) return;
		SchedulerHelper.runAsync(plugin, () -> {
			try {
				String latest = fetchLatestVersion();
				if (latest == null) {
					ConsoleLog.info("Version check: couldn't find any version on Modrinth page.");
					return;
				}
				if (isNewer(latest, currentVersion)) {
					ConsoleLog.info("A new Axior version is available: " + latest + " (you have " + currentVersion + "). See: " + MODRINTH_VERSIONS_URL);
				} else {
					ConsoleLog.info("Axior is up to date (" + currentVersion + ").");
				}
				} catch (IOException e) {
					ConsoleLog.error("Version check failed: " + e.getMessage());
				} catch (Throwable t) {
					ConsoleLog.error("Version check failed", t);
				}
		});
	}

	private static String fetchLatestVersion() throws Exception {
		HttpURLConnection conn = null;
		try {
			URL url = new URL(MODRINTH_VERSIONS_URL);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("User-Agent", "Axior-VersionChecker/1.0 (+https://modrinth.com/plugin/axior)");

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
				throw new IOException("Modrinth API responded with HTTP " + code + ": " + truncate(bodyStr, 500));
			}

			String content = body.toString();
			List<String> versions = new ArrayList<>();

			Matcher m = VERSION_NUM_PATTERN.matcher(content);
			while (m.find()) {
				String v = m.group(1);
				if (v == null) continue;
				v = v.trim();
				Matcher fmt = VERSION_FORMAT_PATTERN.matcher(v);
				if (fmt.matches()) {
					if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
					versions.add(v);
				}
			}

			if (versions.isEmpty()) return null;
			Collections.sort(versions, VersionCheckerBukkit::compareVersions);
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

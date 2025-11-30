package kaiakk.axior.integrations;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionCheckerBukkit {
	private static final Pattern VERSION_PATTERN = Pattern.compile("\\b(\\d+(?:\\.\\d+)+)\\b");
	private static final String MODRINTH_VERSIONS_URL = "https://modrinth.com/plugin/axior/versions";

	public static void check(JavaPlugin plugin, String currentVersion) {
		if (plugin == null) return;
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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
			} catch (Throwable t) {
				plugin.getLogger().warning("Version check failed: " + t.getMessage());
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

			try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				String line;
				List<String> versions = new ArrayList<>();
				while ((line = in.readLine()) != null) {
					Matcher m = VERSION_PATTERN.matcher(line);
					while (m.find()) {
						String v = m.group(1);
						if (v.chars().filter(ch -> ch == '.').count() >= 2) {
							versions.add(v);
						}
					}
				}
				if (versions.isEmpty()) return null;
				Collections.sort(versions, VersionCheckerBukkit::compareVersions);
				return versions.get(versions.size() - 1);
			}
		} finally {
			if (conn != null) conn.disconnect();
		}
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

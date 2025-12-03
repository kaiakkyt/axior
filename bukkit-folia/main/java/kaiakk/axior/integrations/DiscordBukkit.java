package kaiakk.axior.integrations;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.Instant;

public class DiscordBukkit {
	public static void sendReportAsync(JavaPlugin plugin, String webhookUrl, String reported, String reporter, String reason, long timestamp) {
		if (plugin == null) return;
		if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("PASTE_YOUR_WEBHOOK_URL_HERE")) {
			plugin.getLogger().fine("Discord webhook not configured; skipping webhook send.");
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			HttpURLConnection conn = null;

			try {
				URL url = new URL(webhookUrl);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

				conn.setRequestProperty("User-Agent", "Axior");

				conn.setConnectTimeout(10000);
				conn.setReadTimeout(10000);

				String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
				String isoTime = Instant.ofEpochMilli(timestamp).toString();

				String title = "Player Report";
				int color = 16724740;

				if (reason != null) {
					String low = reason.trim().toLowerCase();
					if (low.startsWith("server health") || (reported != null && reported.equalsIgnoreCase("server"))) {
						title = "Server Health Report";
						color = 3066993;
					} else if (low.startsWith("warn") || low.startsWith("warning") || low.startsWith("warned")) {
						title = "Player Warned";
						color = 16763904;
					} else if (low.startsWith("banned")) {
						title = "Player Banned";
						color = 15158332;
					} else if (low.startsWith("pardoned") || low.startsWith("pardon")) {
						title = "Ban Pardoned";
						color = 3066993;
					} else if (low.startsWith("muted")) {
						title = "Player Muted";
						color = 15105570;
					} else if (low.startsWith("unmuted") || low.startsWith("un-mute") || low.startsWith("unmute")) {
						title = "Player Unmuted";
						color = 3066993;
					}
				}

				String playerVal = jsonEscape(escapeForMarkdown(reported));
				String reporterVal = jsonEscape(escapeForMarkdown(reporter));

				String reasonVal;
				{
					String reasonRaw = reason == null ? "" : reason;
					String reasonForJson;
					if ("Server Health Report".equals(title) || reasonRaw.trim().toLowerCase().startsWith("server health")) {
						String sanitized = reasonRaw.replaceAll("(?i)\\(?uptime[:=]?\\s*[^)\\n,]*\\)?", "").trim();
						sanitized = sanitized.replaceAll("\\s{2,}", " ").replaceAll("^[,\\s]+|[,\\s]+$", "");
						reasonForJson = escapeForMarkdown(sanitized);
					} else {
						reasonForJson = escapeForMarkdown(reasonRaw);
					}
					reasonVal = jsonEscape(reasonForJson);
				}

				String footerText = jsonEscape("Sent by Axior at " + (time));

				StringBuilder jsonBuilder = new StringBuilder();
				jsonBuilder.append('{');
				jsonBuilder.append("\"embeds\":[");
				jsonBuilder.append('{');
				jsonBuilder.append("\"title\":\"").append(jsonEscape(title)).append("\",");
				jsonBuilder.append("\"color\":").append(color).append(',');

				if ("Server Health Report".equals(title)) {
					jsonBuilder.append("\"fields\":[");
					jsonBuilder.append('{').append("\"name\":\"Server Health\",\"value\":\"").append(reasonVal).append("\",\"inline\":false").append('}');
					jsonBuilder.append(']');
				} else {
					jsonBuilder.append("\"fields\":[");
					jsonBuilder.append('{').append("\"name\":\"Player\",\"value\":\"").append(playerVal).append("\",\"inline\":true").append('}');
					jsonBuilder.append(',');
					jsonBuilder.append('{').append("\"name\":\"Staff\",\"value\":\"").append(reporterVal).append("\",\"inline\":true").append('}');
					jsonBuilder.append(',');
					jsonBuilder.append('{').append("\"name\":\"Reason\",\"value\":\"").append(reasonVal).append("\",\"inline\":false").append('}');
					jsonBuilder.append(']');
				}
				jsonBuilder.append(',');
				jsonBuilder.append("\"timestamp\":\"").append(isoTime).append("\"");
				jsonBuilder.append(',');
				jsonBuilder.append("\"footer\":{\"text\":\"").append(footerText).append("\"}");
				jsonBuilder.append('}');
				jsonBuilder.append(']');
				jsonBuilder.append('}');

				String json = jsonBuilder.toString();

				byte[] out = json.getBytes(StandardCharsets.UTF_8);
				conn.setFixedLengthStreamingMode(out.length);

				try (OutputStream os = conn.getOutputStream()) {
					os.write(out);
				}

				int code = conn.getResponseCode();

				if (code == 429) {
					plugin.getLogger().warning("Discord webhook rate-limited (HTTP 429). Slow down reports.");
				} else if (code >= 200 && code < 300) {
					plugin.getLogger().fine("Sent report to Discord webhook (HTTP " + code + ")");
				} else {
					plugin.getLogger().warning("Discord webhook returned HTTP " + code);
				}

			} catch (Exception e) {
				plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
			} finally {
				if (conn != null) conn.disconnect();
			}
		});
	}

	private static String jsonEscape(String s) {
		if (s == null) return "";
		return s
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r");
	}

	private static String escapeForMarkdown(String s) {
		if (s == null) return "";
		return s
				.replace("*", "\\*")
				.replace("_", "\\_")
				.replace("`", "\\`")
				.replace("@", "@\u200B");
	}
}

package kaiakk.axior.integrations;

public class PowerhouseBukkit {

	private static org.bukkit.scheduler.BukkitTask task;
	private static boolean warned = false;
	private static final double DEFAULT_THRESHOLD_MS = 50.0D;
	private static final long DEFAULT_INTERVAL_TICKS = 20L * 10L;

	public static void start(org.bukkit.plugin.Plugin plugin) {
		stop();
		task = kaiakk.multimedia.classes.SchedulerHelper.runTimerTracked(plugin, () -> {
			try {
				Class<?> api = Class.forName("kaiakk.powerhouse.external.PowerhouseAPI");
				java.lang.reflect.Method m = api.getMethod("getAverageMspt");
				Object res = m.invoke(null);
				double mspt = -1D;
				if (res instanceof Number) mspt = ((Number) res).doubleValue();

				if (mspt > DEFAULT_THRESHOLD_MS) {
					if (!warned) {
						warnAdmins(plugin, mspt);
						warned = true;
					}
				} else {
					warned = false;
				}
			} catch (ClassNotFoundException cnfe) {
				stop();
			} catch (Throwable ignored) {
			}
		}, 0L, DEFAULT_INTERVAL_TICKS);
	}

	public static void stop() {
		try {
			if (task != null) {
				try { kaiakk.multimedia.classes.SchedulerHelper.cancelTask(task); } catch (Throwable ignored) { task.cancel(); }
				task = null;
			}
		} catch (Throwable ignored) {}
		warned = false;
	}

	private static void warnAdmins(org.bukkit.plugin.Plugin plugin, double mspt) {
		String message = "[Axior] Powerhouse spike detected: average MSPT=" + String.format("%.2f", mspt) + "ms";
		try { plugin.getLogger().warning(message); } catch (Throwable ignored) {}

		kaiakk.multimedia.classes.SchedulerHelper.run(plugin, () -> {
			try {
				for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
					try {
						if (p.isOp() || p.hasPermission("axior.admin") || p.hasPermission("axior.owner")) p.sendMessage(message);
					} catch (Throwable ignored) {}
				}
			} catch (Throwable ignored) {}
		});
	}
}

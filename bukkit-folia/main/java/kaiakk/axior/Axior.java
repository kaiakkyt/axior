package kaiakk.axior;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public final class Axior extends JavaPlugin {
	private PluginDelegate delegate;

	@Override
	public void onEnable() {
		boolean folia = isFolia();
		getLogger().info("Platform detection: Folia? " + folia);

		if (folia) {
			try {
				delegate = new AxiorFolia(this);
			} catch (Throwable t) {
				getLogger().severe("Detected Folia but failed to instantiate AxiorFolia: " + t.getMessage() + ". Aborting enable to avoid running Bukkit delegate on Folia.");
				throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
			}
		} else {
			delegate = new AxiorBukkit(this);
		}

		try {
			if (delegate != null) delegate.onEnable();
		} catch (Throwable t) {
			getLogger().severe("Failed to initialize Axior delegate: " + t.getMessage());
			throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
		}
	}

	@Override
	public void onDisable() {
		if (delegate != null) {
			try { delegate.onDisable(); } catch (Throwable ignored) {}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (delegate != null) return delegate.onCommand(sender, command, label, args);
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (delegate != null) return delegate.onTabComplete(sender, command, alias, args);
		return Collections.emptyList();
	}

	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (delegate != null) delegate.onPluginMessageReceived(channel, player, message);
	}

	private boolean isFolia() {
		final String foliaClass = "io.papermc.paper.threadedregions.RegionScheduler";

		if (tryLoadClass(foliaClass, this.getClass().getClassLoader())) return true;
		if (tryLoadClass(foliaClass, Thread.currentThread().getContextClassLoader())) return true;
		if (tryLoadClass(foliaClass, ClassLoader.getSystemClassLoader())) return true;

		try {
			if (getServer() != null) {
				String name = getServer().getName();
				String version = getServer().getVersion();
				String bukkitVersion = getServer().getBukkitVersion();

				for (String probe : new String[] { name, version, bukkitVersion }) {
					if (probe != null && probe.toLowerCase().contains("folia")) return true;
				}
			}
		} catch (Throwable ignored) {}

		return false;
	}

	private static boolean tryLoadClass(String className, ClassLoader loader) {
		if (loader == null) return false;
		try {
			Class.forName(className, false, loader);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		} catch (Throwable ignored) {
			return false;
		}
	}
}

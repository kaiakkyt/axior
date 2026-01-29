package kaiakk.axior;

import kaiakk.multimedia.classes.ConsoleLog;
import kaiakk.multimedia.classes.SchedulerHelper;
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
		try { ConsoleLog.init(this); } catch (Throwable ignored) {}
		try { SchedulerHelper.setPlugin(this); } catch (Throwable ignored) {}

		try {
			delegate = new AxiorBukkit(this);
		} catch (Throwable t) {
			ConsoleLog.error("Failed to instantiate AxiorBukkit: " + t.getMessage());
			throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
		}

		try {
			if (delegate != null) delegate.onEnable();
		} catch (Throwable t) {
			ConsoleLog.error("Failed to initialize Axior delegate: " + t.getMessage());
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

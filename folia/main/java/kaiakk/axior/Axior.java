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
		try {
			delegate = new AxiorFolia(this);
		} catch (Throwable t) {
			getLogger().severe("Failed to instantiate AxiorFolia: " + t.getMessage());
			throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
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

}

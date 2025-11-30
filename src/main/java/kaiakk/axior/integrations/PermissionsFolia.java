package kaiakk.axior.integrations;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

public class PermissionsFolia {
	private static volatile Object apiProvider = null;
	private static volatile Class<?> apiClass = null;
	private static volatile Method mHasPermissionPlayer = null;
	private static volatile Method mHasPermissionUuid = null;

	public static void init(JavaPlugin plugin) {
		if (apiProvider != null) return;
		Logger logger = plugin.getLogger();
		try {
			apiClass = Class.forName("kaiakk.foliaPerms.api.FoliaPermsAPI");
			RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration((Class) apiClass);
			if (rsp != null) {
				apiProvider = rsp.getProvider();
				try {
					mHasPermissionPlayer = apiClass.getMethod("hasPermission", Player.class, String.class);
				} catch (NoSuchMethodException ignored) {
				}
				try {
					mHasPermissionUuid = apiClass.getMethod("hasPermission", UUID.class, String.class);
				} catch (NoSuchMethodException ignored) {
				}
				logger.info("FoliaPerms hooked successfully.");
			} else {
				logger.warning("FoliaPerms API not found via ServicesManager.");
			}
		} catch (ClassNotFoundException e) {
			logger.info("FoliaPerms API class not present: " + e.getMessage());
		} catch (Throwable t) {
			logger.warning("Failed to initialize FoliaPerms integration: " + t.getMessage());
		}
	}

	public static boolean hasPermission(Player player, String node) {
		if (apiProvider == null) return false;
		try {
			if (mHasPermissionPlayer != null) {
				Object res = mHasPermissionPlayer.invoke(apiProvider, player, node);
				if (res instanceof Boolean) return (Boolean) res;
			} else if (mHasPermissionUuid != null) {
				Object res = mHasPermissionUuid.invoke(apiProvider, player.getUniqueId(), node);
				if (res instanceof Boolean) return (Boolean) res;
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	public static boolean hasPermission(UUID uuid, String node) {
		if (apiProvider == null) return false;
		try {
			if (mHasPermissionUuid != null) {
				Object res = mHasPermissionUuid.invoke(apiProvider, uuid, node);
				if (res instanceof Boolean) return (Boolean) res;
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	public static boolean isAvailable() {
		return apiProvider != null && apiClass != null;
	}
}

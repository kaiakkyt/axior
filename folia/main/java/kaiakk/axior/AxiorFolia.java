package kaiakk.axior;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.net.InetSocketAddress;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Biome;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.lang.reflect.Method;

import kaiakk.axior.integrations.DiscordFolia;
import kaiakk.axior.integrations.WebManagerFolia;
import kaiakk.axior.integrations.VersionCheckerFolia;
import kaiakk.axior.integrations.PermissionsFolia;
import kaiakk.axior.proxy.ProxyListener;

public final class AxiorFolia implements org.bukkit.command.CommandExecutor, TabCompleter, Listener, PluginMessageListener, PluginDelegate {
    private final JavaPlugin plugin;
    private WebManagerFolia webManager;
    private ProxyListener proxyListener;

    public AxiorFolia(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    private JavaPlugin getPlugin() { return this.plugin; }
    private org.bukkit.Server getServer() { return this.plugin.getServer(); }
    private org.bukkit.configuration.file.FileConfiguration getConfig() { return this.plugin.getConfig(); }
    private java.util.logging.Logger getLogger() { return this.plugin.getLogger(); }
    private void saveDefaultConfig() { this.plugin.saveDefaultConfig(); }
    private void saveConfig() { this.plugin.saveConfig(); }
    private PluginDescriptionFile getDescription() { return this.plugin.getDescription(); }
    private String getJarFileName() {
        try {
            java.net.URL url = this.plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (url != null) {
                String path = url.getPath();
                if (path == null) return "unknown";
                String name = new java.io.File(path).getName();
                return name == null || name.isEmpty() ? "unknown" : name;
            }
        } catch (Throwable ignored) {}
        return "unknown";
    }
    
    private static final class FoliaScheduler {
        private static volatile boolean initialized = false;
        private static volatile boolean foliaAvailable = false;

        private static java.lang.reflect.Method mRun = null;
        private static java.lang.reflect.Method mRunLater = null;
        private static java.lang.reflect.Method mRunTimer = null;
        private static java.lang.reflect.Method mGetRegionScheduler = null;

        private static synchronized void init(JavaPlugin plugin) {
            if (initialized) return;
            initialized = true;
            try {
                Class<?> rsClass = Class.forName("io.papermc.paper.threadedregions.RegionScheduler");
                foliaAvailable = true;

                try {
                    mGetRegionScheduler = plugin.getServer().getClass().getMethod("getRegionScheduler");
                } catch (NoSuchMethodException ignored) {
                    for (Method candidate : plugin.getServer().getClass().getMethods()) {
                        if (candidate.getParameterCount() == 0 && rsClass.isAssignableFrom(candidate.getReturnType())) {
                            mGetRegionScheduler = candidate;
                            break;
                        }
                    }
                }

                Object rsInstance = null;
                if (mGetRegionScheduler != null) {
                    try { rsInstance = mGetRegionScheduler.invoke(plugin.getServer()); } catch (Throwable ignored) { rsInstance = null; }
                }

                Method[] methodsToInspect = (rsInstance != null) ? rsInstance.getClass().getMethods() : rsClass.getMethods();
                for (Method mm : methodsToInspect) {
                    Class<?>[] pts = mm.getParameterTypes();
                    if (mRun == null && pts.length == 1 && Runnable.class.isAssignableFrom(pts[0])) {
                        mRun = mm;
                        continue;
                    }
                    if (mRunLater == null && pts.length == 2 && Runnable.class.isAssignableFrom(pts[0]) && (pts[1] == long.class || pts[1] == Long.class)) {
                        mRunLater = mm;
                        continue;
                    }
                    if (mRunTimer == null && pts.length == 3 && Runnable.class.isAssignableFrom(pts[0]) && (pts[1] == long.class || pts[1] == Long.class) && (pts[2] == long.class || pts[2] == Long.class)) {
                        mRunTimer = mm;
                        continue;
                    }
                }
            } catch (ClassNotFoundException e) {
                foliaAvailable = false;
            } catch (Throwable t) {
                foliaAvailable = false;
            }
        }

        private static Object getRegionSchedulerInstance(JavaPlugin plugin) {
            if (mGetRegionScheduler == null) return null;
            try {
                return mGetRegionScheduler.invoke(plugin.getServer());
            } catch (Throwable ignored) {
                return null;
            }
        }

            static boolean isAvailable(JavaPlugin plugin) {
                init(plugin);
                return foliaAvailable;
            }
            static void runTask(JavaPlugin plugin, Runnable task) {
                init(plugin);
                if (!foliaAvailable) {
                    try { plugin.getLogger().severe("Folia RegionScheduler not available; skipping scheduled task."); } catch (Throwable ignored) {}
                    return;
                }
                Object rs = getRegionSchedulerInstance(plugin);
                if (rs != null && mRun != null) {
                    try {
                        mRun.invoke(rs, task);
                        return;
                    } catch (Throwable t) {
                        try { plugin.getLogger().warning("Folia RegionScheduler invocation failed: " + t.getMessage()); } catch (Throwable ignored) {}
                        return;
                    }
                }
                try { plugin.getLogger().warning("Folia RegionScheduler instance or run method not found; skipping scheduled task."); } catch (Throwable ignored) {}
            }

            static void runTaskLater(JavaPlugin plugin, Runnable task, long delay) {
                init(plugin);
                if (!foliaAvailable) {
                    try { plugin.getLogger().severe("Folia RegionScheduler not available; skipping delayed task."); } catch (Throwable ignored) {}
                    return;
                }
                Object rs = getRegionSchedulerInstance(plugin);
                if (rs != null && mRunLater != null) {
                    try {
                        mRunLater.invoke(rs, task, delay);
                        return;
                    } catch (Throwable t) {
                        try { plugin.getLogger().warning("Folia RegionScheduler runLater invocation failed: " + t.getMessage()); } catch (Throwable ignored) {}
                        return;
                    }
                }
                try { plugin.getLogger().warning("Folia RegionScheduler instance or runLater method not found; skipping delayed task."); } catch (Throwable ignored) {}
            }

            static void runTaskTimerAsync(JavaPlugin plugin, Runnable task, long delay, long period) {
                init(plugin);
                if (!foliaAvailable) {
                    try { plugin.getLogger().severe("Folia RegionScheduler not available; skipping repeating task."); } catch (Throwable ignored) {}
                    return;
                }
                Object rs = getRegionSchedulerInstance(plugin);
                if (rs != null && mRunTimer != null) {
                    try {
                        mRunTimer.invoke(rs, task, delay, period);
                        return;
                    } catch (Throwable t) {
                        try { plugin.getLogger().warning("Folia RegionScheduler runTimer invocation failed: " + t.getMessage()); } catch (Throwable ignored) {}
                        return;
                    }
                }

                if (rs != null && mRunLater != null) {
                    try {
                        Runnable wrapper = new Runnable() {
                            @Override
                            public void run() {
                                try { task.run(); } catch (Throwable ignored) {}
                                try { runTaskLater(plugin, this, period); } catch (Throwable ignored) {}
                            }
                        };
                        mRunLater.invoke(rs, wrapper, delay);
                        return;
                    } catch (Throwable t) {
                        try { plugin.getLogger().warning("Folia RegionScheduler runLater invocation failed while emulating timer: " + t.getMessage()); } catch (Throwable ignored) {}
                        return;
                    }
                }

                try { plugin.getLogger().warning("Folia RegionScheduler instance or suitable repeating method not found; skipping repeating task."); } catch (Throwable ignored) {}
            }
    }

    private static void runTask(JavaPlugin plugin, Runnable task) { FoliaScheduler.runTask(plugin, task); }
    private static void runTaskLater(JavaPlugin plugin, Runnable task, long delay) { FoliaScheduler.runTaskLater(plugin, task, delay); }
    private static void runTaskTimerAsync(JavaPlugin plugin, Runnable task, long delay, long period) { FoliaScheduler.runTaskTimerAsync(plugin, task, delay, period); }
    private File getDataFolder() { return this.plugin.getDataFolder(); }

    public void onEnable() {
        webManager = new WebManagerFolia();
        webManager.init();

        proxyListener = new ProxyListener(getPlugin());
        proxyListener.register();
        getLogger().info("ProxyListener initialized for BungeeCord/Waterfall/Velocity support");

        saveDefaultConfig();

        try {
            PermissionsFolia.init(getPlugin());
        } catch (Throwable t) {
            getLogger().warning("PermissionsFolia.init() failed: " + t.getMessage());
        }
        try {
            if (!PermissionsFolia.isAvailable()) {
                getLogger().warning("FoliaPerms not detected at startup; will re-check during ServerLoadEvent before disabling.");
            }
        } catch (Throwable t) {
            getLogger().warning("Error while checking FoliaPerms availability at startup: " + t.getMessage());
        }

        try {
            File df = getDataFolder();
            getLogger().info("Folia diagnostic: dataFolder=" + (df == null ? "(null)" : df.getAbsolutePath()) + " exists=" + (df != null && df.exists()));
            File cfgFile = new File(df, "config.yml");
            getLogger().info("Folia diagnostic: data/config.yml exists=" + (cfgFile.exists()) + (cfgFile.exists() ? (" size=" + cfgFile.length()) : ""));
            java.io.InputStream res = plugin.getResource("config.yml");
            getLogger().info("Folia diagnostic: embedded config.yml resource present=" + (res != null));
            try { if (res != null) res.close(); } catch (Throwable ignored) {}

            try {
                String hook = getConfig().getString("discord-webhook-url", "<missing>");
                long health = getConfig().getLong("serverHealthHours", -1L);
                getLogger().info("Folia diagnostic: getConfig.discord-webhook-url=" + (hook == null ? "<null>" : (hook.length() > 64 ? hook.substring(0,64) + "..." : hook)));
                getLogger().info("Folia diagnostic: getConfig.serverHealthHours=" + health);
            } catch (Throwable t) {
                getLogger().warning("Folia diagnostic: failed to read getConfig() values: " + t.getMessage());
            }
        } catch (Throwable ignored) {}

        adminFile = new File(getDataFolder(), "admin.yml");
        if (adminFile.getParentFile() != null && !adminFile.getParentFile().exists()) {
            adminFile.getParentFile().mkdirs();
        }
        if (!adminFile.exists()) {
            try {
                adminFile.createNewFile();
            } catch (IOException ioe) {
                getLogger().warning("Failed to create admin.yml: " + ioe.getMessage());
                adminFile = null;
            }
        }
        if (adminFile != null) {
            adminConfig = YamlConfiguration.loadConfiguration(adminFile);
            try {
                getLogger().info("Folia diagnostic: admin.yml path=" + adminFile.getAbsolutePath() + " exists=" + adminFile.exists() + " size=" + (adminFile.exists() ? adminFile.length() : 0L));
                if (adminConfig != null) {
                    try {
                        getLogger().info("Folia diagnostic: admin.yml top-level keys=" + adminConfig.getKeys(false));
                    } catch (Throwable t) {
                        getLogger().warning("Folia diagnostic: failed to read adminConfig keys: " + t.getMessage());
                    }
                }
            } catch (Throwable ignored) {}
        } else {
            adminConfig = null;
        }

        ensureAdminConfig();

        loadMutesFromConfig();
        loadFrozenFromConfig();
        loadVanishedFromConfig();
        loadSpiesFromConfig();
        loadWarningsFromConfig();
        loadBannedWordsFromConfig();
        loadInvincibleFromConfig();
        loadForceDisabledFromConfig();
        loadMaintenanceFromConfig();

        getServer().getPluginManager().registerEvents(this, getPlugin());

        String[] channels = new String[] {"minecraft:brand", "fml:handshake", "fml:handshake_tag", "forge:handshake", "fabric:client_brand"};
        for (String ch : channels) {
            try {
                getServer().getMessenger().registerIncomingPluginChannel(getPlugin(), ch, this);
                getServer().getMessenger().registerOutgoingPluginChannel(getPlugin(), ch);
            } catch (IllegalArgumentException ignored) {
            }
        }

        getLogger().info("Running Axior v" + getDescription().getVersion() + " by " + String.join(", ", getDescription().getAuthors()));
        getLogger().info("Plugin messaging channels registered: " + Arrays.toString(channels)); 
        getLogger().info("Jar file name: " + getJarFileName());
        try {
            if (PermissionsFolia.isAvailable()) {
                getLogger().info("FoliaPerms detected, using FoliaPerms API for permissions.");
            } else {
                getLogger().warning("FoliaPerms not detected — some permission features may be unavailable.");
            }
        } catch (Throwable ignored) {}
        
        for (org.bukkit.plugin.Plugin pl : Bukkit.getPluginManager().getPlugins()) {
            PluginDescriptionFile desc = pl.getDescription();
            List<String> authors = desc.getAuthors();

            if (authors == null || !authors.contains("KaiakK")) continue;

            String name = desc.getName();

            if (name != null && name.equalsIgnoreCase(getDescription().getName())) continue;

            String version = desc.getVersion();
            getLogger().info("Detected one of KaiakK's plugins: " + name + " v" + version + " by " + String.join(", ", authors) + " — Thanks for using my other plugin " + name + "!");
        }

        getLogger().info("Java runtime: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        getLogger().info("Architecture: " + System.getProperty("os.arch"));
        getLogger().info("Operating system: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        
        getLogger().info("Bukkit/Server version: " + Bukkit.getVersion() + " " + Bukkit.getBukkitVersion());
        getLogger().info("Server software: " + Bukkit.getServer().getName());

        String serverName = Bukkit.getServer().getName().toLowerCase();
        if (serverName.contains("arclight")) {
            getLogger().warning("This works, but is not fully recommended — Arclight is a hybrid between mods and plugins. Use Paper or Purpur for optimal performance.");
        }  else if (serverName.contains("pufferfish") || serverName.contains("leaf")) {
            getLogger().warning("This works, but is not recommended. Use Paper or Purpur for optimal performance.");
        }
        
        getLogger().info("Online mode: " + Bukkit.getOnlineMode());
        getLogger().info("Server port: " + Bukkit.getPort());
        getLogger().info("Max players configured: " + Bukkit.getMaxPlayers());

        long cfgMinutes = getConfig().getLong("reportCooldownMinutes", 5L);
        getLogger().info("Report cooldown: " + cfgMinutes + " minutes");
        getLogger().info("Discord webhook url set to " + getConfig().getString("discord-webhook-url", "not available (not configured)"));



    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        try {
            int worldCount = Bukkit.getWorlds().size();
            List<String> worldNames = Bukkit.getWorlds().stream().map(world -> world.getName()).collect(Collectors.toList());
            getLogger().info("Loaded Worlds (" + worldCount + "): " + worldNames);
            getLogger().info("Players online: " + Bukkit.getOnlinePlayers().size());
        } catch (Throwable ignored) {}

        try {
            VersionCheckerFolia.check(getPlugin(), getDescription().getVersion());
        } catch (Throwable t) {
            getLogger().warning("VersionCheckerFolia failed: " + t.getMessage());
        }

        try {
            PermissionsFolia.init(getPlugin());
        } catch (Throwable t) {
            getLogger().warning("PermissionsFolia.init() retry failed: " + t.getMessage());
        }
        try {
                if (!PermissionsFolia.isAvailable()) {
                getLogger().severe("FoliaPerms not detected — some Folia-specific features will be disabled. Plugin will remain enabled.");
            }
        } catch (Throwable t) {
            getLogger().warning("Error while re-checking FoliaPerms availability: " + t.getMessage());
        }

        try {
            long healthHours = getConfig().getLong("serverHealthHours", 24L);
            if (healthHours > 0) {
                long periodTicks = Math.max(1L, healthHours) * 3600L * 20L;
                String webhook = getConfig().getString("discord-webhook-url", "");
                try {
                    Runnable reportTask = () -> {
                        try {
                            long now = System.currentTimeMillis();
                            long uptimeMs = now - pluginStartTimeMs;
                            long uptimeSec = uptimeMs / 1000L;
                            long days = uptimeSec / 86400L;
                            long hours = (uptimeSec % 86400L) / 3600L;
                            long mins = (uptimeSec % 3600L) / 60L;

                            Runtime rt = Runtime.getRuntime();
                            long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
                            long totalMb = rt.totalMemory() / (1024L * 1024L);
                            int playersOnline = Bukkit.getOnlinePlayers().size();
                            int maxPlayers = Bukkit.getMaxPlayers();
                            int worldCountNow = Bukkit.getWorlds().size();
                            String worldList = Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.joining(", "));

                            StringBuilder reason = new StringBuilder();
                            reason.append("Server Health Report\n");
                            reason.append("Uptime: ").append(days).append("d ").append(hours).append("h ").append(mins).append("m\n");
                            reason.append("Players: ").append(playersOnline).append("/").append(maxPlayers).append("\n");
                            reason.append("Memory: ").append(usedMb).append("MB / ").append(totalMb).append("MB\n");
                            reason.append("Loaded worlds (" + worldCountNow + "): ").append(worldList).append("\n");
                            reason.append("Java: ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")");

                            DiscordFolia.sendReportAsync(getPlugin(), webhook, "Server", "Axior", reason.toString(), System.currentTimeMillis());
                        } catch (Exception ex) {
                            getLogger().warning("Failed to send server health report: " + ex.getMessage());
                        }
                    };

                    if (FoliaScheduler.isAvailable(getPlugin())) {
                        runTaskTimerAsync(getPlugin(), reportTask, 20L, periodTicks);
                    } else {
                        getLogger().severe("Folia RegionScheduler not available — skipping scheduling of server health reports. Plugin will remain enabled.");
                    }
                } catch (Throwable t) {
                    getLogger().warning("Failed to schedule health reports: " + t.getMessage());
                }
            }
        } catch (Throwable ignored) {}
    }

    public void onDisable() {
        saveMutesToConfig();
        saveConfig();
        if (webManager != null) {
            try {
                webManager.shutdown();
            } catch (Throwable t) {
                getLogger().warning("WebManager shutdown failed: " + t.getMessage());
            }
        }
        if (proxyListener != null) {
            try {
                proxyListener.unregister();
            } catch (Throwable t) {
                getLogger().warning("ProxyListener shutdown failed: " + t.getMessage());
            }
        }
        getLogger().info("Plugin disabled.");
        getLogger().info("Saving all data from config.yml and admin.yml...");
        getLogger().info("Data saved.");
        getLogger().info("Exiting...");
        getLogger().info("Cya!");
    }

    private final Map<UUID, Set<String>> detectedClients = new HashMap<>();
    private final Map<UUID, Long> muteExpiry = new HashMap<>();
    private final Map<UUID, String> muteReason = new HashMap<>();
    private final Map<UUID, String> muteSetBy = new HashMap<>();
    private File adminFile;
    private FileConfiguration adminConfig;
    private final Map<UUID, Long> lastReportTime = new HashMap<>();
    private long reportCooldownMillis = 5 * 60L * 1000L;
    private long pluginStartTimeMs = System.currentTimeMillis();
    private final Map<UUID, Long> frozenExpiry = new HashMap<>();
    private final Map<UUID, Long> frozenSetAt = new HashMap<>();
    private final Set<UUID> vanished = new HashSet<>();
    private final Map<UUID, Long> vanishedSince = new HashMap<>();
    private final Map<UUID, org.bukkit.GameMode> previousGamemode = new HashMap<>();
    private final Set<UUID> spies = new HashSet<>();
    private final Map<UUID, Integer> warningsCount = new HashMap<>();
    private final Set<String> bannedWordsDecoded = Collections.synchronizedSet(new HashSet<>());
    private final List<Pattern> bannedWordPatterns = Collections.synchronizedList(new java.util.ArrayList<>());
    private final Set<UUID> invinciblePlayers = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> forceDisabled = Collections.synchronizedSet(new HashSet<>());
    private boolean maintenanceMode = false;

    private boolean isFrozen(UUID id) {
        Long exp = frozenExpiry.get(id);
        if (exp == null) return false;
        long now = System.currentTimeMillis();
        if (exp == Long.MAX_VALUE) return true;
        if (now < exp) return true;
        frozenExpiry.remove(id);
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        detectedClients.computeIfAbsent(p.getUniqueId(), k -> Collections.synchronizedSet(new HashSet<>()));

        if (maintenanceMode) {
            try {
                if (p != null && !(p.hasPermission("axior.mod") || p.hasPermission("axior.admin") || p.hasPermission("axior.owner"))) {
                    p.kickPlayer(getMaintenanceKickMessage());
                    return;
                }
            } catch (Throwable ignored) {}
        }



        Runnable joinInspect = () -> {
            Set<String> s = detectedClients.get(p.getUniqueId());
            String playerName = p.getName();
            if (s == null || s.isEmpty()) {
                getLogger().info(playerName + " client: unknown (likely vanilla or not reporting)");
            } else {
                synchronized (s) {
                    getLogger().info(playerName + " detected client(s): " + String.join(", ", s));
                }
            }
        };

        try {
            runTaskLater(getPlugin(), joinInspect, 60L);
        } catch (Throwable t) {
            try {
                getLogger().severe("Folia scheduler failed in onPlayerJoin: " + t.getMessage());
            } catch (Throwable ignored) {}
        }
        handleVanishVisibilityForJoining(p);
        checkAltsOnJoin(p);
    }

    private void checkAltsOnJoin(Player p) {
        if (p == null) return;
        try {
            InetSocketAddress addr = p.getAddress();
            if (addr == null || addr.getAddress() == null) return;
            String ip = addr.getAddress().getHostAddress();

            try {
                BanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
                if (ipBanList != null && ipBanList.isBanned(ip)) {
                    getLogger().warning("Player '" + p.getName() + "' joined from banned IP " + ip + " — possible alt of a banned account.");
                }
            } catch (Exception ignored) {
            }

            try {
                BanList nameBanList = Bukkit.getBanList(BanList.Type.NAME);
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (other == null) continue;
                    if (other.getUniqueId().equals(p.getUniqueId())) continue;
                    InetSocketAddress otherAddr = other.getAddress();
                    if (otherAddr == null || otherAddr.getAddress() == null) continue;
                    String otherIp = otherAddr.getAddress().getHostAddress();
                    if (!ip.equals(otherIp)) continue;
                    if (nameBanList != null && nameBanList.isBanned(other.getName())) {
                        getLogger().warning("Player '" + p.getName() + "' joined from the same IP (" + ip + ") as name-banned account '" + other.getName() + "'. Possible alt.");
                    }
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
    }

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (p == null) return;
        UUID id = p.getUniqueId();
        if (!isFrozen(id)) return;

        org.bukkit.Location from = event.getFrom();
        org.bukkit.Location to = event.getTo();
        if (to == null) return;

        boolean movedHorizontally = Math.abs(from.getX() - to.getX()) > 0.0001 || Math.abs(from.getZ() - to.getZ()) > 0.0001;
        boolean jumped = to.getY() > from.getY() + 0.0001;
        if (movedHorizontally || jumped) {
            org.bukkit.Location target = from.clone();
            target.setYaw(to.getYaw());
            target.setPitch(to.getPitch());
            p.teleport(target);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        UUID id = p.getUniqueId();
        Long exp = muteExpiry.get(id);
        if (exp != null) {
            long now = System.currentTimeMillis();
            if (exp == Long.MAX_VALUE) {
                String reason = muteReason.getOrDefault(id, "No reason provided");
                p.sendMessage(ChatColor.RED + "You are permanently muted. Reason: " + reason);
                event.setCancelled(true);
                return;
            }
            if (now < exp) {
                long remainingMs = exp - now;
                long remainingMin = (remainingMs + 59999L) / 60000L;
                String reason = muteReason.getOrDefault(id, "No reason provided");
                p.sendMessage(ChatColor.RED + "You are muted for " + remainingMin + " more minute(s). Reason: " + reason);
                event.setCancelled(true);
                return;
            } else {
                muteExpiry.remove(id);
                muteReason.remove(id);
                muteSetBy.remove(id);
            }
        }
        try {
            String raw = event.getMessage();
            if (raw != null && !raw.isEmpty() && !bannedWordsDecoded.isEmpty()) {
                String normalized = ChatColor.stripColor(raw).toLowerCase();
                    synchronized (bannedWordPatterns) {
                        for (Pattern pat : bannedWordPatterns) {
                            if (pat == null) continue;
                            try {
                                if (pat.matcher(normalized).find()) {
                                    event.setCancelled(true);
                                        try {
                                            final UUID offenderId = p.getUniqueId();
                                            final String offenderName = p.getName();
                                            runTask(getPlugin(), () -> {
                                                try { getLogger().warning(offenderName + " attempted to send a message that matched a banned word. Message blocked."); } catch (Exception ignored) {}

                                                for (Player admin : Bukkit.getOnlinePlayers()) {
                                                    try { if (admin != null && (admin.hasPermission("axior.mod") || admin.hasPermission("axior.admin") || admin.hasPermission("axior.owner"))) admin.sendMessage(ChatColor.RED + offenderName + " has said a banned word! Take action if needed."); } catch (Exception ignored) {}
                                                }

                                                try { Player off = Bukkit.getPlayer(offenderId); if (off != null && off.isOnline()) off.sendMessage(ChatColor.RED + "Your message was blocked for containing a banned word."); } catch (Exception ignored) {}

                                                try { runTaskLater(getPlugin(), () -> { try { Player off2 = Bukkit.getPlayer(offenderId); if (off2 != null && off2.isOnline()) off2.sendMessage(ChatColor.RED + "Reminder: your previous message was blocked. Avoid banned words."); } catch (Exception ignored) {} }, 100L); } catch (Exception ignored) {}
                                            });
                                        } catch (Exception ignored) {}
                                    return;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
            }
        } catch (Exception ignored) {}
    }

    private boolean isMuted(UUID id) {
        Long exp = muteExpiry.get(id);
        if (exp == null) return false;
        long now = System.currentTimeMillis();
        if (now < exp) return true;
        muteExpiry.remove(id);
        muteReason.remove(id);
        muteSetBy.remove(id);
        return false;
    }

    private long getRemainingMinutes(UUID id) {
        Long exp = muteExpiry.get(id);
        if (exp == null) return 0L;
        if (exp == Long.MAX_VALUE) return -1L;
        long now = System.currentTimeMillis();
        if (now >= exp) return 0L;
        long remainingMs = exp - now;
        return (remainingMs + 59999L) / 60000L;
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (msg == null || msg.length() == 0) return;
        String[] parts = msg.split(" ");
        String cmd = parts[0].toLowerCase();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        List<String> pmCommands = Arrays.asList("msg", "tell", "w", "whisper", "pm", "message", "m", "t", "wh", "r",
            "me", "emote", "action", "em");
        if (pmCommands.contains(cmd)) {
            Player p = event.getPlayer();
            if (p == null) return;
            if (isMuted(p.getUniqueId())) {
                long rem = getRemainingMinutes(p.getUniqueId());
                String reason = muteReason.getOrDefault(p.getUniqueId(), "No reason provided");
                if (rem == -1L) {
                    p.sendMessage(ChatColor.RED + "You are permanently muted and cannot send private messages. Reason: " + reason);
                } else {
                    p.sendMessage(ChatColor.RED + "You are muted and cannot send private messages. (" + rem + " minute(s) remaining) Reason: " + reason);
                }
                event.setCancelled(true);
            }
            if (!event.isCancelled()) {
                if (parts.length >= 2) {
                    String targetName = parts[1];
                    String privateMsg = (parts.length >= 3) ? String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)) : "";
                    for (Player spy : Bukkit.getOnlinePlayers()) {
                        if (spy == null) continue;
                        if (!spies.contains(spy.getUniqueId())) continue;
                        if (!(spy.hasPermission("axior.mod") || spy.hasPermission("axior.admin") || spy.hasPermission("axior.owner"))) continue;
                        if (spy.getUniqueId().equals(p.getUniqueId())) continue;
                        try {
                            spy.sendMessage(ChatColor.GRAY + "[Spy] " + p.getName() + " -> " + targetName + ": " + privateMsg);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player p = event.getPlayer();
        if (p == null) return;
        if (isMuted(p.getUniqueId())) {
            long rem = getRemainingMinutes(p.getUniqueId());
            String reason = muteReason.getOrDefault(p.getUniqueId(), "No reason provided");
            if (rem == -1L) {
                p.sendMessage(ChatColor.RED + "You are permanently muted and cannot edit signs. Reason: " + reason);
            } else {
                p.sendMessage(ChatColor.RED + "You are muted and cannot edit signs. (" + rem + " minute(s) remaining) Reason: " + reason);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        Player p = event.getPlayer();
        if (p == null) return;
        if (isMuted(p.getUniqueId())) {
            long rem = getRemainingMinutes(p.getUniqueId());
            String reason = muteReason.getOrDefault(p.getUniqueId(), "No reason provided");
            if (rem == -1L) {
                p.sendMessage(ChatColor.RED + "You are permanently muted and cannot edit books. Reason: " + reason);
            } else {
                p.sendMessage(ChatColor.RED + "You are muted and cannot edit books. (" + rem + " minute(s) remaining) Reason: " + reason);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event == null) return;
        try {
            org.bukkit.inventory.InventoryView view = event.getView();
            if (view == null) return;
            String title = view.getTitle();
            if (title != null && title.startsWith("Inventory of ")) {
                event.setCancelled(true);
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event == null) return;
        try {
            org.bukkit.inventory.InventoryView view = event.getView();
            if (view == null) return;
            String title = view.getTitle();
            if (title != null && title.startsWith("Inventory of ")) {
                event.setCancelled(true);
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event == null) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (invinciblePlayers.contains(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event == null) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (!invinciblePlayers.contains(p.getUniqueId())) return;
        event.setCancelled(true);
        org.bukkit.entity.Entity damager = event.getDamager();
        if (damager instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) damager;
            try {
                org.bukkit.util.Vector vel = proj.getVelocity();
                if (vel != null) {
                    double speed = vel.length();
                    org.bukkit.util.Vector newVel = vel.clone().multiply(-1.0);
                    if (newVel.length() > 0.0001) newVel = newVel.normalize().multiply(Math.max(speed, 1.0));
                    newVel.setY(newVel.getY() + 0.15);

                    try { proj.teleport(p.getLocation().add(0, 1.0, 0)); } catch (Throwable ignored) {}

                    try { proj.setVelocity(newVel); } catch (Throwable ignored) {}
                    try { proj.setShooter((org.bukkit.projectiles.ProjectileSource) p); } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
    }

    private void loadMutesFromConfig() {
        muteExpiry.clear();
        muteReason.clear();
        muteSetBy.clear();
        if (adminConfig == null) return;
        if (adminConfig.getConfigurationSection("mutes") == null) return;
        for (String key : adminConfig.getConfigurationSection("mutes").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                long expiry = adminConfig.getLong("mutes." + key + ".expiry", 0L);
                if (expiry <= 0L) continue;
                String reason = adminConfig.getString("mutes." + key + ".reason", "No reason provided");
                String setBy = adminConfig.getString("mutes." + key + ".setBy", "unknown");
                muteExpiry.put(id, expiry);
                muteReason.put(id, reason);
                muteSetBy.put(id, setBy);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void ensureAdminConfig() {
        if (adminFile == null) return;
        adminConfig = YamlConfiguration.loadConfiguration(adminFile);
        boolean changed = false;
        if (adminConfig.getConfigurationSection("mutes") == null) {
            adminConfig.createSection("mutes");
            changed = true;
        }
        if (adminConfig.getConfigurationSection("reports") == null) {
            adminConfig.createSection("reports");
            changed = true;
        }
        if (adminConfig.getConfigurationSection("frozen") == null) {
            adminConfig.createSection("frozen");
            changed = true;
        }
        if (adminConfig.getConfigurationSection("vanished") == null) {
            adminConfig.createSection("vanished");
            changed = true;
        }
        if (adminConfig.getConfigurationSection("spy") == null) {
            adminConfig.createSection("spy");
            changed = true;
        }
        if (adminConfig.getConfigurationSection("warnings") == null) {
            adminConfig.createSection("warnings");
            changed = true;
        }
        if (adminConfig.getConfigurationSection("invincible") == null) {
            adminConfig.createSection("invincible");
            changed = true;
        }
        if (changed) {
            try {
                adminConfig.save(adminFile);
            } catch (IOException ex) {
                getLogger().warning("Failed to write default admin.yml: " + ex.getMessage());
            }
        }
    }

    private void saveMutesToConfig() {
        if (adminFile == null) return;
        adminConfig = YamlConfiguration.loadConfiguration(adminFile);
        adminConfig.set("mutes", null);
        for (Map.Entry<UUID, Long> e : muteExpiry.entrySet()) {
            UUID id = e.getKey();
            long expiry = e.getValue();
            String base = "mutes." + id.toString() + ".";
            adminConfig.set(base + "expiry", expiry);
            adminConfig.set(base + "reason", muteReason.getOrDefault(id, "No reason provided"));
            adminConfig.set(base + "setBy", muteSetBy.getOrDefault(id, "unknown"));
        }
        adminConfig.set("frozen", null);
        for (Map.Entry<UUID, Long> e : frozenExpiry.entrySet()) {
            UUID id = e.getKey();
            long expiry = e.getValue();
            String base = "frozen." + id.toString() + ".";
            adminConfig.set(base + "expiry", expiry);
            long sat = frozenSetAt.getOrDefault(id, System.currentTimeMillis());
            adminConfig.set(base + "setAt", sat);
        }
        adminConfig.set("vanished", null);
        for (UUID id : vanished) {
            long since = vanishedSince.getOrDefault(id, System.currentTimeMillis());
            String base = "vanished." + id.toString() + ".";
            adminConfig.set(base + "since", since);
            org.bukkit.GameMode pg = previousGamemode.get(id);
            if (pg != null) {
                adminConfig.set(base + "previousGameMode", pg.name());
            }
        }
        adminConfig.set("spy", null);
        for (UUID id : spies) {
            adminConfig.set("spy." + id.toString(), true);
        }
        adminConfig.set("warnings", null);
        for (Map.Entry<UUID, Integer> e : warningsCount.entrySet()) {
            UUID id = e.getKey();
            int count = e.getValue() == null ? 0 : e.getValue();
            adminConfig.set("warnings." + id.toString() + ".count", count);
        }
        adminConfig.set("invincible", null);
        for (UUID id : invinciblePlayers) {
            try {
                String base = "invincible." + id.toString() + ".";
                long since = System.currentTimeMillis();
                adminConfig.set(base + "since", since);
            } catch (Throwable ignored) {}
        }
        try {
            adminConfig.save(adminFile);
        } catch (IOException ex) {
            getLogger().warning("Failed to save admin.yml: " + ex.getMessage());
        }
    }

    private void loadFrozenFromConfig() {
        frozenExpiry.clear();
        frozenSetAt.clear();
        if (adminConfig == null) return;
        if (adminConfig.getConfigurationSection("frozen") == null) return;
        for (String key : adminConfig.getConfigurationSection("frozen").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                long expiry = adminConfig.getLong("frozen." + key + ".expiry", Long.MAX_VALUE);
                long setAt = adminConfig.getLong("frozen." + key + ".setAt", 0L);
                if (setAt <= 0L) {
                    if (expiry != Long.MAX_VALUE) setAt = Math.max(0L, expiry - 60000L);
                    else setAt = System.currentTimeMillis();
                }
                long now = System.currentTimeMillis();
                if (expiry == Long.MAX_VALUE || now < expiry) {
                    frozenExpiry.put(id, expiry);
                    frozenSetAt.put(id, setAt);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void loadVanishedFromConfig() {
        vanished.clear();
        vanishedSince.clear();
        previousGamemode.clear();
        if (adminConfig == null) return;
        if (adminConfig.getConfigurationSection("vanished") == null) return;
        for (String key : adminConfig.getConfigurationSection("vanished").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                long since = adminConfig.getLong("vanished." + key + ".since", 0L);
                if (since <= 0L) since = System.currentTimeMillis();
                vanished.add(id);
                vanishedSince.put(id, since);
                String pgStr = adminConfig.getString("vanished." + key + ".previousGameMode", null);
                if (pgStr != null) {
                    try {
                        org.bukkit.GameMode gm = org.bukkit.GameMode.valueOf(pgStr);
                        previousGamemode.put(id, gm);
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        for (UUID id : new java.util.ArrayList<>(vanished)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                applyVanishToPlayer(p);
            }
        }
    }

    private void loadSpiesFromConfig() {
        spies.clear();
        if (adminConfig == null) return;
        if (adminConfig.getConfigurationSection("spy") == null) return;
        for (String key : adminConfig.getConfigurationSection("spy").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                boolean enabled = adminConfig.getBoolean("spy." + key, false);
                if (enabled) spies.add(id);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadWarningsFromConfig() {
        warningsCount.clear();
        if (adminConfig == null) return;
        if (adminConfig.getConfigurationSection("warnings") == null) return;
        for (String key : adminConfig.getConfigurationSection("warnings").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                int count = adminConfig.getInt("warnings." + key + ".count", 0);
                if (count > 0) warningsCount.put(id, count);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadInvincibleFromConfig() {
        invinciblePlayers.clear();
        if (adminConfig == null) return;
        if (adminConfig.getConfigurationSection("invincible") == null) return;
        for (String key : adminConfig.getConfigurationSection("invincible").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                invinciblePlayers.add(id);
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                        PotionEffectType resistanceType = PotionEffectType.getByName("RESISTANCE");
                        if (resistanceType != null) {
                            PotionEffect resistanceEffect = new PotionEffect(resistanceType, Integer.MAX_VALUE, 255, true, false);
                            p.addPotionEffect(resistanceEffect, true);
                        }
                        try {
                            PotionEffect saturationEffect = new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 255, true, false);
                            p.addPotionEffect(saturationEffect, true);
                        } catch (Throwable ignoredInner) {}
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadForceDisabledFromConfig() {
        forceDisabled.clear();
        if (adminConfig == null) return;
        List<String> list = adminConfig.getStringList("force-disabled");
        if (list == null || list.isEmpty()) return;
        for (String name : list) {
            if (name == null) continue;
            String n = name.trim();
            if (n.isEmpty()) continue;
            forceDisabled.add(n);
            try {
                org.bukkit.plugin.Plugin p = Bukkit.getPluginManager().getPlugin(n);
                if (p != null && p.isEnabled()) {
                    try {
                        Bukkit.getPluginManager().disablePlugin(p);
                        getLogger().info("Force-disabled plugin from admin.yml: " + n);
                    } catch (Throwable t) {
                        getLogger().warning("Failed to disable plugin " + n + ": " + t.getMessage());
                    }
                } else {
                    getLogger().fine("Configured force-disabled plugin not loaded or already disabled: " + n);
                }
            } catch (Throwable t) {
                getLogger().warning("Error while enforcing force-disabled plugin '" + n + "': " + t.getMessage());
            }
        }
    }

    private void saveForceDisabledToConfig() {
        if (adminFile == null) return;
        adminConfig = YamlConfiguration.loadConfiguration(adminFile);
        adminConfig.set("force-disabled", new java.util.ArrayList<>(forceDisabled));
        try {
            adminConfig.save(adminFile);
        } catch (IOException ex) {
            getLogger().warning("Failed to save force-disabled list to admin.yml: " + ex.getMessage());
        }
    }

    private void loadMaintenanceFromConfig() {
        maintenanceMode = false;
        if (adminConfig == null) return;
        try {
            boolean m = adminConfig.getBoolean("maintenance", false);
            maintenanceMode = m;
            if (maintenanceMode) {
                getLogger().info("Maintenance mode enabled from admin.yml — non-admins will be kicked on join.");
                enforceMaintenanceNow(null);
            }
        } catch (Throwable t) {
            getLogger().warning("Failed to load maintenance state from admin.yml: " + t.getMessage());
        }
    }

    private void saveMaintenanceToConfig() {
        if (adminFile == null) return;
        adminConfig = YamlConfiguration.loadConfiguration(adminFile);
        adminConfig.set("maintenance", maintenanceMode);
        try {
            adminConfig.save(adminFile);
        } catch (IOException ex) {
            getLogger().warning("Failed to save maintenance state to admin.yml: " + ex.getMessage());
        }
    }

    private String getMaintenanceKickMessage() {
        String m = "Server is currently in maintenance mode, try again later!";
        try {
            m = getConfig().getString("general-info.maintenanceMsg", m);
        } catch (Throwable ignored) {}
        return ChatColor.translateAlternateColorCodes('&', m);
    }

    private void enforceMaintenanceNow(String kickMessage) {
        String msg;
        if (kickMessage == null || kickMessage.isEmpty()) {
            msg = getMaintenanceKickMessage();
        } else {
            msg = ChatColor.translateAlternateColorCodes('&', kickMessage);
        }
        for (Player pl : Bukkit.getOnlinePlayers()) {
            try {
                if (!(pl.hasPermission("axior.mod") || pl.hasPermission("axior.admin") || pl.hasPermission("axior.owner"))) {
                    pl.kickPlayer(msg);
                }
            } catch (Throwable ignored) {}
        }
    }

    

    private void loadBannedWordsFromConfig() {
        try {
            List<String> entries = getConfig().getStringList("bannedWordsList");
            if (entries == null || entries.isEmpty()) {
                synchronized (bannedWordsDecoded) { bannedWordsDecoded.clear(); }
                return;
            }
            Base64.Decoder dec = Base64.getDecoder();
            Set<String> temp = new HashSet<>();
            int invalidCount = 0;
            int idx = 0;
            for (String e : entries) {
                idx++;
                if (e == null) continue;
                String trimmed = e.trim();
                if (trimmed.isEmpty()) continue;
                if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
                    trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
                }
                trimmed = trimmed.replaceAll("\\s+", "");
                if (trimmed.isEmpty()) continue;
                try {
                    byte[] bytes = dec.decode(trimmed);
                    String word = new String(bytes, StandardCharsets.UTF_8).toLowerCase().trim();
                    if (!word.isEmpty()) temp.add(word);
                } catch (IllegalArgumentException iae) {
                    invalidCount++;
                }
            }
            synchronized (bannedWordsDecoded) {
                bannedWordsDecoded.clear();
                bannedWordsDecoded.addAll(temp);
            }
            synchronized (bannedWordPatterns) {
                bannedWordPatterns.clear();
                for (String w : temp) {
                    if (w == null || w.isEmpty()) continue;
                    try {
                        String pat = "\\b" + Pattern.quote(w) + "\\b";
                        bannedWordPatterns.add(Pattern.compile(pat));
                    } catch (Exception ignored) {}
                }
            }
            getLogger().info("Loaded " + bannedWordsDecoded.size() + " banned word(s) from config (base64-decoded)." + (invalidCount > 0 ? " Skipped " + invalidCount + " invalid entry(ies)." : ""));
        } catch (Exception ex) {
            getLogger().warning("Failed to load bannedWordsList from config: " + ex.getMessage());
        }
    }

    public static String encodeToBase64(String plain) {
        if (plain == null) return "";
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    public static List<String> encodeListToBase64Lines(List<String> plainWords) {
        if (plainWords == null) return Collections.emptyList();
        return plainWords.stream()
                .filter(w -> w != null && !w.trim().isEmpty())
                .map(w -> encodeToBase64(w.trim()))
                .collect(Collectors.toList());
    }

    private void applyVanishToPlayer(Player target) {
        try {
            previousGamemode.putIfAbsent(target.getUniqueId(), target.getGameMode());
            target.setGameMode(GameMode.SPECTATOR);
        } catch (Exception ignored) {
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(target.getUniqueId())) continue;
            if (vanished.contains(viewer.getUniqueId())) {
                try { viewer.showPlayer(getPlugin(), target); } catch (Exception ignored) {}
            } else {
                try { viewer.hidePlayer(getPlugin(), target); } catch (Exception ignored) {}
            }
        }
    }

    private void removeVanishFromPlayer(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(target.getUniqueId())) continue;
            try { viewer.showPlayer(getPlugin(), target); } catch (Exception ignored) {}
        }
        org.bukkit.GameMode prev = previousGamemode.remove(target.getUniqueId());
        try {
            if (prev != null) target.setGameMode(prev);
            else target.setGameMode(GameMode.SURVIVAL);
        } catch (Exception ignored) {}
    }

    private void handleVanishVisibilityForJoining(Player joiner) {
        for (UUID id : new java.util.ArrayList<>(vanished)) {
            Player vanishedPlayer = Bukkit.getPlayer(id);
            if (vanishedPlayer == null) continue;
            if (joiner.getUniqueId().equals(vanishedPlayer.getUniqueId())) continue;
            if (vanished.contains(joiner.getUniqueId())) {
                try { joiner.showPlayer(getPlugin(), vanishedPlayer); } catch (Exception ignored) {}
            } else {
                try { joiner.hidePlayer(getPlugin(), vanishedPlayer); } catch (Exception ignored) {}
            }
        }
        if (vanished.contains(joiner.getUniqueId())) {
            applyVanishToPlayer(joiner);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (player == null) return;
        String ch = channel.toLowerCase();
        UUID id = player.getUniqueId();
        Set<String> set = detectedClients.computeIfAbsent(id, k -> new HashSet<>());

        if (ch.contains("neoforge") && set.add("neoforge")) {
            getLogger().info(player.getName() + " reported client: neoforge (via channel)");
        }
        if (ch.contains("quilt") && set.add("quilt")) {
            getLogger().info(player.getName() + " reported client: quilt (via channel)");
        }
        if (ch.contains("fabric") && !ch.contains("quilt") && set.add("fabric")) {
            getLogger().info(player.getName() + " reported client: fabric (via channel)");
        }
        if ((ch.contains("forge") || ch.contains("fml")) && !ch.contains("neoforge") && set.add("forge")) {
            getLogger().info(player.getName() + " reported client: forge/fml (via channel)");
        }

        String asString = null;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            try {
                asString = in.readUTF();
            } catch (Exception ignored) {
                try {
                    asString = new String(message, StandardCharsets.UTF_8);
                } catch (Exception ignored2) {
                    asString = null;
                }
            }
        } catch (Exception ignored) {
            try {
                asString = new String(message, StandardCharsets.UTF_8);
            } catch (Exception ignored2) {
                asString = null;
            }
        }

        if (asString != null) {
            String pl = asString.toLowerCase();
            if (pl.contains("neoforge") && set.add("neoforge")) {
                getLogger().info(player.getName() + " reported client: neoforge (via payload)");
            }
            if (pl.contains("quilt") && set.add("quilt")) {
                getLogger().info(player.getName() + " reported client: quilt (via payload)");
            }
            if ((pl.contains("fabric") || pl.contains("fabricloader") || pl.contains("fabric-mod")) && !pl.contains("quilt") && set.add("fabric")) {
                getLogger().info(player.getName() + " reported client: fabric (via payload)");
            }
            if (!(pl.contains("neoforge")) && (pl.contains("forge") || pl.contains("fml") || pl.contains("minecraftforge")) && set.add("forge")) {
                getLogger().info(player.getName() + " reported client: forge/fml (via payload)");
            }
        }
    }

    @Override
    public ProxyListener getProxyListener() {
        return proxyListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("axmaintenence")) cmd = "axmaintenance";

        if (cmd.equals("axinvincible")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            Player p = (Player) sender;
            if (!p.hasPermission("axior.admin")) {
                p.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length == 0) {
                p.sendMessage(ChatColor.YELLOW + "Usage: /axinvincible <on|off>");
                return true;
            }
            String sub = args[0].toLowerCase();
            if (sub.equals("on") || sub.equals("true")) { 
                boolean added = invinciblePlayers.add(p.getUniqueId()); 
                try { 
                    PotionEffectType resistanceType = PotionEffectType.getByName("RESISTANCE");
                    if (resistanceType != null) {
                        PotionEffect resistanceEffect = new PotionEffect(resistanceType, Integer.MAX_VALUE, 255, true, false); 
                        p.addPotionEffect(resistanceEffect, true);
                    } 
                } catch (Exception ignored) { 
                    p.sendMessage(ChatColor.RED + "Error applying potion effect."); 
                } 
                try { 
                    PotionEffect saturationEffect = new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 255, true, false); 
                    p.addPotionEffect(saturationEffect, true); 
                } catch (Exception ignored) {} 
                saveMutesToConfig(); 
                if (added) { 
                    p.sendMessage(ChatColor.GREEN + "Invincibility enabled."); 
                } else { 
                    p.sendMessage(ChatColor.YELLOW + "You are already invincible."); 
                } 
                return true; 
            } else if (sub.equals("off") || sub.equals("false")) {
                boolean removed = invinciblePlayers.remove(p.getUniqueId());
                try {
                    PotionEffectType resistanceType = PotionEffectType.getByName("RESISTANCE");
                    if (resistanceType != null) {
                        p.removePotionEffect(resistanceType);
                    }
                } catch (Exception ignored) {}
                try {
                    p.removePotionEffect(PotionEffectType.SATURATION);
                } catch (Exception ignored) {}
                saveMutesToConfig();
                if (removed) {
                    p.sendMessage(ChatColor.GREEN + "Invincibility disabled.");
                } else {
                    p.sendMessage(ChatColor.YELLOW + "You were not invincible.");
                }
                return true;
            } else {
                p.sendMessage(ChatColor.YELLOW + "Usage: /axinvincible <on|off>");
                return true;
            }
        }

        if (cmd.equals("axmaintenance")) {
            if (!sender.hasPermission("axior.owner")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axmaintenance <on|off>");
                return true;
            }
            String sub = args[0].toLowerCase();
            if (sub.equals("on") || sub.equals("true")) {
                if (maintenanceMode) {
                    sender.sendMessage(ChatColor.YELLOW + "Maintenance mode is already enabled.");
                    return true;
                }
                maintenanceMode = true;
                saveMaintenanceToConfig();
                enforceMaintenanceNow(null);
                sender.sendMessage(ChatColor.GREEN + "Maintenance mode enabled. Non-admin players were kicked.");
                getLogger().info(sender.getName() + " enabled maintenance mode.");
                return true;
            } else if (sub.equals("off") || sub.equals("false")) {
                if (!maintenanceMode) {
                    sender.sendMessage(ChatColor.YELLOW + "Maintenance mode is not enabled.");
                    return true;
                }
                maintenanceMode = false;
                saveMaintenanceToConfig();
                sender.sendMessage(ChatColor.GREEN + "Maintenance mode disabled. Players may join again.");
                getLogger().info(sender.getName() + " disabled maintenance mode.");
                return true;
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axmaintenance <on|off>");
                return true;
            }
        }

        if (cmd.equals("axban")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axban <username> <duration> <type> [reason]");
                return true;
            }

            String targetName = args[0];
            String durationStr = args[1];
            String type = args[2].toLowerCase();
            String reason = (args.length >= 4) ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Banned by an operator";

            if (sender instanceof Player) {
                Player senderPlayer = (Player) sender;
                if (senderPlayer.getName().equalsIgnoreCase(targetName)) {
                    sender.sendMessage(ChatColor.YELLOW + "You cannot ban yourself.");
                    return true;
                }

                if (senderPlayer.isOp()) {
                    OfflinePlayer potentialTarget = Bukkit.getOfflinePlayer(targetName);
                    if (potentialTarget != null && potentialTarget.isOp()) {
                        sender.sendMessage(ChatColor.YELLOW + "Operators cannot ban other operators.");
                        return true;
                    }
                }

                Player targetOnlineCheck = Bukkit.getPlayerExact(targetName);
                if (targetOnlineCheck != null) {
                    if (targetOnlineCheck.isOp() || (targetOnlineCheck.hasPermission("axior.mod") || targetOnlineCheck.hasPermission("axior.admin") || targetOnlineCheck.hasPermission("axior.owner"))) {
                        sender.sendMessage(ChatColor.YELLOW + "You cannot ban another operator or a player with ban permission.");
                        return true;
                    }
                } else {
                    OfflinePlayer offlineTargetCheck = Bukkit.getOfflinePlayer(targetName);
                    if (offlineTargetCheck != null && offlineTargetCheck.isOp()) {
                        sender.sendMessage(ChatColor.YELLOW + "You cannot ban another operator.");
                        return true;
                    }
                }

            }

            long days;
            try {
                days = Long.parseLong(durationStr);
                if (days < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "<duration> must be a non-negative integer (days). Use 0 for permanent.");
                return true;
            }

            Date expires = null;
            if (days > 0) {
                expires = new Date(System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L);
            }

            if (type.equals("normal") || type.equals("name") || type.equals("player")) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                String nameToBan = target.getName() != null ? target.getName() : targetName;
                BanList nameList = Bukkit.getBanList(BanList.Type.NAME);
                if (nameList.isBanned(nameToBan)) {
                    sender.sendMessage(ChatColor.RED + "This user is already banned!");
                    return true;
                }
                nameList.addBan(nameToBan, reason, expires, sender.getName());
                sender.sendMessage("Player '" + nameToBan + "' has been banned (type: name)" + (days == 0 ? " permanently." : " for " + days + " day(s)."));
                getLogger().info("[Ban] Player '" + nameToBan + "' was banned by '" + sender.getName() + "' (type: name)" + (days == 0 ? " permanently." : " for " + days + " day(s)."));

                if (target.isOnline()) {
                    Player p = Bukkit.getPlayer(target.getUniqueId());
                        if (p != null) {
                        p.kickPlayer(ChatColor.RED + "You have been banned: " + reason);
                    }
                }

                try {
                    String webhook = getConfig().getString("discord-webhook-url", "");
                    DiscordFolia.sendReportAsync(getPlugin(), webhook, nameToBan, sender.getName(), "Banned (name) - " + reason, System.currentTimeMillis());
                } catch (Exception e) {
                    getLogger().warning("Failed to dispatch Discord webhook for ban: " + e.getMessage());
                }

                return true;
            } else if (type.equals("ip") || type.equals("ipban") || type.equals("address") || type.equals("addr")) {
                Player p = Bukkit.getPlayerExact(targetName);
                if (p == null) {
                    sender.sendMessage(ChatColor.RED + "Player must be online to perform an IP ban (so we can read their current IP).");
                    return true;
                }

                InetSocketAddress addr = p.getAddress();
                if (addr == null || addr.getAddress() == null) {
                    sender.sendMessage(ChatColor.RED + "Could not determine player's IP address.");
                    return true;
                }

                String ipAddress = addr.getAddress().getHostAddress();
                BanList ipList = Bukkit.getBanList(BanList.Type.IP);
                if (ipList.isBanned(ipAddress)) {
                    sender.sendMessage(ChatColor.RED + "This IP is already banned!");
                    return true;
                }
                ipList.addBan(ipAddress, reason, expires, sender.getName());
                sender.sendMessage("IP '" + ipAddress + "' has been banned (type: ip)" + (days == 0 ? " permanently." : " for " + days + " day(s)."));
                getLogger().info("[Ban] IP '" + ipAddress + "' (player: '" + targetName + "') was banned by '" + sender.getName() + "' (type: ip)" + (days == 0 ? " permanently." : " for " + days + " day(s)."));

                p.kickPlayer(ChatColor.RED + "You have been IP banned: " + reason);

                try {
                    String webhook = getConfig().getString("discord-webhook-url", "");
                    DiscordFolia.sendReportAsync(getPlugin(), webhook, ipAddress + " (player: " + targetName + ")", sender.getName(), "Banned (ip) - " + reason, System.currentTimeMillis());
                } catch (Exception e) {
                    getLogger().warning("Failed to dispatch Discord webhook for ip ban: " + e.getMessage());
                }

                return true;
            } else {
                sender.sendMessage("<type> must be 'normal' or 'ip'.");
                return true;
            }
        } else if (cmd.equals("axpardon")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axpardon <username>");
                return true;
            }

            String targetName = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            BanList nameBanList = Bukkit.getBanList(BanList.Type.NAME);
            BanList ipBanList = Bukkit.getBanList(BanList.Type.IP);

            boolean namePardoned = false;
            boolean ipPardoned = false;

            if (nameBanList.isBanned(target.getName())) {
                nameBanList.pardon(target.getName());
                namePardoned = true;
            }

            Player online = Bukkit.getPlayerExact(targetName);
            if (online != null) {
                InetSocketAddress addr = online.getAddress();
                if (addr != null && addr.getAddress() != null) {
                    String ip = addr.getAddress().getHostAddress();
                    if (ipBanList.isBanned(ip)) {
                        ipBanList.pardon(ip);
                        ipPardoned = true;
                    }
                }
            } else {
                if (ipBanList.isBanned(targetName)) {
                    ipBanList.pardon(targetName);
                    ipPardoned = true;
                }
            }

            if (namePardoned || ipPardoned) {
                StringBuilder msg = new StringBuilder("Pardoned:");
                if (namePardoned) msg.append(" name");
                if (ipPardoned) msg.append(" ip");
                sender.sendMessage(msg.toString());
                try {
                    String webhook = getConfig().getString("discord-webhook-url", "");
                    if (namePardoned) {
                        DiscordFolia.sendReportAsync(getPlugin(), webhook, target.getName(), sender.getName(), "Pardoned (name)", System.currentTimeMillis());
                    }
                    if (ipPardoned) {
                        DiscordFolia.sendReportAsync(getPlugin(), webhook, targetName + " (IP pardoned)", sender.getName(), "Pardoned (ip)", System.currentTimeMillis());
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to dispatch Discord webhook for pardon: " + e.getMessage());
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "No name or IP ban found for '" + targetName + "'.");
            }

            return true;
        }

        if (cmd.equals("ping")) {
            if (!sender.hasPermission("axior.general")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("Pong!");
                return true;
            }
            final CommandSender cs = sender;
            final long start = System.nanoTime();
            runTaskLater(getPlugin(), () -> {
                try {
                    long elapsed = (System.nanoTime() - start) / 1_000_000L;
                    cs.sendMessage(ChatColor.GREEN + "Pong! Response time: " + elapsed + " ms");
                } catch (Throwable t) {
                    try { cs.sendMessage("Pong!"); } catch (Throwable ignored) {}
                }
            }, 1L);
            return true;
        }

        if (cmd.equals("coordinates")) {
            if (!sender.hasPermission("axior.general")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players may use /coordinates.");
                return true;
            }
            Player pl = (Player) sender;
            org.bukkit.Location loc = pl.getLocation();
            String worldName = (loc.getWorld() != null) ? loc.getWorld().getName() : "unknown";
                String coordMsg = ChatColor.AQUA + "Coordinates: "
                    + ChatColor.YELLOW + "X: " + ChatColor.RESET + String.format("%.3f", loc.getX())
                    + " " + ChatColor.YELLOW + "Y: " + ChatColor.RESET + String.format("%.3f", loc.getY())
                    + " " + ChatColor.YELLOW + "Z: " + ChatColor.RESET + String.format("%.3f", loc.getZ())
                    + " (World: " + worldName + ")";
                pl.sendMessage(coordMsg);
            try {
                pl.sendMessage(ChatColor.GRAY + "Block: " + ChatColor.YELLOW + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                pl.sendMessage(ChatColor.GRAY + "Facing: " + ChatColor.YELLOW + String.format("yaw=%.1f pitch=%.1f", loc.getYaw(), loc.getPitch()));
            } catch (Throwable ignored) {}
            return true;
        }

        if (cmd.equals("whoami")) {
            if (!sender.hasPermission("axior.general")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players may use /whoami.");
                return true;
            }
            Player pl = (Player) sender;
            try {
                pl.sendMessage(ChatColor.AQUA + "-- Your account info --");
                pl.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + pl.getName());
                pl.sendMessage(ChatColor.YELLOW + "UUID: " + ChatColor.WHITE + pl.getUniqueId().toString());
                try {
                    InetSocketAddress addr = pl.getAddress();
                    if (addr != null && addr.getAddress() != null) {
                        pl.sendMessage(ChatColor.YELLOW + "IP: " + ChatColor.WHITE + addr.getAddress().getHostAddress());
                    } else {
                        pl.sendMessage(ChatColor.YELLOW + "IP: " + ChatColor.WHITE + "unknown");
                    }
                } catch (Throwable ignored) {
                    pl.sendMessage(ChatColor.YELLOW + "IP: " + ChatColor.WHITE + "unknown");
                }
                pl.sendMessage(ChatColor.YELLOW + "Gamemode: " + ChatColor.WHITE + pl.getGameMode().name());
                pl.sendMessage(ChatColor.YELLOW + "Health: " + ChatColor.WHITE + String.format("%.1f", pl.getHealth()) + ChatColor.YELLOW + "  Food: " + ChatColor.WHITE + pl.getFoodLevel());
                try {
                    long ticks = pl.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                    long seconds = ticks / 20L;
                    long hrs = seconds / 3600L;
                    long mins = (seconds % 3600L) / 60L;
                    long secs = seconds % 60L;
                    pl.sendMessage(ChatColor.YELLOW + "Playtime: " + ChatColor.WHITE + String.format("%dh %dm %ds", hrs, mins, secs));
                } catch (Throwable ignored) {
                    pl.sendMessage(ChatColor.YELLOW + "Playtime: " + ChatColor.WHITE + "unknown");
                }
            } catch (Throwable t) {
                sender.sendMessage(ChatColor.RED + "Failed to gather your info: " + t.getMessage());
            }
            return true;
        }

        else if (cmd.equals("playtime")) {
            if (!sender.hasPermission("axior.general")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /playtime <player>");
                    return true;
                }
                Player pl = (Player) sender;
                try {
                    long ticks = pl.getStatistic(Statistic.PLAY_ONE_MINUTE);
                    long seconds = ticks / 20L;
                    long hrs = seconds / 3600L;
                    long mins = (seconds % 3600L) / 60L;
                    long secs = seconds % 60L;
                    sender.sendMessage(ChatColor.AQUA + "Your playtime: " + ChatColor.WHITE + String.format("%dh %dm %ds", hrs, mins, secs));
                } catch (Throwable t) {
                    sender.sendMessage(ChatColor.RED + "Failed to retrieve playtime: " + t.getMessage());
                }
                return true;
            } else if (args.length == 1) {
                String targetName = args[0];
                Player online = Bukkit.getPlayerExact(targetName);
                if (online != null) {
                    try {
                        long ticks = online.getStatistic(Statistic.PLAY_ONE_MINUTE);
                        long seconds = ticks / 20L;
                        long hrs = seconds / 3600L;
                        long mins = (seconds % 3600L) / 60L;
                        long secs = seconds % 60L;
                        sender.sendMessage(ChatColor.AQUA + "Playtime for " + ChatColor.WHITE + online.getName() + ChatColor.AQUA + ": " + ChatColor.WHITE + String.format("%dh %dm %ds", hrs, mins, secs));
                    } catch (Throwable t) {
                        sender.sendMessage(ChatColor.RED + "Failed to retrieve playtime: " + t.getMessage());
                    }
                } else {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
                    if (off == null || !off.hasPlayedBefore()) {
                        sender.sendMessage(ChatColor.YELLOW + "Player not found or never played: " + targetName);
                    } else {
                        Date d = new Date(off.getLastPlayed());
                        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        sender.sendMessage(ChatColor.AQUA + "Player " + ChatColor.WHITE + off.getName() + ChatColor.AQUA + " is offline. Last played: " + ChatColor.WHITE + fmt.format(d));
                        sender.sendMessage(ChatColor.GRAY + "Playtime for offline players is not available while they are offline.");
                    }
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /playtime [player]");
                return true;
            }
        }

        else if (cmd.equals("seen")) {
            if (!sender.hasPermission("axior.general")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /seen <player>");
                return true;
            }
            String targetName = args[0];
            Player online = Bukkit.getPlayerExact(targetName);
            if (online != null) {
                org.bukkit.Location loc = online.getLocation();
                String worldName = (loc.getWorld() != null) ? loc.getWorld().getName() : "unknown";
                sender.sendMessage(ChatColor.GREEN + targetName + " is currently online.");
                sender.sendMessage(ChatColor.YELLOW + "World: " + ChatColor.WHITE + worldName + ChatColor.YELLOW + " X: " + ChatColor.WHITE + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ());
                return true;
            } else {
                OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
                if (off == null || !off.hasPlayedBefore()) {
                    sender.sendMessage(ChatColor.YELLOW + "Player not found or never played: " + targetName);
                    return true;
                }
                Date d = new Date(off.getLastPlayed());
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sender.sendMessage(ChatColor.AQUA + "Player " + ChatColor.WHITE + (off.getName() != null ? off.getName() : targetName) + ChatColor.AQUA + " is offline. Last seen: " + ChatColor.WHITE + fmt.format(d));
                return true;
            }
        }

        else if (cmd.equals("axinventory")) {
            if (!sender.hasPermission("axior.mod")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players may use /axinventory to open a GUI.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axinventory <player>");
                return true;
            }
            Player viewer = (Player) sender;
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found or not online: " + args[0]);
                return true;
            }

            Inventory inv = Bukkit.createInventory(null, 54, "Inventory of " + target.getName());

            try {
                ItemStack[] contents = target.getInventory().getContents();
                for (int i = 0; i < contents.length && i < 36; i++) {
                    inv.setItem(i, contents[i]);
                }

                ItemStack[] armor = target.getInventory().getArmorContents();
                if (armor != null) {
                    if (armor.length > 0 && armor[0] != null) inv.setItem(45, armor[0]);
                    if (armor.length > 1 && armor[1] != null) inv.setItem(46, armor[1]);
                    if (armor.length > 2 && armor[2] != null) inv.setItem(47, armor[2]);
                    if (armor.length > 3 && armor[3] != null) inv.setItem(48, armor[3]);
                }

                ItemStack off = target.getInventory().getItemInOffHand();
                if (off != null) inv.setItem(53, off);

                try {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta sm = (SkullMeta) head.getItemMeta();
                    if (sm != null) {
                        sm.setOwningPlayer(target);
                        sm.setDisplayName(target.getName());
                        head.setItemMeta(sm);
                        inv.setItem(52, head);
                    }
                } catch (Exception ignored) {}

                viewer.openInventory(inv);
                viewer.sendMessage(ChatColor.GREEN + "Opened inventory of " + target.getName() + ".");
                try { getLogger().info(sender.getName() + " opened inventory of " + target.getName()); } catch (Exception ignored) {}
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + "Failed to open inventory: " + ex.getMessage());
                getLogger().warning("axinventory failed: " + ex.getMessage());
            }

            return true;
        }

        else if (cmd.equals("axwarn")) {
            if (!sender.hasPermission("axior.mod")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axwarn <username> [reason]");
                return true;
            }

            String targetName = args[0];
            String reason = (args.length >= 2) ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

            if (sender instanceof Player) {
                Player senderPlayer = (Player) sender;
                if (senderPlayer.getName().equalsIgnoreCase(targetName)) {
                    sender.sendMessage(ChatColor.YELLOW + "You cannot warn yourself.");
                    return true;
                }
                OfflinePlayer potentialTarget = Bukkit.getOfflinePlayer(targetName);
                if (potentialTarget != null && potentialTarget.isOp()) {
                    sender.sendMessage(ChatColor.YELLOW + "You cannot warn another operator.");
                    return true;
                }
            }

            Player online = Bukkit.getPlayerExact(targetName);
            UUID targetUuid;
            String nameToUse = targetName;
            if (online != null) {
                targetUuid = online.getUniqueId();
                nameToUse = online.getName();
            } else {
                OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
                targetUuid = off.getUniqueId();
                if (off.getName() != null) nameToUse = off.getName();
            }

            int newCount = warningsCount.getOrDefault(targetUuid, 0) + 1;
            warningsCount.put(targetUuid, newCount);
            saveMutesToConfig();

            String msg = "Player '" + nameToUse + "' warned (" + newCount + ")" + (reason != null && !reason.isEmpty() ? ": " + reason : "");
            sender.sendMessage(ChatColor.GREEN + msg);
            getLogger().info("[Warn] " + nameToUse + " warned by " + sender.getName() + " (count=" + newCount + ") Reason: " + reason);

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != null && (p.hasPermission("axior.mod") || p.hasPermission("axior.admin") || p.hasPermission("axior.owner"))) {
                    p.sendMessage(ChatColor.YELLOW + "[Warn] " + nameToUse + " warned by " + sender.getName() + " (" + newCount + ")");
                }
            }

            try {
                String webhook = getConfig().getString("discord-webhook-url", "");
                DiscordFolia.sendReportAsync(getPlugin(), webhook, nameToUse, sender.getName(), "Warned - " + reason, System.currentTimeMillis());
            } catch (Exception e) {
                getLogger().warning("Failed to dispatch Discord webhook for warn: " + e.getMessage());
            }

            int threshold = getConfig().getInt("warningsToBan", 0);
            if (threshold > 0 && newCount >= threshold) {
                try {
                    BanList nameList = Bukkit.getBanList(BanList.Type.NAME);
                    String banReason = "Auto-banned after " + newCount + " warnings. Reason: " + reason;
                    nameList.addBan(nameToUse, banReason, (Date) null, "Axior-Auto");
                    if (online != null) {
                        online.kickPlayer(ChatColor.RED + "You have been banned: " + banReason);
                    }
                    getLogger().info("[AutoBan] " + nameToUse + " auto-banned after " + newCount + " warnings.");
                    try {
                        String webhook = getConfig().getString("discord-webhook-url", "");
                        DiscordFolia.sendReportAsync(getPlugin(), webhook, nameToUse, "Axior-Auto", "Banned (auto) - " + reason, System.currentTimeMillis());
                    } catch (Exception ex) {
                        getLogger().warning("Failed to dispatch Discord webhook for auto-ban: " + ex.getMessage());
                    }
                    warningsCount.remove(targetUuid);
                    saveMutesToConfig();
                } catch (Exception ex) {
                    getLogger().warning("Failed to auto-ban player after warnings: " + ex.getMessage());
                }
            }

            return true;
        }
        else if (cmd.equals("report")) {
            if (args.length < 1) {
                sender.sendMessage("Usage: /report <username> [reason]");
                return true;
            }

            String targetName = args[0];
            Player reported = Bukkit.getPlayerExact(targetName);
            if (reported == null) {
                sender.sendMessage("Player must be online to report.");
                return true;
            }
            String reason = (args.length >= 2) ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

            if (sender instanceof Player) {
                Player reporter = (Player) sender;
                if (reporter.getUniqueId().equals(reported.getUniqueId())) {
                    reporter.sendMessage(ChatColor.YELLOW + "You cannot report yourself.");
                    return true;
                }
                if (reported.isOp() || (reported.hasPermission("axior.mod") || reported.hasPermission("axior.admin") || reported.hasPermission("axior.owner"))) {
                    reporter.sendMessage(ChatColor.YELLOW + "You cannot report server staff or operators.");
                    return true;
                }
                if (vanished.contains(reported.getUniqueId())) {
                    reporter.sendMessage(ChatColor.YELLOW + "You cannot report that player right now.");
                    return true;
                }

                UUID rid = reporter.getUniqueId();
                long now = System.currentTimeMillis();
                Long last = lastReportTime.get(rid);
                if (last != null && reportCooldownMillis > 0 && now - last < reportCooldownMillis) {
                    long remainingMs = reportCooldownMillis - (now - last);
                    long remainingMin = (remainingMs + 59999L) / 60000L;
                    reporter.sendMessage(ChatColor.YELLOW + "You must wait " + remainingMin + " more minute(s) before sending another report.");
                    return true;
                }
                lastReportTime.put(rid, now);
            }

                getLogger().info("[Report] " + targetName + " has been reported by " + sender.getName() + " - reason: " + reason);

                String alert = targetName + " has been reported by " + sender.getName() + "! Reason: " + reason;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p != null && (p.hasPermission("axior.mod") || p.hasPermission("axior.admin") || p.hasPermission("axior.owner"))) {
                        p.sendMessage(ChatColor.YELLOW + alert);
                    }
                }

                sender.sendMessage("Report submitted for " + targetName + ". Wait for an operator to investigate.");
            if (adminConfig != null && adminFile != null) {
                long ts = System.currentTimeMillis();
                adminConfig = YamlConfiguration.loadConfiguration(adminFile);
                String base = "reports." + ts + ".";
                adminConfig.set(base + "reported", targetName);
                adminConfig.set(base + "reporter", sender.getName());
                adminConfig.set(base + "reason", reason);
                    try {
                        adminConfig.save(adminFile);
                    } catch (IOException ex) {
                        getLogger().warning("Failed to save report to admin.yml: " + ex.getMessage());
                    }

                    try {
                        String webhook = getConfig().getString("discord-webhook-url", "");
                        DiscordFolia.sendReportAsync(getPlugin(), webhook, targetName, sender.getName(), reason, ts);
                    } catch (Exception e) {
                        getLogger().warning("Failed to dispatch Discord webhook task: " + e.getMessage());
                    }
            }
            return true;
        }

        else if (cmd.equals("axmute")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axmute <username> <duration_minutes> [reason]");
                return true;
            }

            String targetName = args[0];
            String durationStr = args[1];
            String reason = (args.length >= 3) ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Muted by an operator";

            if (sender instanceof Player) {
                Player senderPlayer = (Player) sender;
                if (senderPlayer.getName().equalsIgnoreCase(targetName)) {
                    sender.sendMessage(ChatColor.YELLOW + "You cannot mute yourself.");
                    return true;
                }

                if (senderPlayer.isOp()) {
                    OfflinePlayer potentialTarget = Bukkit.getOfflinePlayer(targetName);
                    if (potentialTarget != null && potentialTarget.isOp()) {
                        sender.sendMessage(ChatColor.YELLOW + "Operators cannot mute other operators.");
                        return true;
                    }
                }

                Player targetOnlineCheck = Bukkit.getPlayerExact(targetName);
                if (targetOnlineCheck != null) {
                    if (targetOnlineCheck.isOp() || (targetOnlineCheck.hasPermission("axior.mod") || targetOnlineCheck.hasPermission("axior.admin") || targetOnlineCheck.hasPermission("axior.owner"))) {
                        sender.sendMessage(ChatColor.YELLOW + "You cannot mute another operator or a player with mute permission.");
                        return true;
                    }
                } else {
                    OfflinePlayer offlineTargetCheck = Bukkit.getOfflinePlayer(targetName);
                    if (offlineTargetCheck != null && offlineTargetCheck.isOp()) {
                        sender.sendMessage(ChatColor.YELLOW + "You cannot mute another operator.");
                        return true;
                    }
                }
            }

            int minutes;
            try {
                minutes = Integer.parseInt(durationStr);
                if (minutes < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "<duration_minutes> must be a non-negative integer.");
                return true;
            }

            long expiry = (minutes == 0) ? Long.MAX_VALUE : System.currentTimeMillis() + minutes * 60L * 1000L;

            Player online = Bukkit.getPlayerExact(targetName);
            UUID targetUuid;
            if (online != null) {
                targetUuid = online.getUniqueId();
            } else {
                OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
                targetUuid = off.getUniqueId();
            }

            muteExpiry.put(targetUuid, expiry);
            muteReason.put(targetUuid, reason);
            muteSetBy.put(targetUuid, sender.getName());

            String timeDescShort = (minutes == 0) ? "permanently" : (minutes + " minute(s)");
            String timeDescLog = (minutes == 0) ? "permanently" : ("for " + minutes + " minute(s)");

            sender.sendMessage("Player '" + targetName + "' has been muted " + timeDescShort + ". Reason: " + reason);
            getLogger().info("[Mute] Player '" + targetName + "' was muted by '" + sender.getName() + "' " + timeDescLog + ". Reason: " + reason);

            if (online != null) {
                online.sendMessage(ChatColor.RED + "You have been muted " + timeDescShort + ". Reason: " + reason);
            }

            saveMutesToConfig();

            try {
                String webhook = getConfig().getString("discord-webhook-url", "");
                DiscordFolia.sendReportAsync(getPlugin(), webhook, targetName, sender.getName(), "Muted " + timeDescLog + " - " + reason, System.currentTimeMillis());
            } catch (Exception e) {
                getLogger().warning("Failed to dispatch Discord webhook for mute: " + e.getMessage());
            }

            return true;
        }

        else if (cmd.equals("axunmute")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axunmute <username>");
                return true;
            }

            String targetName = args[0];

            Player online = Bukkit.getPlayerExact(targetName);
            UUID targetUuid;
            if (online != null) {
                targetUuid = online.getUniqueId();
            } else {
                OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
                targetUuid = off.getUniqueId();
            }

            if (!muteExpiry.containsKey(targetUuid)) {
                sender.sendMessage(ChatColor.YELLOW + "Player '" + targetName + "' is not muted.");
                return true;
            }

            muteExpiry.remove(targetUuid);
            muteReason.remove(targetUuid);
            muteSetBy.remove(targetUuid);

            saveMutesToConfig();

            sender.sendMessage("Player '" + targetName + "' has been unmuted.");
            getLogger().info("[Unmute] Player '" + targetName + "' was unmuted by '" + sender.getName() + "'.");

            try {
                String webhook = getConfig().getString("discord-webhook-url", "");
                DiscordFolia.sendReportAsync(getPlugin(), webhook, targetName, sender.getName(), "Unmuted", System.currentTimeMillis());
            } catch (Exception e) {
                getLogger().warning("Failed to dispatch Discord webhook for unmute: " + e.getMessage());
            }

            return true;
        }

        else if (cmd.equals("axinfo")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axinfo <username>");
                return true;
            }

            String targetName = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            String lastLogin;
            if (target.hasPlayedBefore()) {
                Date d = new Date(target.getLastPlayed());
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                lastLogin = fmt.format(d) + " (epoch=" + target.getLastPlayed() + ")";
            } else {
                lastLogin = "never played or unknown";
            }

            String totalPlaytime = "unknown (offline or unavailable)";
            Player online = Bukkit.getPlayerExact(targetName);
            if (online != null) {
                try {
                    long ticks = online.getStatistic(Statistic.PLAY_ONE_MINUTE);
                    long seconds = ticks / 20L;
                    long hrs = seconds / 3600;
                    long mins = (seconds % 3600) / 60;
                    long secs = seconds % 60;
                    totalPlaytime = String.format("%dh %dm %ds", hrs, mins, secs);
                } catch (Exception ignored) {
                    totalPlaytime = "unknown (stat retrieval failed)";
                }
            }

            String ip = "unknown (offline or not available)";
            if (online != null) {
                InetSocketAddress addr = online.getAddress();
                if (addr != null && addr.getAddress() != null) {
                    ip = addr.getAddress().getHostAddress();
                }
            }

            sender.sendMessage("Info for: " + targetName);
            sender.sendMessage("- Last login: " + lastLogin);
            sender.sendMessage("- Total playtime: " + totalPlaytime);
            sender.sendMessage("- IP: " + ip);

            return true;
        }

        else if (cmd.equals("axserverinfo")) {
            if (!sender.hasPermission("axior.owner")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            sender.sendMessage(ChatColor.AQUA + "Server info!");
            sender.sendMessage(ChatColor.YELLOW + "Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
            sender.sendMessage(ChatColor.YELLOW + "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
            sender.sendMessage(ChatColor.YELLOW + "Bukkit: " + Bukkit.getVersion() + " " + Bukkit.getBukkitVersion());
            sender.sendMessage(ChatColor.YELLOW + "Server: " + Bukkit.getServer().getName());
            sender.sendMessage(ChatColor.YELLOW + "Online players: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
            sender.sendMessage(ChatColor.YELLOW + "Online mode: " + Bukkit.getOnlineMode() + "  Port: " + Bukkit.getPort());

            String worldNames = Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.joining(", "));
            sender.sendMessage(ChatColor.YELLOW + "Loaded Worlds (" + Bukkit.getWorlds().size() + "): " + worldNames);

            long cfgMinutes = getConfig().getLong("reportCooldownMinutes", 5L);
            if (cfgMinutes < 0) cfgMinutes = 0L;
            sender.sendMessage(ChatColor.YELLOW + "Report cooldown: " + cfgMinutes + " minute(s)");

            sender.sendMessage(ChatColor.YELLOW + "Detected client entries: " + detectedClients.size());
            sender.sendMessage(ChatColor.YELLOW + "Vanished staff: " + vanished.size());
            sender.sendMessage(ChatColor.YELLOW + "Spy staff: " + spies.size());
            sender.sendMessage(ChatColor.YELLOW + "Frozen players: " + frozenExpiry.size());
            sender.sendMessage(ChatColor.YELLOW + "Muted players: " + muteExpiry.size());

            if (adminFile != null) sender.sendMessage(ChatColor.YELLOW + "Admin data file: " + adminFile.getAbsolutePath());
            return true;
        }

        else if (cmd.equals("axclean")) {
            if (!sender.hasPermission("axior.owner")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=item]");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=arrow]");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=spectral_arrow]");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=experience_orb]");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=falling_block]");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=lingering_potion]");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=evoker_fangs]");
                Bukkit.broadcastMessage(ChatColor.GREEN + "Cleaned!");
                getLogger().info("axclean executed console commands to remove various entities.");
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + "Failed to execute cleanup commands: " + ex.getMessage());
                getLogger().warning("axclean command execution failed: " + ex.getMessage());
            }
            return true;
        }

        else if (cmd.equals("axexecute")) {
            if (!sender.hasPermission("axior.owner")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axexecute <command>");
                return true;
            }

            String commandLine = String.join(" ", args).trim();
            if (commandLine.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axexecute <command>");
                return true;
            }

            commandLine = commandLine.replaceFirst("^/", "").trim();
            String[] partsLine = commandLine.split(" ");
            String root = (partsLine.length > 0 && partsLine[0] != null) ? partsLine[0].replaceFirst("^/", "").toLowerCase() : "";

            String namespacedTarget = null;
            if (root.contains(":")) {
                int idx = root.indexOf(':');
                if (idx >= 0 && idx < root.length() - 1) {
                    namespacedTarget = root.substring(idx + 1);
                }
            }

            java.util.List<String> blocked = getConfig().getStringList("blocked-executions");
            if (blocked != null) {
                for (String b : blocked) {
                    if (b == null) continue;
                    String check = b.trim().toLowerCase();
                    if (check.isEmpty()) continue;
                    if (root.equalsIgnoreCase(check) || (namespacedTarget != null && namespacedTarget.equalsIgnoreCase(check))) {
                        sender.sendMessage(ChatColor.RED + "That command is blocked from /axexecute.");
                        return true;
                    }
                }
            }

            if (partsLine.length > 0 && partsLine[0] != null && partsLine[0].contains(":")) {
                String originalFirst = partsLine[0];
                String replacementFirst = originalFirst.replaceFirst(":", " ");
                try {
                    commandLine = commandLine.replaceFirst(java.util.regex.Pattern.quote(originalFirst), replacementFirst);
                } catch (Throwable ignored) {}
            }

            try {
                boolean ok = Bukkit.dispatchCommand(sender, commandLine);
                if (ok) {
                    sender.sendMessage(ChatColor.GREEN + "Executed: " + ChatColor.WHITE + commandLine);
                    getLogger().info("axexecute by " + sender.getName() + " (as " + (sender instanceof Player ? "player" : "console") + "): " + commandLine);
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Command executed but returned false: " + commandLine);
                    getLogger().info("axexecute returned false for " + sender.getName() + ": " + commandLine);
                }
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + "Failed to execute command: " + ex.getMessage());
                getLogger().warning("axexecute failed: " + ex.getMessage());
            }
            return true;
        }

        else if (cmd.equals("axtp")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axtp <player> <target>");
                return true;
            }
            String sourceName = args[0];
            String targetName = args[1];
            Player source = Bukkit.getPlayerExact(sourceName);
            Player target = Bukkit.getPlayerExact(targetName);
            if (source == null) {
                sender.sendMessage(ChatColor.RED + "Source player not found or not online: " + sourceName);
                return true;
            }
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Target player not found or not online: " + targetName);
                return true;
            }
            source.teleport(target.getLocation());
            sender.sendMessage(ChatColor.GREEN + "Teleported " + source.getName() + " to " + target.getName() + ".");
            try {
                source.sendMessage(ChatColor.GREEN + "You were teleported to " + target.getName() + " by " + sender.getName() + ".");
            } catch (Exception ignored) {}
            getLogger().info(sender.getName() + " used /axtp to teleport " + source.getName() + " -> " + target.getName());
            return true;
        }

        else if (cmd.equals("axworldtp")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players may use /axworldtp to teleport themselves.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axworldtp <world>");
                return true;
            }
            World w = Bukkit.getWorld(args[0]);
            if (w == null) {
                sender.sendMessage(ChatColor.RED + "World not found: " + args[0]);
                return true;
            }
            Player me = (Player) sender;
            org.bukkit.Location spawn = w.getSpawnLocation();
            if (spawn == null) {
                sender.sendMessage(ChatColor.RED + "World spawn is not set for world: " + w.getName());
                return true;
            }

            if (w.getEnvironment() == World.Environment.NETHER) {
                org.bukkit.Location safe = findSafeNetherSpawn(w, spawn);
                if (safe == null) {
                    sender.sendMessage(ChatColor.YELLOW + "Could not find a nearby safe Nether spawn; teleporting to configured spawn instead (use with caution).");
                    try {
                        me.teleport(spawn);
                        sender.sendMessage(ChatColor.GREEN + "Teleported to spawn of world " + w.getName() + ".");
                    } catch (Exception ex) {
                        sender.sendMessage(ChatColor.RED + "Failed to teleport: " + ex.getMessage());
                    }
                } else {
                    try {
                        me.teleport(safe);
                        sender.sendMessage(ChatColor.GREEN + "Teleported to a safe location in Nether world " + w.getName() + ".");
                    } catch (Exception ex) {
                        sender.sendMessage(ChatColor.RED + "Failed to teleport: " + ex.getMessage());
                    }
                }
                return true;
            }

            if (w.getEnvironment() == World.Environment.THE_END) {
                org.bukkit.Location safeEnd = findSafeEndSpawn(w, spawn);
                if (safeEnd == null) {
                    sender.sendMessage(ChatColor.YELLOW + "Could not find a nearby safe End spawn; teleporting to configured spawn instead (use with caution).");
                    try {
                        me.teleport(spawn);
                        sender.sendMessage(ChatColor.GREEN + "Teleported to spawn of world " + w.getName() + ".");
                    } catch (Exception ex) {
                        sender.sendMessage(ChatColor.RED + "Failed to teleport: " + ex.getMessage());
                    }
                } else {
                    try {
                        me.teleport(safeEnd);
                        sender.sendMessage(ChatColor.GREEN + "Teleported to a safe location in End world " + w.getName() + ".");
                    } catch (Exception ex) {
                        sender.sendMessage(ChatColor.RED + "Failed to teleport: " + ex.getMessage());
                    }
                }
                return true;
            }

            try {
                if (w.getEnvironment() == World.Environment.NORMAL) {
                    org.bukkit.Location safeOver = findSafeOverworldSpawn(w, spawn);
                    if (safeOver == null) {
                        sender.sendMessage(ChatColor.YELLOW + "Could not find a nearby safe Overworld spawn; teleporting to configured spawn instead (use with caution).");
                        try {
                            me.teleport(spawn);
                            sender.sendMessage(ChatColor.GREEN + "Teleported to spawn of world " + w.getName() + ".");
                        } catch (Exception ex) {
                            sender.sendMessage(ChatColor.RED + "Failed to teleport: " + ex.getMessage());
                        }
                    } else {
                        try {
                            me.teleport(safeOver);
                            sender.sendMessage(ChatColor.GREEN + "Teleported to a safe location in world " + w.getName() + ".");
                        } catch (Exception ex) {
                            sender.sendMessage(ChatColor.RED + "Failed to teleport: " + ex.getMessage());
                        }
                    }
                } else {
                    me.teleport(spawn);
                    sender.sendMessage(ChatColor.GREEN + "Teleported to spawn of world " + w.getName() + ".");
                }
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + "Failed to teleport: " + ex.getMessage());
            }
            return true;
        }

        else if (cmd.equals("axworldlist")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            sender.sendMessage(ChatColor.AQUA + "Loaded Worlds:");
            for (World w : Bukkit.getWorlds()) {
                sender.sendMessage(ChatColor.YELLOW + "- " + w.getName() + ChatColor.WHITE + " (" + w.getEnvironment().name() + ")");
            }
            return true;
        }

        else if (cmd.equals("axbroadcast")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axbroadcast [message]");
                return true;
            }
            String msg = String.join(" ", args);
            msg = ChatColor.translateAlternateColorCodes('&', msg);
            Bukkit.broadcastMessage(msg);
            return true;
        }

        else if (cmd.equals("axsend")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axsend <player> <server>");
                return true;
            }
            if (proxyListener == null) {
                sender.sendMessage(ChatColor.RED + "ProxyListener is not available. Are you running on a proxy network?");
                return true;
            }
            Player executor = (Player) sender;
            String targetName = args[0];
            String targetServer = args[1];
            
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + targetName);
                return true;
            }
            
            proxyListener.connectOtherPlayer(executor, target.getName(), targetServer);
            sender.sendMessage(ChatColor.GREEN + "Sent " + target.getName() + " to server: " + targetServer);
            getLogger().info(sender.getName() + " sent " + target.getName() + " to server: " + targetServer);
            return true;
        }

        else if (cmd.equals("axserver")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            Player player = (Player) sender;
            if (args.length < 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axserver <servername>");
                return true;
            }
            if (proxyListener == null) {
                sender.sendMessage(ChatColor.RED + "ProxyListener is not available. Are you running on a proxy network?");
                return true;
            }
            String targetServer = args[0];
            proxyListener.connectPlayer(player, targetServer);
            player.sendMessage(ChatColor.GREEN + "Connecting to: " + targetServer);
            return true;
        }

        else if (cmd.equals("axnetworkkick")) {
            if (!sender.hasPermission("axior.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axnetworkkick <player> [reason]");
                return true;
            }
            if (proxyListener == null) {
                sender.sendMessage(ChatColor.RED + "ProxyListener is not available. Are you running on a proxy network?");
                return true;
            }
            Player executor = (Player) sender;
            String targetName = args[0];
            String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Kicked from network";
            
            proxyListener.kickPlayer(executor, targetName, reason);
            sender.sendMessage(ChatColor.GREEN + "Network kick sent for: " + targetName);
            getLogger().info(sender.getName() + " kicked " + targetName + " from network. Reason: " + reason);
            return true;
        }

        else if (cmd.equals("axserverlist")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            if (proxyListener == null) {
                sender.sendMessage(ChatColor.RED + "ProxyListener is not available. Are you running on a proxy network?");
                return true;
            }
            Player player = (Player) sender;
            proxyListener.requestServerList(player);
            sender.sendMessage(ChatColor.YELLOW + "Requesting server list from proxy... Check console for response.");
            return true;
        }

        else if (cmd.equals("axreports")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            int limit = 10;
            if (args.length >= 1) {
                try {
                    limit = Integer.parseInt(args[0]);
                    if (limit < 1) limit = 1;
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number for limit.");
                    return true;
                }
            }

            if (adminConfig == null || adminFile == null) {
                sender.sendMessage(ChatColor.YELLOW + "No admin.yml available.");
                return true;
            }
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            sender.sendMessage(ChatColor.YELLOW + "Recent reports (showing up to " + limit + "): ");
            if (adminConfig.getConfigurationSection("reports") == null) {
                sender.sendMessage(ChatColor.YELLOW + "No reports found.");
                return true;
            }
            List<Long> timestamps = adminConfig.getConfigurationSection("reports").getKeys(false).stream().map(k -> {
                try { return Long.parseLong(k); } catch (NumberFormatException nfe) { return 0L; }
            }).filter(t -> t > 0L).sorted((a,b) -> Long.compare(b,a)).collect(Collectors.toList());
            int shown = 0;
            for (Long ts : timestamps) {
                if (shown >= limit) break;
                String reported = adminConfig.getString("reports." + ts + ".reported", "unknown");
                String reporter = adminConfig.getString("reports." + ts + ".reporter", "unknown");
                String reason = adminConfig.getString("reports." + ts + ".reason", "No reason provided");
                String time = fmt.format(new Date(ts));
                sender.sendMessage(ChatColor.WHITE + time + " - " + reported + " reported by " + reporter + " - " + reason);
                shown++;
            }
            if (timestamps.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No reports found.");
            }

            return true;
        }

        else if (cmd.equals("axreload")) {
            if (!sender.hasPermission("axior.owner")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            getPlugin().reloadConfig();

            loadBannedWordsFromConfig();

            if (adminFile != null) {
                adminConfig = YamlConfiguration.loadConfiguration(adminFile);
                ensureAdminConfig();
                loadMutesFromConfig();
                loadFrozenFromConfig();
                loadVanishedFromConfig();
                loadSpiesFromConfig();
                loadWarningsFromConfig();
                loadInvincibleFromConfig();
            }

            sender.sendMessage(ChatColor.GREEN + "Axior: configuration reloaded.");
            getLogger().info("Axior: configuration reloaded by " + sender.getName());
            return true;
        }
        else if (cmd.equals("axfreeze")) {
            if (args.length != 2) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axfreeze <username> <duration_minutes>");
                return true;
            }
            if (!sender.hasPermission("axior.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            String targetName = args[0];
            if (sender instanceof Player) {
                Player senderPlayer = (Player) sender;
                if (senderPlayer.getName().equalsIgnoreCase(targetName)) {
                    sender.sendMessage(ChatColor.YELLOW + "You cannot freeze yourself.");
                    return true;
                }
            }
            Player target = Bukkit.getPlayerExact(targetName);
            OfflinePlayer off = null;
            UUID targetUuid;
            if (target != null) {
                targetUuid = target.getUniqueId();
            } else {
                off = Bukkit.getOfflinePlayer(targetName);
                targetUuid = off.getUniqueId();
            }

            if (sender instanceof Player) {
                Player senderPlayer = (Player) sender;
                if (senderPlayer.getUniqueId().equals(targetUuid)) {
                    sender.sendMessage(ChatColor.YELLOW + "You cannot freeze yourself.");
                    return true;
                }
            }

            long expiry;
            try {
                int minutes = Integer.parseInt(args[1]);
                if (minutes < 0) throw new NumberFormatException();
                expiry = (minutes == 0) ? Long.MAX_VALUE : System.currentTimeMillis() + minutes * 60L * 1000L;
            } catch (NumberFormatException nfe) {
                sender.sendMessage(ChatColor.RED + "<duration_minutes> must be a non-negative integer. Use 0 for permanent.");
                return true;
            }

            if (isFrozen(targetUuid)) {
                sender.sendMessage(ChatColor.YELLOW + "Player '" + targetName + "' is already frozen.");
                return true;
            }

            frozenExpiry.put(targetUuid, expiry);
            frozenSetAt.put(targetUuid, System.currentTimeMillis());

            if (target != null) {
                target.setVelocity(new org.bukkit.util.Vector(0,0,0));
                target.teleport(target.getLocation());
                target.sendMessage(ChatColor.RED + "You have been frozen by staff.");
            }

            saveMutesToConfig();

            sender.sendMessage("Player '" + targetName + "' has been frozen.");
            getLogger().info("[Freeze] Player '" + targetName + "' frozen by '" + sender.getName() + "'.");
            return true;
        }
        else if (cmd.equals("axunfreeze")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axunfreeze <username>");
                return true;
            }
            if (!sender.hasPermission("axior.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            String targetName = args[0];
            Player target = Bukkit.getPlayerExact(targetName);
            OfflinePlayer off = null;
            UUID targetUuid;
            if (target != null) {
                targetUuid = target.getUniqueId();
            } else {
                off = Bukkit.getOfflinePlayer(targetName);
                targetUuid = off.getUniqueId();
            }

            if (!isFrozen(targetUuid)) {
                sender.sendMessage(ChatColor.YELLOW + "Player '" + targetName + "' is not frozen.");
                return true;
            }

            frozenExpiry.remove(targetUuid);
            if (target != null) {
                target.sendMessage(ChatColor.GREEN + "You have been unfrozen.");
            }

            saveMutesToConfig();

            sender.sendMessage("Player '" + targetName + "' has been unfrozen.");
            getLogger().info("[Unfreeze] Player '" + targetName + "' unfrozen by '" + sender.getName() + "'.");
            return true;
        }

        else if (cmd.equals("axghost")) {
            if (!sender.hasPermission("axior.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use /axghost to ghost themselves.");
                return true;
            }
            Player player = (Player) sender;
            UUID targetUuid = player.getUniqueId();
            String targetName = player.getName();

            if (vanished.contains(targetUuid)) {
                sender.sendMessage(ChatColor.YELLOW + "You are already ghosted.");
                return true;
            }

            vanished.add(targetUuid);
            vanishedSince.put(targetUuid, System.currentTimeMillis());
            applyVanishToPlayer(player);
            player.sendMessage(ChatColor.GRAY + "You are now in ghost mode.");
            saveMutesToConfig();
            sender.sendMessage("You are now ghosted.");
            getLogger().info("[Ghost] Player '" + targetName + "' ghosted themselves.");
            return true;
        }

        else if (cmd.equals("axunghost")) {
            if (!sender.hasPermission("axior.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use /axunghost to unghost themselves.");
                return true;
            }
            Player player = (Player) sender;
            UUID targetUuid = player.getUniqueId();
            String targetName = player.getName();

            if (!vanished.contains(targetUuid)) {
                sender.sendMessage(ChatColor.YELLOW + "You are not ghosted.");
                return true;
            }

            vanished.remove(targetUuid);
            vanishedSince.remove(targetUuid);
            removeVanishFromPlayer(player);
            player.sendMessage(ChatColor.GREEN + "You are no longer in ghost mode.");
            saveMutesToConfig();
            sender.sendMessage("You have been un-ghosted.");
            getLogger().info("[Unghost] Player '" + targetName + "' unghosted themselves.");
            return true;
        }

        else if (cmd.equals("axspy")) {
            if (!sender.hasPermission("axior.mod")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can toggle spy mode for themselves.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axspy <on|off>");
                return true;
            }
            String mode = args[0].toLowerCase();
            Player pl = (Player) sender;
            UUID id = pl.getUniqueId();
            if (mode.equals("on")) {
                spies.add(id);
                saveMutesToConfig();
                sender.sendMessage(ChatColor.GREEN + "Spy mode enabled. You will see private messages.");
                getLogger().info("[Spy] " + sender.getName() + " enabled spy mode.");
                return true;
            } else if (mode.equals("off")) {
                spies.remove(id);
                saveMutesToConfig();
                sender.sendMessage(ChatColor.GREEN + "Spy mode disabled.");
                getLogger().info("[Spy] " + sender.getName() + " disabled spy mode.");
                return true;
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axspy <on|off>");
                return true;
            }
        }

        else if (cmd.equals("axfrozen")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            int limit = 10;
            if (frozenExpiry.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No players are currently frozen.");
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "Currently frozen players (showing up to " + limit + "): ");
            long now = System.currentTimeMillis();
            int shown = 0;
            List<UUID> sorted = frozenExpiry.keySet().stream()
                    .filter(id -> isFrozen(id))
                    .sorted((a, b) -> Long.compare(frozenSetAt.getOrDefault(b, 0L), frozenSetAt.getOrDefault(a, 0L)))
                    .collect(Collectors.toList());
            for (UUID id : sorted) {
                if (shown >= limit) break;
                long expiry = frozenExpiry.getOrDefault(id, Long.MAX_VALUE);
                String name = Bukkit.getOfflinePlayer(id).getName();
                if (name == null) name = id.toString();
                String timeDesc;
                if (expiry == Long.MAX_VALUE) timeDesc = "permanent";
                else {
                    long remMs = expiry - now;
                    long remMin = (remMs + 59999L) / 60000L;
                    timeDesc = remMin + " minute(s) remaining";
                }
                sender.sendMessage(ChatColor.WHITE + "- " + name + ": " + timeDesc);
                shown++;
            }
            if (shown == 0) sender.sendMessage(ChatColor.YELLOW + "No players are currently frozen.");
            return true;
        }

        else if (cmd.equals("axghosted")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            int limit = 10;
            if (vanished.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No staff are currently ghosted.");
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "Ghosted staff (showing up to " + limit + "): ");
            long now = System.currentTimeMillis();
            int shownG = 0;
            List<UUID> sorted = vanished.stream()
                    .sorted((a,b) -> Long.compare(vanishedSince.getOrDefault(b,0L), vanishedSince.getOrDefault(a,0L)))
                    .collect(Collectors.toList());
            for (UUID id : sorted) {
                if (shownG >= limit) break;
                Player p = Bukkit.getOfflinePlayer(id).getPlayer();
                String name = (p != null) ? p.getName() : Bukkit.getOfflinePlayer(id).getName();
                if (name == null) name = id.toString();
                long since = vanishedSince.getOrDefault(id, 0L);
                long agoMin = (since <= 0L) ? 0L : ((now - since + 59999L) / 60000L);
                String desc = (agoMin <= 0L) ? "just now" : (agoMin + " minute(s) ago");
                sender.sendMessage(ChatColor.WHITE + "- " + name + ": ghosted " + desc);
                shownG++;
            }
            if (shownG == 0) sender.sendMessage(ChatColor.YELLOW + "No staff are currently ghosted.");
            return true;
        }

        else if (cmd.equals("axmutes")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            int limit = 10;
            if (muteExpiry.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No players are currently muted.");
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "Muted players (showing up to " + limit + "): ");
            long now2 = System.currentTimeMillis();
            List<UUID> sortedMutes = muteExpiry.keySet().stream()
                    .sorted((a,b) -> Long.compare(muteExpiry.getOrDefault(b,0L), muteExpiry.getOrDefault(a,0L)))
                    .collect(Collectors.toList());
            int shownM = 0;
            for (UUID id : sortedMutes) {
                if (shownM >= limit) break;
                long expiry = muteExpiry.getOrDefault(id, 0L);
                String name = Bukkit.getOfflinePlayer(id).getName();
                if (name == null) name = id.toString();
                String timeDesc;
                long remMin;
                if (expiry == Long.MAX_VALUE) {
                    timeDesc = "permanent";
                    remMin = -1L;
                } else if (now2 >= expiry) {
                    timeDesc = "expired";
                    remMin = 0L;
                } else {
                    long remMs = expiry - now2;
                    remMin = (remMs + 59999L) / 60000L;
                    timeDesc = remMin + " minute(s) remaining";
                }
                String reason = muteReason.getOrDefault(id, "No reason provided");
                String setBy = muteSetBy.getOrDefault(id, "unknown");
                sender.sendMessage(ChatColor.WHITE + "- " + name + ": " + timeDesc + " (by " + setBy + ") - " + reason);
                shownM++;
            }
            if (shownM == 0) sender.sendMessage(ChatColor.YELLOW + "No players are currently muted.");
            return true;
        }

        else if (cmd.equals("axior")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Axior — Moderation & Admin Tools");
            sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + getDescription().getVersion());
            try {
                sender.sendMessage(ChatColor.YELLOW + "Author(s): " + ChatColor.WHITE + String.join(", ", getDescription().getAuthors()));
            } catch (Exception ignored) {}
            String desc = null;
            try { desc = getDescription().getDescription(); } catch (Exception ignored) {}
            if (desc == null || desc.isEmpty()) desc = "Provides a more advanced system for admin related tools and even more things.";
            sender.sendMessage(ChatColor.YELLOW + "About: " + ChatColor.WHITE + desc);
            sender.sendMessage(ChatColor.YELLOW + "Features: " + ChatColor.WHITE + "ban / mute / report / freeze / vanish / spy / client-detection / admin persistence / etc.");
            if (adminFile != null) {
                sender.sendMessage(ChatColor.YELLOW + "Data file: " + ChatColor.WHITE + adminFile.getName());
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Data file: " + ChatColor.WHITE + "admin.yml (not available)");
            }
            sender.sendMessage(ChatColor.YELLOW + "Key commands: " + ChatColor.WHITE + "/axban /axpardon /axmute /axunmute /report /axreports /axfreeze /axunfreeze /axghost /axunghost /axspy /axserver /axior");
            sender.sendMessage(ChatColor.GRAY + "Use /axserver for server/runtime details.");
            return true;
        }
        else if (cmd.equals("axlocate")) {
            if (!(sender.hasPermission("axior.mod") || sender.hasPermission("axior.admin") || sender.hasPermission("axior.owner"))) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axlocate <player>");
                return true;
            }
            String targetName = args[0];
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found or not online: " + targetName);
                return true;
            }
            org.bukkit.Location loc = target.getLocation();
            String worldName = (loc.getWorld() != null) ? loc.getWorld().getName() : "unknown";
            sender.sendMessage(ChatColor.AQUA + "Location for " + ChatColor.WHITE + target.getName() + ChatColor.AQUA + ":");
            sender.sendMessage(ChatColor.YELLOW + "World: " + ChatColor.WHITE + worldName);
            sender.sendMessage(ChatColor.YELLOW + "X: " + ChatColor.WHITE + String.format("%.3f", loc.getX()) + "  " + ChatColor.YELLOW + "Y: " + ChatColor.WHITE + String.format("%.3f", loc.getY()) + "  " + ChatColor.YELLOW + "Z: " + ChatColor.WHITE + String.format("%.3f", loc.getZ()));
            try { sender.sendMessage(ChatColor.GRAY + "Block: " + ChatColor.YELLOW + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()); } catch (Throwable ignored) {}
            try { sender.sendMessage(ChatColor.GRAY + "Facing: " + ChatColor.YELLOW + String.format("yaw=%.1f pitch=%.1f", loc.getYaw(), loc.getPitch())); } catch (Throwable ignored) {}
            return true;
        }

        else if (cmd.equals("axdisabler")) {
            if (!sender.hasPermission("axior.owner")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axdisabler <plugin> [confirm]");
                sender.sendMessage(ChatColor.YELLOW + "Use /axdisabler list to see persisted force-disabled plugins.");
                return true;
            }
            String target = args[0];
            if (target.equalsIgnoreCase("list")) {
                if (forceDisabled.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No force-disabled plugins configured.");
                } else {
                    sender.sendMessage(ChatColor.AQUA + "Force-disabled plugins:");
                    for (String s : forceDisabled) sender.sendMessage(ChatColor.YELLOW + " - " + s);
                }
                return true;
            }
            if (target.equalsIgnoreCase(getDescription().getName())) {
                sender.sendMessage(ChatColor.RED + "Refusing to disable Axior itself.");
                return true;
            }
            org.bukkit.plugin.Plugin p = Bukkit.getPluginManager().getPlugin(target);
            if (p == null) {
                sender.sendMessage(ChatColor.RED + "Plugin not found: " + target);
                return true;
            }
            boolean alreadyPersisted = forceDisabled.contains(p.getName());
            boolean currentlyEnabled = p.isEnabled();

            if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                try {
                    if (currentlyEnabled) {
                        Bukkit.getPluginManager().disablePlugin(p);
                        getLogger().info("Disabled plugin on request: " + p.getName());
                    } else {
                        getLogger().fine("Plugin already disabled when requested: " + p.getName());
                    }
                } catch (Throwable t) {
                    sender.sendMessage(ChatColor.RED + "Failed to disable plugin: " + t.getMessage());
                    getLogger().warning("Failed to disable plugin " + p.getName() + ": " + t.getMessage());
                    return true;
                }

                boolean added = forceDisabled.add(p.getName());
                saveForceDisabledToConfig();

                if (currentlyEnabled) {
                    sender.sendMessage(ChatColor.GREEN + "Plugin '" + p.getName() + "' disabled and persisted (force-disabled).");
                } else if (alreadyPersisted) {
                    sender.sendMessage(ChatColor.YELLOW + "Plugin '" + p.getName() + "' was already disabled and is persisted in the force-disabled list.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Plugin '" + p.getName() + "' is currently disabled and has been added to the force-disabled list.");
                }
                getLogger().info(sender.getName() + " force-disabled plugin " + p.getName());
                return true;
            } else {
                if (currentlyEnabled && !alreadyPersisted) {
                    sender.sendMessage(ChatColor.YELLOW + "WARNING: This will force-disable the plugin even on reload. Run again with 'confirm'.");
                } else if (!currentlyEnabled && !alreadyPersisted) {
                    sender.sendMessage(ChatColor.YELLOW + "Plugin '" + p.getName() + "' is already disabled. Run with 'confirm' to persist this state across restarts.");
                } else if (alreadyPersisted) {
                    sender.sendMessage(ChatColor.YELLOW + "Plugin '" + p.getName() + "' is already in the force-disabled list.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Run again with 'confirm' to force-disable the plugin and persist that state.");
                }
                return true;
            }
        }

        else if (cmd.equals("axenabler")) {
            if (!sender.hasPermission("axior.owner")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /axenabler <plugin>");
                return true;
            }
            String target = args[0];
            if (target.equalsIgnoreCase(getDescription().getName())) {
                sender.sendMessage(ChatColor.YELLOW + "Axior is always managed separately.");
                return true;
            }
            org.bukkit.plugin.Plugin p = Bukkit.getPluginManager().getPlugin(target);
            if (p == null) {
                boolean removed = forceDisabled.remove(target);
                if (removed) {
                    saveForceDisabledToConfig();
                    sender.sendMessage(ChatColor.GREEN + "Removed '" + target + "' from force-disabled list (plugin not currently loaded).");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Plugin not found: " + target);
                }
                return true;
            }

            boolean currentlyEnabled = p.isEnabled();
            if (currentlyEnabled) {
                boolean removed = forceDisabled.remove(p.getName());
                if (removed) saveForceDisabledToConfig();
                sender.sendMessage(ChatColor.YELLOW + "Plugin '" + p.getName() + "' is already enabled." + (removed ? " Removed from force-disabled list." : ""));
                getLogger().info(sender.getName() + " attempted to enable plugin already enabled: " + p.getName());
                return true;
            }


            try {
                sender.sendMessage(ChatColor.YELLOW + "Warning: some plugins may not initialize when enabled at runtime.\nConsider restarting the server or try using /axdisabler than /reload confirm or a clean startup. Proceeding to enable now...");
            } catch (Throwable ignored) {}

            try {
                Bukkit.getPluginManager().enablePlugin(p);
            } catch (Throwable t) {
                sender.sendMessage(ChatColor.RED + "Failed to enable plugin: " + t.getMessage());
                getLogger().warning("Failed to enable plugin " + p.getName() + ": " + t.getMessage());
                return true;
            }
            boolean removed = forceDisabled.remove(p.getName());
            if (removed) saveForceDisabledToConfig();
            sender.sendMessage(ChatColor.GREEN + "Plugin '" + p.getName() + "' enabled." + (removed ? " Removed from force-disabled list." : ""));
            getLogger().info(sender.getName() + " enabled plugin " + p.getName());
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("axban")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }

            if (args.length == 2) {
                List<String> suggestions = Arrays.asList("0", "1", "7", "30");
                String partial = args[1].toLowerCase();
                return suggestions.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
            }

            if (args.length == 3) {
                List<String> types = Arrays.asList("normal", "ip");
                String partial = args[2].toLowerCase();
                return types.stream().filter(t -> t.startsWith(partial)).collect(Collectors.toList());
            }

            return Collections.emptyList();
        }

        if (cmd.equals("axpardon")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axmute")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }

            if (args.length == 2) {
                List<String> suggestions = Arrays.asList("5", "10", "30", "60", "1440");
                String partial = args[1].toLowerCase();
                return suggestions.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
            }

            return Collections.emptyList();
        }

        if (cmd.equals("axwarn")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axinfo")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("report")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axtp")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            if (args.length == 2) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axworldtp")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getWorlds().stream()
                        .map(World::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axworldlist")) {
            return Collections.emptyList();
        }

        if (cmd.equals("axbroadcast")) {
            return Collections.emptyList();
        }

        if (cmd.equals("axfreeze")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            if (args.length == 2) {
                List<String> suggestions = Arrays.asList("0", "5", "10", "30", "60", "1440");
                String partial = args[1].toLowerCase();
                return suggestions.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axunfreeze")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axghost") || cmd.equals("axunghost")) {
            return Collections.emptyList();
        }

        if (cmd.equals("axspy")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                List<String> suggestions = Arrays.asList("on", "off");
                return suggestions.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axmaintenance")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                List<String> suggestions = Arrays.asList("on", "off");
                return suggestions.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axinventory")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axghosted") || cmd.equals("axmutes") || cmd.equals("axfrozen")) {
            return Collections.emptyList();
        }

        if (cmd.equals("axlocate")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axdisabler") || cmd.equals("axenabler")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return java.util.Arrays.stream(Bukkit.getPluginManager().getPlugins())
                        .map(pl -> pl.getName())
                        .filter(n -> n != null && n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            if (args.length == 2) {
                List<String> suggestions = Arrays.asList("confirm");
                String partial = args[1].toLowerCase();
                return suggestions.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("playtime")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("seen")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axsend")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            if (args.length == 2) {
                List<String> suggestions = Arrays.asList("lobby", "survival", "creative", "hub");
                String partial = args[1].toLowerCase();
                return suggestions.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axserver")) {
            if (args.length == 1) {
                List<String> suggestions = Arrays.asList("lobby", "survival", "creative", "hub");
                String partial = args[0].toLowerCase();
                return suggestions.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axnetworkkick")) {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .sorted(String::compareToIgnoreCase)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (cmd.equals("axserverlist")) {
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private org.bukkit.Location findSafeNetherSpawn(World w, org.bukkit.Location center) {
        if (w == null || center == null) return null;

        final int centerX = center.getBlockX();
        final int centerZ = center.getBlockZ();
        final int radius = 64;

        int worldMax = w.getMaxHeight() - 2;
        int worldMin = w.getMinHeight() + 1;
        worldMax = Math.min(worldMax, 126);
        worldMin = Math.max(worldMin, 1);

        java.util.List<int[]> coords = new java.util.ArrayList<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                int dist2 = dx * dx + dz * dz;
                coords.add(new int[] { x, z, dist2 });
            }
        }
        coords.sort((a, b) -> Integer.compare(a[2], b[2]));

        for (int[] c : coords) {
            int x = c[0];
            int z = c[1];

            try {
                Biome bi = w.getBiome(x, z);
                if (bi != null) {
                    String bn = bi.name().toUpperCase();
                    if (bn.contains("SOUL_SAND") || bn.contains("SOUL_SAND_VALLEY")) continue;
                }
            } catch (Throwable ignored) {}

            int highest = w.getHighestBlockYAt(x, z);
            int startY = Math.min(highest + 1, worldMax);
            startY = Math.max(startY, worldMin);

            for (int y = startY; y >= worldMin; y--) {
                try {
                    org.bukkit.block.Block floor = w.getBlockAt(x, y - 1, z);
                    org.bukkit.block.Block bFeet = w.getBlockAt(x, y, z);
                    org.bukkit.block.Block bHead = w.getBlockAt(x, y + 1, z);

                    if (floor.isLiquid() || bFeet.isLiquid() || bHead.isLiquid()) continue;

                    if (!floor.getType().isSolid()) continue;

                    if (bFeet.getType().isSolid() || bHead.getType().isSolid()) continue;

                    boolean nearbyUnsafe = false;
                    for (int nx = -1; nx <= 1 && !nearbyUnsafe; nx++) {
                        for (int nz = -1; nz <= 1 && !nearbyUnsafe; nz++) {
                            org.bukkit.block.Block nbFloor = w.getBlockAt(x + nx, y - 1, z + nz);
                            org.bukkit.block.Block nbFeet = w.getBlockAt(x + nx, y, z + nz);
                            org.bukkit.block.Block nbHead = w.getBlockAt(x + nx, y + 1, z + nz);
                            if (nbFloor.isLiquid() || nbFeet.isLiquid() || nbHead.isLiquid()) {
                                nearbyUnsafe = true; break;
                            }
                            if (nbFeet.getType().isSolid() && !(nx == 0 && nz == 0)) {
                                nearbyUnsafe = true; break;
                            }
                        }
                    }
                    if (nearbyUnsafe) continue;

                    if (y <= worldMin || y >= worldMax) continue;

                    org.bukkit.Location loc = new org.bukkit.Location(w, x + 0.5, y, z + 0.5);
                    loc.setYaw(center.getYaw());
                    loc.setPitch(center.getPitch());
                    return loc;
                } catch (Throwable ignored) {}
            }
        }

        return null;
    }

    private org.bukkit.Location findSafeEndSpawn(World w, org.bukkit.Location center) {
        if (w == null || center == null) return null;

        final int centerX = center.getBlockX();
        final int centerZ = center.getBlockZ();
        final int radius = 64;
        final int centralExclusion = 24;

        int worldMax = w.getMaxHeight() - 2;
        int worldMin = w.getMinHeight() + 1;
        worldMax = Math.min(worldMax, 254);
        worldMin = Math.max(worldMin, 1);

        java.util.List<int[]> coords = new java.util.ArrayList<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                int dist2 = dx * dx + dz * dz;
                coords.add(new int[] { x, z, dist2 });
            }
        }
        coords.sort((a, b) -> Integer.compare(a[2], b[2]));

        for (int[] c : coords) {
            int x = c[0];
            int z = c[1];

            if (Math.abs(x - centerX) <= centralExclusion && Math.abs(z - centerZ) <= centralExclusion) continue;

            int highest = w.getHighestBlockYAt(x, z);
            int startY = Math.min(highest + 1, worldMax);
            startY = Math.max(startY, worldMin);

            for (int y = startY; y >= worldMin; y--) {
                try {
                    org.bukkit.block.Block floor = w.getBlockAt(x, y - 1, z);
                    org.bukkit.block.Block bFeet = w.getBlockAt(x, y, z);
                    org.bukkit.block.Block bHead = w.getBlockAt(x, y + 1, z);

                    org.bukkit.Material floorType = floor.getType();
                    if (floorType == Material.BEDROCK || floorType == Material.OBSIDIAN || floorType == Material.END_PORTAL || floorType == Material.END_PORTAL_FRAME)
                        continue;

                    if (!floor.getType().isSolid()) continue;

                    if (bFeet.getType().isSolid() || bHead.getType().isSolid()) continue;

                    org.bukkit.block.Block below = w.getBlockAt(x, y - 2, z);
                    if (below == null || !below.getType().isSolid()) continue;


                    boolean nearbyUnsafe = false;
                    for (int nx = -2; nx <= 2 && !nearbyUnsafe; nx++) {
                        for (int nz = -2; nz <= 2 && !nearbyUnsafe; nz++) {
                            org.bukkit.block.Block nbFloor = w.getBlockAt(x + nx, y - 1, z + nz);
                            org.bukkit.block.Block nbFeet = w.getBlockAt(x + nx, y, z + nz);
                            org.bukkit.block.Block nbHead = w.getBlockAt(x + nx, y + 1, z + nz);
                            org.bukkit.Material nt = nbFloor.getType();
                            if (nt == Material.BEDROCK || nt == Material.OBSIDIAN || nt == Material.END_PORTAL || nt == Material.END_PORTAL_FRAME) { nearbyUnsafe = true; break; }
                            if (nbFeet.getType().isSolid() && !(nx == 0 && nz == 0)) { nearbyUnsafe = true; break; }
                        }
                    }
                    if (nearbyUnsafe) continue;

                    boolean pillarNearby = false;
                    int scanUp = Math.min(20, worldMax - y);
                    for (int vy = y; vy <= y + scanUp && !pillarNearby; vy++) {
                        for (int nx = -1; nx <= 1 && !pillarNearby; nx++) {
                            for (int nz = -1; nz <= 1 && !pillarNearby; nz++) {
                                org.bukkit.block.Block b = w.getBlockAt(x + nx, vy, z + nz);
                                if (b != null && b.getType() == Material.OBSIDIAN) { pillarNearby = true; break; }
                            }
                        }
                    }
                    if (pillarNearby) continue;

                    int minSafeY = Math.max(worldMin + 10, 50);
                    if (y < minSafeY || y <= worldMin || y >= worldMax) continue;

                    org.bukkit.Location loc = new org.bukkit.Location(w, x + 0.5, y, z + 0.5);
                    loc.setYaw(center.getYaw());
                    loc.setPitch(center.getPitch());
                    return loc;
                } catch (Throwable ignored) {}
            }
        }

        return null;
    }

    private org.bukkit.Location findSafeOverworldSpawn(World w, org.bukkit.Location center) {
        if (w == null || center == null) return null;

        final int centerX = center.getBlockX();
        final int centerZ = center.getBlockZ();
        final int radius = 32;

        int worldMax = w.getMaxHeight() - 2;
        int worldMin = w.getMinHeight() + 1;
        worldMax = Math.min(worldMax, 254);
        worldMin = Math.max(worldMin, 1);

        java.util.List<int[]> coords = new java.util.ArrayList<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                int dist2 = dx * dx + dz * dz;
                coords.add(new int[] { x, z, dist2 });
            }
        }
        coords.sort((a, b) -> Integer.compare(a[2], b[2]));

        java.util.Set<Material> dangerous = new java.util.HashSet<>();
        dangerous.add(Material.MAGMA_BLOCK);
        dangerous.add(Material.CACTUS);
        dangerous.add(Material.FIRE);
        dangerous.add(Material.SOUL_FIRE);
        dangerous.add(Material.CAMPFIRE);
        dangerous.add(Material.SOUL_CAMPFIRE);

        for (int[] c : coords) {
            int x = c[0];
            int z = c[1];

            int highest = w.getHighestBlockYAt(x, z);
            int startY = Math.min(highest + 1, worldMax);
            startY = Math.max(startY, worldMin);

            for (int y = startY; y >= Math.max(worldMin, highest - 2); y--) {
                try {
                    org.bukkit.block.Block floor = w.getBlockAt(x, y - 1, z);
                    org.bukkit.block.Block bFeet = w.getBlockAt(x, y, z);
                    org.bukkit.block.Block bHead = w.getBlockAt(x, y + 1, z);

                    if (floor.isLiquid() || bFeet.isLiquid() || bHead.isLiquid()) continue;

                    if (floor.getType().name().contains("LEAVES")) continue;

                    if (!floor.getType().isSolid()) continue;
                    if (dangerous.contains(floor.getType())) continue;

                    if (bFeet.getType().isSolid() || bHead.getType().isSolid()) continue;
                    if (bFeet.getType() == Material.FIRE || bFeet.getType() == Material.SOUL_FIRE) continue;
                    if (bHead.getType() == Material.FIRE || bHead.getType() == Material.SOUL_FIRE) continue;
                    if (bFeet.getType().name().contains("LEAVES") || bHead.getType().name().contains("LEAVES")) continue;

                    if (Math.abs(highest - (y - 1)) > 2) continue;

                    try {
                        Biome bi = w.getBiome(x, z);
                        if (bi != null) {
                            String bn = bi.name().toUpperCase();
                            if (bn.contains("OCEAN") || bn.contains("RIVER") || bn.contains("DEEP_OCEAN")) {
                                org.bukkit.block.Block highestBlock = w.getBlockAt(x, highest, z);
                                if (highestBlock != null && highestBlock.isLiquid()) continue;
                            }
                        }
                    } catch (Throwable ignored) {}

                    boolean nearbyBad = false;
                    for (int nx = -1; nx <= 1 && !nearbyBad; nx++) {
                        for (int nz = -1; nz <= 1 && !nearbyBad; nz++) {
                            org.bukkit.block.Block nbFloor = w.getBlockAt(x + nx, y - 1, z + nz);
                            org.bukkit.block.Block nbFeet = w.getBlockAt(x + nx, y, z + nz);
                            if (nbFloor.isLiquid() || nbFeet.isLiquid()) { nearbyBad = true; break; }
                            if (dangerous.contains(nbFloor.getType())) { nearbyBad = true; break; }
                            if (nbFloor.getType().name().contains("LEAVES")) { nearbyBad = true; break; }
                        }
                    }
                    if (nearbyBad) continue;

                    if (y <= worldMin || y >= worldMax) continue;

                    org.bukkit.Location loc = new org.bukkit.Location(w, x + 0.5, y, z + 0.5);
                    loc.setYaw(center.getYaw());
                    loc.setPitch(center.getPitch());
                    return loc;
                } catch (Throwable ignored) {}
            }
        }

        return null;
    }
}
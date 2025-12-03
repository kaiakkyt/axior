package kaiakk.axior;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class Axior extends Plugin implements Listener {

    private File adminFile;
    private Configuration adminConfig;

    private final Map<UUID, Long> muteExpiry = new ConcurrentHashMap<>();
    private final Map<UUID, String> muteReason = new ConcurrentHashMap<>();
    private final Map<UUID, String> muteSetBy = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastReportTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> warningsCount = new ConcurrentHashMap<>();
    private final Set<UUID> bannedPlayers = Collections.synchronizedSet(new HashSet<>());
    private final Map<UUID, String> banReasons = new ConcurrentHashMap<>();
    private final Map<UUID, Long> banExpiry = new ConcurrentHashMap<>();
    private final Set<String> bannedIPs = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> spies = Collections.synchronizedSet(new HashSet<>());
    
    private long reportCooldownMillis = 5 * 60L * 1000L;
    private long pluginStartTimeMs = System.currentTimeMillis();
    private boolean maintenanceMode = false;

    @Override
    public void onEnable() {
        getLogger().info("Enabling Axior for BungeeCord/Waterfall");
        getLogger().info("Running Axior v" + getDescription().getVersion() + " by " + getDescription().getAuthor());
        
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
            try {
                adminConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(adminFile);
            } catch (IOException e) {
                getLogger().warning("Failed to load admin.yml: " + e.getMessage());
            }
        }
        
        getProxy().getPluginManager().registerListener(this, this);
        
        registerCommands();
        
        loadMutesFromConfig();
        loadBansFromConfig();
        loadWarningsFromConfig();
        loadSpiesFromConfig();
        loadMaintenanceFromConfig();
        
        getLogger().info("Axior enabled successfully!");
        getLogger().info("Proxy: " + getProxy().getName() + " " + getProxy().getVersion());
        getLogger().info("Online mode: " + getProxy().getConfig().isOnlineMode());
        getLogger().info("Servers: " + getProxy().getServers().keySet());
        
        checkBackendServers();
    }

    @Override
    public void onDisable() {
        saveMutesToConfig();
        saveBansToConfig();
        saveWarningsToConfig();
        saveSpiesFromConfig();
        saveMaintenanceToConfig();
        saveAdminConfig();
        getLogger().info("Plugin disabled.");
        getLogger().info("Saving all data to admin.yml...");
        getLogger().info("Data saved.");
        getLogger().info("Cya!");
    }

    private void saveAdminConfig() {
        try {
            if (adminConfig != null && adminFile != null) {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(adminConfig, adminFile);
            }
        } catch (IOException e) {
            getLogger().warning("Failed to save admin.yml: " + e.getMessage());
        }
    }

    private void registerCommands() {
        getProxy().getPluginManager().registerCommand(this, new AxiorCommand());
        
        getProxy().getPluginManager().registerCommand(this, new AxSendCommand());
        getProxy().getPluginManager().registerCommand(this, new AxSendAllCommand());
        getProxy().getPluginManager().registerCommand(this, new AxServerListCommand());
        getProxy().getPluginManager().registerCommand(this, new AxGListCommand());
        getProxy().getPluginManager().registerCommand(this, new AxFindCommand());
        getProxy().getPluginManager().registerCommand(this, new AxAlertCommand());
        
        getProxy().getPluginManager().registerCommand(this, new AxMuteCommand());
        getProxy().getPluginManager().registerCommand(this, new AxUnmuteCommand());
        getProxy().getPluginManager().registerCommand(this, new AxKickCommand());
        getProxy().getPluginManager().registerCommand(this, new AxBanCommand());
        getProxy().getPluginManager().registerCommand(this, new AxUnbanCommand());
        getProxy().getPluginManager().registerCommand(this, new AxWarnCommand());
        getProxy().getPluginManager().registerCommand(this, new AxReportCommand());
        
        getProxy().getPluginManager().registerCommand(this, new AxIPCommand());
        getProxy().getPluginManager().registerCommand(this, new AxInfoCommand());
        getProxy().getPluginManager().registerCommand(this, new AxProxyInfoCommand());
        
        getProxy().getPluginManager().registerCommand(this, new AxMaintenanceCommand());
        getProxy().getPluginManager().registerCommand(this, new AxSpyCommand());
        getProxy().getPluginManager().registerCommand(this, new AxBroadcastCommand());
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        
        if (maintenanceMode) {
            if (!player.hasPermission("axior.admin") && !player.hasPermission("axior.owner")) {
                player.disconnect(new TextComponent(ChatColor.RED + "Server is in maintenance mode. Try again later!"));
                return;
            }
        }
        
        if (bannedPlayers.contains(player.getUniqueId())) {
            Long expiry = banExpiry.get(player.getUniqueId());
            if (expiry != null) {
                if (expiry == Long.MAX_VALUE || System.currentTimeMillis() < expiry) {
                    String reason = banReasons.getOrDefault(player.getUniqueId(), "Banned");
                    player.disconnect(new TextComponent(ChatColor.RED + "You are banned: " + reason));
                    return;
                }
            }
        }
        
        String ip = player.getAddress().getAddress().getHostAddress();
        if (bannedIPs.contains(ip)) {
            player.disconnect(new TextComponent(ChatColor.RED + "Your IP is banned from this network."));
            return;
        }
        
        getLogger().info(player.getName() + " [" + ip + "] connected to the proxy");
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        getLogger().info(player.getName() + " disconnected from the proxy");
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) return;
        if (event.isCancelled()) return;
        if (event.isCommand()) return;
        
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        UUID id = player.getUniqueId();
        
        Long exp = muteExpiry.get(id);
        if (exp != null) {
            long now = System.currentTimeMillis();
            if (exp == Long.MAX_VALUE) {
                String reason = muteReason.getOrDefault(id, "No reason provided");
                player.sendMessage(new TextComponent(ChatColor.RED + "You are permanently muted. Reason: " + reason));
                event.setCancelled(true);
                return;
            }
            if (now < exp) {
                long remainingMs = exp - now;
                long remainingMin = (remainingMs + 59999L) / 60000L;
                String reason = muteReason.getOrDefault(id, "No reason provided");
                player.sendMessage(new TextComponent(ChatColor.RED + "You are muted for " + remainingMin + " more minute(s). Reason: " + reason));
                event.setCancelled(true);
                return;
            } else {
                muteExpiry.remove(id);
                muteReason.remove(id);
                muteSetBy.remove(id);
            }
        }
    }

    private void loadMutesFromConfig() {
        if (adminConfig == null) return;
        Configuration mutesSection = adminConfig.getSection("mutes");
        if (mutesSection == null) return;
        
        for (String key : mutesSection.getKeys()) {
            try {
                UUID id = UUID.fromString(key);
                long expiry = mutesSection.getLong(key + ".expiry", 0L);
                String reason = mutesSection.getString(key + ".reason", "No reason");
                String setBy = mutesSection.getString(key + ".setBy", "unknown");
                
                if (expiry > 0) {
                    muteExpiry.put(id, expiry);
                    muteReason.put(id, reason);
                    muteSetBy.put(id, setBy);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveMutesToConfig() {
        if (adminConfig == null) return;
        adminConfig.set("mutes", null);
        
        for (Map.Entry<UUID, Long> entry : muteExpiry.entrySet()) {
            String key = "mutes." + entry.getKey().toString();
            adminConfig.set(key + ".expiry", entry.getValue());
            adminConfig.set(key + ".reason", muteReason.getOrDefault(entry.getKey(), "No reason"));
            adminConfig.set(key + ".setBy", muteSetBy.getOrDefault(entry.getKey(), "unknown"));
        }
    }

    private void loadBansFromConfig() {
        if (adminConfig == null) return;
        Configuration bansSection = adminConfig.getSection("bans");
        if (bansSection == null) return;
        
        for (String key : bansSection.getKeys()) {
            try {
                UUID id = UUID.fromString(key);
                long expiry = bansSection.getLong(key + ".expiry", Long.MAX_VALUE);
                String reason = bansSection.getString(key + ".reason", "Banned");
                
                bannedPlayers.add(id);
                banExpiry.put(id, expiry);
                banReasons.put(id, reason);
            } catch (IllegalArgumentException ignored) {
            }
        }
        
        List<String> ips = adminConfig.getStringList("bannedIPs");
        if (ips != null) {
            bannedIPs.addAll(ips);
        }
    }

    private void saveBansToConfig() {
        if (adminConfig == null) return;
        adminConfig.set("bans", null);
        
        for (UUID id : bannedPlayers) {
            String key = "bans." + id.toString();
            adminConfig.set(key + ".expiry", banExpiry.getOrDefault(id, Long.MAX_VALUE));
            adminConfig.set(key + ".reason", banReasons.getOrDefault(id, "Banned"));
        }
        
        adminConfig.set("bannedIPs", new ArrayList<>(bannedIPs));
    }

    private void loadWarningsFromConfig() {
        if (adminConfig == null) return;
        Configuration warningsSection = adminConfig.getSection("warnings");
        if (warningsSection == null) return;
        
        for (String key : warningsSection.getKeys()) {
            try {
                UUID id = UUID.fromString(key);
                int count = warningsSection.getInt(key + ".count", 0);
                if (count > 0) {
                    warningsCount.put(id, count);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveWarningsToConfig() {
        if (adminConfig == null) return;
        adminConfig.set("warnings", null);
        
        for (Map.Entry<UUID, Integer> entry : warningsCount.entrySet()) {
            String key = "warnings." + entry.getKey().toString();
            adminConfig.set(key + ".count", entry.getValue());
        }
    }

    private void loadSpiesFromConfig() {
        if (adminConfig == null) return;
        List<String> spyList = adminConfig.getStringList("spies");
        if (spyList != null) {
            for (String uuidStr : spyList) {
                try {
                    spies.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private void saveSpiesFromConfig() {
        if (adminConfig == null) return;
        List<String> spyList = spies.stream().map(UUID::toString).collect(Collectors.toList());
        adminConfig.set("spies", spyList);
    }

    private void loadMaintenanceFromConfig() {
        if (adminConfig == null) return;
        maintenanceMode = adminConfig.getBoolean("maintenance", false);
        if (maintenanceMode) {
            getLogger().info("Maintenance mode enabled from admin.yml");
        }
    }

    private void saveMaintenanceToConfig() {
        if (adminConfig == null) return;
        adminConfig.set("maintenance", maintenanceMode);
    }

    private void checkBackendServers() {
        getLogger().info("Checking backend servers...");
        int totalServers = getProxy().getServers().size();
        int availableServers = 0;
        
        for (Map.Entry<String, ServerInfo> entry : getProxy().getServers().entrySet()) {
            String serverName = entry.getKey();
            ServerInfo serverInfo = entry.getValue();
            
            try {
                serverInfo.ping((result, error) -> {
                    if (error == null && result != null) {
                        getLogger().info("Server '" + serverName + "' is online (" + 
                            serverInfo.getAddress().getHostString() + ":" + serverInfo.getAddress().getPort() + ") - " +
                            "Players: " + result.getPlayers().getOnline() + "/" + result.getPlayers().getMax() + 
                            " | Version: " + result.getVersion().getName());
                    } else {
                        getLogger().warning("Server '" + serverName + "' is offline or unreachable (" + 
                            serverInfo.getAddress().getHostString() + ":" + serverInfo.getAddress().getPort() + ")");
                    }
                });
            } catch (Exception e) {
                getLogger().warning("Failed to ping server '" + serverName + "': " + e.getMessage());
            }
        }
        
        getLogger().info("Total configured servers: " + totalServers);
    }

    private class AxiorCommand extends Command {
        public AxiorCommand() {
            super("axior", "axior.general", "ax");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            sender.sendMessage(new TextComponent(ChatColor.AQUA + "Axior v" + getDescription().getVersion() + " by " + getDescription().getAuthor()));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Proxy plugin for BungeeCord/Waterfall"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Use /axproxyinfo for detailed server information"));
        }
    }

    private class AxSendCommand extends Command {
        public AxSendCommand() {
            super("axsend", "axior.admin");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axsend <player> <server>"));
                return;
            }

            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found: " + args[0]));
                return;
            }

            ServerInfo server = getProxy().getServerInfo(args[1]);
            if (server == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Server not found: " + args[1]));
                return;
            }

            target.connect(server);
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Sent " + target.getName() + " to " + server.getName()));
            getLogger().info(sender.getName() + " sent " + target.getName() + " to " + server.getName());
        }
    }

    private class AxSendAllCommand extends Command {
        public AxSendAllCommand() {
            super("axsendall", "axior.admin");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axsendall <server>"));
                return;
            }

            ServerInfo server = getProxy().getServerInfo(args[0]);
            if (server == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Server not found: " + args[0]));
                return;
            }

            int count = 0;
            for (ProxiedPlayer player : getProxy().getPlayers()) {
                player.connect(server);
                count++;
            }

            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Sent " + count + " player(s) to " + server.getName()));
            getLogger().info(sender.getName() + " sent all players to " + server.getName());
        }
    }

    private class AxServerListCommand extends Command {
        public AxServerListCommand() {
            super("axserverlist", "axior.general", "axservers");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            sender.sendMessage(new TextComponent(ChatColor.AQUA + "=== Server List ==="));
            for (Map.Entry<String, ServerInfo> entry : getProxy().getServers().entrySet()) {
                int players = entry.getValue().getPlayers().size();
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + entry.getKey() + ChatColor.WHITE + ": " + players + " player(s)"));
            }
        }
    }

    private class AxGListCommand extends Command {
        public AxGListCommand() {
            super("axglist", "axior.general");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            sender.sendMessage(new TextComponent(ChatColor.AQUA + "=== Global Player List ==="));
            int total = 0;
            for (Map.Entry<String, ServerInfo> entry : getProxy().getServers().entrySet()) {
                Collection<ProxiedPlayer> players = entry.getValue().getPlayers();
                if (!players.isEmpty()) {
                    String names = players.stream().map(ProxiedPlayer::getName).collect(Collectors.joining(", "));
                    sender.sendMessage(new TextComponent(ChatColor.YELLOW + entry.getKey() + " (" + players.size() + "): " + ChatColor.WHITE + names));
                    total += players.size();
                }
            }
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Total: " + total + " player(s) online"));
        }
    }

    private class AxFindCommand extends Command {
        public AxFindCommand() {
            super("axfind", "axior.mod");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axfind <player>"));
                return;
            }

            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found: " + args[0]));
                return;
            }

            ServerInfo server = target.getServer() != null ? target.getServer().getInfo() : null;
            String serverName = server != null ? server.getName() : "unknown";
            sender.sendMessage(new TextComponent(ChatColor.GREEN + target.getName() + " is on server: " + ChatColor.YELLOW + serverName));
        }
    }

    private class AxAlertCommand extends Command {
        public AxAlertCommand() {
            super("axalert", "axior.admin");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axalert <message>"));
                return;
            }

            String message = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));
            for (ProxiedPlayer player : getProxy().getPlayers()) {
                player.sendMessage(new TextComponent(ChatColor.RED + "[ALERT] " + ChatColor.RESET + message));
            }
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Alert sent to all players"));
        }
    }

    private class AxMuteCommand extends Command {
        public AxMuteCommand() {
            super("axmute", "axior.mod");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axmute <player> <duration_minutes> [reason]"));
                return;
            }

            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found: " + args[0]));
                return;
            }

            int minutes;
            try {
                minutes = Integer.parseInt(args[1]);
                if (minutes < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Invalid duration"));
                return;
            }

            String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Muted by staff";
            long expiry = minutes == 0 ? Long.MAX_VALUE : System.currentTimeMillis() + minutes * 60L * 1000L;

            muteExpiry.put(target.getUniqueId(), expiry);
            muteReason.put(target.getUniqueId(), reason);
            muteSetBy.put(target.getUniqueId(), sender.getName());
            saveMutesToConfig();
            saveAdminConfig();

            String timeDesc = minutes == 0 ? "permanently" : "for " + minutes + " minute(s)";
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Muted " + target.getName() + " " + timeDesc));
            target.sendMessage(new TextComponent(ChatColor.RED + "You have been muted " + timeDesc + ". Reason: " + reason));
            getLogger().info(sender.getName() + " muted " + target.getName() + " " + timeDesc);
        }
    }

    private class AxUnmuteCommand extends Command {
        public AxUnmuteCommand() {
            super("axunmute", "axior.mod");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axunmute <player>"));
                return;
            }

            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            UUID targetId = target != null ? target.getUniqueId() : null;

            if (targetId != null && muteExpiry.containsKey(targetId)) {
                muteExpiry.remove(targetId);
                muteReason.remove(targetId);
                muteSetBy.remove(targetId);
                saveMutesToConfig();
                saveAdminConfig();

                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Unmuted " + target.getName()));
                target.sendMessage(new TextComponent(ChatColor.GREEN + "You have been unmuted"));
                getLogger().info(sender.getName() + " unmuted " + target.getName());
            } else {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Player is not muted"));
            }
        }
    }

    private class AxKickCommand extends Command {
        public AxKickCommand() {
            super("axkick", "axior.mod");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axkick <player> [reason]"));
                return;
            }

            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found: " + args[0]));
                return;
            }

            String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Kicked by staff";
            target.disconnect(new TextComponent(ChatColor.RED + "Kicked: " + reason));
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Kicked " + target.getName()));
            getLogger().info(sender.getName() + " kicked " + target.getName() + ": " + reason);
        }
    }

    private class AxBanCommand extends Command {
        public AxBanCommand() {
            super("axban", "axior.admin");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axban <player> [duration_days] [reason]"));
                return;
            }

            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            UUID targetId = target != null ? target.getUniqueId() : null;
            
            if (targetId == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found or not online"));
                return;
            }

            long days = 0;
            if (args.length >= 2) {
                try {
                    days = Long.parseLong(args[1]);
                } catch (NumberFormatException ignored) {
                }
            }

            String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Banned by staff";
            long expiry = days == 0 ? Long.MAX_VALUE : System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L;

            bannedPlayers.add(targetId);
            banExpiry.put(targetId, expiry);
            banReasons.put(targetId, reason);
            saveBansToConfig();
            saveAdminConfig();

            String timeDesc = days == 0 ? "permanently" : "for " + days + " day(s)";
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Banned " + args[0] + " " + timeDesc));
            
            if (target != null) {
                target.disconnect(new TextComponent(ChatColor.RED + "You have been banned " + timeDesc + "\nReason: " + reason));
            }
            
            getLogger().info(sender.getName() + " banned " + args[0] + " " + timeDesc);
        }
    }

    private class AxUnbanCommand extends Command {
        public AxUnbanCommand() {
            super("axunban", "axior.admin");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axunban <player_uuid>"));
                return;
            }

            try {
                UUID targetId = UUID.fromString(args[0]);
                if (bannedPlayers.contains(targetId)) {
                    bannedPlayers.remove(targetId);
                    banExpiry.remove(targetId);
                    banReasons.remove(targetId);
                    saveBansToConfig();
                    saveAdminConfig();
                    sender.sendMessage(new TextComponent(ChatColor.GREEN + "Unbanned player with UUID: " + targetId));
                    getLogger().info(sender.getName() + " unbanned player: " + targetId);
                } else {
                    sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Player is not banned"));
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Invalid UUID format"));
            }
        }
    }

    private class AxWarnCommand extends Command {
        public AxWarnCommand() {
            super("axwarn", "axior.mod");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axwarn <player> [reason]"));
                return;
            }

            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found: " + args[0]));
                return;
            }

            String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Warned by staff";
            int newCount = warningsCount.getOrDefault(target.getUniqueId(), 0) + 1;
            warningsCount.put(target.getUniqueId(), newCount);
            saveWarningsToConfig();
            saveAdminConfig();

            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Warned " + target.getName() + " (Total: " + newCount + ")"));
            target.sendMessage(new TextComponent(ChatColor.RED + "You have been warned: " + reason + " (Warning #" + newCount + ")"));
            getLogger().info(sender.getName() + " warned " + target.getName() + ": " + reason);
        }
    }

    private class AxReportCommand extends Command {
        public AxReportCommand() {
            super("axreport", "axior.general");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Only players can use this command"));
                return;
            }

            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axreport <player> [reason]"));
                return;
            }

            ProxiedPlayer reporter = (ProxiedPlayer) sender;
            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            
            if (target == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found: " + args[0]));
                return;
            }

            UUID rid = reporter.getUniqueId();
            long now = System.currentTimeMillis();
            Long last = lastReportTime.get(rid);
            
            if (last != null && reportCooldownMillis > 0 && now - last < reportCooldownMillis) {
                long remainingMs = reportCooldownMillis - (now - last);
                long remainingMin = (remainingMs + 59999L) / 60000L;
                reporter.sendMessage(new TextComponent(ChatColor.YELLOW + "You must wait " + remainingMin + " more minute(s) before reporting again"));
                return;
            }

            lastReportTime.put(rid, now);
            String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Report submitted for " + target.getName()));
            
            for (ProxiedPlayer staff : getProxy().getPlayers()) {
                if (staff.hasPermission("axior.mod") || staff.hasPermission("axior.admin")) {
                    staff.sendMessage(new TextComponent(ChatColor.YELLOW + "[REPORT] " + reporter.getName() + " reported " + target.getName() + ": " + reason));
                }
            }
            
            getLogger().info("[REPORT] " + reporter.getName() + " reported " + target.getName() + ": " + reason);
        }
    }

    private class AxIPCommand extends Command {
        public AxIPCommand() {
            super("axip", "axior.admin");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axip <player>"));
                return;
            }

            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found: " + args[0]));
                return;
            }

            String ip = target.getAddress().getAddress().getHostAddress();
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + target.getName() + "'s IP: " + ChatColor.WHITE + ip));
        }
    }

    private class AxInfoCommand extends Command {
        public AxInfoCommand() {
            super("axinfo", "axior.mod");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axinfo <player>"));
                return;
            }

            ProxiedPlayer target = getProxy().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found: " + args[0]));
                return;
            }

            ServerInfo server = target.getServer() != null ? target.getServer().getInfo() : null;
            String serverName = server != null ? server.getName() : "unknown";
            String ip = target.getAddress().getAddress().getHostAddress();
            int ping = target.getPing();

            sender.sendMessage(new TextComponent(ChatColor.AQUA + "=== " + target.getName() + " ==="));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "UUID: " + ChatColor.WHITE + target.getUniqueId()));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Server: " + ChatColor.WHITE + serverName));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "IP: " + ChatColor.WHITE + ip));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Ping: " + ChatColor.WHITE + ping + "ms"));
            
            Integer warnings = warningsCount.get(target.getUniqueId());
            if (warnings != null && warnings > 0) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Warnings: " + ChatColor.RED + warnings));
            }
            
            if (muteExpiry.containsKey(target.getUniqueId())) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Currently muted"));
            }
        }
    }

    private class AxProxyInfoCommand extends Command {
        public AxProxyInfoCommand() {
            super("axproxyinfo", "axior.admin");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            long uptime = System.currentTimeMillis() - pluginStartTimeMs;
            long uptimeSec = uptime / 1000L;
            long days = uptimeSec / 86400L;
            long hours = (uptimeSec % 86400L) / 3600L;
            long mins = (uptimeSec % 3600L) / 60L;

            sender.sendMessage(new TextComponent(ChatColor.AQUA + "=== Proxy Info ==="));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Software: " + ChatColor.WHITE + getProxy().getName() + " " + getProxy().getVersion()));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Uptime: " + ChatColor.WHITE + days + "d " + hours + "h " + mins + "m"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Players: " + ChatColor.WHITE + getProxy().getOnlineCount() + " online"));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Servers: " + ChatColor.WHITE + getProxy().getServers().size()));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Online mode: " + ChatColor.WHITE + getProxy().getConfig().isOnlineMode()));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Muted players: " + ChatColor.WHITE + muteExpiry.size()));
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Banned players: " + ChatColor.WHITE + bannedPlayers.size()));
        }
    }

    private class AxMaintenanceCommand extends Command {
        public AxMaintenanceCommand() {
            super("axmaintenance", "axior.owner", "axmaint");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axmaintenance <on|off>"));
                return;
            }

            if (args[0].equalsIgnoreCase("on")) {
                maintenanceMode = true;
                saveMaintenanceToConfig();
                saveAdminConfig();
                
                for (ProxiedPlayer player : getProxy().getPlayers()) {
                    if (!player.hasPermission("axior.admin") && !player.hasPermission("axior.owner")) {
                        player.disconnect(new TextComponent(ChatColor.RED + "Server is now in maintenance mode"));
                    }
                }
                
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Maintenance mode enabled"));
                getLogger().info(sender.getName() + " enabled maintenance mode");
            } else if (args[0].equalsIgnoreCase("off")) {
                maintenanceMode = false;
                saveMaintenanceToConfig();
                saveAdminConfig();
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Maintenance mode disabled"));
                getLogger().info(sender.getName() + " disabled maintenance mode");
            } else {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axmaintenance <on|off>"));
            }
        }
    }

    private class AxSpyCommand extends Command {
        public AxSpyCommand() {
            super("axspy", "axior.mod");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Only players can use this command"));
                return;
            }

            ProxiedPlayer player = (ProxiedPlayer) sender;
            UUID id = player.getUniqueId();

            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axspy <on|off>"));
                return;
            }

            if (args[0].equalsIgnoreCase("on")) {
                spies.add(id);
                saveSpiesFromConfig();
                saveAdminConfig();
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Spy mode enabled"));
            } else if (args[0].equalsIgnoreCase("off")) {
                spies.remove(id);
                saveSpiesFromConfig();
                saveAdminConfig();
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Spy mode disabled"));
            } else {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axspy <on|off>"));
            }
        }
    }

    private class AxBroadcastCommand extends Command {
        public AxBroadcastCommand() {
            super("axbroadcast", "axior.admin", "axbc");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /axbroadcast <message>"));
                return;
            }

            String message = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));
            for (ProxiedPlayer player : getProxy().getPlayers()) {
                player.sendMessage(new TextComponent(ChatColor.GOLD + "[Broadcast] " + ChatColor.RESET + message));
            }
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "Broadcast sent"));
        }
    }
}

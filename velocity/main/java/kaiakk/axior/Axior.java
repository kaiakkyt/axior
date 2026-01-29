package kaiakk.axior;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Plugin(id = "axior", name = "Axior", version = "1.01.1", description = "A plugin that updates Minecraft operator tools, brings new systems and helps finding info in console on startup!", url = "https://modrinth.com/plugin/axior", authors = {"KaiakK"})
public class Axior {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private File adminFile;
    private Map<String, Object> adminConfig;

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

    @Inject
    public Axior(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Enabling Axior for Velocity");
        logger.info("Running Axior v1.00.0 by KaiakK");
        
        adminFile = new File(dataDirectory.toFile(), "admin.yml");
        if (adminFile.getParentFile() != null && !adminFile.getParentFile().exists()) {
            adminFile.getParentFile().mkdirs();
        }
        if (!adminFile.exists()) {
            try {
                adminFile.createNewFile();
            } catch (IOException ioe) {
                logger.warn("Failed to create admin.yml: " + ioe.getMessage());
                adminFile = null;
            }
        }
        
        loadAdminConfig();
        
        registerCommands();
        
        loadMutesFromConfig();
        loadBansFromConfig();
        loadWarningsFromConfig();
        loadSpiesFromConfig();
        loadMaintenanceFromConfig();
        
        logger.info("Axior enabled successfully!");
        logger.info("Proxy: Velocity " + proxy.getVersion());
        logger.info("Servers: " + proxy.getAllServers().stream().map(s -> s.getServerInfo().getName()).collect(Collectors.joining(", ")));
        
        checkBackendServers();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        saveMutesToConfig();
        saveBansToConfig();
        saveWarningsToConfig();
        saveSpiesFromConfig();
        saveMaintenanceToConfig();
        saveAdminConfig();
        logger.info("Plugin disabled.");
        logger.info("Saving all data to admin.yml...");
        logger.info("Data saved.");
        logger.info("Cya!");
    }

    @SuppressWarnings("unchecked")
    private void loadAdminConfig() {
        if (adminFile != null && adminFile.exists()) {
            try (FileInputStream fis = new FileInputStream(adminFile)) {
                Yaml yaml = new Yaml();
                adminConfig = yaml.load(fis);
                if (adminConfig == null) {
                    adminConfig = new LinkedHashMap<>();
                }
            } catch (IOException e) {
                logger.warn("Failed to load admin.yml: " + e.getMessage());
                adminConfig = new LinkedHashMap<>();
            }
        } else {
            adminConfig = new LinkedHashMap<>();
        }
    }

    private void saveAdminConfig() {
        if (adminFile != null && adminConfig != null) {
            try (FileWriter writer = new FileWriter(adminFile)) {
                Yaml yaml = new Yaml();
                yaml.dump(adminConfig, writer);
            } catch (IOException e) {
                logger.warn("Failed to save admin.yml: " + e.getMessage());
            }
        }
    }

    private void registerCommands() {
        CommandManager manager = proxy.getCommandManager();
        
        manager.register(manager.metaBuilder("axior").aliases("ax").build(), new AxiorCommand());
        
        manager.register("axsend", new AxSendCommand());
        manager.register("axsendall", new AxSendAllCommand());
        manager.register(manager.metaBuilder("axserverlist").aliases("axservers").build(), new AxServerListCommand());
        manager.register("axglist", new AxGListCommand());
        manager.register("axfind", new AxFindCommand());
        manager.register("axalert", new AxAlertCommand());
        
        manager.register("axmute", new AxMuteCommand());
        manager.register("axunmute", new AxUnmuteCommand());
        manager.register("axkick", new AxKickCommand());
        manager.register("axban", new AxBanCommand());
        manager.register("axunban", new AxUnbanCommand());
        manager.register("axwarn", new AxWarnCommand());
        manager.register("axreport", new AxReportCommand());
        
        manager.register("axip", new AxIPCommand());
        manager.register("axinfo", new AxInfoCommand());
        manager.register("axproxyinfo", new AxProxyInfoCommand());
        
        manager.register(manager.metaBuilder("axmaintenance").aliases("axmaint").build(), new AxMaintenanceCommand());
        manager.register("axspy", new AxSpyCommand());
        manager.register(manager.metaBuilder("axbroadcast").aliases("axbc").build(), new AxBroadcastCommand());
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        
        if (maintenanceMode) {
            if (!player.hasPermission("axior.admin") && !player.hasPermission("axior.owner")) {
                event.setResult(LoginEvent.ComponentResult.denied(
                    Component.text("Server is in maintenance mode. Try again later!", NamedTextColor.RED)
                ));
                return;
            }
        }
        
        if (bannedPlayers.contains(player.getUniqueId())) {
            Long expiry = banExpiry.get(player.getUniqueId());
            if (expiry != null) {
                if (expiry == Long.MAX_VALUE || System.currentTimeMillis() < expiry) {
                    String reason = banReasons.getOrDefault(player.getUniqueId(), "Banned");
                    event.setResult(LoginEvent.ComponentResult.denied(
                        Component.text("You are banned: " + reason, NamedTextColor.RED)
                    ));
                    return;
                }
            }
        }
        
        String ip = player.getRemoteAddress().getAddress().getHostAddress();
        if (bannedIPs.contains(ip)) {
            event.setResult(LoginEvent.ComponentResult.denied(
                Component.text("Your IP is banned from this network.", NamedTextColor.RED)
            ));
            return;
        }
        
        logger.info(player.getUsername() + " [" + ip + "] connected to the proxy");
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        logger.info(player.getUsername() + " disconnected from the proxy");
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        
        Long exp = muteExpiry.get(id);
        if (exp != null) {
            long now = System.currentTimeMillis();
            if (exp == Long.MAX_VALUE) {
                String reason = muteReason.getOrDefault(id, "No reason provided");
                player.sendMessage(Component.text("You are permanently muted. Reason: " + reason, NamedTextColor.RED));
                event.setResult(PlayerChatEvent.ChatResult.denied());
                return;
            }
            if (now < exp) {
                long remainingMs = exp - now;
                long remainingMin = (remainingMs + 59999L) / 60000L;
                String reason = muteReason.getOrDefault(id, "No reason provided");
                player.sendMessage(Component.text("You are muted for " + remainingMin + " more minute(s). Reason: " + reason, NamedTextColor.RED));
                event.setResult(PlayerChatEvent.ChatResult.denied());
                return;
            } else {
                muteExpiry.remove(id);
                muteReason.remove(id);
                muteSetBy.remove(id);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadMutesFromConfig() {
        if (adminConfig == null) return;
        Map<String, Object> mutesSection = (Map<String, Object>) adminConfig.get("mutes");
        if (mutesSection == null) return;
        
        for (Map.Entry<String, Object> entry : mutesSection.entrySet()) {
            try {
                UUID id = UUID.fromString(entry.getKey());
                Map<String, Object> muteData = (Map<String, Object>) entry.getValue();
                long expiry = ((Number) muteData.getOrDefault("expiry", 0L)).longValue();
                String reason = (String) muteData.getOrDefault("reason", "No reason");
                String setBy = (String) muteData.getOrDefault("setBy", "unknown");
                
                if (expiry > 0) {
                    muteExpiry.put(id, expiry);
                    muteReason.put(id, reason);
                    muteSetBy.put(id, setBy);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void saveMutesToConfig() {
        if (adminConfig == null) return;
        Map<String, Object> mutesSection = new LinkedHashMap<>();
        
        for (Map.Entry<UUID, Long> entry : muteExpiry.entrySet()) {
            Map<String, Object> muteData = new LinkedHashMap<>();
            muteData.put("expiry", entry.getValue());
            muteData.put("reason", muteReason.getOrDefault(entry.getKey(), "No reason"));
            muteData.put("setBy", muteSetBy.getOrDefault(entry.getKey(), "unknown"));
            mutesSection.put(entry.getKey().toString(), muteData);
        }
        
        adminConfig.put("mutes", mutesSection);
    }

    @SuppressWarnings("unchecked")
    private void loadBansFromConfig() {
        if (adminConfig == null) return;
        Map<String, Object> bansSection = (Map<String, Object>) adminConfig.get("bans");
        if (bansSection != null) {
            for (Map.Entry<String, Object> entry : bansSection.entrySet()) {
                try {
                    UUID id = UUID.fromString(entry.getKey());
                    Map<String, Object> banData = (Map<String, Object>) entry.getValue();
                    long expiry = banData.containsKey("expiry") ? ((Number) banData.get("expiry")).longValue() : Long.MAX_VALUE;
                    String reason = (String) banData.getOrDefault("reason", "Banned");
                    
                    bannedPlayers.add(id);
                    banExpiry.put(id, expiry);
                    banReasons.put(id, reason);
                } catch (Exception ignored) {
                }
            }
        }
        
        List<String> ips = (List<String>) adminConfig.get("bannedIPs");
        if (ips != null) {
            bannedIPs.addAll(ips);
        }
    }

    private void saveBansToConfig() {
        if (adminConfig == null) return;
        Map<String, Object> bansSection = new LinkedHashMap<>();
        
        for (UUID id : bannedPlayers) {
            Map<String, Object> banData = new LinkedHashMap<>();
            banData.put("expiry", banExpiry.getOrDefault(id, Long.MAX_VALUE));
            banData.put("reason", banReasons.getOrDefault(id, "Banned"));
            bansSection.put(id.toString(), banData);
        }
        
        adminConfig.put("bans", bansSection);
        adminConfig.put("bannedIPs", new ArrayList<>(bannedIPs));
    }

    @SuppressWarnings("unchecked")
    private void loadWarningsFromConfig() {
        if (adminConfig == null) return;
        Map<String, Object> warningsSection = (Map<String, Object>) adminConfig.get("warnings");
        if (warningsSection == null) return;
        
        for (Map.Entry<String, Object> entry : warningsSection.entrySet()) {
            try {
                UUID id = UUID.fromString(entry.getKey());
                Map<String, Object> warnData = (Map<String, Object>) entry.getValue();
                int count = ((Number) warnData.getOrDefault("count", 0)).intValue();
                if (count > 0) {
                    warningsCount.put(id, count);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void saveWarningsToConfig() {
        if (adminConfig == null) return;
        Map<String, Object> warningsSection = new LinkedHashMap<>();
        
        for (Map.Entry<UUID, Integer> entry : warningsCount.entrySet()) {
            Map<String, Object> warnData = new LinkedHashMap<>();
            warnData.put("count", entry.getValue());
            warningsSection.put(entry.getKey().toString(), warnData);
        }
        
        adminConfig.put("warnings", warningsSection);
    }

    @SuppressWarnings("unchecked")
    private void loadSpiesFromConfig() {
        if (adminConfig == null) return;
        List<String> spyList = (List<String>) adminConfig.get("spies");
        if (spyList != null) {
            for (String uuidStr : spyList) {
                try {
                    spies.add(UUID.fromString(uuidStr));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void saveSpiesFromConfig() {
        if (adminConfig == null) return;
        List<String> spyList = spies.stream().map(UUID::toString).collect(Collectors.toList());
        adminConfig.put("spies", spyList);
    }

    private void loadMaintenanceFromConfig() {
        if (adminConfig == null) return;
        Object maintenance = adminConfig.get("maintenance");
        if (maintenance instanceof Boolean) {
            maintenanceMode = (Boolean) maintenance;
            if (maintenanceMode) {
                logger.info("Maintenance mode enabled from admin.yml");
            }
        }
    }

    private void saveMaintenanceToConfig() {
        if (adminConfig == null) return;
        adminConfig.put("maintenance", maintenanceMode);
    }

    private void checkBackendServers() {
        logger.info("Checking backend servers...");
        int totalServers = proxy.getAllServers().size();
        
        for (RegisteredServer server : proxy.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            
            server.ping().thenAccept(ping -> {
                logger.info("Server '" + serverName + "' is online (" + 
                    server.getServerInfo().getAddress().getHostString() + ":" + 
                    server.getServerInfo().getAddress().getPort() + ") - " +
                    "Players: " + ping.getPlayers().map(p -> p.getOnline() + "/" + p.getMax()).orElse("?") + 
                    " | Version: " + ping.getVersion().getName());
            }).exceptionally(error -> {
                logger.warn("Server '" + serverName + "' is offline or unreachable (" + 
                    server.getServerInfo().getAddress().getHostString() + ":" + 
                    server.getServerInfo().getAddress().getPort() + ")");
                return null;
            });
        }
        
        logger.info("Total configured servers: " + totalServers);
    }

    private class AxiorCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            source.sendMessage(Component.text("Axior v1.00.0 by KaiakK", NamedTextColor.AQUA));
            source.sendMessage(Component.text("Proxy plugin for Velocity", NamedTextColor.YELLOW));
            source.sendMessage(Component.text("Use /axproxyinfo for detailed server information", NamedTextColor.YELLOW));
        }
    }

    private class AxSendCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.admin")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 2) {
                source.sendMessage(Component.text("Usage: /axsend <player> <server>", NamedTextColor.YELLOW));
                return;
            }

            Optional<Player> target = proxy.getPlayer(args[0]);
            if (!target.isPresent()) {
                source.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return;
            }

            Optional<RegisteredServer> server = proxy.getServer(args[1]);
            if (!server.isPresent()) {
                source.sendMessage(Component.text("Server not found: " + args[1], NamedTextColor.RED));
                return;
            }

            target.get().createConnectionRequest(server.get()).fireAndForget();
            source.sendMessage(Component.text("Sent " + target.get().getUsername() + " to " + server.get().getServerInfo().getName(), NamedTextColor.GREEN));
            logger.info(getSourceName(source) + " sent " + target.get().getUsername() + " to " + server.get().getServerInfo().getName());
        }
    }

    private class AxSendAllCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.admin")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axsendall <server>", NamedTextColor.YELLOW));
                return;
            }

            Optional<RegisteredServer> server = proxy.getServer(args[0]);
            if (!server.isPresent()) {
                source.sendMessage(Component.text("Server not found: " + args[0], NamedTextColor.RED));
                return;
            }

            int count = 0;
            for (Player player : proxy.getAllPlayers()) {
                player.createConnectionRequest(server.get()).fireAndForget();
                count++;
            }

            source.sendMessage(Component.text("Sent " + count + " player(s) to " + server.get().getServerInfo().getName(), NamedTextColor.GREEN));
            logger.info(getSourceName(source) + " sent all players to " + server.get().getServerInfo().getName());
        }
    }

    private class AxServerListCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            
            if (!source.hasPermission("axior.general")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            source.sendMessage(Component.text("=== Server List ===", NamedTextColor.AQUA));
            for (RegisteredServer server : proxy.getAllServers()) {
                int players = server.getPlayersConnected().size();
                source.sendMessage(Component.text(server.getServerInfo().getName() + ": " + players + " player(s)", NamedTextColor.YELLOW));
            }
        }
    }

    private class AxGListCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            
            if (!source.hasPermission("axior.general")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            source.sendMessage(Component.text("=== Global Player List ===", NamedTextColor.AQUA));
            int total = 0;
            for (RegisteredServer server : proxy.getAllServers()) {
                Collection<Player> players = server.getPlayersConnected();
                if (!players.isEmpty()) {
                    String names = players.stream().map(Player::getUsername).collect(Collectors.joining(", "));
                    source.sendMessage(Component.text(server.getServerInfo().getName() + " (" + players.size() + "): " + names, NamedTextColor.YELLOW));
                    total += players.size();
                }
            }
            source.sendMessage(Component.text("Total: " + total + " player(s) online", NamedTextColor.GREEN));
        }
    }

    private class AxFindCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.mod")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axfind <player>", NamedTextColor.YELLOW));
                return;
            }

            Optional<Player> target = proxy.getPlayer(args[0]);
            if (!target.isPresent()) {
                source.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return;
            }

            String serverName = target.get().getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("unknown");
            source.sendMessage(Component.text(target.get().getUsername() + " is on server: " + serverName, NamedTextColor.GREEN));
        }
    }

    private class AxAlertCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.admin")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axalert <message>", NamedTextColor.YELLOW));
                return;
            }

            String message = String.join(" ", args).replace("&", "§");
            for (Player player : proxy.getAllPlayers()) {
                player.sendMessage(Component.text("[ALERT] ", NamedTextColor.RED).append(Component.text(message)));
            }
            source.sendMessage(Component.text("Alert sent to all players", NamedTextColor.GREEN));
        }
    }

    private class AxMuteCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.mod")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 2) {
                source.sendMessage(Component.text("Usage: /axmute <player> <duration_minutes> [reason]", NamedTextColor.YELLOW));
                return;
            }

            Optional<Player> target = proxy.getPlayer(args[0]);
            if (!target.isPresent()) {
                source.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return;
            }

            int minutes;
            try {
                minutes = Integer.parseInt(args[1]);
                if (minutes < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                source.sendMessage(Component.text("Invalid duration", NamedTextColor.RED));
                return;
            }

            String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "Muted by staff";
            long expiry = minutes == 0 ? Long.MAX_VALUE : System.currentTimeMillis() + minutes * 60L * 1000L;

            muteExpiry.put(target.get().getUniqueId(), expiry);
            muteReason.put(target.get().getUniqueId(), reason);
            muteSetBy.put(target.get().getUniqueId(), getSourceName(source));
            saveMutesToConfig();
            saveAdminConfig();

            String timeDesc = minutes == 0 ? "permanently" : "for " + minutes + " minute(s)";
            source.sendMessage(Component.text("Muted " + target.get().getUsername() + " " + timeDesc, NamedTextColor.GREEN));
            target.get().sendMessage(Component.text("You have been muted " + timeDesc + ". Reason: " + reason, NamedTextColor.RED));
            logger.info(getSourceName(source) + " muted " + target.get().getUsername() + " " + timeDesc);
        }
    }

    private class AxUnmuteCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.mod")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axunmute <player>", NamedTextColor.YELLOW));
                return;
            }

            Optional<Player> target = proxy.getPlayer(args[0]);
            if (!target.isPresent()) {
                source.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return;
            }

            UUID targetId = target.get().getUniqueId();
            if (muteExpiry.containsKey(targetId)) {
                muteExpiry.remove(targetId);
                muteReason.remove(targetId);
                muteSetBy.remove(targetId);
                saveMutesToConfig();
                saveAdminConfig();

                source.sendMessage(Component.text("Unmuted " + target.get().getUsername(), NamedTextColor.GREEN));
                target.get().sendMessage(Component.text("You have been unmuted", NamedTextColor.GREEN));
                logger.info(getSourceName(source) + " unmuted " + target.get().getUsername());
            } else {
                source.sendMessage(Component.text("Player is not muted", NamedTextColor.YELLOW));
            }
        }
    }

    private class AxKickCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.mod")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axkick <player> [reason]", NamedTextColor.YELLOW));
                return;
            }

            Optional<Player> target = proxy.getPlayer(args[0]);
            if (!target.isPresent()) {
                source.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return;
            }

            String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Kicked by staff";
            target.get().disconnect(Component.text("Kicked: " + reason, NamedTextColor.RED));
            source.sendMessage(Component.text("Kicked " + target.get().getUsername(), NamedTextColor.GREEN));
            logger.info(getSourceName(source) + " kicked " + target.get().getUsername() + ": " + reason);
        }
    }

    private class AxBanCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.admin")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axban <player> [duration_days] [reason]", NamedTextColor.YELLOW));
                return;
            }

            Optional<Player> target = proxy.getPlayer(args[0]);
            if (!target.isPresent()) {
                source.sendMessage(Component.text("Player not found or not online", NamedTextColor.RED));
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

            UUID targetId = target.get().getUniqueId();
            bannedPlayers.add(targetId);
            banExpiry.put(targetId, expiry);
            banReasons.put(targetId, reason);
            saveBansToConfig();
            saveAdminConfig();

            String timeDesc = days == 0 ? "permanently" : "for " + days + " day(s)";
            source.sendMessage(Component.text("Banned " + args[0] + " " + timeDesc, NamedTextColor.GREEN));
            target.get().disconnect(Component.text("You have been banned " + timeDesc + "\nReason: " + reason, NamedTextColor.RED));
            logger.info(getSourceName(source) + " banned " + args[0] + " " + timeDesc);
        }
    }

    private class AxUnbanCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.admin")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axunban <player_uuid>", NamedTextColor.YELLOW));
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
                    source.sendMessage(Component.text("Unbanned player with UUID: " + targetId, NamedTextColor.GREEN));
                    logger.info(getSourceName(source) + " unbanned player: " + targetId);
                } else {
                    source.sendMessage(Component.text("Player is not banned", NamedTextColor.YELLOW));
                }
            } catch (IllegalArgumentException e) {
                source.sendMessage(Component.text("Invalid UUID format", NamedTextColor.RED));
            }
        }
    }

    private class AxWarnCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.mod")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axwarn <player> [reason]", NamedTextColor.YELLOW));
                return;
            }

            Optional<Player> target = proxy.getPlayer(args[0]);
            if (!target.isPresent()) {
                source.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return;
            }

            String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Warned by staff";
            int newCount = warningsCount.getOrDefault(target.get().getUniqueId(), 0) + 1;
            warningsCount.put(target.get().getUniqueId(), newCount);
            saveWarningsToConfig();
            saveAdminConfig();

            source.sendMessage(Component.text("Warned " + target.get().getUsername() + " (Total: " + newCount + ")", NamedTextColor.GREEN));
            target.get().sendMessage(Component.text("You have been warned: " + reason + " (Warning #" + newCount + ")", NamedTextColor.RED));
            logger.info(getSourceName(source) + " warned " + target.get().getUsername() + ": " + reason);
        }
    }

    private class AxReportCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!(source instanceof Player)) {
                source.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
                return;
            }
            
            if (!source.hasPermission("axior.general")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }

            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axreport <player> [reason]", NamedTextColor.YELLOW));
                return;
            }

            Player reporter = (Player) source;
            Optional<Player> target = proxy.getPlayer(args[0]);
            
            if (!target.isPresent()) {
                source.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return;
            }

            UUID rid = reporter.getUniqueId();
            long now = System.currentTimeMillis();
            Long last = lastReportTime.get(rid);
            
            if (last != null && reportCooldownMillis > 0 && now - last < reportCooldownMillis) {
                long remainingMs = reportCooldownMillis - (now - last);
                long remainingMin = (remainingMs + 59999L) / 60000L;
                reporter.sendMessage(Component.text("You must wait " + remainingMin + " more minute(s) before reporting again", NamedTextColor.YELLOW));
                return;
            }

            lastReportTime.put(rid, now);
            String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

            source.sendMessage(Component.text("Report submitted for " + target.get().getUsername(), NamedTextColor.GREEN));
            
            for (Player staff : proxy.getAllPlayers()) {
                if (staff.hasPermission("axior.mod") || staff.hasPermission("axior.admin")) {
                    staff.sendMessage(Component.text("[REPORT] " + reporter.getUsername() + " reported " + target.get().getUsername() + ": " + reason, NamedTextColor.YELLOW));
                }
            }
            
            logger.info("[REPORT] " + reporter.getUsername() + " reported " + target.get().getUsername() + ": " + reason);
        }
    }

    private class AxIPCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.admin")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axip <player>", NamedTextColor.YELLOW));
                return;
            }

            Optional<Player> target = proxy.getPlayer(args[0]);
            if (!target.isPresent()) {
                source.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return;
            }

            String ip = target.get().getRemoteAddress().getAddress().getHostAddress();
            source.sendMessage(Component.text(target.get().getUsername() + "'s IP: " + ip, NamedTextColor.YELLOW));
        }
    }

    private class AxInfoCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.mod")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axinfo <player>", NamedTextColor.YELLOW));
                return;
            }

            Optional<Player> target = proxy.getPlayer(args[0]);
            if (!target.isPresent()) {
                source.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return;
            }

            String serverName = target.get().getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("unknown");
            String ip = target.get().getRemoteAddress().getAddress().getHostAddress();
            long ping = target.get().getPing();

            source.sendMessage(Component.text("=== " + target.get().getUsername() + " ===", NamedTextColor.AQUA));
            source.sendMessage(Component.text("UUID: " + target.get().getUniqueId(), NamedTextColor.YELLOW));
            source.sendMessage(Component.text("Server: " + serverName, NamedTextColor.YELLOW));
            source.sendMessage(Component.text("IP: " + ip, NamedTextColor.YELLOW));
            source.sendMessage(Component.text("Ping: " + ping + "ms", NamedTextColor.YELLOW));
            
            Integer warnings = warningsCount.get(target.get().getUniqueId());
            if (warnings != null && warnings > 0) {
                source.sendMessage(Component.text("Warnings: " + warnings, NamedTextColor.RED));
            }
            
            if (muteExpiry.containsKey(target.get().getUniqueId())) {
                source.sendMessage(Component.text("Currently muted", NamedTextColor.RED));
            }
        }
    }

    private class AxProxyInfoCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            
            if (!source.hasPermission("axior.admin")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            long uptime = System.currentTimeMillis() - pluginStartTimeMs;
            long uptimeSec = uptime / 1000L;
            long days = uptimeSec / 86400L;
            long hours = (uptimeSec % 86400L) / 3600L;
            long mins = (uptimeSec % 3600L) / 60L;

            source.sendMessage(Component.text("=== Proxy Info ===", NamedTextColor.AQUA));
            source.sendMessage(Component.text("Software: Velocity " + proxy.getVersion(), NamedTextColor.YELLOW));
            source.sendMessage(Component.text("Uptime: " + days + "d " + hours + "h " + mins + "m", NamedTextColor.YELLOW));
            source.sendMessage(Component.text("Players: " + proxy.getPlayerCount() + " online", NamedTextColor.YELLOW));
            source.sendMessage(Component.text("Servers: " + proxy.getAllServers().size(), NamedTextColor.YELLOW));
            source.sendMessage(Component.text("Muted players: " + muteExpiry.size(), NamedTextColor.YELLOW));
            source.sendMessage(Component.text("Banned players: " + bannedPlayers.size(), NamedTextColor.YELLOW));
        }
    }

    private class AxMaintenanceCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.owner")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axmaintenance <on|off>", NamedTextColor.YELLOW));
                return;
            }

            if (args[0].equalsIgnoreCase("on")) {
                maintenanceMode = true;
                saveMaintenanceToConfig();
                saveAdminConfig();
                
                for (Player player : proxy.getAllPlayers()) {
                    if (!player.hasPermission("axior.admin") && !player.hasPermission("axior.owner")) {
                        player.disconnect(Component.text("Server is now in maintenance mode", NamedTextColor.RED));
                    }
                }
                
                source.sendMessage(Component.text("Maintenance mode enabled", NamedTextColor.GREEN));
                logger.info(getSourceName(source) + " enabled maintenance mode");
            } else if (args[0].equalsIgnoreCase("off")) {
                maintenanceMode = false;
                saveMaintenanceToConfig();
                saveAdminConfig();
                source.sendMessage(Component.text("Maintenance mode disabled", NamedTextColor.GREEN));
                logger.info(getSourceName(source) + " disabled maintenance mode");
            } else {
                source.sendMessage(Component.text("Usage: /axmaintenance <on|off>", NamedTextColor.YELLOW));
            }
        }
    }

    private class AxSpyCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!(source instanceof Player)) {
                source.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
                return;
            }
            
            if (!source.hasPermission("axior.mod")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }

            Player player = (Player) source;
            UUID id = player.getUniqueId();

            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axspy <on|off>", NamedTextColor.YELLOW));
                return;
            }

            if (args[0].equalsIgnoreCase("on")) {
                spies.add(id);
                saveSpiesFromConfig();
                saveAdminConfig();
                source.sendMessage(Component.text("Spy mode enabled", NamedTextColor.GREEN));
            } else if (args[0].equalsIgnoreCase("off")) {
                spies.remove(id);
                saveSpiesFromConfig();
                saveAdminConfig();
                source.sendMessage(Component.text("Spy mode disabled", NamedTextColor.GREEN));
            } else {
                source.sendMessage(Component.text("Usage: /axspy <on|off>", NamedTextColor.YELLOW));
            }
        }
    }

    private class AxBroadcastCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            
            if (!source.hasPermission("axior.admin")) {
                source.sendMessage(Component.text("You don't have permission to use this command", NamedTextColor.RED));
                return;
            }
            
            if (args.length < 1) {
                source.sendMessage(Component.text("Usage: /axbroadcast <message>", NamedTextColor.YELLOW));
                return;
            }

            String message = String.join(" ", args).replace("&", "§");
            for (Player player : proxy.getAllPlayers()) {
                player.sendMessage(Component.text("[Broadcast] ", NamedTextColor.GOLD).append(Component.text(message)));
            }
            source.sendMessage(Component.text("Broadcast sent", NamedTextColor.GREEN));
        }
    }

    private String getSourceName(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUsername();
        }
        return "Console";
    }
}

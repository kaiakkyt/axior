package kaiakk.axior;

import kaiakk.multimedia.classes.ConsoleLog;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

public class Clients implements PluginMessageListener {
    
    private static final Map<UUID, Set<String>> detectedClients = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> registeredChannels = new ConcurrentHashMap<>();
    private static final Map<UUID, String> reportedBrands = new ConcurrentHashMap<>();
    private static final Map<UUID, String> clientVersions = new ConcurrentHashMap<>();
    private static Plugin plugin;
    private static final Clients INSTANCE = new Clients();
    private static boolean packetEventsAvailable = false;
    private static Method peGetAPI = null;
    private static Method apiGetPlayerManager = null;
    private static Method pmGetUser = null;
    private static Method userGetClientVersion = null;
    private static Method versionGetReleaseName = null;
    private static Method versionGetProtocolVersion = null;
    
    private static final String[] CHANNELS = {
        "minecraft:brand",
        "MC|Brand",
        "fml:handshake",
        "fml:login",
        "fabric:hello",
        "quilt:hello",
        "lunar:apollo",
        "wurst:hello",
        "meteor:hello",
        "lb:hello"
    };
    
    private static final Set<String> KNOWN_HACKED_CHANNELS = new HashSet<>(Arrays.asList(
        "wurst", "lb",
        "meteor", "meteorclient",
        "liquidbounce",
        "sigma", "sigma5",
        "flux", "fluxclient",
        "aristois",
        "impact",
        "wolfram",
        "mathax",
        "inertia"
    ));
    
    private static final Map<String, Set<String>> REQUIRED_CHANNELS = new HashMap<String, Set<String>>() {{
        put("lunar", new HashSet<>(Arrays.asList("lunar:apollo")));
        put("forge", new HashSet<>(Arrays.asList("fml")));
    }};
    
    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
        for (String channel : CHANNELS) {
            try {
                Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, INSTANCE);
                Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel);
            } catch (IllegalArgumentException ignored) {
            }
        }
        try {
            setupPacketEventsReflection();
            if (packetEventsAvailable) ConsoleLog.info("Optional PacketEvents integration available for client version detection");
        } catch (Throwable t) {
            packetEventsAvailable = false;
        }
        
        ConsoleLog.info("Client detection initialized - monitoring for modded clients");
    }
    
    public static void trackPlayerOnJoin(Player player) {
        UUID uuid = player.getUniqueId();
        detectedClients.putIfAbsent(uuid, Collections.synchronizedSet(new HashSet<>()));
        registeredChannels.putIfAbsent(uuid, Collections.synchronizedSet(new HashSet<>()));
        
        try {
            kaiakk.multimedia.classes.SchedulerHelper.runLaterSeconds(plugin, () -> {
                Set<String> clientTypes = detectedClients.get(uuid);
                Set<String> channels = registeredChannels.get(uuid);
                String reportedBrand = reportedBrands.get(uuid);
                String playerName = player.getName();

                String mcVersion = getMinecraftVersion(player);
                clientVersions.put(uuid, mcVersion);

                boolean isSpoofed = detectSpoofing(uuid, reportedBrand, channels);

                if (isSpoofed) {
                    ConsoleLog.error("CLIENT SPOOFING DETECTED!");
                    ConsoleLog.error("Player: " + playerName);
                    ConsoleLog.error("Claimed brand: " + (reportedBrand != null ? reportedBrand : "none"));
                    ConsoleLog.error("Game version: " + mcVersion);
                    ConsoleLog.error("Registered channels: " + (channels != null ? String.join(", ", channels) : "none"));
                } else if (clientTypes == null || clientTypes.isEmpty()) {
                    ConsoleLog.info(playerName + " client: vanilla (or not reporting) | Game version: " + mcVersion);
                } else {
                    synchronized (clientTypes) {
                        ConsoleLog.info(playerName + " detected client(s): " + String.join(", ", clientTypes) + " | Game version: " + mcVersion);
                    }
                }
            }, 3);
        } catch (Throwable ignored) {}
    }
    
    public static Set<String> getClientTypes(UUID uuid) {
        Set<String> types = detectedClients.get(uuid);
        return types != null ? new HashSet<>(types) : new HashSet<>();
    }
    
    public static boolean isVanilla(UUID uuid) {
        Set<String> types = detectedClients.get(uuid);
        return types == null || types.isEmpty();
    }
    
    public static boolean isModded(UUID uuid) {
        return !isVanilla(uuid);
    }
    
    public static boolean isHackedClient(UUID uuid) {
        Set<String> types = detectedClients.get(uuid);
        if (types == null) return false;
        
        return types.contains("wurst") || types.contains("meteor") || types.contains("liquidbounce");
    }
    
    private static boolean detectSpoofing(UUID uuid, String reportedBrand, Set<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return false;
        }
        
        if (reportedBrand != null && (reportedBrand.equalsIgnoreCase("vanilla") || reportedBrand.isEmpty())) {
            Set<String> nonBrandChannels = new HashSet<>(channels);
            nonBrandChannels.removeIf(ch -> ch.toLowerCase().contains("brand"));
            
            if (!nonBrandChannels.isEmpty()) {
                ConsoleLog.warn("Spoof detected: Vanilla brand with extra channels: " + String.join(", ", nonBrandChannels));
                return true;
            }
        }
        
        for (String channel : channels) {
            String channelLower = channel.toLowerCase();
            for (String hackedChannel : KNOWN_HACKED_CHANNELS) {
                if (channelLower.contains(hackedChannel)) {
                    ConsoleLog.warn("Spoof detected: Blacklisted hacked client channel: " + channel);
                    return true;
                }
            }
        }
        
        if (reportedBrand != null) {
            for (Map.Entry<String, Set<String>> entry : REQUIRED_CHANNELS.entrySet()) {
                String clientName = entry.getKey();
                Set<String> requiredChannels = entry.getValue();
                
                if (reportedBrand.toLowerCase().contains(clientName)) {
                    boolean hasAllRequired = true;
                    for (String required : requiredChannels) {
                        boolean found = false;
                        for (String channel : channels) {
                            if (channel.toLowerCase().contains(required.toLowerCase())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            hasAllRequired = false;
                            break;
                        }
                    }
                    
                    if (!hasAllRequired) {
                        ConsoleLog.warn("Spoof detected: Claims " + clientName + " but missing required channels");
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    public static boolean isSpoofing(UUID uuid) {
        String brand = reportedBrands.get(uuid);
        Set<String> channels = registeredChannels.get(uuid);
        return detectSpoofing(uuid, brand, channels);
    }
    
    public static String getClientVersion(UUID uuid) {
        return clientVersions.getOrDefault(uuid, "Unknown");
    }
    
    private static String getMinecraftVersion(Player player) {
        if (player == null) return "Unknown";
        if (!packetEventsAvailable) return "Unknown";
        try {
            Object api = peGetAPI.invoke(null);
            if (api == null) return "Unknown";
            Object pm = apiGetPlayerManager.invoke(api);
            if (pm == null) return "Unknown";
            Object user = null;
            try {
                user = pmGetUser.invoke(pm, player);
            } catch (IllegalArgumentException iae) {
                user = pmGetUser.invoke(pm, player.getUniqueId());
            }
            if (user == null) return "Unknown";
            Object ver = null;
            if (userGetClientVersion != null) {
                ver = userGetClientVersion.invoke(user);
            } else {
                try {
                    Method m = user.getClass().getMethod("getClientVersion");
                    ver = m.invoke(user);
                } catch (Throwable ignored) {}
            }
            if (ver == null) return "Unknown";
            try {
                Object release = null;
                if (versionGetReleaseName != null) release = versionGetReleaseName.invoke(ver);
                else {
                    try { Method m = ver.getClass().getMethod("getReleaseName"); release = m.invoke(ver); } catch (Throwable ignored) {}
                }
                if (release != null && release instanceof String) {
                    String rel = (String) release;
                    if (!rel.equalsIgnoreCase("unknown") && !rel.trim().isEmpty()) return rel;
                }
            } catch (Throwable ignored) {}
            try {
                Object proto = null;
                if (versionGetProtocolVersion != null) proto = versionGetProtocolVersion.invoke(ver);
                else { try { Method m = ver.getClass().getMethod("getProtocolVersion"); proto = m.invoke(ver); } catch (Throwable ignored) {} }
                if (proto != null) return "Protocol " + proto.toString();
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            packetEventsAvailable = false;
            ConsoleLog.warn("PacketEvents reflection failed: " + t.getMessage());
        }
        return "Unknown";
    }

    private static void setupPacketEventsReflection() {
        String[] candidates = new String[] {
            "org.inventivetalent.packetevents.PacketEvents",
            "org.inventivetalent.packetwrapper.PacketEvents",
            "com.github.retrooper.packetevents.PacketEvents",
            "PacketEvents"
        };
        Class<?> peClass = null;
        for (String cname : candidates) {
            try {
                peClass = Class.forName(cname);
                if (peClass != null) break;
            } catch (ClassNotFoundException ignored) {}
        }
        if (peClass == null) {
            packetEventsAvailable = false;
            return;
        }
        try {
            peGetAPI = peClass.getMethod("getAPI");
            Object api = peGetAPI.invoke(null);
            if (api == null) { packetEventsAvailable = false; return; }
            try {
                apiGetPlayerManager = api.getClass().getMethod("getPlayerManager");
            } catch (NoSuchMethodException ex) {
                apiGetPlayerManager = api.getClass().getMethod("getUserManager");
            }
            Object pm = apiGetPlayerManager.invoke(api);
            if (pm == null) { packetEventsAvailable = false; return; }
            try {
                pmGetUser = pm.getClass().getMethod("getUser", org.bukkit.entity.Player.class);
            } catch (NoSuchMethodException ex) {
                pmGetUser = pm.getClass().getMethod("getUser", java.util.UUID.class);
            }
            try {
                Class<?> userCls = Class.forName("org.inventivetalent.packet.User");
                userGetClientVersion = userCls.getMethod("getClientVersion");
            } catch (Throwable ignored) {}
            try {
                Class<?> verCls = Class.forName("org.inventivetalent.packet.ClientVersion");
                try { versionGetReleaseName = verCls.getMethod("getReleaseName"); } catch (NoSuchMethodException e) {}
                try { versionGetProtocolVersion = verCls.getMethod("getProtocolVersion"); } catch (NoSuchMethodException e) {}
            } catch (Throwable ignored) {}
            packetEventsAvailable = true;
        } catch (Throwable t) {
            packetEventsAvailable = false;
        }
    }
    
    public static void cleanup(UUID uuid) {
        detectedClients.remove(uuid);
        registeredChannels.remove(uuid);
        reportedBrands.remove(uuid);
        clientVersions.remove(uuid);
    }

    public static Clients getInstance() {
        return INSTANCE;
    }

    public static int getTrackedCount() {
        return detectedClients.size();
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (player == null) return;
        
        String channelLower = channel.toLowerCase();
        UUID uuid = player.getUniqueId();
        Set<String> clientSet = detectedClients.computeIfAbsent(uuid, k -> Collections.synchronizedSet(new HashSet<>()));
        Set<String> channelSet = registeredChannels.computeIfAbsent(uuid, k -> Collections.synchronizedSet(new HashSet<>()));
        
        channelSet.add(channel);
        
        if (channelLower.contains("neoforge") && clientSet.add("neoforge")) {
            ConsoleLog.info(player.getName() + " reported client: NeoForge (via channel)");
        }
        if (channelLower.contains("quilt") && clientSet.add("quilt")) {
            ConsoleLog.info(player.getName() + " reported client: Quilt (via channel)");
        }
        if (channelLower.contains("fabric") && !channelLower.contains("quilt") && clientSet.add("fabric")) {
            ConsoleLog.info(player.getName() + " reported client: Fabric (via channel)");
        }
        if ((channelLower.contains("forge") || channelLower.contains("fml")) && !channelLower.contains("neoforge") && clientSet.add("forge")) {
            ConsoleLog.info(player.getName() + " reported client: Forge/FML (via channel)");
        }
        
        if ((channelLower.contains("lunar") || channelLower.contains("apollo")) && clientSet.add("lunar")) {
            ConsoleLog.warn(player.getName() + " reported client: Lunar Client (via channel)");
        }
        if (channelLower.contains("wurst") && clientSet.add("wurst")) {
            ConsoleLog.warn(player.getName() + " reported client: WURST HACKED CLIENT (via channel)");
        }
        if (channelLower.contains("meteor") && clientSet.add("meteor")) {
            ConsoleLog.warn(player.getName() + " reported client: METEOR HACKED CLIENT (via channel)");
        }
        if ((channelLower.contains("liquidbounce") || channelLower.contains("lb:")) && clientSet.add("liquidbounce")) {
            ConsoleLog.warn(player.getName() + " reported client: LIQUIDBOUNCE HACKED CLIENT (via channel)");
        }
        
        String payloadString = parsePayload(message);
        
        if (payloadString != null) {
            String payloadLower = payloadString.toLowerCase();
            
            if (payloadLower.contains("neoforge") && clientSet.add("neoforge")) {
                ConsoleLog.info(player.getName() + " reported client: NeoForge (via payload: " + payloadString + ")");
            }
            if (payloadLower.contains("quilt") && clientSet.add("quilt")) {
                ConsoleLog.info(player.getName() + " reported client: Quilt (via payload: " + payloadString + ")");
            }
            if ((payloadLower.contains("fabric") || payloadLower.contains("fabricloader") || payloadLower.contains("fabric-mod")) 
                && !payloadLower.contains("quilt") && clientSet.add("fabric")) {
                ConsoleLog.info(player.getName() + " reported client: Fabric (via payload: " + payloadString + ")");
            }
            if (!payloadLower.contains("neoforge") && (payloadLower.contains("forge") || payloadLower.contains("fml") || payloadLower.contains("minecraftforge")) 
                && clientSet.add("forge")) {
                ConsoleLog.info(player.getName() + " reported client: Forge/FML (via payload: " + payloadString + ")");
            }
            
            if ((payloadLower.contains("lunar") || payloadLower.contains("lunarclient")) && clientSet.add("lunar")) {
                ConsoleLog.warn(player.getName() + " reported client: Lunar Client (via payload: " + payloadString + ")");
            }
            if (payloadLower.contains("wurst") && clientSet.add("wurst")) {
                ConsoleLog.warn(player.getName() + " reported client: WURST HACKED CLIENT (via payload: " + payloadString + ")");
            }
            if (payloadLower.contains("meteor") && clientSet.add("meteor")) {
                ConsoleLog.warn(player.getName() + " reported client: METEOR HACKED CLIENT (via payload: " + payloadString + ")");
            }
            if (payloadLower.contains("liquidbounce") && clientSet.add("liquidbounce")) {
                ConsoleLog.warn(player.getName() + " reported client: LIQUIDBOUNCE HACKED CLIENT (via payload: " + payloadString + ")");
            }
            
            if (payloadLower.equals("vanilla") && clientSet.isEmpty()) {
                ConsoleLog.info(player.getName() + " reported client: Vanilla (via brand)");
                reportedBrands.put(uuid, "vanilla");
            } else if (payloadLower.contains("forge")) {
                reportedBrands.put(uuid, "forge");
            } else if (payloadLower.contains("fabric")) {
                reportedBrands.put(uuid, "fabric");
            } else if (payloadLower.contains("lunar")) {
                reportedBrands.put(uuid, "lunar");
            } else if (!payloadLower.isEmpty()) {
                reportedBrands.put(uuid, payloadString);
            }
        }
    }
    
    private String parsePayload(byte[] message) {
        String result = null;
        
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            try {
                result = in.readUTF();
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
        
        if (result == null) {
            try {
                result = new String(message, StandardCharsets.UTF_8);
                if (result.isEmpty() || result.contains("\0")) {
                    result = null;
                }
            } catch (Exception ignored) {
                result = null;
            }
        }
        
        return result;
    }
}

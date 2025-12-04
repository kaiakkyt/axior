package kaiakk.axior.proxy;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ProxyListener implements PluginMessageListener {
    
    private final Plugin plugin;
    private final Logger logger;
    
    public static final String CHANNEL_BUNGEE = "BungeeCord";
    public static final String CHANNEL_VELOCITY_MODERN = "velocity:main";
    public static final String CHANNEL_VELOCITY_LEGACY = "velocityplugin:main";
    
    private static final String SUBCHANNEL_CONNECT = "Connect";
    private static final String SUBCHANNEL_CONNECT_OTHER = "ConnectOther";
    private static final String SUBCHANNEL_IP = "IP";
    private static final String SUBCHANNEL_PLAYER_COUNT = "PlayerCount";
    private static final String SUBCHANNEL_PLAYER_LIST = "PlayerList";
    private static final String SUBCHANNEL_GET_SERVERS = "GetServers";
    private static final String SUBCHANNEL_GET_SERVER = "GetServer";
    private static final String SUBCHANNEL_UUID = "UUID";
    private static final String SUBCHANNEL_UUID_OTHER = "UUIDOther";
    private static final String SUBCHANNEL_SERVER_IP = "ServerIP";
    private static final String SUBCHANNEL_KICK_PLAYER = "KickPlayer";
    private static final String SUBCHANNEL_MESSAGE = "Message";
    private static final String SUBCHANNEL_MESSAGE_RAW = "MessageRaw";
    private static final String SUBCHANNEL_FORWARD = "Forward";
    private static final String SUBCHANNEL_FORWARD_TO_PLAYER = "ForwardToPlayer";
    
    private final Map<UUID, String> playerIPs = new ConcurrentHashMap<>();
    private final Map<String, Integer> serverPlayerCounts = new ConcurrentHashMap<>();
    private final Map<String, String[]> serverPlayerLists = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerUUIDs = new ConcurrentHashMap<>();
    
    private MessageCallback messageCallback;
    
    public ProxyListener(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    public void register() {
        try {
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_BUNGEE, this);
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_BUNGEE);
            logger.info("Registered BungeeCord plugin messaging channel");
        } catch (Exception e) {
            logger.warning("Failed to register BungeeCord channel: " + e.getMessage());
        }
        
        try {
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_VELOCITY_MODERN, this);
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_VELOCITY_MODERN);
            logger.info("Registered Velocity modern plugin messaging channel");
        } catch (Exception e) {
            logger.warning("Failed to register Velocity modern channel: " + e.getMessage());
        }
        
        try {
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_VELOCITY_LEGACY, this);
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_VELOCITY_LEGACY);
            logger.info("Registered Velocity legacy plugin messaging channel");
        } catch (Exception e) {
            logger.warning("Failed to register Velocity legacy channel: " + e.getMessage());
        }
    }
    
    public void unregister() {
        try {
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_BUNGEE, this);
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_BUNGEE);
        } catch (Exception ignored) {}
        
        try {
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_VELOCITY_MODERN, this);
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_VELOCITY_MODERN);
        } catch (Exception ignored) {}
        
        try {
            plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_VELOCITY_LEGACY, this);
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_VELOCITY_LEGACY);
        } catch (Exception ignored) {}
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equalsIgnoreCase(CHANNEL_BUNGEE)) {
            handleBungeeCordMessage(player, message);
        } else if (channel.equalsIgnoreCase(CHANNEL_VELOCITY_MODERN) || channel.equalsIgnoreCase(CHANNEL_VELOCITY_LEGACY)) {
            handleVelocityMessage(player, message);
        }
    }
    
    private void handleBungeeCordMessage(Player player, byte[] message) {
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            
            switch (subchannel) {
                case SUBCHANNEL_IP:
                    handleIPResponse(player, in);
                    break;
                case SUBCHANNEL_PLAYER_COUNT:
                    handlePlayerCountResponse(in);
                    break;
                case SUBCHANNEL_PLAYER_LIST:
                    handlePlayerListResponse(in);
                    break;
                case SUBCHANNEL_GET_SERVERS:
                    handleGetServersResponse(in);
                    break;
                case SUBCHANNEL_GET_SERVER:
                    handleGetServerResponse(in);
                    break;
                case SUBCHANNEL_UUID:
                case SUBCHANNEL_UUID_OTHER:
                    handleUUIDResponse(in);
                    break;
                case SUBCHANNEL_SERVER_IP:
                    handleServerIPResponse(in);
                    break;
                case SUBCHANNEL_FORWARD:
                case SUBCHANNEL_FORWARD_TO_PLAYER:
                    handleForwardedMessage(player, in);
                    break;
                default:
                    logger.fine("Received unknown BungeeCord subchannel: " + subchannel);
                    break;
            }
        } catch (Exception e) {
            logger.warning("Error handling BungeeCord message: " + e.getMessage());
        }
    }
    
    private void handleVelocityMessage(Player player, byte[] message) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String subchannel = in.readUTF();
            
            logger.fine("Received Velocity message on subchannel: " + subchannel);
            
            if (messageCallback != null) {
                messageCallback.onMessage(player, subchannel, message);
            }
        } catch (Exception e) {
            logger.warning("Error handling Velocity message: " + e.getMessage());
        }
    }
    
    
    public void connectPlayer(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_CONNECT);
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void connectOtherPlayer(Player sender, String targetPlayer, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_CONNECT_OTHER);
        out.writeUTF(targetPlayer);
        out.writeUTF(serverName);
        sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void requestPlayerIP(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_IP);
        player.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void requestPlayerCount(Player sender, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_PLAYER_COUNT);
        out.writeUTF(serverName);
        sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void requestPlayerList(Player sender, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_PLAYER_LIST);
        out.writeUTF(serverName);
        sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void requestServerList(Player sender) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_GET_SERVERS);
        sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void requestCurrentServer(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_GET_SERVER);
        player.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void requestPlayerUUID(Player sender, String playerName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_UUID_OTHER);
        out.writeUTF(playerName);
        sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void requestServerIP(Player sender, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_SERVER_IP);
        out.writeUTF(serverName);
        sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void kickPlayer(Player sender, String targetPlayer, String reason) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_KICK_PLAYER);
        out.writeUTF(targetPlayer);
        out.writeUTF(reason);
        sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void messagePlayer(Player sender, String targetPlayer, String message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_MESSAGE);
        out.writeUTF(targetPlayer);
        out.writeUTF(message);
        sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void messagePlayerRaw(Player sender, String targetPlayer, String jsonMessage) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(SUBCHANNEL_MESSAGE_RAW);
        out.writeUTF(targetPlayer);
        out.writeUTF(jsonMessage);
        sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, out.toByteArray());
    }
    
    public void forwardToServer(Player sender, String serverName, String subchannel, byte[] data) {
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgOut = new DataOutputStream(msgBytes);
            msgOut.writeUTF(SUBCHANNEL_FORWARD);
            msgOut.writeUTF(serverName);
            msgOut.writeUTF(subchannel);
            msgOut.writeShort(data.length);
            msgOut.write(data);
            
            sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, msgBytes.toByteArray());
        } catch (IOException e) {
            logger.warning("Error forwarding message to server: " + e.getMessage());
        }
    }
    
    public void forwardToPlayer(Player sender, String targetPlayer, String subchannel, byte[] data) {
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgOut = new DataOutputStream(msgBytes);
            msgOut.writeUTF(SUBCHANNEL_FORWARD_TO_PLAYER);
            msgOut.writeUTF(targetPlayer);
            msgOut.writeUTF(subchannel);
            msgOut.writeShort(data.length);
            msgOut.write(data);
            
            sender.sendPluginMessage(plugin, CHANNEL_BUNGEE, msgBytes.toByteArray());
        } catch (IOException e) {
            logger.warning("Error forwarding message to player: " + e.getMessage());
        }
    }
    
    
    private void handleIPResponse(Player player, ByteArrayDataInput in) {
        String ip = in.readUTF();
        int port = in.readInt();
        playerIPs.put(player.getUniqueId(), ip + ":" + port);
        logger.fine("Received IP for " + player.getName() + ": " + ip + ":" + port);
    }
    
    private void handlePlayerCountResponse(ByteArrayDataInput in) {
        String serverName = in.readUTF();
        int playerCount = in.readInt();
        serverPlayerCounts.put(serverName, playerCount);
        logger.fine("Received player count for " + serverName + ": " + playerCount);
    }
    
    private void handlePlayerListResponse(ByteArrayDataInput in) {
        String serverName = in.readUTF();
        String playerListStr = in.readUTF();
        String[] players = playerListStr.split(", ");
        serverPlayerLists.put(serverName, players);
        logger.fine("Received player list for " + serverName + ": " + playerListStr);
    }
    
    private void handleGetServersResponse(ByteArrayDataInput in) {
        String serverListStr = in.readUTF();
        logger.info("Network servers: " + serverListStr);
    }
    
    private void handleGetServerResponse(ByteArrayDataInput in) {
        String serverName = in.readUTF();
        logger.fine("Current server: " + serverName);
    }
    
    private void handleUUIDResponse(ByteArrayDataInput in) {
        String playerName = in.readUTF();
        String uuidString = in.readUTF();
        try {
            UUID uuid = UUID.fromString(uuidString);
            playerUUIDs.put(UUID.nameUUIDFromBytes(playerName.getBytes()), uuid);
            logger.fine("Received UUID for " + playerName + ": " + uuid);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid UUID received: " + uuidString);
        }
    }
    
    private void handleServerIPResponse(ByteArrayDataInput in) {
        String serverName = in.readUTF();
        String ip = in.readUTF();
        short port = in.readShort();
        logger.fine("Received IP for server " + serverName + ": " + ip + ":" + port);
    }
    
    private void handleForwardedMessage(Player player, ByteArrayDataInput in) {
        short length = in.readShort();
        byte[] data = new byte[length];
        in.readFully(data);
        
        if (messageCallback != null) {
            try {
                DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(data));
                String subchannel = dataIn.readUTF();
                messageCallback.onMessage(player, subchannel, data);
            } catch (IOException e) {
                logger.warning("Error processing forwarded message: " + e.getMessage());
            }
        }
    }
    
    
    public String getPlayerIP(UUID playerId) {
        return playerIPs.get(playerId);
    }
    
    public Integer getServerPlayerCount(String serverName) {
        return serverPlayerCounts.get(serverName);
    }
    
    public String[] getServerPlayerList(String serverName) {
        return serverPlayerLists.get(serverName);
    }
    
    public UUID getPlayerUUID(String playerName) {
        return playerUUIDs.get(UUID.nameUUIDFromBytes(playerName.getBytes()));
    }
    
    public void setMessageCallback(MessageCallback callback) {
        this.messageCallback = callback;
    }
    
    public interface MessageCallback {
        void onMessage(Player player, String subchannel, byte[] data);
    }
}

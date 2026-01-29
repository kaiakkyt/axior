package kaiakk.axior;

import kaiakk.axior.proxy.ProxyListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public interface PluginDelegate {
    void onEnable();
    void onDisable();
    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
    List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args);
    void onPluginMessageReceived(String channel, Player player, byte[] message);
    ProxyListener getProxyListener();
}

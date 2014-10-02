package com.connorlinfoot.actionbarapi;

import net.minecraft.server.v1_7_R4.ChatSerializer;
import net.minecraft.server.v1_7_R4.IChatBaseComponent;
import net.minecraft.server.v1_7_R4.PacketPlayOutChat;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;


public class API extends JavaPlugin implements Listener {

    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        Server server = getServer();
        ConsoleCommandSender console = server.getConsoleSender();

        console.sendMessage("");
        console.sendMessage(ChatColor.BLUE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        console.sendMessage("");
        console.sendMessage(ChatColor.AQUA + getDescription().getName());
        console.sendMessage(ChatColor.AQUA + "Version " + getDescription().getVersion());
        console.sendMessage("");
        console.sendMessage(ChatColor.BLUE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        console.sendMessage("");
    }

    public void onDisable() {
        getLogger().info(getDescription().getName() + " has been disabled!");
    }

    public static void sendActionBar(Player player, String message){
        CraftPlayer p = (CraftPlayer) player;
        if (p.getHandle().playerConnection.networkManager.getVersion() != 47) return; // Don't run if player is not on 1.8
        IChatBaseComponent cbc = ChatSerializer.a("{\"text\": \"" + message + "\"}");
        PacketPlayOutChat ppoc = new PacketPlayOutChat(cbc,2);
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(ppoc);
    }
}

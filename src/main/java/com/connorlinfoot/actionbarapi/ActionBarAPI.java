package com.connorlinfoot.actionbarapi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;


public class ActionBarAPI extends JavaPlugin implements Listener {
    private static Plugin plugin;
    private static String nmsver;
    private static boolean useOldMethods = false;

    public void onEnable() {
        plugin = this;
        getConfig().options().copyDefaults(true);
        saveConfig();

        CLUpdate clUpdate = new CLUpdate(this);

        Server server = getServer();
        ConsoleCommandSender console = server.getConsoleSender();

        nmsver = Bukkit.getServer().getClass().getPackage().getName();
        nmsver = nmsver.substring(nmsver.lastIndexOf(".") + 1);

        if (nmsver.equalsIgnoreCase("v1_8_R1") || nmsver.startsWith("v1_7_")) { // Not sure if 1_7 works for the protocol hack?
            useOldMethods = true;
        }

        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }

        console.sendMessage(ChatColor.AQUA + getDescription().getName() + " V" + getDescription().getVersion() + " has been enabled!");
        Bukkit.getPluginManager().registerEvents(clUpdate, this);
    }


    private static Class<?> craftPlayerClass, packetPlayOutChatClass, packetClass, chatSerializerClass,
            iChatBaseComponentClass, chatComponentTextClass, chatMessageTypeClass;
    private static Method m3;
    private static Constructor<?> chatComponentTextConstructor;
    //Depending on the version this is the constructor using chatMessageType or byte
    private static Constructor<?> packetPlayOutChatConstructor;
    private static Method craftPlayerHandleMethod;
    private static Field playerConnectionField;

    private static Object chatMessageType = null;

    //Initialize reflections
    private static void init() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
        craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsver + ".entity.CraftPlayer");
        packetPlayOutChatClass = Class.forName("net.minecraft.server." + nmsver + ".PacketPlayOutChat");
        packetClass = Class.forName("net.minecraft.server." + nmsver + ".Packet");

        chatSerializerClass = Class.forName("net.minecraft.server." + nmsver + ".ChatSerializer");
        iChatBaseComponentClass = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
        m3 = chatSerializerClass.getDeclaredMethod("a", String.class);

        chatComponentTextClass = Class.forName("net.minecraft.server." + nmsver + ".ChatComponentText");
        chatComponentTextConstructor = chatComponentTextClass.getConstructor(new Class<?>[]{String.class});

        try {
            chatMessageTypeClass = Class.forName("net.minecraft.server." + nmsver + ".ChatMessageType");
            Object[] chatMessageTypes = chatMessageTypeClass.getEnumConstants();
            for (Object obj : chatMessageTypes) {
                if (obj.toString().equals("GAME_INFO")) {
                    chatMessageType = obj;
                }
            }

            packetPlayOutChatConstructor = packetPlayOutChatClass.getConstructor(new Class<?>[]{iChatBaseComponentClass, chatMessageTypeClass});
        }catch(ClassNotFoundException e) {
            //Fallback
            packetPlayOutChatConstructor = packetPlayOutChatClass.getConstructor(new Class<?>[]{iChatBaseComponentClass, byte.class});
        }

        craftPlayerHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
        playerConnectionField = craftPlayerClass.getDeclaredField("playerConnection");
    }

    public static void sendActionBar(Player player, String message) {
        if (!player.isOnline()) {
            return; // Player may have logged out
        }

        // Call the event, if cancelled don't send Action Bar
        ActionBarMessageEvent actionBarMessageEvent = new ActionBarMessageEvent(player, message);
        Bukkit.getPluginManager().callEvent(actionBarMessageEvent);
        if (actionBarMessageEvent.isCancelled())
            return;

        try {
            Object craftPlayer = craftPlayerClass.cast(player);
            Object packet;

            if (useOldMethods) {
                Object cbc = iChatBaseComponentClass.cast(m3.invoke(chatSerializerClass, "{\"text\": \"" + message + "\"}"));
                packet = packetPlayOutChatClass.getConstructor(new Class<?>[]{iChatBaseComponentClass, byte.class}).newInstance(cbc, (byte) 2);
            } else {
                Object chatComponentText = chatComponentTextConstructor.newInstance(message);
                if (chatMessageTypeClass == null) {
                    packet = packetPlayOutChatConstructor.newInstance(chatComponentText, chatMessageType);
                }else{
                    packet = packetPlayOutChatConstructor.newInstance(chatComponentText, (byte) 2);
                }
            }

            Object craftPlayerHandle = craftPlayerHandleMethod.invoke(craftPlayer);
            Object playerConnection = playerConnectionField.get(craftPlayerHandle);
            Method sendPacketMethod = playerConnection.getClass().getDeclaredMethod("sendPacket", packetClass);
            sendPacketMethod.invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendActionBar(final Player player, final String message, int duration) {
        sendActionBar(player, message);

        if (duration >= 0) {
            // Sends empty message at the end of the duration. Allows messages shorter than 3 seconds, ensures precision.
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendActionBar(player, "");
                }
            }.runTaskLater(plugin, duration + 1);
        }

        // Re-sends the messages every 3 seconds so it doesn't go away from the player's screen.
        while (duration > 40) {
            duration -= 40;
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendActionBar(player, message);
                }
            }.runTaskLater(plugin, (long) duration);
        }
    }

    public static void sendActionBarToAllPlayers(String message) {
        sendActionBarToAllPlayers(message, -1);
    }

    public static void sendActionBarToAllPlayers(String message, int duration) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendActionBar(p, message, duration);
        }
    }
}
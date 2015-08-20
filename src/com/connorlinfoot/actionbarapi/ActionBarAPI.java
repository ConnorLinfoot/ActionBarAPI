package com.connorlinfoot.actionbarapi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class ActionBarAPI extends JavaPlugin
{
    public static Plugin plugin;
    public static boolean works = true;
    public static String nmsver;

    public void onEnable()
    {
        plugin = this;
        getConfig().options().copyDefaults(true);
        saveConfig();
        Server server = getServer();
        ConsoleCommandSender console = server.getConsoleSender();
        
        nmsver = Bukkit.getServer().getClass().getPackage().getName();
        nmsver = nmsver.substring(nmsver.lastIndexOf(".") + 1);

        console.sendMessage("");
        console.sendMessage(ChatColor.BLUE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        console.sendMessage("");
        console.sendMessage(ChatColor.AQUA + getDescription().getName());
        console.sendMessage(ChatColor.AQUA + "Version " + getDescription().getVersion());
        console.sendMessage("");
        console.sendMessage(ChatColor.BLUE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        console.sendMessage("");
    }

    public void onDisable()
    {
        getLogger().info(getDescription().getName() + " has been disabled!");
    }

    public static void sendActionBar(Player player, String message)
    {
        try
        {
            Class<?> c1 = Class.forName("org.bukkit.craftbukkit." + nmsver + ".entity.CraftPlayer");
            Object p = c1.cast(player);
            Object ppoc = null;
            Class<?> c4 = Class.forName("net.minecraft.server." + nmsver + ".PacketPlayOutChat");
            Class<?> c5 = Class.forName("net.minecraft.server." + nmsver + ".Packet");
            if (nmsver.equalsIgnoreCase("v1_8_R1") || !nmsver.startsWith("v1_8_"))
            {
                Class<?> c2 = Class.forName("net.minecraft.server." + nmsver + ".ChatSerializer");
                Class<?> c3 = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
                Method m3 = c2.getDeclaredMethod("a", new Class<?>[] {String.class});
                Object cbc = c3.cast(m3.invoke(c2, "{\"text\": \"" + message + "\"}"));
                ppoc = c4.getConstructor(new Class<?>[] {c3, byte.class}).newInstance(new Object[] {cbc, (byte) 2});
            } else
            {
                Class<?> c2 = Class.forName("net.minecraft.server." + nmsver + ".ChatComponentText");
                Class<?> c3 = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
                Object o = c2.getConstructor(new Class<?>[] {String.class}).newInstance(new Object[] {message});
                ppoc = c4.getConstructor(new Class<?>[] {c3, byte.class}).newInstance(new Object[] {o, (byte) 2});
            }
            Method m1 = c1.getDeclaredMethod("getHandle", new Class<?>[] {});
            Object h = m1.invoke(p);
            Field f1 = h.getClass().getDeclaredField("playerConnection");
            Object pc = f1.get(h);
            Method m5 = pc.getClass().getDeclaredMethod("sendPacket", new Class<?>[] {c5});
            m5.invoke(pc, ppoc);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            works = false;
        }
    }

    public static void sendActionBar(final Player player, final String message, int duration)
    {
        sendActionBar(player, message);

        // Sends empty message at the end of the duration. Allows messages shorter than 3 seconds, ensures precision.
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                sendActionBar(player, "");
            }
        }.runTaskLater(plugin, duration+1);

        // Re-sends the messages every 3 seconds so it doesn't go away from the player's screen.
        while (duration > 60)
        {
            duration -= 60;
            int sched = duration % 60;
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    sendActionBar(player, message);
                }
            }.runTaskLater(plugin, (long) sched);
        }

    }
}
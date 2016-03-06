package com.connorlinfoot.actionbarapi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class ActionBarAPI extends JavaPlugin implements Listener {
	public static Plugin plugin;
	public static boolean works = true;
	public static String nmsver;
	private String pluginPrefix = ChatColor.GRAY + "[" + ChatColor.AQUA + "ActionBarAPI" + ChatColor.GRAY + "] " + ChatColor.RESET;
	private String pluginMessage = null;
	private String updateMessage = null;
	private boolean updateAvailable = false;

	public void onEnable() {
		plugin = this;
		getConfig().options().copyDefaults(true);
		saveConfig();

		CLUpdate clUpdate = new CLUpdate(this);
		CLUpdate.UpdateResult updateResult = clUpdate.getResult();

		if (clUpdate.getMessage() != null) {
			pluginMessage = clUpdate.getMessage();
		}

		switch (updateResult) {
			default:
			case NO_UPDATE:
				updateAvailable = false;
				updateMessage = pluginPrefix + "No update was found, you are running the latest version.";
				break;
			case DISABLED:
				updateAvailable = false;
				updateMessage = pluginPrefix + "You currently have update checks disabled";
				break;
			case UPDATE_AVAILABLE:
				updateAvailable = true;
				updateMessage = pluginPrefix + "An update for " + getDescription().getName() + " is available, new version is " + clUpdate.getVersion() + ". Your installed version is " + getDescription().getVersion() + ".\nPlease update to the latest version :)";
				break;
		}

		Server server = getServer();
		ConsoleCommandSender console = server.getConsoleSender();

		nmsver = Bukkit.getServer().getClass().getPackage().getName();
		nmsver = nmsver.substring(nmsver.lastIndexOf(".") + 1);

		console.sendMessage(ChatColor.AQUA + getDescription().getName() + " V" + getDescription().getVersion() + " has been enabled!");
		if (updateMessage != null)
			console.sendMessage(updateMessage);
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (updateAvailable && event.getPlayer().isOp()) {
			event.getPlayer().sendMessage(updateMessage);
		}
		if (pluginMessage != null && event.getPlayer().isOp()) {
			event.getPlayer().sendMessage(pluginMessage);
		}
	}

	public static void sendActionBar(Player player, String message) {
		// Call the event, if cancelled don't send Action Bar
		ActionBarMessageEvent actionBarMessageEvent = new ActionBarMessageEvent(player, message);
		Bukkit.getPluginManager().callEvent(actionBarMessageEvent);
		if (actionBarMessageEvent.isCancelled())
			return;

		try {
			Class<?> c1 = Class.forName("org.bukkit.craftbukkit." + nmsver + ".entity.CraftPlayer");
			Object p = c1.cast(player);
			Object ppoc;
			Class<?> c4 = Class.forName("net.minecraft.server." + nmsver + ".PacketPlayOutChat");
			Class<?> c5 = Class.forName("net.minecraft.server." + nmsver + ".Packet");
			if ((nmsver.equalsIgnoreCase("v1_8_R1") || !nmsver.startsWith("v1_8_")) && !nmsver.startsWith("v1_9_")) {
				Class<?> c2 = Class.forName("net.minecraft.server." + nmsver + ".ChatSerializer");
				Class<?> c3 = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
				Method m3 = c2.getDeclaredMethod("a", String.class);
				Object cbc = c3.cast(m3.invoke(c2, "{\"text\": \"" + message + "\"}"));
				ppoc = c4.getConstructor(new Class<?>[]{c3, byte.class}).newInstance(cbc, (byte) 2);
			} else {
				Class<?> c2 = Class.forName("net.minecraft.server." + nmsver + ".ChatComponentText");
				Class<?> c3 = Class.forName("net.minecraft.server." + nmsver + ".IChatBaseComponent");
				Object o = c2.getConstructor(new Class<?>[]{String.class}).newInstance(message);
				ppoc = c4.getConstructor(new Class<?>[]{c3, byte.class}).newInstance(o, (byte) 2);
			}
			Method m1 = c1.getDeclaredMethod("getHandle");
			Object h = m1.invoke(p);
			Field f1 = h.getClass().getDeclaredField("playerConnection");
			Object pc = f1.get(h);
			Method m5 = pc.getClass().getDeclaredMethod("sendPacket", c5);
			m5.invoke(pc, ppoc);
		} catch (Exception ex) {
			ex.printStackTrace();
			works = false;
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
		while (duration > 60) {
			duration -= 60;
			int sched = duration % 60;
			new BukkitRunnable() {
				@Override
				public void run() {
					sendActionBar(player, message);
				}
			}.runTaskLater(plugin, (long) sched);
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
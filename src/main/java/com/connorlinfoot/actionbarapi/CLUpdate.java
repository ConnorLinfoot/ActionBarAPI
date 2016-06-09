package com.connorlinfoot.actionbarapi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class CLUpdate implements Listener {
	private CLUpdate.UpdateResult result = CLUpdate.UpdateResult.DISABLED;
	private String version;
	private Plugin plugin;
	private String message = null;
	private String pluginMessage = null;
	private String updateMessage = null;
	private boolean updateAvailable = false;

	public enum UpdateResult {
		NO_UPDATE,
		DISABLED,
		UPDATE_AVAILABLE
	}

	public CLUpdate(JavaPlugin plugin) {
		this.plugin = plugin;
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				doCheck();
			}
		});
	}

	private void doCheck() {
		String data = null;
		String url = "http://api.connorlinfoot.com/v1/resource/release/" + plugin.getDescription().getName().toLowerCase() + "/";
		try {
			data = doCurl(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
		JSONParser jsonParser = new JSONParser();
		try {
			JSONObject obj = (JSONObject) jsonParser.parse(data);
			if (obj.get("version") != null) {
				String newestVersion = (String) obj.get("version");
				String currentVersion = plugin.getDescription().getVersion().replaceAll("-SNAPSHOT-", "."); // Changes 4.0.0-SNAPSHOT-4 to 4.0.0.4
				if (Integer.parseInt(newestVersion.replace(".", "")) > Integer.parseInt(currentVersion.replace(".", ""))) {
					result = UpdateResult.UPDATE_AVAILABLE;
					version = (String) obj.get("version");
				} else {
					result = UpdateResult.NO_UPDATE;
				}
				if (obj.containsKey("message")) {
					this.message = ChatColor.translateAlternateColorCodes('&', (String) obj.get("message"));
					Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', (String) obj.get("message")));
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Bukkit.getScheduler().runTask(plugin, new Runnable() {
			@Override
			public void run() {
				handleResult();
			}
		});
	}

	public String getVersion() {
		return version;
	}

	public String doCurl(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setInstanceFollowRedirects(true);
		con.setDoOutput(true);
		con.setDoInput(true);
		DataOutputStream output = new DataOutputStream(con.getOutputStream());
		output.close();
		DataInputStream input = new DataInputStream(con.getInputStream());
		int c;
		StringBuilder resultBuf = new StringBuilder();
		while ((c = input.read()) != -1) {
			resultBuf.append((char) c);
		}
		input.close();
		return resultBuf.toString();
	}

	public String getMessage() {
		return message;
	}

	public void handleResult() {
		if (getMessage() != null) {
			pluginMessage = getMessage();
		}

		switch (result) {
			default:
			case NO_UPDATE:
				updateAvailable = false;
				updateMessage = "No update was found, you are running the latest version.";
				break;
			case DISABLED:
				updateAvailable = false;
				updateMessage = "You currently have update checks disabled";
				break;
			case UPDATE_AVAILABLE:
				updateAvailable = true;
				updateMessage = "An update for " + plugin.getDescription().getName() + " is available, new version is " + getVersion() + ". Your installed version is " + plugin.getDescription().getVersion() + ".\nPlease update to the latest version :)";
				break;
		}

		plugin.getLogger().info(updateMessage);
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

}
package com.connorlinfoot.actionbarapi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

public class CLUpdate {
	private CLUpdate.UpdateResult result = CLUpdate.UpdateResult.DISABLED;
	private String version;
	private Plugin plugin;
	private String message = null;

	public enum UpdateResult {
		NO_UPDATE,
		DISABLED,
		UPDATE_AVAILABLE
	}

	public CLUpdate(JavaPlugin plugin) {
		this.plugin = plugin;
		doCheck();
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
	}

	public UpdateResult getResult() {
		return result;
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
}
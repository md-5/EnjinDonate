package com.md_5.enjin.donate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Enjin extends JavaPlugin {

    private int interval;
    private String url;
    private String command;
    private String message;

    @Override
    public void onEnable() {
        FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        interval = conf.getInt("interval") * 1200;
        url = conf.getString("url");
        command = conf.getString("command");
        message = conf.getString("message");
        getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Checker(), 0, interval);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    public List<DonationData> getJSON(String url) {
        try {
            InputStreamReader reader = new InputStreamReader(new URL(url).openStream());
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            Gson gson = new Gson();
            List<DonationData> donations = new ArrayList<DonationData>();
            for (JsonElement el : array) {
                donations.add(gson.fromJson(el, DonationData.class));
            }
            reader.close();
            return donations;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public boolean isClaimed(String date) {
        return getConfig().getStringList("claims").contains(date);
    }

    public void claim(DonationData donation) {
        String user = donation.getCustom_field();
        String rank = donation.getItem_name();
        getServer().dispatchCommand(getServer().getConsoleSender(), String.format(command, user, rank));
        List<String> claims = getConfig().getStringList("claims");
        claims.add(donation.getPurchase_date());
        getConfig().set("claims", claims);
        saveConfig();
        getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + String.format(message, user, rank));
    }

    private class Checker implements Runnable {

        public void run() {
            getLogger().info("Checking for new donations");
            for (DonationData donation : getJSON(url)) {
                if (!isClaimed(donation.getPurchase_date())) {
                    claim(donation);
                }
            }
        }
    }
}

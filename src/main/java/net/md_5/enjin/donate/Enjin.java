package net.md_5.enjin.donate;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.plugin.java.JavaPlugin;

public class Enjin extends JavaPlugin {

    private Gson gson = new Gson();
    private int interval;
    private String url;
    private String command;
    private String message;
    private Map<String, Object> items;

    @Override
    public void onEnable() {
        FileConfiguration conf = getConfig();
        conf.options().copyDefaults(true);
        saveConfig();
        //
        interval = conf.getInt("interval") * 1200;
        url = conf.getString("url");
        command = conf.getString("command");
        message = conf.getString("message");
        //
        items = conf.getConfigurationSection("items").getValues(false);
        //
        if (url != null) {
            getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Checker(), 0, interval);
        } else {
            getLogger().severe("Please set your shop url in config.yml");
        }
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    private void setClaimed(String date) {
        List<String> claims = getConfig().getStringList("claims");
        claims.add(date);
        if (claims.size() > 100) {
            claims.remove(0);
        }
        getConfig().set("claims", claims);
        saveConfig();
    }

    private boolean isClaimed(String date) {
        return getConfig().getStringList("claims").contains(date);
    }

    private void setMoney(String player, int amount) {
        getConfig().set("players." + player, amount);
        saveConfig();
    }

    private int getMoney(String player) {
        return getConfig().getInt("players." + player);
    }

    private String getReward(int funds) {
        String itemName = null;
        int largest = 0;
        for (Map.Entry<String, Object> i : items.entrySet()) {
            int cost = (Integer) i.getValue();
            if (cost > largest && cost <= funds) {
                largest = cost;
                itemName = i.getKey();
            }
        }
        return itemName;
    }

    private DonationData[] getJSON(String url) {
        try {
            URLConnection con = new URL(url).openConnection();
            con.setRequestProperty("User-Agent", "EnjinDonate by md_5");
            InputStreamReader reader = new InputStreamReader(con.getInputStream());
            DonationData[] donations = gson.fromJson(reader, DonationData[].class);
            reader.close();
            return donations;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void claim(DonationData donation) {
        String user = donation.getCustom_field();
        int paid = Math.round(Float.parseFloat(donation.getItem_price()));
        if (user != null) {
            int current = getMoney(user) + paid;
            setMoney(user, current);
            //
            setClaimed(donation.getPurchase_date());
            //
            String rank = getReward(current);
            getServer().dispatchCommand(getServer().getConsoleSender(), MessageFormat.format(command, user, rank));
            //
            if (message != null) {
                getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + MessageFormat.format(message, user, rank));
            }
        }
    }

    private class Checker implements Runnable {

        @Override
        public void run() {
            getLogger().info("Checking for new donations");
            for (DonationData donation : getJSON(url)) {
                if (!isClaimed(donation.getPurchase_date())) {
                    claim(donation);
                }
            }
        }
    }

    @Data
    public class DonationData {

        private UserDetails user;
        private String item_name;
        private String item_price;
        private String purchase_date;
        private String currency;
        private String item_id;
        private String custom_field;
        private String character;

        @Data
        public class UserDetails {

            private String user_id;
            private String username;
        }
    }
}

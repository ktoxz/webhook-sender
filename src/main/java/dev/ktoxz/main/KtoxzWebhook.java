package dev.ktoxz.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ktoxz.commands.SetCentralChest;
import dev.ktoxz.db.Mongo;
import dev.ktoxz.listener.ChestListener;

public class KtoxzWebhook extends JavaPlugin {

    @Override
    public void onEnable() {
        // Đăng ký lệnh
        getCommand("setcentralchest").setExecutor(new SetCentralChest(this));

        // Đăng ký listener
        Bukkit.getPluginManager().registerEvents(new ChestListener(this), this);
        
        // Connect to DB
        Mongo.getInstance().Connect();
    }
}

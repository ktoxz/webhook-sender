package dev.ktoxz.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class KtoxzWebhook extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("✅ KtoxzWebhook đã bật!");

        // Đăng ký lệnh
        getCommand("setcentralchest").setExecutor(new SetCentralChest(this));

        // Đăng ký listener
        Bukkit.getPluginManager().registerEvents(new ChestListener(this), this);
    }
}

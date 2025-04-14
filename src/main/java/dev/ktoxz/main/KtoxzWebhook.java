package dev.ktoxz.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ktoxz.commands.*;
import dev.ktoxz.db.Mongo;
import dev.ktoxz.listener.ChestListener;
import dev.ktoxz.manager.ItemPriceCache;

public class KtoxzWebhook extends JavaPlugin {

    @Override
    public void onEnable() {
    	
        // Đăng ký lệnh
        getCommand("setcentralchest").setExecutor(new SetCentralChest(this));
        getCommand("addspot").setExecutor(new AddSpot(this));
        getCommand("givebalance").setExecutor(new GiveBalance(this));
        getCommand("balance").setExecutor(new Balance(this));
        getCommand("pay").setExecutor(new Pay(this));
        getCommand("ac").setExecutor(new AcceptPay(this));
        // Đăng ký listener
        Bukkit.getPluginManager().registerEvents(new ChestListener(this), this);
        getCommand("tp").setExecutor(new Teleport(this));
        getCommand("tp").setTabCompleter(new Teleport(this));
        // Connect to DB
        Mongo.getInstance().Connect();
        ItemPriceCache.load();

    }
}

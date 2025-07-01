package dev.ktoxz.main;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import dev.ktoxz.commands.*;
import dev.ktoxz.db.Mongo;
import dev.ktoxz.listener.ChestListener;
import dev.ktoxz.listener.PvpSessionListener;
import dev.ktoxz.manager.ItemPriceCache;
import dev.ktoxz.pvp.*;

public class KtoxzWebhook extends JavaPlugin {

    private WorldGuardPlugin worldGuardPlugin; // Thêm biến này

    @Override
    public void onEnable() {
    	 ChestManager.init(this);
    	 PvpSessionManager.init(this);
    	 PvpEventManager.init(this);
    	// Lấy WorldGuardPlugin instance
         worldGuardPlugin = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
         if (worldGuardPlugin == null) {
             getLogger().warning("WorldGuard not found! Region protection will not work.");
         }

        // Đăng ký lệnh
        getCommand("setcentralchest").setExecutor(new SetCentralChest(this));
        getCommand("addspot").setExecutor(new AddSpot(this));
        getCommand("givebalance").setExecutor(new GiveBalance(this));
        getCommand("balance").setExecutor(new Balance(this));
        getCommand("pay").setExecutor(new Pay(this));
        getCommand("ac").setExecutor(new AcceptPay(this));
        getCommand("link").setExecutor(new LinkDiscord(this));
        
        getCommand("dual").setExecutor(new Dual(this));
        getCommand("join").setExecutor(new Join(this));
        getCommand("cancel").setExecutor(new Cancel(this));
        getCommand("start").setExecutor(new Start(this));
        
        getCommand("addArena").setExecutor(new AddArena(this));
        getCommand("addArenaSpot").setExecutor(new AddArenaSpot(this));

        getCommand("watch").setExecutor(new Watch(this));

        // Đăng ký listener
        Bukkit.getPluginManager().registerEvents(new ChestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PvpSessionListener(this), this);

        getCommand("tp").setExecutor(new Teleport(this));
        getCommand("tp").setTabCompleter(new Teleport(this));
        
        // Connect to DB
        Mongo.getInstance().Connect();
        ItemPriceCache.load();
        //skills

    }
    
    public WorldGuardPlugin getWorldGuardPlugin() { // Phương thức này
        return worldGuardPlugin;
    }
}

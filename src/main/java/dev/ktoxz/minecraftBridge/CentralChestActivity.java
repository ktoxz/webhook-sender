package dev.ktoxz.minecraftBridge;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ktoxz.manager.WebhookManager;
import dev.ktoxz.commands.Constant;

public class CentralChestActivity extends JavaPlugin implements Listener {
	@Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("WebhookSender enabled!");
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
    	if (ChestBoolean.isCentralChest(event)) {
            String playerName = event.getPlayer().getName();
            String message = "\uD83D\uDCE6 " + playerName + " vừa mở rương trung tâm!";
            
            WebhookManager.getInstance().sendWebhook(message);
        }
    }
    
    @EventHandler
    public void onChestPut(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getClickedInventory() == null) return;
        if (e.getClickedInventory().getHolder() instanceof Chest chest) {
            Location loc = chest.getLocation();
            if (loc.equals(Constant.get(Constant.CONSTANT.CENTRAL_CHEST_POS))) {
                // Người chơi shift-click bỏ item vào rương
                ItemStack item = e.getCurrentItem();
                if (item != null && item.getType() != Material.AIR) {
                    String player = e.getWhoClicked().getName();
                    String itemType = item.getType().name();
                    int itemAmount = item.getAmount();
                    WebhookManager.getInstance().sendWebhook(player+" đã gửi "+itemAmount+" "+itemType);
                }
            }
        }
    }
    
 
}

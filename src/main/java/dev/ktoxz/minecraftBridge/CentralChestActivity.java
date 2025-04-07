package dev.ktoxz.minecraftBridge;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
            
            sendWebhook(message);
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
                    sendWebhook(player+" đã gửi "+itemAmount+" "+itemType);
                }
            }
        }
    }
    
private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1357654266768265247/opzaEhNzCcYfiMOB4TUtxGCV9fkjC4ruUbxoZp9NTa1CCOtprylIvdRZHl21e5pf5TZS";
    
    private static WebhookManager ins;
    
    public static WebhookManager getInstance() {
    	if(ins == null) ins = new WebhookManager();
    	return ins;
    }
    
    public void sendWebhook(String content) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(WEBHOOK_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String json = String.format("{\"content\":\"%s\"}", content);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes());
                    os.flush();
                }

                connection.getInputStream().close();
                connection.disconnect();
            } catch (Exception e) {
                getLogger().warning("Không thể gửi webhook: " + e.getMessage());
            }
        });
    }
    
 
}

package dev.ktoxz.webhooksender;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebhookSender extends JavaPlugin implements Listener {

    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/PASTE_YOUR_WEBHOOK_HERE";

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("WebhookSender enabled!");
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
    	if (event.getInventory().getType() == InventoryType.CHEST &&
    		    event.getInventory().getLocation() != null &&
    		    event.getInventory().getLocation().getBlockX() == 363 &&
    		    event.getInventory().getLocation().getBlockY() == 66 &&
    		    event.getInventory().getLocation().getBlockZ() == 280) {
            String playerName = event.getPlayer().getName();
            String message = "\uD83D\uDCE6 " + playerName + " vừa mở rương trung tâm!";
            sendWebhook(message);
        }
    }

    private void sendWebhook(String content) {
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

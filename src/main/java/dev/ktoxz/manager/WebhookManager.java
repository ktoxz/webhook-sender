package dev.ktoxz.manager;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebhookManager extends JavaPlugin implements Listener {

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

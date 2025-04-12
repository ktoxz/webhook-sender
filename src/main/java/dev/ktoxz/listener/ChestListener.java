package dev.ktoxz.listener;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import dev.ktoxz.manager.TransactionManager;
import dev.ktoxz.manager.UserManager;
import dev.ktoxz.manager.EffectManager;
import dev.ktoxz.manager.ItemPriceCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChestListener implements Listener {

    private final Plugin plugin;
    private Player chestOwner = null;

    public ChestListener(Plugin plugin) {
        this.plugin = plugin;
    }

    private Location getCentralChestLocation() {
        if (!plugin.getConfig().isConfigurationSection("central-chest")) return null;
        Map<String, Object> locMap = plugin.getConfig().getConfigurationSection("central-chest").getValues(false);
        return Location.deserialize(locMap);
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent e) {
        if (!(e.getInventory().getHolder() instanceof Chest chest)) return;

        Location loc = chest.getLocation();
        Location central = getCentralChestLocation();
        if (central == null || !loc.equals(central)) return;

        Player player = (Player) e.getPlayer();

        if (chestOwner != null && !chestOwner.equals(player)) {
            Bukkit.getScheduler().runTask(plugin, player::closeInventory);
            player.sendMessage("§c❌ Có người đang mở rương trung tâm, vui lòng đợi...");
            plugin.getLogger().info("🚫 " + player.getName() + " bị chặn vì " + chestOwner.getName() + " đang mở.");
            return;
        }

        chestOwner = player;
        plugin.getLogger().info("✅ " + player.getName() + " đang mở rương trung tâm.");
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof Chest chest)) return;

        Location loc = chest.getLocation();
        Location central = getCentralChestLocation();
        if (central == null || !loc.equals(central)) return;

        Player player = (Player) e.getPlayer();

        if (!player.equals(chestOwner)) {
            plugin.getLogger().info("ℹ️ " + player.getName() + " đóng rương nhưng không phải người mở chính.");
            return;
        }

        plugin.getLogger().info("📦 " + player.getName() + " đã đóng rương trung tâm.");

        List<Document> tradeableItems = new ArrayList<>();
        final double[] totalPrice = {0};
        boolean isLeftover = false;
        for (int i = 0; i < e.getInventory().getSize(); i++) {
            ItemStack item = e.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            String itemId = item.getType().name();
            if (ItemPriceCache.contains(itemId)) {
                double price = ItemPriceCache.getPrice(itemId);
                int quantity = item.getAmount();
                totalPrice[0] += price * quantity;

                tradeableItems.add(new Document()
                        .append("item", itemId)
                        .append("quantity", quantity)
                        .append("price", price)
                );

                e.getInventory().setItem(i, null); // remove item
            } else {
            	isLeftover = true;
                plugin.getLogger().info("⚠️ Không có giá quy đổi cho item: " + itemId + ", giữ lại.");
            }
        }
        
        if(isLeftover) {
        	player.sendMessage("Còn đồ trong rương do không trao đổi được!");
            EffectManager.showTradeLeftover(player);

        }

        TransactionManager.insertTransactionAsync(tradeableItems, player, () -> {
            player.sendMessage("§a✔ Giao dịch thành công! Tổng cộng: " + String.format("%.2f",totalPrice[0]) + " xu.");
            EffectManager.showTradeComplete(player);
            UserManager.showBalance(player);
            
        });

        chestOwner = null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (chestOwner != null && chestOwner.equals(e.getPlayer())) {
            plugin.getLogger().info("⚠️ " + e.getPlayer().getName() + " rời game khi đang mở rương. Reset quyền.");
            chestOwner = null;
        }
    }
}

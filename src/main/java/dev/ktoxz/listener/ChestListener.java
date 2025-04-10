package dev.ktoxz.listener;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import dev.ktoxz.db.MongoFind;
import dev.ktoxz.manager.TransactionManager;
import dev.ktoxz.manager.UserManager;

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

        // Chỉ người mở mới được giải phóng quyền
        if (!player.equals(chestOwner)) {
            plugin.getLogger().info("ℹ️ " + player.getName() + " đóng rương nhưng không phải người mở chính.");
            return;
        }

        plugin.getLogger().info("📦 " + player.getName() + " đã đóng rương trung tâm.");
        List<Document> tradeableItems = new ArrayList<>();
        MongoFind priceFinder = new MongoFind("minecraft", "itemTrade");
        double totalPrice = 0;
        Boolean isLeft = false;
        for (int i = 0; i < e.getInventory().getSize(); i++) {
            ItemStack item = e.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            String itemId = item.getType().name();
            // Kiểm tra có trong bảng giá không
            Document itemPriceDoc = priceFinder.One(new Document("_id", itemId), null);
            
            if (itemPriceDoc != null) {
            	double price = itemPriceDoc.getDouble("price") * item.getAmount();
                plugin.getLogger().info("✅ Đã phát hiện item có thể quy đổi: " + itemId + " x" + item.getAmount());
                totalPrice += price;
                // Lưu vào danh sách transaction
                tradeableItems.add(new Document()
                    .append("item", itemId)
                    .append("quantity", item.getAmount())
                    .append("price", price)
                );

                // Xoá khỏi inventory rương
                e.getInventory().setItem(i, null);
            } else {
                plugin.getLogger().info("⚠️ Không có giá quy đổi cho item: " + itemId + ", giữ nguyên.");
                isLeft = true;
            }
        }
        
        if(isLeft) {
        	player.sendMessage("Có đồ còn ở trong rương do không trao đổi được!");
        }

        
        int reCode = TransactionManager.insertTransaction(tradeableItems, player, totalPrice);
        if(reCode == 2 || reCode == 1) {
        	player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 2f, 1f);
        	player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2f, 1.3f);
        	player.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 10);
        	plugin.getLogger().info("Thêm transaction mới thành công");
        	plugin.getLogger().info("Thêm thành công "+totalPrice+" cho người chơi "+player.getName());
        	UserManager.showBalance(player);
        } else if(reCode == -1) {
        	plugin.getLogger().warning("Không có gì để thêm");
        }
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

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
        List<Document> itemList = new ArrayList<>();
        for (ItemStack item : e.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                plugin.getLogger().info("📥 Rương còn: " + item.getAmount() + " " + item.getType());
                Document itemDoc = new Document()
                        .append("item", item.getType().name())
                        .append("quantity", item.getAmount())
                        .append("price", 0);
                    itemList.add(itemDoc);
            }
        }

        
        int reCode = TransactionManager.insertTransaction(itemList, player);
        if(reCode == 1) {
        	plugin.getLogger().info("Thêm transaction mới thành công");
        } else if(reCode == -1) {
        	plugin.getLogger().warning("Không có gì để thêm");

        }
        e.getInventory().clear();
        chestOwner = null;
        player.sendMessage("§7✅ Rương trung tâm đã đóng.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (chestOwner != null && chestOwner.equals(e.getPlayer())) {
            plugin.getLogger().info("⚠️ " + e.getPlayer().getName() + " rời game khi đang mở rương. Reset quyền.");
            chestOwner = null;
        }
    }
}

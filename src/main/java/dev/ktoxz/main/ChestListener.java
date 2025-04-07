package dev.ktoxz.main;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class ChestListener implements Listener {

    private final Plugin plugin;

    public ChestListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChestPut(InventoryClickEvent e) {
        // ✅ Chỉ xử lý nếu là Player
        if (!(e.getWhoClicked() instanceof Player player)) return;

        // ✅ Lấy inventory phía trên (rương đang mở)
        Inventory topInventory = e.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof Chest chest)) return;

        // ✅ Kiểm tra config có lưu rương trung tâm không
        if (!plugin.getConfig().isConfigurationSection("central-chest")) {
            plugin.getLogger().warning("⚠ Rương trung tâm chưa được đặt.");
            return;
        }

        // ✅ Lấy vị trí rương trung tâm từ config
        Map<String, Object> locMap = plugin.getConfig().getConfigurationSection("central-chest").getValues(false);
        Location centralChestLoc = Location.deserialize(locMap);

        // ✅ So sánh vị trí rương
        if (!chest.getLocation().equals(centralChestLoc)) {
            plugin.getLogger().info("📦 Rương không phải rương trung tâm.");
            return;
        }

        // ✅ Kiểm tra item được click
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        // ✅ Chỉ log nếu player click vào rương (không phải inventory của mình)
        if (e.getClickedInventory() == topInventory) {
            plugin.getLogger().info("📥 " + player.getName() + " gửi " + item.getAmount() + " " + item.getType() + " vào rương trung tâm.");
        }
    }
}

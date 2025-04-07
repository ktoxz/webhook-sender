package dev.ktoxz.main;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class ChestListener implements Listener {

    private final Plugin plugin;

    public ChestListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChestPut(InventoryClickEvent e) {
    	plugin.getLogger().info("📦 InventoryClickEvent: " + e.toString());

        if (!(e.getWhoClicked() instanceof Player player)) {
            plugin.getLogger().info("❌ Người click không phải Player.");
            return;
        }

        plugin.getLogger().info("👤 Người click: " + player.getName());

        if (e.getClickedInventory() == null) {
            plugin.getLogger().info("❌ Clicked inventory là null.");
            return;
        }

        plugin.getLogger().info("📁 Loại inventory: " + e.getClickedInventory().getType());

        if (!(e.getClickedInventory().getHolder() instanceof Chest chest)) {
            plugin.getLogger().info("❌ Inventory không phải là Chest.");
            plugin.getLogger().info("📌 Holder class: " + e.getClickedInventory().getHolder());
            return;
        }
        plugin.getLogger().info("✅ Đã tương tác với chest tại: " + chest.getLocation());


        Map<String, Object> locMap = plugin.getConfig().getConfigurationSection("central-chest").getValues(false);
        Location central = Location.deserialize(locMap);

        if (!chest.getLocation().equals(central)) return;

        var item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        String playerName = e.getWhoClicked().getName();
        plugin.getLogger().info("📥 " + playerName + " gửi " + item.getAmount() + " " + item.getType() + " vào rương trung tâm.");
    }
}

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
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getClickedInventory().getHolder() instanceof Chest chest)) return;

        Map<String, Object> locMap = plugin.getConfig().getConfigurationSection("central-chest").getValues(false);
        Location central = Location.deserialize(locMap);

        if (!chest.getLocation().equals(central)) return;

        var item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        String player = e.getWhoClicked().getName();
        plugin.getLogger().info("📥 " + player + " gửi " + item.getAmount() + " " + item.getType() + " vào rương trung tâm.");
    }
}

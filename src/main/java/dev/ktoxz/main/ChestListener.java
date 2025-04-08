package dev.ktoxz.main;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class ChestListener implements Listener {

    private final Plugin plugin;
    private boolean isCentralChestOpen = false;

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

        if (isCentralChestOpen) {
            e.getPlayer().closeInventory();
            e.getPlayer().sendMessage("§c❌ Có người đang mở rương trung tâm, vui lòng đợi...");
            plugin.getLogger().info("🚫 " + e.getPlayer().getName() + " bị chặn vì rương trung tâm đang mở.");
        } else {
            isCentralChestOpen = true;
            plugin.getLogger().info("✅ " + e.getPlayer().getName() + " đang mở rương trung tâm.");
        }
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent e) {
    	if (isCentralChestOpen) {
            e.getPlayer().sendMessage("§c❌ Có người đang mở rương trung tâm, vui lòng đợi nhé...");
            return;
    	}
        if (!(e.getInventory().getHolder() instanceof Chest chest)) return;

        Location loc = chest.getLocation();
        Location central = getCentralChestLocation();
        if (central == null || !loc.equals(central)) return;

        plugin.getLogger().info("📦 " + e.getPlayer().getName() + " đã đóng rương trung tâm.");

        for (ItemStack item : e.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                plugin.getLogger().info("📥 Rương còn: " + item.getAmount() + " " + item.getType());
            }
        }
        
        e.getInventory().clear();
        isCentralChestOpen = false;
        e.getPlayer().sendMessage("§7✅ Rương trung tâm đã đóng.");
    }
}

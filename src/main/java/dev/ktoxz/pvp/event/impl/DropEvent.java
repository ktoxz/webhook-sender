package dev.ktoxz.pvp.event.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import dev.ktoxz.pvp.PvpSessionManager;
import dev.ktoxz.pvp.event.PvpEvent;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class DropEvent extends PvpEvent {

    private static final Random random = new Random();

    // 🎯 Định nghĩa từng item drop sẵn sàng (bao gồm cả meta)
    private record DropItemDefinition(ItemStack item, String message) {}

    private static final List<DropItemDefinition> ALL_DROP_ITEMS = List.of(
        new DropItemDefinition(new ItemStack(Material.TOTEM_OF_UNDYING), msg("Totem rơi gần bạn!")),
        new DropItemDefinition(new ItemStack(Material.GOLDEN_APPLE), msg("Golden Apple spawn!")),
        new DropItemDefinition(new ItemStack(Material.SHIELD), msg("Khiên spawn!")),
        new DropItemDefinition(new ItemStack(Material.ENDER_PEARL, 3), msg("Ender Pearl x3 spawn!")),
        new DropItemDefinition(buildGodSword(), msg("Kiếm 1-hit spawn!"))
    );

    private static String msg(String m) {
        return "[ITEM] " + m;
    }

    public DropEvent(Plugin plugin) {
        super("Drop Event", "Một vật phẩm sẽ được thả xuống đấu trường", plugin);
        
    }

    @Override
    public void trigger(Set<Player> players) {
        DropItemDefinition dropDef = ALL_DROP_ITEMS.get(random.nextInt(ALL_DROP_ITEMS.size()));
        
		Location loc = PvpSessionManager.getActiveSession().getRandomLocationInArena().add(0, new Random().nextInt(20), 0);
        World world = loc.getWorld();
        world.dropItemNaturally(loc, dropDef.item().clone()); // clone để tránh shared item


        broadcastActionBar(players, dropDef.message());
    }

    // --- Vật phẩm mẫu định nghĩa sẵn ---

    private static ItemStack buildGodSword() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 10, true);
            if (meta instanceof Damageable damageable) {
                int max = sword.getType().getMaxDurability();
                damageable.setDamage(max - 1); // chỉ còn 1 lần chém
            }
            meta.setDisplayName("§cOne-Hit Sword");
            sword.setItemMeta(meta);
        }
        return sword;
    }

    @Override
    public void onEndMatch() {
        
    }

}

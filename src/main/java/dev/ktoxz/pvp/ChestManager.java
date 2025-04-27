package dev.ktoxz.pvp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ChestManager {

    private static final Map<UUID, Block> playerChests = new HashMap<>();
    private static final Random random = new Random();

    private static final Set<Material> weaponMaterials = Set.of(
            Material.IRON_SWORD, Material.IRON_AXE, Material.BOW, Material.TRIDENT
    );

    private static final Set<Material> utilityMaterials = Set.of(
            Material.ENDER_PEARL, Material.TOTEM_OF_UNDYING, Material.FIRE_CHARGE
    );

    private static final Set<Material> foodMaterials = Set.of(
            Material.GOLDEN_APPLE, Material.BAKED_POTATO, Material.GOLDEN_CARROT
    );

    private static final Set<Material> potionMaterials = Set.of(
            Material.POTION, Material.SPLASH_POTION
    );

    private static final Set<Material> specialMaterials = Set.of(
            Material.TNT
    );

    // Ghi chú Plugin để sử dụng schedule task
    private static Plugin plugin;

    public static void init(Plugin pl) {
        plugin = pl;
    }

    public static void spawnChestNearPlayer(Player player) {
        Location loc = player.getLocation().clone();
        loc.add(loc.getDirection().setY(0).normalize().multiply(2));
        loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);

        Block block = loc.getBlock();
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest chest) {
            fillChestWithItems(chest.getInventory());
        }

        playerChests.put(player.getUniqueId(), block);

        startCountdown(player);
    }

    static void fillChestWithItems(Inventory inv) {
        inv.addItem(createRandomWeapon());
        inv.addItem(createRandomUtility());
        inv.addItem(createRandomFood());
        inv.addItem(createRandomPotion());
        inv.addItem(createSpecialItem());
    }

    private static ItemStack createRandomWeapon() {
        Material material = getRandomElement(weaponMaterials);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (material.toString().contains("SWORD") || material.toString().contains("AXE")) {
                meta.addEnchant(Enchantment.SHARPNESS, 1, true); // Sharpness I
            } else if (material == Material.BOW) {
                meta.addEnchant(Enchantment.POWER, 1, true); // Power I
            } else if (material == Material.TRIDENT) {
                meta.addEnchant(Enchantment.LOYALTY, 1, true); // Loyalty I
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createRandomUtility() {
        Material material = getRandomElement(utilityMaterials);
        if (material == Material.ENDER_PEARL) return new ItemStack(material, 10);
        if (material == Material.FIRE_CHARGE) return new ItemStack(material, 64);
        return new ItemStack(material, 1);
    }

    private static ItemStack createRandomFood() {
        Material material = getRandomElement(foodMaterials);
        if (material == Material.GOLDEN_APPLE) return new ItemStack(material, 2);
        if (material == Material.GOLDEN_CARROT) return new ItemStack(material, 6);
        return new ItemStack(material, 15);
    }

    private static ItemStack createRandomPotion() {
        Material material = getRandomElement(potionMaterials);
        return new ItemStack(material, 1);
    }

    private static ItemStack createSpecialItem() {
        return new ItemStack(Material.TNT, 5);
    }

    private static <T> T getRandomElement(Set<T> set) {
        int index = random.nextInt(set.size());
        return new ArrayList<>(set).get(index);
    }

    private static void startCountdown(Player player) {
        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (countdown == 0) {
                    destroyPlayerChest(player);
                    player.sendTitle("§cBẮT ĐẦU!", "§aChiến đấu ngay!", 0, 40, 0);
                    cancel();
                    return;
                }

                player.sendTitle("§eChuẩn bị lấy đồ!", "§f" + countdown + " giây", 0, 20, 0);
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // mỗi giây (20 ticks)
    }

    public static void destroyPlayerChest(Player player) {
        Block chestBlock = playerChests.get(player.getUniqueId());
        if (chestBlock != null && chestBlock.getType() == Material.CHEST) {
            Location loc = chestBlock.getLocation().add(0.5, 0.5, 0.5);

            // Phá rương
            chestBlock.setType(Material.AIR);

            // 1. Hiệu ứng explosion mạnh hơn
            loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3); // Explosion lớn
            loc.getWorld().spawnParticle(Particle.FLAME, loc, 50, 0.5, 0.5, 0.5, 0.05); // Lửa tung tóe
            loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 30, 0.5, 0.5, 0.5, 0.1); // Khói lớn

            // 2. Âm thanh
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1f); // Boom mạnh
            loc.getWorld().playSound(loc, Sound.BLOCK_CHEST_CLOSE, 1f, 0.8f); // Âm chest đóng lại

            // 3. Nhạc nền chiến đấu
            player.stopSound(Sound.MUSIC_OVERWORLD_MEADOW); // Dừng nhạc nền cũ nếu có
            player.playSound(player.getLocation(), Sound.MUSIC_DISC_PIGSTEP, SoundCategory.MUSIC, 100f, 1f);
            // Hoặc chọn nhạc bạn thích: Pigstep (nether music), music intense

        }
        playerChests.remove(player.getUniqueId());
    }

    
    @EventHandler
    public void onChestClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (event.getCurrentItem() == null) return;

        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        // Kiểm tra nếu inventory đang mở là rương PvP (là chest và đang lấy đồ)
        if (clickedInventory.getType() == org.bukkit.event.inventory.InventoryType.CHEST) {
            int currentItemCount = countValidItems(player);

            // Nếu đã có 10 vật phẩm → không cho nhặt thêm
            if (currentItemCount >= 10) {
                event.setCancelled(true);
                player.sendMessage("§c❌ Bạn đã lấy đủ 10 vật phẩm được phép!");
            }
        }
    }

    private int countValidItems(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            count++;
        }
        return count;
    }
}

package dev.ktoxz.pvp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
            Material.ENDER_PEARL, Material.FIRE_CHARGE, Material.WIND_CHARGE
    );

    private static final Set<Material> foodMaterials = Set.of(
            Material.GOLDEN_APPLE, Material.BAKED_POTATO, Material.GOLDEN_CARROT
    );

    private static final Set<Material> potionMaterials = Set.of(
            Material.POTION, Material.SPLASH_POTION
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

        // 🛠 Xoay chest theo hướng người chơi
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Chest chestData) {
            chestData.setFacing(getFacingDirection(player));
            block.setBlockData(chestData);
        }

        playerChests.put(player.getUniqueId(), block);

        startCountdown(player);
    }
    
    private static BlockFace getFacingDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        yaw = (yaw % 360 + 360) % 360;

        if (yaw >= 45 && yaw < 135) {
            return BlockFace.WEST; // nhìn qua trái
        } else if (yaw >= 135 && yaw < 225) {
            return BlockFace.NORTH; // nhìn về sau
        } else if (yaw >= 225 && yaw < 315) {
            return BlockFace.EAST; // nhìn qua phải
        } else {
            return BlockFace.SOUTH; // mặc định nhìn phía trước
        }
    }


    static void fillChestWithItems(Inventory inv) {
        int slot = 0;
        int size = inv.getSize(); // lấy số lượng slot của rương

        for (Material material : weaponMaterials) {
            if (slot >= size) break;
            inv.setItem(slot++, createWeapon(material));
        }

        slot = Math.max(slot, 9);
        for (Material material : utilityMaterials) {
            if (slot >= size) break;
            inv.setItem(slot++, createUtility(material));
        }

        slot = Math.max(slot, 18);
        for (Material material : foodMaterials) {
            if (slot >= size) break;
            inv.setItem(slot++, createFood(material));
        }

        slot = Math.max(slot, 27);
        for (Material material : potionMaterials) {
            if (slot >= size) break;
            inv.setItem(slot++, createPotion(material));
        }
    }


    private static ItemStack createWeapon(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (material.toString().contains("SWORD") || material.toString().contains("AXE")) {
                meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            } else if (material == Material.BOW) {
                meta.addEnchant(Enchantment.POWER, 1, true);
            } else if (material == Material.TRIDENT) {
                meta.addEnchant(Enchantment.LOYALTY, 1, true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createUtility(Material material) {
        if (material == Material.ENDER_PEARL) return new ItemStack(material, 10);
        if (material == Material.FIRE_CHARGE) return new ItemStack(material, 64);
        return new ItemStack(material, 1);
    }

    private static ItemStack createFood(Material material) {
        if (material == Material.GOLDEN_APPLE) return new ItemStack(material, 2);
        if (material == Material.GOLDEN_CARROT) return new ItemStack(material, 6);
        return new ItemStack(material, 15);
    }

    private static ItemStack createPotion(Material material) {
        return new ItemStack(material, 1);
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

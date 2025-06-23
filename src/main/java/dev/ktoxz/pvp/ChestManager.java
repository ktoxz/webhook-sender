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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ChestManager {

    private static final Map<UUID, Block> playerChests = new HashMap<>();

   

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
        inv.clear(); // Xóa tất cả vật phẩm hiện có trong rương

        // 1. Kiếm sắt enchant Sharpness II
        ItemStack sword = new ItemStack(Material.STONE_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 2);
        inv.setItem(0, sword); // Đặt vào slot đầu tiên

        // 2. Cung enchant Power II
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.POWER, 2);
        inv.setItem(1, bow);

        // 3. 10 mũi tên
        ItemStack arrows = new ItemStack(Material.ARROW, 10);
        inv.setItem(2, arrows);

        // 4. Bộ giáp lưới
        ItemStack chainHelmet = new ItemStack(Material.IRON_HELMET);
        inv.setItem(3, chainHelmet);

        ItemStack chainChestplate = new ItemStack(Material.IRON_CHESTPLATE);
        inv.setItem(4, chainChestplate);

        ItemStack chainLeggings = new ItemStack(Material.IRON_LEGGINGS);
        inv.setItem(5, chainLeggings);

        ItemStack chainBoots = new ItemStack(Material.IRON_BOOTS);
        inv.setItem(6, chainBoots);

        // 5. 1 Táo vàng
        ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE, 1);
        inv.setItem(7, goldenApple);
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
            player.playSound(player.getLocation(), Sound.MUSIC_END, SoundCategory.MASTER, 1f, 1f);
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
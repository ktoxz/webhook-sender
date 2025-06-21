package dev.ktoxz.pvp.event.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import dev.ktoxz.pvp.PvpSessionManager;
import dev.ktoxz.pvp.event.PvpEvent;

public class PitfallTrapEvent extends PvpEvent{

    // Không cần activeTasks ở đây nữa, nó đã ở PvpEvent
    // private final List<BukkitTask> activeTasks = new ArrayList<>();

    public PitfallTrapEvent(Plugin plugin) {
        super("Bẫy", "Hiển thị bẫy dưới chân", plugin);
    }

    @Override
    public void trigger(Set<Player> players) {
        List<Consumer<Set<Player>>> trapBehaviors = List.of(
            this::TrapUnder,
            this::iceTrapArea
        );

        Random random = new Random();
        trapBehaviors.get(random.nextInt(trapBehaviors.size())).accept(players);
    }

    // Không cần onEndMatch() nữa nếu không có logic cụ thể nào khác
    // @Override
    // public void onEndMatch() {
    //     super.onEndMatch(); // Gọi phương thức của lớp cha để hủy tác vụ
    // }

    private void TrapUnder(Set<Player> players) {
        Map<Location, Material> originalBlocks = new HashMap<>();

        for (Player p : players) {
            Location loc = p.getLocation().clone().subtract(0, 1, 0).getBlock().getLocation();
            Block block = loc.getBlock();
            originalBlocks.put(loc, block.getType());
            block.setType(Material.LAVA);
        }

        broadcastActionBar(players, "[ENVIRONMENT] Bẫy Lava dưới chân bạn!");

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
                    Block block = entry.getKey().getBlock();
                    if (block.getType() == Material.LAVA) {
                        block.setType(entry.getValue());
                    }
                }
                broadcastActionBar(players, "[ENVIRONMENT] Đã tắt bẫy Lava!");
            }
        }.runTaskLater(plugin, 20 * 7);
        activeTasks.add(task); // Rút gọn: chỉ cần add thẳng vào activeTasks của lớp cha
    }

    private void iceTrapArea(Set<Player> players) {
        Map<Location, Material> frozenBlocks = new HashMap<>();
        Random rand = new Random();

        for (int i = 0; i < 9; i++) {
            Location randomLoc = 
                PvpSessionManager.getActiveSession()
                    .getRandomLocationInArena()
                    .add(0, 20 + rand.nextInt(10), 0)
            ;

            Block block = randomLoc.getBlock();
            if (!frozenBlocks.containsKey(randomLoc) && block.getType().isSolid()) {
                frozenBlocks.put(randomLoc, block.getType());
                block.setType(Material.PACKED_ICE);
            }
        }

        broadcastActionBar(players, "[ENVIRONMENT] 🧊 Bẫy băng giá đã đóng băng sàn đấu!");

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Location, Material> entry : frozenBlocks.entrySet()) {
                    Block block = entry.getKey().getBlock();
                    if (block.getType() == Material.PACKED_ICE) {
                        block.setType(entry.getValue());
                    }
                }
                broadcastActionBar(players, "[ENVIRONMENT] 🌡️ Băng đã tan, sàn đấu trở lại bình thường!");
            }
        }.runTaskLater(plugin, 20 * 7);
        activeTasks.add(task); // Rút gọn: chỉ cần add thẳng vào activeTasks của lớp cha
    }
}
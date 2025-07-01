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
        Map<Location, Material> lavaBlocks = new HashMap<>();

        List<Location> locs = PvpSessionManager.getActiveSession().getGround();
        if (locs == null || locs.isEmpty()) return;

        for (Location loc : locs) {
            Block block = loc.getBlock();
            if (block.getType() == Material.CHISELED_DEEPSLATE) {
                lavaBlocks.put(loc, block.getType());
                block.setType(Material.LAVA);
            }
        }

        if (!lavaBlocks.isEmpty()) {
            broadcastActionBar(players, "[ENVIRONMENT] 🔥 Bẫy Lava đã được kích hoạt!");
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Location, Material> entry : lavaBlocks.entrySet()) {
                    Block block = entry.getKey().getBlock();
                    if (block.getType() == Material.LAVA) {
                        block.setType(entry.getValue());
                    }
                }
                if (!lavaBlocks.isEmpty()) {
                    broadcastActionBar(players, "[ENVIRONMENT] 💧 Đã tắt bẫy Lava!");
                }
            }
        }.runTaskLater(plugin, 20 * 7);
    }



    private void iceTrapArea(Set<Player> players) {
        Map<Location, Material> frozenBlocks = new HashMap<>();

        List<Location> locs = PvpSessionManager.getActiveSession().getGround();
        if (locs == null || locs.isEmpty()) return;

        for (Location loc : locs) {
            Block block = loc.getBlock();
            if (block.getType() == Material.CHISELED_DEEPSLATE) {
                frozenBlocks.put(loc, block.getType());
                block.setType(Material.PACKED_ICE);
            }
        }

        if (!frozenBlocks.isEmpty()) {
            broadcastActionBar(players, "[ENVIRONMENT] 🧊 Bẫy băng giá đã đóng băng sàn đấu!");
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Location, Material> entry : frozenBlocks.entrySet()) {
                    Block block = entry.getKey().getBlock();
                    if (block.getType() == Material.PACKED_ICE) {
                        block.setType(entry.getValue());
                    }
                }
                if (!frozenBlocks.isEmpty()) {
                    broadcastActionBar(players, "[ENVIRONMENT] 🌡️ Băng đã tan, sàn đấu trở lại bình thường!");
                }
            }
        }.runTaskLater(plugin, 20 * 7); // 7 giây sau
    }

}
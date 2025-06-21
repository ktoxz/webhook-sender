package dev.ktoxz.pvp.event.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import dev.ktoxz.pvp.PvpSessionManager;
import dev.ktoxz.pvp.event.PvpEvent;

public class FunEvent extends PvpEvent{

	private static final List<Consumer<Set<Player>>> FUN_ACTIONS = List.of(
		    FunEvent::chickenShow,
		    FunEvent::fireworkShow
		);

	
	public FunEvent(Plugin plugin) {
		super("Vui vẻ", "Sự kiện vui vẻ, lúc vui lúc không", plugin);
		
	}
	

	@Override
	public void trigger(Set<Player> players) {
	    if (players == null || players.isEmpty()) return;

	    Consumer<Set<Player>> chosen = FUN_ACTIONS.get(new Random().nextInt(FUN_ACTIONS.size()));
	    chosen.accept(players);
	}

	@Override
	public void onEndMatch() {
		// TODO Auto-generated method stub
		
	}
	
	private static void chickenShow(Set<Player> players) {
		List<Chicken> chickens = new ArrayList<>();
        World world = PvpSessionManager.getActiveSession().getRandomLocationInArena().getWorld();

        // Spawn 5 con gà ngẫu nhiên trong đấu trường
        for (int i = 0; i < 5; i++) {
            Location loc = PvpSessionManager.getActiveSession().getRandomLocationInArena();
            Chicken chicken = (Chicken) world.spawnEntity(loc.add(0, 1 + new Random().nextInt(5), 0), EntityType.CHICKEN);
            chickens.add(chicken);
        }

        broadcastActionBar(players, "[FUN] 🐔 Gà xâm chiếm đấu trường!");

        // Đợi 5–10 giây rồi tiêu diệt gà và xử lý hiệu ứng
        Random rand = new Random();
        int delaySeconds = 5 + rand.nextInt(6); // 5 đến 10 giây
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Chicken chicken : chickens) {
                    if (chicken.isValid()) {
                        Location loc = chicken.getLocation();

                        // 50% bắn pháo hoa, 50% phát nổ như creeper
                        chicken.remove();
                        if (rand.nextBoolean()) {
                            // Pháo hoa
                            Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
                            FireworkMeta meta = fw.getFireworkMeta();
                            meta.addEffect(FireworkEffect.builder()
                                    .withColor(Color.RED)
                                    .with(Type.CREEPER)
                                    .trail(true)
                                    .build());
                            meta.setPower(3);
                            fw.setFireworkMeta(meta);
                            fw.detonate();
                        } else {
                            loc.getWorld().createExplosion(loc, 2.0f, false, false); // power 2, không đốt, không phá block
                        }

                        
                    }
                }

                broadcastActionBar(players, "[FUN] 💥 Cuộc xâm lăng của gà đã kết thúc!");
            }
        }.runTaskLater(plugin, delaySeconds * 20L); // Chuyển giây thành ticks
	}
	
    private static void fireworkShow(Set<Player> players) {
        for (int i = 0; i < 3; i++) {
            Location loc = PvpSessionManager.getActiveSession().getRandomLocationInArena().add(0, 2, 0);
            Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().flicker(true).trail(true).withColor(Color.AQUA).build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
        broadcastActionBar(players, "[FUN] Bắn pháo hoa khắp đấu trường!");
    }

    
}

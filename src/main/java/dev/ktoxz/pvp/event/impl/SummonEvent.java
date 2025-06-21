package dev.ktoxz.pvp.event.impl;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.loot.Lootable;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import dev.ktoxz.pvp.PvpSessionManager;
import dev.ktoxz.pvp.event.PvpEvent;

public class SummonEvent extends PvpEvent {

    private final Random random = new Random();
    private final List<Entity> summonedEntities = new ArrayList<>();

    private static final List<SummonStrategy> ALL_SUMMONS = List.of(
        SummonEvent::summonShulkerRain,
        SummonEvent::summonSkeletonOnBee,
        SummonEvent::summonEvokerFangsSpam
    );

    public SummonEvent(Plugin plugin) {
        super("Summon Event", "Triệu hồi quái vật đặc biệt tấn công đấu trường", plugin);
    }

    @Override
    public void trigger(Set<Player> players) {
        if (players == null || players.isEmpty()) return;

        SummonStrategy strategy = ALL_SUMMONS.get(random.nextInt(ALL_SUMMONS.size()));
        strategy.execute(players, summonedEntities);
    }

    @Override
    public void onEndMatch() {
        for (Entity e : summonedEntities) {
            if (e != null && !e.isDead()) {
                e.remove();
            }
        }
        summonedEntities.clear();
    }

    @FunctionalInterface
    private interface SummonStrategy {
        void execute(Set<Player> players, List<Entity> summonedEntities);
    }

    // ===== TRIỆU HỒI =====

    private static void summonShulkerRain(Set<Player> players, List<Entity> summonedEntities) {
        broadcastActionBar(players, "[SUMMON] ☠️ Shulker mưa đạn xuất hiện!");

        for (int i = 0; i < 3; i++) {
            Location loc = PvpSessionManager.getActiveSession().getRandomLocationInArena().add(0, 1, 0);
            World world = loc.getWorld();
            if (world == null) continue;

            Shulker shulker = (Shulker) world.spawnEntity(loc, EntityType.SHULKER);
            makeWeak(shulker);
            summonedEntities.add(shulker);
        }
    }

    private static void summonSkeletonOnBee(Set<Player> players, List<Entity> summonedEntities) {
        broadcastActionBar(players, "[SUMMON] 🐝 Skeleton cưỡi ong đã xuất hiện!");
        for (int i = 0; i < 3; i++) {
	        Location loc = PvpSessionManager.getActiveSession().getRandomLocationInArena();
	        World world = loc.getWorld();
	        if (world == null) return;
	
	        Bee bee = (Bee) world.spawnEntity(loc, EntityType.BEE);
	        Skeleton skeleton = (Skeleton) world.spawnEntity(loc, EntityType.SKELETON);
	
	        makeWeak(bee);
	        makeWeak(skeleton);
	
	        bee.addPassenger(skeleton);
	
	        summonedEntities.add(bee);
	        summonedEntities.add(skeleton);
        }
    }

    private static void summonEvokerFangsSpam(Set<Player> players, List<Entity> summonedEntities) {
        broadcastActionBar(players, "[SUMMON] 🌀 Evoker đang triệu hồi móng vuốt liên tục!");

        new BukkitRunnable() {
            int ticks = 0;
            final Random rand = new Random();

            @Override
            public void run() {
                if (ticks >= 8) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 7 + rand.nextInt(2); i++) {
                    Location randomLoc = 
                        PvpSessionManager.getActiveSession()
                            .getRandomLocationInArena()
                            .add(0, 20 + rand.nextInt(10), 0)
                    ;

                    EvokerFangs fang = (EvokerFangs) randomLoc.getWorld().spawnEntity(randomLoc, EntityType.EVOKER_FANGS);
                    summonedEntities.add(fang);
                }

                if (rand.nextDouble() < 0.3) {
                    Player[] playerArr = players.toArray(Player[]::new);
                    if (playerArr.length > 0) {
                        Player randomPlayer = playerArr[rand.nextInt(playerArr.length)];
                        Location playerLoc = randomPlayer.getLocation();
                        EvokerFangs fang = (EvokerFangs) playerLoc.getWorld().spawnEntity(playerLoc, EntityType.EVOKER_FANGS);
                        summonedEntities.add(fang);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // chạy mỗi giây
    }


    // ===== HỖ TRỢ =====

    private static void makeWeak(LivingEntity entity) {
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(4.0); // 2 hit chết
        entity.setHealth(4.0);
        entity.setRemoveWhenFarAway(false);
        ((Lootable) entity).setLootTable(null); // Không rơi đồ
    }

    
}

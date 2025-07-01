package dev.ktoxz.pvp.event.impl;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.loot.Lootable;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import dev.ktoxz.pvp.PvpSessionManager;
import dev.ktoxz.pvp.event.PvpEvent;

public class SummonEvent extends PvpEvent {

    private final Random random = new Random();
    private final List<Entity> summonedEntities = new ArrayList<>();
    // Không cần activeTasks ở đây nữa, nó đã ở PvpEvent
    // private final List<BukkitTask> activeTasks = new ArrayList<>();

    private final List<SummonStrategy> ALL_SUMMONS = List.of(
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
        super.onEndMatch(); // Gọi phương thức của lớp cha để hủy tác vụ
    }

    @FunctionalInterface
    private interface SummonStrategy {
        void execute(Set<Player> players, List<Entity> summonedEntities);
    }

    // ===== TRIỆU HỒI =====

    private static void summonShulkerRain(Set<Player> players, List<Entity> summonedEntities) { // Giữ static
        broadcastActionBar(players, "[SUMMON] ☠️ Shulker mưa đạn xuất hiện!");

        for (int i = 0; i < 3; i++) {
            Location loc = PvpSessionManager.getActiveSession().getRandomLocationGround().add(0, 1, 0);
            World world = loc.getWorld();
            if (world == null) continue;

            Shulker shulker = (Shulker) world.spawnEntity(loc, EntityType.SHULKER);
            makeCustom(shulker, 4f, 4f, 0f);
            summonedEntities.add(shulker);
        }
    }

    private static void summonSkeletonOnBee(Set<Player> players, List<Entity> summonedEntities) { // Giữ static
        broadcastActionBar(players, "[SUMMON] 🐝 Skeleton cưỡi ong đã xuất hiện!");
        for (int i = 0; i < 3; i++) {
            Location loc = PvpSessionManager.getActiveSession().getRandomLocationInArena();
            World world = loc.getWorld();
            if (world == null) return;

            Bee bee = (Bee) world.spawnEntity(loc, EntityType.BEE);
            Skeleton skeleton = (Skeleton) world.spawnEntity(loc, EntityType.SKELETON);

            makeCustom(bee, 4f, 4f, 4f);
            makeCustom(skeleton, 8f, 8f, 4f);

            bee.addPassenger(skeleton);

            summonedEntities.add(bee);
            summonedEntities.add(skeleton);
        }
    }

    private static void summonEvokerFangsSpam(Set<Player> players, List<Entity> summonedEntities) { // Chuyển non-static
        broadcastActionBar(players, "[SUMMON] 🌀 Evoker đang triệu hồi móng vuốt liên tục!");

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final Random rand = new Random();

            @Override
            public void run() {
                if (ticks >= 8) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 7 + rand.nextInt(5); i++) {
                    Location randomLoc = 
                        PvpSessionManager.getActiveSession()
                            .getRandomLocationGround()
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
        }.runTaskTimer(plugin, 0L, 20L);
        activeTasks.add(task); // Rút gọn
    }
    
    private static void summonInvisibleCreaking(Set<Player> players, List<Entity> summonedEntities) {
        broadcastActionBar(players, "[SUMMON] 👻 Hai Creeper tàng hình đã xuất hiện!");

        for (int i = 0; i < 2; i++) {
            Location loc = PvpSessionManager.getActiveSession().getRandomLocationGround();
            World world = loc.getWorld();
            if (world == null) continue;

            Creeper creaking = (Creeper) world.spawnEntity(loc, EntityType.CREAKING);

            makeCustom(creaking, 20f, 20f, 6f); // Máu cao hơn bình thường một chút

            // Cho tàng hình trong 5 giây
            creaking.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 5, 1, false, false, false));

            summonedEntities.add(creaking);
        }

        // Sau 10 giây thì remove
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity e : new ArrayList<>(summonedEntities)) {
                    if (e instanceof Creeper && !e.isDead()) {
                        e.remove();
                        summonedEntities.remove(e);
                    }
                }
            }
        }.runTaskLater(plugin, 20 * 10);
    }


    // ===== HỖ TRỢ =====
    
    private static void makeCustom(LivingEntity entity, float baseHealth, float health, float atk) {
        // 1. Đặt máu tối đa và hiện tại
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHealth);
        entity.setHealth(health);

        // 2. Đặt damage
       
        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null && atk != 0f) {
            entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(atk);
        }

        // 3. Không despawn khi xa người chơi
        entity.setRemoveWhenFarAway(false);

        // 4. Không có loot
        if (entity instanceof Lootable lootable) {
            lootable.setLootTable(null);
        }
    }
}
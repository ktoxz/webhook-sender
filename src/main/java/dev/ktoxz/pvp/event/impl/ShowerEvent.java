package dev.ktoxz.pvp.event.impl;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import dev.ktoxz.pvp.PvpSessionManager;
import dev.ktoxz.pvp.event.PvpEvent;

public class ShowerEvent extends PvpEvent {

    @FunctionalInterface
    private interface ShowerAction extends BiConsumer<Set<Player>, Plugin> {}

    private final List<ShowerAction> ALL_EFFECTS = List.of(
        (players, plugin) -> spawnEntityRain(players, plugin, EntityType.WIND_CHARGE, "Mưa gió bão kéo tới~", 15, 3, 6, 10),
        (players, plugin) -> spawnEntityRain(players, plugin, EntityType.ARROW, "Mưa tên kéo tới như vũ bão", 15, 5, 10, 10),
        (players, plugin) -> spawnEntityRain(players, plugin, EntityType.SHULKER_BULLET, "Coi chừng bị bay", 5, 5, 10, 20),
        ShowerEvent::customTntRain,
        ShowerEvent::meteorShower,
        ShowerEvent::randomPotionRain
    );

    public ShowerEvent(Plugin plugin) {
        super("Shower Event", "Những sự kiện liên quan đến mưa rơi, nhưng mưa này hơi lạ", plugin);
    }

    @Override
    public void trigger(Set<Player> players) {
        if (players == null || players.isEmpty()) return;

        ShowerAction action = ALL_EFFECTS.get(new Random().nextInt(ALL_EFFECTS.size()));
        action.accept(players, plugin);
    }

    @Override
    public void onEndMatch() {
        // Không cần xử lý gì khi trận kết thúc
    }

    // === Logic mưa thực thể cơ bản ===
    private static void spawnEntityRain(Set<Player> players, Plugin plugin, EntityType type, String message,
                                        int totalTicks, int minPerTick, int maxPerTick, int delayTicks) {
        broadcastActionBar(players, msg(message));

        new BukkitRunnable() {
            int ticks = 0;
            final Random rand = new Random();

            @Override
            public void run() {
                if (ticks++ >= totalTicks) {
                    cancel();
                    return;
                }

                int amount = rand.nextInt(maxPerTick - minPerTick + 1) + minPerTick;
                for (int i = 0; i < amount; i++) {
                    Location loc = PvpSessionManager.getActiveSession().getRandomLocationInArena().add(0, 20 + rand.nextInt(10), 0);
                    World world = loc.getWorld();
                    if (world == null) continue;

                    Entity entity = world.spawnEntity(loc, type);
                    entity.setVelocity(new Vector(0, -1, 0));
                }
            }
        }.runTaskTimer(plugin, 0L, delayTicks);
    }

    // === Mưa TNT ===
    private static void customTntRain(Set<Player> players, Plugin plugin) {
        broadcastActionBar(players, msg("💣 Mưa TNT!"));

        new BukkitRunnable() {
            int ticks = 0;
            final Random rand = new Random();

            @Override
            public void run() {
                if (ticks++ >= 10) {
                    cancel();
                    return;
                }

                int count = 1 + rand.nextInt(3);
                for (int i = 0; i < count; i++) {
                    Location loc = PvpSessionManager.getActiveSession().getRandomLocationInArena().add(0, 20 + rand.nextInt(5), 0);
                    World world = loc.getWorld();
                    if (world == null) continue;

                    TNTPrimed tnt = (TNTPrimed) world.spawnEntity(loc, EntityType.TNT);
                    tnt.setFuseTicks(40);
                    tnt.setYield(1.0f);
                    tnt.setIsIncendiary(false);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // === Mưa thiên thạch ===
    private static void meteorShower(Set<Player> players, Plugin plugin) {
        broadcastActionBar(players, msg("☄️ Meteor Shower diễn ra!"));

        new BukkitRunnable() {
            int ticks = 0;
            final Random rand = new Random();

            @Override
            public void run() {
                if (ticks++ >= 15) {
                    cancel();
                    return;
                }

                int fireballs = 2 + rand.nextInt(4);
                for (int i = 0; i < fireballs; i++) {
                    Location loc = PvpSessionManager.getActiveSession().getRandomLocationInArena().add(0, 28 + rand.nextInt(5), 0);
                    Fireball fb = loc.getWorld().spawn(loc, Fireball.class);
                    fb.setVelocity(new Vector(0, -1, 0));
                    fb.setIsIncendiary(true);
                    fb.setYield(2.0f);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private static void randomPotionRain(Set<Player> players, Plugin plugin) {
        broadcastActionBar(players, msg("🧪 Mưa bình thuốc đang đổ xuống!"));

        List<org.bukkit.potion.PotionEffectType> effectPool = List.of(
            org.bukkit.potion.PotionEffectType.SPEED,
            org.bukkit.potion.PotionEffectType.SLOWNESS,
            org.bukkit.potion.PotionEffectType.REGENERATION,
            org.bukkit.potion.PotionEffectType.BLINDNESS,
            org.bukkit.potion.PotionEffectType.POISON,
            org.bukkit.potion.PotionEffectType.ABSORPTION,
            org.bukkit.potion.PotionEffectType.WEAKNESS
        );

        new BukkitRunnable() {
            int ticks = 0;
            final Random rand = new Random();

            @Override
            public void run() {
                if (ticks++ >= 10) {
                    cancel();
                    return;
                }

                int amount = 3 + rand.nextInt(4); // 3–6 bình mỗi lần
                for (int i = 0; i < amount; i++) {
                    Location loc = PvpSessionManager.getActiveSession().getRandomLocationInArena().add(0, 25 + rand.nextInt(5), 0);
                    World world = loc.getWorld();

                    org.bukkit.inventory.ItemStack potionItem = new org.bukkit.inventory.ItemStack(org.bukkit.Material.SPLASH_POTION);
                    org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) potionItem.getItemMeta();

                    org.bukkit.potion.PotionEffectType effectType = effectPool.get(rand.nextInt(effectPool.size()));
                    meta.addCustomEffect(new org.bukkit.potion.PotionEffect(effectType, 20 * 10, 1), true);
                    potionItem.setItemMeta(meta);

                    org.bukkit.entity.ThrownPotion potion = (org.bukkit.entity.ThrownPotion) world.spawnEntity(loc, EntityType.SPLASH_POTION);
                    potion.setItem(potionItem);
                    potion.setVelocity(new Vector(0, -1, 0));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static String msg(String m) {
        return "[ENVIRONMENT] " + m;
    }
}

package dev.ktoxz.pvp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Random;
import java.util.Set;

public class RandomEvent {

    private static final Random random = new Random();
    private static Location corner1;
    private static Location corner2;

    public static void triggerRandomEvent(Set<Player> players, Location c1, Location c2) {
        RandomEvent.corner1 = c1;
        RandomEvent.corner2 = c2;
        double chance = Math.random();

        if (chance < 0.02) strengthV(players);
        else if (chance < 0.05) speedBoost(players);
        else if (chance < 0.08) strengthI(players);
        else if (chance < 0.11) regeneration(players);
        else if (chance < 0.14) jumpBoost(players);
        else if (chance < 0.17) resistance(players);
        else if (chance < 0.20) haste(players);

        else if (chance < 0.23) slowness(players);
        else if (chance < 0.26) weakness(players);
        else if (chance < 0.29) blindness(players);
        else if (chance < 0.32) wither(players);
        else if (chance < 0.35) poison(players);
        else if (chance < 0.38) hunger(players);
        else if (chance < 0.41) nausea(players);
        else if (chance < 0.44) slowFalling(players);
        else if (chance < 0.47) instantDamage(players);

        else if (chance < 0.48) dropElytra(players);
        else if (chance < 0.49) dropTotem(players);
        else if (chance < 0.525) dropGoldenApple(players);
        else if (chance < 0.5575) dropIronSword(players);
        else if (chance < 0.59) dropEnderPearl(players);
        else if (chance < 0.6225) dropShield(players);

        else if (chance < 0.655) tntRain(players);
        else if (chance < 0.6875) lightningStorm(players);
        else if (chance < 0.72) meteorShower(players);
        else if (chance < 0.7525) randomBlockFall(players);
        else if (chance < 0.785) pitfallTrap(players);
        else if (chance < 0.8175) randomWeather(players);

        else if (chance < 0.91) chickenArmy(players);
        else fireworkShow(players);
    }

    private static Location getRandomLocationInArena() {
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        int y = corner1.getBlockY();

        int x = random.nextInt(maxX - minX + 1) + minX;
        int z = random.nextInt(maxZ - minZ + 1) + minZ;

        return new Location(corner1.getWorld(), x, y, z);
    }
    //
    private static void dropElytra(Set<Player> players) {
        Location center = getRandomLocationInArena();
        center.getWorld().dropItemNaturally(center, new ItemStack(Material.ELYTRA));
        center.getWorld().dropItemNaturally(center, new ItemStack(Material.FIREWORK_ROCKET, 20));
        broadcastActionBar(players, "[ITEM] Elytra + Pháo hoa spawn giữa đấu trường!");
    }

    private static void dropTotem(Set<Player> players) {
        for (Player p : players) {
            p.getWorld().dropItemNaturally(p.getLocation(), new ItemStack(Material.TOTEM_OF_UNDYING));
        }
        broadcastActionBar(players, "[ITEM] Totem spawn gần bạn!");
    }

    private static void dropGoldenApple(Set<Player> players) {
        dropCommonItem(players, Material.GOLDEN_APPLE, 1, "[ITEM] Golden Apple spawn!");
    }

    private static void dropIronSword(Set<Player> players) {
        for (Player p : players) {
            ItemStack sword = new ItemStack(Material.IRON_SWORD);
            sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 10);
            ItemMeta meta = sword.getItemMeta();
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(Material.IRON_SWORD.getMaxDurability() - 1);
                sword.setItemMeta(meta);
            }
            p.getWorld().dropItemNaturally(getRandomLocationInArena(), sword);
        }
        broadcastActionBar(players, "[ITEM] Kiếm 1-hit spawn!");
    }

    private static void dropEnderPearl(Set<Player> players) {
        dropCommonItem(players, Material.ENDER_PEARL, 3, "[ITEM] Ender Pearl x3 spawn!");
    }

    private static void dropShield(Set<Player> players) {
        dropCommonItem(players, Material.SHIELD, 1, "[ITEM] Khiên spawn!");
    }

    private static void dropCommonItem(Set<Player> players, Material material, int amount, String message) {
        for (Player p : players) {
            ItemStack item = new ItemStack(material, amount);
            p.getWorld().dropItemNaturally(getRandomLocationInArena(), item);
        }
        broadcastActionBar(players, message);
    }

    private static void tntRain(Set<Player> players) {
        for (int i = 0; i < 10; i++) {
            Location loc = getRandomLocationInArena().add(0, 15, 0);
            loc.getWorld().spawnEntity(loc, EntityType.TNT);
        }
        broadcastActionBar(players, "[ENVIRONMENT] TNT Rain bắt đầu!");
    }

    private static void lightningStorm(Set<Player> players) {
        for (int i = 0; i < 10; i++) {
            getRandomLocationInArena().getWorld().strikeLightning(getRandomLocationInArena());
        }
        broadcastActionBar(players, "[ENVIRONMENT] Lightning Storm diễn ra!");
    }

    private static void meteorShower(Set<Player> players) {
        for (int i = 0; i < 15; i++) {
            Location loc = getRandomLocationInArena().add(0, 30, 0);
            Fireball fb = loc.getWorld().spawn(loc, Fireball.class);
            fb.setVelocity(new Vector(0, -1, 0));
            fb.setIsIncendiary(true);
            fb.setYield(2F);
        }
        broadcastActionBar(players, "[ENVIRONMENT] Meteor Shower diễn ra!");
    }

    private static void randomBlockFall(Set<Player> players) {
        for (int i = 0; i < 10; i++) {
            Location loc = getRandomLocationInArena().add(0, 15, 0);
            Material mat = random.nextBoolean() ? Material.ANVIL : Material.GRAVEL;
            FallingBlock block = loc.getWorld().spawnFallingBlock(loc, mat.createBlockData());
            block.setDropItem(false);
        }
        broadcastActionBar(players, "[ENVIRONMENT] Block Fall bắt đầu!");
    }

    private static void pitfallTrap(Set<Player> players) {
        for (Player p : players) {
            Location loc = p.getLocation().subtract(0, 1, 0);
            loc.getBlock().setType(Material.LAVA);
        }
        broadcastActionBar(players, "[ENVIRONMENT] Bẫy Lava dưới chân bạn!");
    }

    private static void randomWeather(Set<Player> players) {
        World world = corner1.getWorld();
        boolean storm = random.nextBoolean();
        world.setStorm(storm);
        world.setThundering(storm);
        broadcastActionBar(players, storm ? "[ENVIRONMENT] Bắt đầu Mưa!" : "[ENVIRONMENT] Trời đẹp!");
    }

    private static void chickenArmy(Set<Player> players) {
        for (int i = 0; i < 20; i++) {
            getRandomLocationInArena().getWorld().spawnEntity(getRandomLocationInArena(), EntityType.CHICKEN);
        }
        broadcastActionBar(players, "[FUN] Gà xâm chiếm đấu trường!");
    }

    private static void fireworkShow(Set<Player> players) {
        for (int i = 0; i < 20; i++) {
            Location loc = getRandomLocationInArena().add(0, 2, 0);
            Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().flicker(true).trail(true).withColor(Color.AQUA).build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
        broadcastActionBar(players, "[FUN] Bắn pháo hoa khắp đấu trường!");
    }
    
    
    // === BUFF/DEBUFF EVENTS ===

    private static void speedBoost(Set<Player> players) { applyEffect(players, PotionEffectType.SPEED, 20*20, 1, "[BUFF] +Speed II trong 20s!"); }
    private static void strengthI(Set<Player> players) { applyEffect(players, PotionEffectType.STRENGTH, 20*20, 0, "[BUFF] +Strength I trong 20s!"); }
    private static void regeneration(Set<Player> players) { applyEffect(players, PotionEffectType.REGENERATION, 20*15, 1, "[BUFF] +Regeneration II trong 15s!"); }
    private static void jumpBoost(Set<Player> players) { applyEffect(players, PotionEffectType.JUMP_BOOST, 20*20, 2, "[BUFF] +Jump Boost III trong 20s!"); }
    private static void resistance(Set<Player> players) { applyEffect(players, PotionEffectType.RESISTANCE, 20*20, 1, "[BUFF] +Resistance II trong 20s!"); }
    private static void haste(Set<Player> players) { applyEffect(players, PotionEffectType.HASTE, 20*20, 1, "[BUFF] +Haste II trong 20s!"); }
    private static void strengthV(Set<Player> players) { applyEffect(players, PotionEffectType.STRENGTH, 20*10, 4, "[BUFF] +Strength V trong 10s! (Hiếm)"); }

    private static void slowness(Set<Player> players) { applyEffect(players, PotionEffectType.SLOWNESS, 20*10, 0, "[DEBUFF] -Slowness I trong 10s!"); }
    private static void weakness(Set<Player> players) { applyEffect(players, PotionEffectType.WEAKNESS, 20*10, 1, "[DEBUFF] -Weakness II trong 10s!"); }
    private static void blindness(Set<Player> players) { applyEffect(players, PotionEffectType.BLINDNESS, 20*7, 0, "[DEBUFF] -Blindness trong 7s!"); }
    private static void wither(Set<Player> players) { applyEffect(players, PotionEffectType.WITHER, 20*10, 0, "[DEBUFF] -Wither trong 10s!"); }
    private static void poison(Set<Player> players) { applyEffect(players, PotionEffectType.POISON, 20*10, 0, "[DEBUFF] -Poison trong 10s!"); }
    private static void hunger(Set<Player> players) { applyEffect(players, PotionEffectType.HUNGER, 20*10, 0, "[DEBUFF] -Hunger trong 10s!"); }
    private static void nausea(Set<Player> players) { applyEffect(players, PotionEffectType.NAUSEA, 20*10, 0, "[DEBUFF] -Nausea trong 10s!"); }
    private static void slowFalling(Set<Player> players) { applyEffect(players, PotionEffectType.SLOW_FALLING, 20*15, 0, "[DEBUFF] -Slow Falling trong 15s!"); }
    private static void instantDamage(Set<Player> players) { applyEffect(players, PotionEffectType.INSTANT_DAMAGE, 1, 0, "[DEBUFF] -Instant Damage!"); }

    // === EFFECT HANDLER ===

    private static void applyEffect(Set<Player> players, PotionEffectType type, int duration, int amplifier, String message) {
        for (Player p : players) {
            p.addPotionEffect(new PotionEffect(type, duration, amplifier));
        }
        broadcastActionBar(players, message);
    }

    private static void broadcastActionBar(Set<Player> players, String message) {
        for (Player p : players) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }

    private static void broadcast(Set<Player> players, String message) {
        broadcastActionBar(players, message);
    }
}

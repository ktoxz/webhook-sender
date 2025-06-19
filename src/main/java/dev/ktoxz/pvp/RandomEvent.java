package dev.ktoxz.pvp;

import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class RandomEvent {

    private static final Random random = new Random();
    private static Location corner1;
    private static Location corner2;
    private static Plugin plugin;
    private static final List<EventEntry> eventTable = List.of(
            new EventEntry(0.02, RandomEvent::strengthV),
            new EventEntry(0.05, RandomEvent::speedBoost),
            new EventEntry(0.08, RandomEvent::strengthI),
            new EventEntry(0.11, RandomEvent::regeneration),
            new EventEntry(0.14, RandomEvent::jumpBoost),
            new EventEntry(0.17, RandomEvent::resistance),
            new EventEntry(0.23, RandomEvent::slowness),
            new EventEntry(0.26, RandomEvent::weakness),
            new EventEntry(0.29, RandomEvent::blindness),
            new EventEntry(0.32, RandomEvent::wither),
            new EventEntry(0.35, RandomEvent::poison),
            new EventEntry(0.38, RandomEvent::hunger),
            new EventEntry(0.41, RandomEvent::nausea),
            new EventEntry(0.44, RandomEvent::slowFalling),
            new EventEntry(0.47, RandomEvent::instantDamage),
            new EventEntry(0.48, RandomEvent::windShower),
            new EventEntry(0.49, RandomEvent::dropTotem),
            new EventEntry(0.525, RandomEvent::dropGoldenApple),
            new EventEntry(0.5575, RandomEvent::dropIronSword),
            new EventEntry(0.59, RandomEvent::dropEnderPearl),
            new EventEntry(0.6225, RandomEvent::dropShield),
            new EventEntry(0.6875, RandomEvent::lightningStorm),
            new EventEntry(0.72, RandomEvent::meteorShower),
            new EventEntry(0.7525, RandomEvent::customTntRain),
            new EventEntry(0.785, RandomEvent::pitfallTrap),
            new EventEntry(0.8175, RandomEvent::randomWeather),
            new EventEntry(0.91, RandomEvent::chickenArmy),
            new EventEntry(1.0, RandomEvent::fireworkShow)
        );

        public static void triggerRandomEvent(Set<Player> players, Location c1, Location c2, Plugin plugin1) {
            corner1 = c1;
            corner2 = c2;
            plugin = plugin1;

            double chance = Math.random();
            for (EventEntry entry : eventTable) {
                if (chance < entry.threshold) {
                    entry.action.accept(players);
                    break;
                }
            }
        }

        private record EventEntry(double threshold, java.util.function.Consumer<Set<Player>> action) {}

    private static Location getRandomLocationInArena() {
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        int y = corner1.getBlockY()+1;

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
            ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
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

    private static void customTntRain(Set<Player> players) {
        broadcastActionBar(players, "[ENVIRONMENT] 💣 Mưa TNT!");

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 10) {
                    cancel();
                    return;
                }

                int tntCount = 1 + new Random().nextInt(3); // 1 đến 4 TNT mỗi giây
                for (int i = 0; i < tntCount; i++) {
                    Location loc = getRandomLocationInArena().add(0, 15, 0);
                    World world = loc.getWorld();
                    if (world == null) continue;

                    TNTPrimed tnt = (TNTPrimed) world.spawnEntity(loc, EntityType.TNT);
                    tnt.setFuseTicks(40); // 2 giây nổ
                    tnt.setYield(1.0f);
                    tnt.setIsIncendiary(false);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Mỗi 1 giây (20 ticks)
    }



    private static void lightningStorm(Set<Player> players) {
        World world = getRandomLocationInArena().getWorld();
        WeatherType oldWeather = world.hasStorm() ? WeatherType.DOWNFALL : WeatherType.CLEAR;

        // Bắt đầu bão
        world.setStorm(true);
        broadcastActionBar(players, "[ENVIRONMENT] ⚡ Lightning Storm đang diễn ra!");

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= 10) {
                    world.setStorm(oldWeather == WeatherType.DOWNFALL);
                    broadcastActionBar(players, "[ENVIRONMENT] ☀ Trời đã quang trở lại.");
                    cancel();
                    return;
                }

                Location strikeLoc = getRandomLocationInArena();
                world.strikeLightning(strikeLoc);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // 20 ticks = 1 giây
    }


    private static void meteorShower(Set<Player> players) {
        broadcastActionBar(players, "[ENVIRONMENT] ☄️ Meteor Shower diễn ra!");

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 15) {
                    cancel();
                    return;
                }

                int fireballsThisTick = 2 + new Random().nextInt(4); // Random từ 2 đến 5
                for (int i = 0; i < fireballsThisTick; i++) {
                    Location loc = getRandomLocationInArena().add(0, 30, 0);
                    Fireball fb = loc.getWorld().spawn(loc, Fireball.class);
                    fb.setVelocity(new Vector(0, -1, 0));
                    fb.setIsIncendiary(true);
                    fb.setYield(2F);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // chạy mỗi 20 tick = 1 giây
    }




    private static void pitfallTrap(Set<Player> players) {
        Map<Location, Material> originalBlocks = new HashMap<>();

        for (Player p : players) {
            Location loc = p.getLocation().clone().subtract(0, 1, 0).getBlock().getLocation();
            Block block = loc.getBlock();
            originalBlocks.put(loc, block.getType());
            block.setType(Material.LAVA);
        }

        broadcastActionBar(players, "[ENVIRONMENT] Bẫy Lava dưới chân bạn!");

        new BukkitRunnable() {
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
        }.runTaskLater(plugin, 20 * 7); // hoàn nguyên sau 7 giây
    }

    private static void randomWeather(Set<Player> players) {
        World world = corner1.getWorld();
        boolean storm = random.nextBoolean();
        world.setStorm(storm);
        world.setThundering(storm);

        broadcastActionBar(players, storm ? "[ENVIRONMENT] ⛈️ Bắt đầu Mưa!" : "[ENVIRONMENT] 🌤️ Trời đẹp!");

        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown == 0) {
                    cancel();
                    return;
                }

                // Đánh sét ngẫu nhiên trong arena
                for(int i=0;i<3; i++) {
                    Location randomStrike = getRandomLocationInArena();
                    randomStrike.getWorld().strikeLightning(randomStrike);
                }


                // 50% cơ hội đánh vào người chơi
                if (!players.isEmpty() && Math.random() < 0.5) {
                    Player[] arr = players.toArray(new Player[0]);
                    Player target = arr[random.nextInt(arr.length)];
                    target.getWorld().strikeLightning(target.getLocation());
                }

                countdown--;
            }
        }.runTaskTimer(plugin, 20 * 3, 20); // Bắt đầu sau 3 giây, lặp lại mỗi 1 giây
    }


    private static void chickenArmy(Set<Player> players) {
        List<Chicken> chickens = new ArrayList<>();
        World world = getRandomLocationInArena().getWorld();

        // Spawn 5 con gà ngẫu nhiên trong đấu trường
        for (int i = 0; i < 5; i++) {
            Location loc = getRandomLocationInArena();
            Chicken chicken = (Chicken) world.spawnEntity(loc.add(0, 2, 0), EntityType.CHICKEN);
            chickens.add(chicken);
        }

        broadcastActionBar(players, "[FUN] 🐔 Gà xâm chiếm đấu trường!");

        // Đợi 5–10 giây rồi tiêu diệt gà và xử lý hiệu ứng
        int delaySeconds = 5 + random.nextInt(6); // 5 đến 10 giây
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Chicken chicken : chickens) {
                    if (chicken.isValid()) {
                        Location loc = chicken.getLocation();

                        // 50% bắn pháo hoa, 50% phát nổ như creeper
                        chicken.remove();
                        if (random.nextBoolean()) {
                            // Pháo hoa
                            Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
                            FireworkMeta meta = fw.getFireworkMeta();
                            meta.addEffect(FireworkEffect.builder()
                                    .withColor(Color.RED)
                                    .with(Type.BALL_LARGE)
                                    .trail(true)
                                    .flicker(true)
                                    .build());
                            meta.setPower(1);
                            fw.setFireworkMeta(meta);
                            fw.detonate(); // Kích nổ ngay lập tức
                        } else {
                            // Nổ như creeper
                            loc.getWorld().createExplosion(loc, 2.0f, false, false); // power 2, không đốt, không phá block
                        }

                        
                    }
                }

                broadcastActionBar(players, "[FUN] 💥 Cuộc xâm lăng của gà đã kết thúc!");
            }
        }.runTaskLater(plugin, delaySeconds * 20L); // Chuyển giây thành ticks
    }

    
    private static void spawnFireworkAt(Location loc) {
        Firework firework = (Firework) loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.RED, Color.YELLOW, Color.ORANGE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    private static void windShower(Set<Player> players) {
        broadcastActionBar(players, "[ENVIRONMENT] 💨 Cơn mưa gió cuốn tới!");

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 10) {
                    cancel();
                    return;
                }

                int amount = 3 + new Random().nextInt(4); // 3–6 quả wind charge mỗi giây
                for (int i = 0; i < amount; i++) {
                    Location loc = getRandomLocationInArena().add(0, 20, 0);
                    World world = loc.getWorld();

                    if (world == null) continue;

                    // Chỉ có trong Minecraft 1.21+
                    Entity entity = world.spawnEntity(loc, EntityType.BREEZE_WIND_CHARGE);
                    entity.setVelocity(new Vector(0, -1, 0)); // rơi xuống như mưa
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // mỗi giây
    }


    private static void fireworkShow(Set<Player> players) {
        for (int i = 0; i < 3; i++) {
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
        String coloredMessage = colorizePrefix(message);

        for (Player player : players) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(coloredMessage));
        }
    }


    private static String colorizePrefix(String message) {
        if (message.startsWith("[ITEM]")) {
            return "§6" + message; // Vàng sáng
        } else if (message.startsWith("[ENVIRONMENT]")) {
            return "§b" + message; // Xanh dương
        } else if (message.startsWith("[FUN]")) {
            return "§d" + message; // Hồng tím
        } else {
            return "§f" + message; // Trắng mặc định
        }
    }

}

package dev.ktoxz.pvp;

import dev.ktoxz.manager.TeleportManager;
import org.bson.Document;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PvpSession {

    private final Plugin plugin;
    private final Player owner;
    private final boolean publicRoom;
    private final Set<Player> players = new HashSet<>();
    private final Set<Player> invitedPlayers = new HashSet<>();
    private final Map<Player, Location> playerChestLocations = new HashMap<>();
    private final Map<UUID, ItemStack[]> inventoryBackups = new HashMap<>();
    private final Map<UUID, ItemStack[]> armorBackups = new HashMap<>();
    private boolean started = false;
    private boolean countdownPhase = false;
    private boolean isOver = false;
    private Document chosenArena = null; // 🛠 arena được chọn
    private int teamCounter = 0; // Để gán ID team
    private Map<Player, Integer> playerTeamMap = new HashMap<>();
    
    private static final Set<Material> allowedMainhandItems = Set.of(
        Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
        Material.BOW, Material.CROSSBOW, Material.TRIDENT,
        Material.FISHING_ROD, Material.FLINT_AND_STEEL, Material.SHEARS, Material.SNOWBALL, Material.MACE, Material.WIND_CHARGE
    );

    public PvpSession(Plugin plugin, Player owner, boolean isPublic) {
        this.plugin = plugin;
        this.owner = owner;
        this.publicRoom = isPublic;
        this.players.add(owner);
    }

    // Getter/Setter
    public Player getOwner() { return owner; }
    public boolean isPublicRoom() { return publicRoom; }
    public boolean isStarted() { return started; }
    public boolean isCountdownPhase() { return countdownPhase; }
    public boolean isOver() { return isOver; }
    public Set<Player> getPlayers() { return players; }
    public Set<Player> getInvitedPlayers() { return invitedPlayers; }
    public void setStarted(boolean started) { this.started = started; }

    public void addPlayer(Player player) { players.add(player); }
    public void addInvited(Player player) { invitedPlayers.add(player); }
    public void removePlayer(Player player) {
        players.remove(player);
        playerChestLocations.remove(player);
    }

    public void broadcast(String message) {
        for (Player p : players) {
            p.sendMessage(message);
        }
    }

    public void preparePlayersForBattle() {
        teleportPlayersToArena();
        for (Player player : players) {
            backupInventory(player);
            clearInventoryExceptWhitelist(player);
            spawnChest(player);
        }
    }

    private void backupInventory(Player player) {
        inventoryBackups.put(player.getUniqueId(), player.getInventory().getContents().clone());
        armorBackups.put(player.getUniqueId(), player.getInventory().getArmorContents().clone());
    }
    
    private void assignTeams() {
        List<Player> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers); // Xáo trộn người chơi để phân phối công bằng

        // Chia thành 2 team
        for (Player p: players) {
            playerTeamMap.put(p, 1);
        }
        // Có thể thêm hiệu ứng như màu da cho team
    }

    private void clearInventoryExceptWhitelist(Player player) {
        List<ItemStack> keep = new ArrayList<>();

        // 1. Lưu các item ở slot 0-2 nếu trong allowedMainhandItems
        for (int i = 0; i < 3; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && allowedMainhandItems.contains(item.getType())) {
                keep.add(item.clone());
            }
        }

        // 2. Lưu offhand item
        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        if (offhandItem != null && offhandItem.getType() != Material.AIR && allowedMainhandItems.contains(offhandItem.getType())) {
            offhandItem = offhandItem.clone();
        } else {
            offhandItem = null;
        }

        // 3. Lưu armor contents
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        ItemStack[] savedArmor = new ItemStack[armorContents.length];
        for (int i = 0; i < armorContents.length; i++) {
            if (armorContents[i] != null && armorContents[i].getType() != Material.AIR) {
                savedArmor[i] = armorContents[i].clone();
            } else {
                savedArmor[i] = null;
            }
        }

        // 4. Clear toàn bộ inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);

        // 5. Restore lại slot 0-2
        int slot = 0;
        for (ItemStack item : keep) {
            player.getInventory().setItem(slot++, item);
        }

        // 6. Restore lại offhand
        if (offhandItem != null) {
            player.getInventory().setItemInOffHand(offhandItem);
        }

        // 7. Restore lại armor
        player.getInventory().setArmorContents(savedArmor);
    }


    private void spawnChest(Player player) {
        Location loc = player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(2));
        loc.setY(player.getWorld().getHighestBlockYAt(loc) + 1);
        Block block = loc.getBlock();
        block.setType(Material.CHEST);
        if (block.getState() instanceof Chest chest) {
            ChestManager.fillChestWithItems(chest.getInventory());
        }
        playerChestLocations.put(player, loc);
    }

    public void startCountdown() {
        countdownPhase = true;
        new BukkitRunnable() {
            int countdown = 10;
            @Override
            public void run() {
                if (countdown == 0) {
                    countdownPhase = false;
                    started = true;
                    assignTeams(); // <--- Thêm dòng này để tạo team

                     // 🛠 Teleport vào spot random

                    destroyChests();
                    broadcast("§cBẮT ĐẦU PvP!");
                    startRandomEvents();
                    cancel();
                    return;
                }
                for (Player player : players) {
                    player.sendTitle("§eChuẩn bị!", "§f" + countdown + " giây", 0, 20, 0);
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void teleportPlayersToArena() {
        if (chosenArena == null) {
            chosenArena = dev.ktoxz.manager.TeleportManager.getRandomArena();
            if (chosenArena == null) {
                broadcast("§cKhông tìm thấy Arena hợp lệ!");
                return;
            }
        }

        List<Document> availableSpots = new ArrayList<>(chosenArena.getList("spots", Document.class));
        if (availableSpots.isEmpty()) {
            broadcast("§cArena không có chỗ spawn!");
            return;
        }

        Random random = new Random();

        for (Player player : players) {
            if (availableSpots.isEmpty()) {
                player.sendMessage("§cKhông còn chỗ spawn trống!");
                continue;
            }

            Document spot = availableSpots.remove(random.nextInt(availableSpots.size()));

            int x = spot.getInteger("x");
            int y = spot.getInteger("y");
            int z = spot.getInteger("z");

            World world = Bukkit.getWorld("world");
            if (world != null) {
                player.teleport(new Location(world, x + 0.5, y, z + 0.5));
            }
        }
    }


    private void destroyChests() {
        for (Location loc : playerChestLocations.values()) {
            Block block = loc.getBlock();
            if (block.getType() == Material.CHEST) {
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1f);
                block.getWorld().spawnParticle(Particle.EXPLOSION, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
                block.setType(Material.AIR);
            }
        }
        playerChestLocations.clear();
    }

    public void startRandomEvents() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (players.size() <= 1 || isOver) {
                    cancel();
                    return;
                }
                if (chosenArena != null) {
                    Document c1 = chosenArena.get("corner1", Document.class);
                    Document c2 = chosenArena.get("corner2", Document.class);

                    World world = Bukkit.getWorld("world"); // hoặc world riêng nếu bạn lưu world
                    if (world != null && c1 != null && c2 != null) {
                        Location loc1 = new Location(world, c1.getInteger("x"), c1.getInteger("y"), c1.getInteger("z"));
                        Location loc2 = new Location(world, c2.getInteger("x"), c2.getInteger("y"), c2.getInteger("z"));
                        RandomEvent.triggerRandomEvent(players, loc1, loc2, plugin);
                    }
                }
            }
        }.runTaskTimer(plugin, 20 * 10L, 20 * 10L); // 10s lặp
    }


    public void handleVictory() {
        if (players.size() == 1 && !isOver) {
            isOver = true;
            restoreAllInventories();
            Player winner = players.iterator().next();
            win(winner);
            PvpSessionManager.closeSession();
        }
    }

    public void win(Player winner) {
        if (winner == null) return;

        for (PotionEffect effect : winner.getActivePotionEffects()) {
            winner.removePotionEffect(effect.getType());
        }

        winner.getInventory().clear();

        UUID uuid = winner.getUniqueId();
        if (inventoryBackups.containsKey(uuid)) {
            winner.getInventory().setContents(inventoryBackups.get(uuid));
        }
        if (armorBackups.containsKey(uuid)) {
            winner.getInventory().setArmorContents(armorBackups.get(uuid));
        }

        winner.sendTitle("§6CHIẾN THẮNG!", "§eBạn là người sống sót cuối cùng!", 20, 100, 20);
        Location loc = winner.getLocation();
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        spawnCelebrationFirework(loc);
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        winner.giveExp(100);

        Bukkit.broadcastMessage("§6[Winner] §f" + winner.getName() + " đã chiến thắng trận PvP!");
    }

    private void spawnCelebrationFirework(Location loc) {
        Firework firework = (Firework) loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .flicker(true)
                .trail(true)
                .withColor(Color.AQUA, Color.LIME, Color.YELLOW)
                .withFade(Color.PURPLE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    public void restoreAllInventories() {
        for (UUID uuid : inventoryBackups.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                restoreInventory(player);
            }
        }
    }

    private void restoreInventory(Player player) {
        UUID uuid = player.getUniqueId();
        if (inventoryBackups.containsKey(uuid)) {
            player.getInventory().setContents(inventoryBackups.get(uuid));
        }
        if (armorBackups.containsKey(uuid)) {
            player.getInventory().setArmorContents(armorBackups.get(uuid));
        }
    }

    public void setCountdownPhase(boolean countdownPhase) {
        this.countdownPhase = countdownPhase;
    }
}

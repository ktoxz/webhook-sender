package dev.ktoxz.pvp;

import org.bson.Document;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import dev.ktoxz.manager.TeleportManager;
import dev.ktoxz.manager.EffectManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;

public class PvpSession {
    private final Plugin plugin;
    private final Player owner;
    private final boolean publicRoom;
    private final Set<Player> players = ConcurrentHashMap.newKeySet();
    private final Set<Player> invitedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<Player, Location> playerChestLocations = new HashMap<>();
    private final Map<UUID, ItemStack[]> inventoryBackups = new HashMap<>();
    private final Map<UUID, ItemStack[]> armorBackups = new HashMap<>();
    private final Set<UUID> playersAddedToRegion = ConcurrentHashMap.newKeySet();

    private PvpSessionState currentState;
    private Document chosenArena = null;
    private String arenaRegionName;
    private ProtectedRegion activeRegion;
    private BukkitTask sessionTimeoutTask;
    private BukkitTask countdownTask;
    private BukkitTask randomEventTask;

    private Location arenaMinLoc, arenaMaxLoc;

    private static final Set<Material> ALLOWED_MAINHAND_ITEMS = EnumSet.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT,
            Material.FISHING_ROD, Material.FLINT_AND_STEEL, Material.SHEARS, Material.SNOWBALL, Material.MACE, Material.WIND_CHARGE
    );

    public enum PvpSessionState {
        WAITING,
        COUNTDOWN,
        STARTED,
        OVER,
        CANCELLED
    }

    public PvpSession(Plugin plugin, Player owner, boolean isPublic, boolean includeSelf) {
        this.plugin = plugin;
        this.owner = owner;
        this.publicRoom = isPublic;
        if (includeSelf) {
            this.players.add(owner);
        }
        this.currentState = PvpSessionState.WAITING;
        startSessionTimeout();
    }

    public PvpSession(Plugin plugin, Player owner, boolean isPublic) {
        this(plugin, owner, isPublic, true);
    }

    public Player getOwner() { return owner; }
    public boolean isPublicRoom() { return publicRoom; }
    public boolean isStarted() { return currentState == PvpSessionState.STARTED; }
    public boolean isCountdownPhase() { return currentState == PvpSessionState.COUNTDOWN; }
    public boolean isOver() { return currentState == PvpSessionState.OVER || currentState == PvpSessionState.CANCELLED; }
    public Set<Player> getPlayers() { return players; }
    public Set<Player> getInvitedPlayers() { return invitedPlayers; }
    public void addInvited(Player player) { invitedPlayers.add(player); }
    public String getArenaRegionName() { return arenaRegionName; }

    private void setState(PvpSessionState newState) {
        this.currentState = newState;
        plugin.getLogger().info(String.format("[PvP Session] State changed to: %s", newState));
    }

    public void addPlayer(Player player) {
        if (player == null || players.contains(player)) return;
        players.add(player);

        if (isStarted() && activeRegion != null) {
            addAllPlayersToRegionAndSaveAsync()
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Lỗi khi thêm người chơi mới vào region: " + player.getName(), ex);
                    return null;
                });
        }
    }

    public void removePlayer(Player player) {
        if (player == null || !players.contains(player)) return;

        players.remove(player);
        restoreInventory(player);
        removePotionEffects(player);
        broadcast(String.format("§c%s đã rời phòng PvP!", player.getName()));

        if (activeRegion != null && playersAddedToRegion.contains(player.getUniqueId())) {
            removePlayerFromRegionAsync(player.getUniqueId())
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Lỗi khi xóa người chơi khỏi region: " + player.getName(), ex);
                    return null;
                });
        }

        if ((currentState == PvpSessionState.STARTED || currentState == PvpSessionState.COUNTDOWN) && players.size() <= 1) {
            handleVictory();
        }
    }
    
    public void removePotionEffects(Player player) {
        if (player == null) return;

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType()); // Loại bỏ tất cả hiệu ứng hiện tại
        }
    }

    public void broadcast(String message) {
        players.forEach(p -> p.sendMessage(message));
    }

    public void preparePlayersForBattle() {
        if (currentState != PvpSessionState.WAITING) {
            plugin.getLogger().warning("preparePlayersForBattle được gọi khi session không ở trạng thái WAITING.");
            return;
        }

        teleportPlayersToArena()
            .thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player player : players) {
                        if (player.isOnline()) {
                            backupInventory(player);
                            clearInventoryExceptWhitelist(player);
                            spawnChest(player);
                        }
                    }
                    broadcast("§aĐã chuẩn bị xong. Đang chờ đếm ngược...");
                });
            })
            .exceptionally(ex -> {
                plugin.getLogger().log(Level.SEVERE, "Lỗi khi chuẩn bị người chơi cho trận đấu: " + ex.getMessage(), ex);
                broadcast("§cĐã xảy ra lỗi khi chuẩn bị arena. Vui lòng thử lại.");
                PvpSessionManager.closeSession();
                return null;
            });
    }

    private void backupInventory(Player player) {
        inventoryBackups.put(player.getUniqueId(), player.getInventory().getContents().clone());
        armorBackups.put(player.getUniqueId(), player.getInventory().getArmorContents().clone());
        plugin.getLogger().info("Đã sao lưu inventory cho " + player.getName());
    }

    private void clearInventoryExceptWhitelist(Player player) {
        List<ItemStack> itemsToKeep = new ArrayList<>();
        for (int i = 0; i < Math.min(3, player.getInventory().getSize()); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && ALLOWED_MAINHAND_ITEMS.contains(item.getType())) {
                itemsToKeep.add(item.clone());
            }
        }
        ItemStack offhandItem = player.getInventory().getItemInOffHand();

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);

        for (int i = 0; i < itemsToKeep.size(); i++) {
            player.getInventory().setItem(i, itemsToKeep.get(i));
        }
        if (offhandItem != null && ALLOWED_MAINHAND_ITEMS.contains(offhandItem.getType())) {
            player.getInventory().setItemInOffHand(offhandItem);
        }
        if (armorBackups.containsKey(player.getUniqueId())) {
            player.getInventory().setArmorContents(armorBackups.get(player.getUniqueId()));
        }
        player.updateInventory();
        plugin.getLogger().info("Đã dọn dẹp inventory cho " + player.getName() + " (trừ whitelist)");
    }

    private void spawnChest(Player player) {
        Location loc = player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(2));
        loc.setY(player.getWorld().getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ()) + 1);

        Block block = loc.getBlock();
        if (block.getType() != Material.AIR) {
            loc.add(0, 1, 0);
            block = loc.getBlock();
        }

        if (block.getType() == Material.AIR) {
            block.setType(Material.CHEST);
            if (block.getState() instanceof Chest chest) {
                ChestManager.fillChestWithItems(chest.getInventory());
                playerChestLocations.put(player, loc);
                player.playSound(loc, Sound.BLOCK_CHEST_OPEN, 1f, 1f);
                plugin.getLogger().info("Đã spawn rương cho " + player.getName() + " tại " + loc.toVector());
            } else {
                plugin.getLogger().warning("Không thể đặt rương tại " + loc.toVector() + " cho " + player.getName() + ": Block state không phải Chest.");
            }
        } else {
            plugin.getLogger().warning("Vị trí " + loc.toVector() + " không trống để đặt rương cho " + player.getName() + ": Block type là " + block.getType());
        }
    }

    public void startCountdown() {
        if (currentState != PvpSessionState.WAITING) {
            return;
        }
        teleportOutsidersToAudience();
        setState(PvpSessionState.COUNTDOWN);
        cancelSessionTimeout();

        countdownTask = new BukkitRunnable() {
            int countdown = 20;

            @Override
            public void run() {
                if (currentState != PvpSessionState.COUNTDOWN || !PvpSessionManager.hasActiveSession() || PvpSessionManager.getActiveSession() != PvpSession.this) {
                    cancel();
                    plugin.getLogger().info("Countdown task cancelled due to session state change or no active session.");
                    return;
                }

                if (countdown == 20) {
                    players.forEach(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 9, false, false)));
                    players.forEach(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1000, 20, false, false)));

                    plugin.getLogger().info("Gửi hiệu ứng cho anh em");
                }

                if (countdown <= 0) {
                    setState(PvpSessionState.STARTED);
                    destroyChests();
                    broadcast("§cBẮT ĐẦU PvP!");
                    startRandomEvents();
                    cancel();
                    plugin.getLogger().info("PvP countdown finished. Session started.");
                    return;
                }

                players.forEach(player -> {
                    if (player.isOnline()) {
                        player.sendTitle("§eChuẩn bị!", "§f" + countdown + " giây", 0, 20, 0);
                    }
                });
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void destroyChests() {
        for (Location loc : playerChestLocations.values()) {
            Block block = loc.getBlock();
            if (block.getType() == Material.CHEST) {
                loc.getWorld().playSound(loc, Sound.BLOCK_CHEST_CLOSE, 1f, 1f);
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
                block.setType(Material.AIR);
                plugin.getLogger().info("Đã phá hủy rương tại " + loc.toVector());
            }
        }
        playerChestLocations.clear();
    }

    public void startRandomEvents() {
        if (randomEventTask != null) {
            randomEventTask.cancel();
            plugin.getLogger().info("[PvpSession] Đã hủy tác vụ sự kiện ngẫu nhiên đang tồn tại.");
        }

        randomEventTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentState != PvpSessionState.STARTED || players.size() <= 1) {
                    cancel();
                    plugin.getLogger().info("[PvpSession] Tác vụ sự kiện ngẫu nhiên đã bị hủy: Phiên không ở trạng thái STARTED hoặc quá ít người chơi.");
                    return;
                }
                
                if (arenaMinLoc == null || arenaMaxLoc == null) {
                    plugin.getLogger().warning("[PvpSession] Không thể kích hoạt RandomEvent: Vị trí arena chưa được xác định. Hủy tác vụ.");
                    cancel();
                    return;
                }

                PvpEventManager.triggerRandomEvent(players);
            }
        }.runTaskTimer(plugin, 20 * 10L, 20 * 5L);
        plugin.getLogger().info("[PvpSession] Tác vụ sự kiện ngẫu nhiên đã bắt đầu");
    }

    public void handleVictory() {
        if (players.size() == 1) {
            setState(PvpSessionState.OVER);
            Player winner = players.iterator().next();
            win(winner);

            // Gọi PvpSessionManager.closeSession() trực tiếp
            PvpSessionManager.closeSession();

        } else if (currentState == PvpSessionState.STARTED && players.isEmpty()){
            broadcast("§cPhiên PvP đã kết thúc vì không còn ai trên đấu trường.");
            setState(PvpSessionState.OVER);
            PvpSessionManager.closeSession();
        }
        removeDroppedItemsInRegion(arenaRegionName);
    }

    public void win(Player winner) {
        if (winner == null) return;

        winner.getActivePotionEffects().stream()
              .map(PotionEffect::getType)
              .forEach(winner::removePotionEffect);
              
        restoreInventory(winner);
        
        winner.sendTitle("§6CHIẾN THẮNG!", "§eBạn là người sống sót cuối cùng!", 20, 100, 20);
        Location loc = winner.getLocation();
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        spawnCelebrationFirework(loc);
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        winner.giveExp(100);
        EffectManager.showTradeComplete(winner);
        Bukkit.broadcastMessage("§6[Winner] §f" + winner.getName() + " đã chiến thắng trận PvP!");
        plugin.getLogger().info("Player " + winner.getName() + " won the PvP session.");
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

//    public void restoreAllInventories() {
//        for (UUID uuid : new HashSet<>(inventoryBackups.keySet())) {
//            Player player = Bukkit.getPlayer(uuid);
//            if (player != null && player.isOnline()) {
//                restoreInventory(player);
//            } else {
//                plugin.getLogger().info("Không thể khôi phục inventory cho người chơi offline: " + uuid);
//            }
//        }
//        inventoryBackups.clear();
//        armorBackups.clear();
//        plugin.getLogger().info("Đã khôi phục và xóa tất cả bản sao lưu inventory.");
//    }

    private void restoreInventory(Player player) {
        UUID uuid = player.getUniqueId();
        if (inventoryBackups.containsKey(uuid)) {
            player.getInventory().setContents(inventoryBackups.remove(uuid));
            plugin.getLogger().info("Đã khôi phục inventory chính cho " + player.getName());
        }
        if (armorBackups.containsKey(uuid)) {
            player.getInventory().setArmorContents(armorBackups.remove(uuid));
            plugin.getLogger().info("Đã khôi phục giáp cho " + player.getName());
        }
        player.updateInventory();
    }

    /**
     * Dọn dẹp tài nguyên phiên PvP (chủ yếu là các thao tác WorldGuard không đồng bộ).
     * Các thao tác Bukkit API đồng bộ đã được chuyển lên PvpSessionManager.closeSession().
     * @return CompletableFuture<Void> Hoàn thành khi quá trình dọn dẹp xong.
     */
    public CompletableFuture<Void> clearSessionResources() {
        plugin.getLogger().info("[PvpSession] Bắt đầu dọn dẹp tài nguyên phiên PvP (phần async).");

        return clearMembersFromActiveRegionAsync()
            .exceptionally(ex -> {
                plugin.getLogger().log(Level.SEVERE, "[PvpSession] Lỗi khi dọn dẹp thành viên khỏi region: " + ex.getMessage(), ex);
                return null;
            })
            .thenRun(() -> plugin.getLogger().info("[PvpSession] Đã hoàn tất dọn dẹp tài nguyên phiên PvP (phần async)."));
    }

    // `cancelTasks` giờ đã được gọi trực tiếp trong PvpSessionManager.closeSession()
    // Có thể bỏ hoặc giữ lại nếu muốn gọi riêng từ đây cho mục đích khác
    private void cancelTasks() {
        if (sessionTimeoutTask != null) {
            sessionTimeoutTask.cancel();
            sessionTimeoutTask = null;
            plugin.getLogger().info("Session timeout task cancelled.");
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
            plugin.getLogger().info("Countdown task cancelled.");
        }
        if (randomEventTask != null) {
            randomEventTask.cancel();
            randomEventTask = null;
            plugin.getLogger().info("Random event task cancelled.");
        }
    }

    private void startSessionTimeout() {
        cancelSessionTimeout();
        sessionTimeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentState == PvpSessionState.WAITING) {
                    broadcast("§cPhiên PvP đã hết thời gian chờ và tự động đóng.");
                    PvpSessionManager.closeSession();
                    plugin.getLogger().info("PvP session timed out and closed.");
                }
            }
        }.runTaskLater(plugin, 20L * 180);
        plugin.getLogger().info("Session timeout task started (3 minutes).");
    }

    public void cancelSessionTimeout() {
        if (sessionTimeoutTask != null) {
            sessionTimeoutTask.cancel();
            sessionTimeoutTask = null;
            plugin.getLogger().info("Session timeout task explicitly cancelled.");
        }
    }

    private CompletableFuture<Void> addAllPlayersToRegionAndSaveAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (activeRegion == null) {
            plugin.getLogger().warning("Không có WorldGuard region hoạt động để thêm người chơi.");
            future.complete(null);
            return future;
        }

        RegionManager regionManager = getRegionManager();
        if (regionManager == null) {
            plugin.getLogger().warning("Không thể lấy RegionManager cho thế giới.");
            future.complete(null);
            return future;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DefaultDomain members = activeRegion.getMembers();
            boolean changed = false;

            for (Player player : players) {
                if (!members.contains(player.getUniqueId())) {
                    members.addPlayer(player.getUniqueId());
                    playersAddedToRegion.add(player.getUniqueId());
                    plugin.getLogger().info("Đã thêm người chơi " + player.getName() + " vào region " + activeRegion.getId() + ".");
                    changed = true;
                }
            }

            if (changed) {
                try {
                    regionManager.save();
                    plugin.getLogger().info("WorldGuard region " + activeRegion.getId() + " đã lưu thành công sau khi thêm người chơi.");
                    future.complete(null);
                } catch (StorageException e) {
                    plugin.getLogger().log(Level.SEVERE, "Lỗi khi lưu WorldGuard region " + activeRegion.getId() + ": " + e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            } else {
                plugin.getLogger().info("Không có người chơi mới để thêm vào region " + activeRegion.getId() + " hoặc đã thêm rồi.");
                future.complete(null);
            }
        });
        return future;
    }

    private CompletableFuture<Void> removePlayerFromRegionAsync(UUID playerUUID) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (activeRegion == null) {
            future.complete(null);
            return future;
        }

        RegionManager regionManager = getRegionManager();
        if (regionManager == null) {
            future.complete(null);
            return future;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DefaultDomain members = activeRegion.getMembers();
            if (members.contains(playerUUID)) {
                members.removePlayer(playerUUID);
                playersAddedToRegion.remove(playerUUID);
                plugin.getLogger().info("Đã xóa người chơi " + Bukkit.getOfflinePlayer(playerUUID).getName() + " khỏi region " + activeRegion.getId());
                try {
                    regionManager.save();
                    plugin.getLogger().info("WorldGuard region " + activeRegion.getId() + " đã lưu thành công sau khi xóa người chơi.");
                    future.complete(null);
                } catch (StorageException e) {
                    plugin.getLogger().log(Level.SEVERE, "Lỗi khi lưu WorldGuard region sau khi xóa người chơi " + playerUUID + ": " + e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            } else {
                future.complete(null);
            }
        });
        return future;
    }

    CompletableFuture<Void> clearMembersFromActiveRegionAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (activeRegion == null || playersAddedToRegion.isEmpty()) {
            future.complete(null);
            return future;
        }

        RegionManager regionManager = getRegionManager();
        if (regionManager == null) {
            plugin.getLogger().warning("Không thể lấy RegionManager để xóa thành viên khỏi region.");
            future.complete(null);
            return future;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DefaultDomain members = activeRegion.getMembers();
            boolean changed = false;
            for (UUID uuid : new HashSet<>(playersAddedToRegion)) {
                if (members.contains(uuid)) {
                    members.removePlayer(uuid);
                    plugin.getLogger().info("Đã xóa " + Bukkit.getOfflinePlayer(uuid).getName() + " khỏi region " + activeRegion.getId());
                    changed = true;
                }
            }
            playersAddedToRegion.clear();

            if (changed) {
                try {
                    regionManager.save();
                    plugin.getLogger().info("[PvP] RegionManager đã được lưu thành công sau khi xóa thành viên.");
                    future.complete(null);
                } catch (StorageException e) {
                    plugin.getLogger().log(Level.SEVERE, "[PvP] Lỗi khi lưu RegionManager sau khi xóa thành viên:", e);
                    future.completeExceptionally(e);
                }
            } else {
                plugin.getLogger().info("Không có thành viên nào để xóa khỏi region hoặc đã được xóa.");
                future.complete(null);
            }
        });
        return future;
    }
    
    private void teleportOutsidersToAudience() {
        if (activeRegion == null || chosenArena == null || !chosenArena.containsKey("audience")) {
            plugin.getLogger().warning("[PvpSession] Không thể xử lý outsiders: thiếu dữ liệu.");
            return;
        }

        World world = Bukkit.getWorld("world");
        if (world == null) {
            plugin.getLogger().warning("[PvpSession] World 'world' không tồn tại.");
            return;
        }

        List<Document> audienceSpots = chosenArena.getList("audience", Document.class);
        if (audienceSpots.isEmpty()) return;

        Random rand = new Random();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (players.contains(player)) continue; // Người đang thi đấu

            Location loc = player.getLocation();
            BlockVector3 bv = BukkitAdapter.asBlockVector(loc);

            if (activeRegion.contains(bv)) { // ✅ Cách đúng với WG 7+
                Document spot = audienceSpots.get(rand.nextInt(audienceSpots.size()));
                int x = spot.getInteger("x");
                int y = spot.getInteger("y");
                int z = spot.getInteger("z");

                Location target = new Location(world, x + 0.5, y, z + 0.5);
                player.teleport(target);
                player.sendMessage("§eBạn đã được chuyển ra ngoài đấu trường.");
                plugin.getLogger().info("[PvpSession] Đã chuyển outsider " + player.getName() + " đến audience.");
            }
        }
    }



    private RegionManager getRegionManager() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            plugin.getLogger().warning("Thế giới 'world' không tồn tại.");
            return null;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt(world));
    }

    private CompletableFuture<Void> teleportPlayersToArena() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (chosenArena == null) {
            chosenArena = TeleportManager.getRandomArena();
            if (chosenArena == null) {
                broadcast("§cKhông tìm thấy Arena hợp lệ để bắt đầu PvP!");
                future.completeExceptionally(new IllegalStateException("No valid arena found."));
                return future;
            }
            arenaRegionName = chosenArena.getString("_id");
            plugin.getLogger().info("Đã chọn arena: " + arenaRegionName);
        }

        World world = Bukkit.getWorld("world");
        if (world == null) {
            broadcast("§cThế giới 'world' không tồn tại.");
            future.completeExceptionally(new IllegalStateException("World 'world' not found."));
            return future;
        }

        RegionManager regionManager = getRegionManager();
        if (regionManager == null) {
            broadcast("§cKhông tìm thấy RegionManager!");
            future.completeExceptionally(new IllegalStateException("RegionManager not found."));
            return future;
        }

        ProtectedRegion region = regionManager.getRegion(arenaRegionName);
        if (region == null) {
            broadcast("§cKhông tìm thấy WorldGuard region với tên: " + arenaRegionName);
            future.completeExceptionally(new IllegalStateException("WorldGuard region '" + arenaRegionName + "' not found."));
            return future;
        }
        this.activeRegion = region;

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        arenaMinLoc = BukkitAdapter.adapt(world, min);
        arenaMaxLoc = BukkitAdapter.adapt(world, max);
        plugin.getLogger().info("Arena bounds set: " + arenaMinLoc.toVector() + " to " + arenaMaxLoc.toVector());

        List<Document> availableSpots = new ArrayList<>();
        if (chosenArena.containsKey("spots")) {
            availableSpots.addAll(chosenArena.getList("spots", Document.class));
        }

        if (availableSpots.isEmpty()) {
            broadcast("§cArena không có chỗ spawn!");
            future.completeExceptionally(new IllegalStateException("Arena has no spawn spots."));
            return future;
        }

        addAllPlayersToRegionAndSaveAsync()
            .thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Random random = new Random();
                    List<Document> tempSpots = new ArrayList<>(availableSpots);

                    for (Player player : players) {
                        if (!player.isOnline()) {
                            plugin.getLogger().warning("Skipping teleport for offline player: " + player.getName());
                            continue;
                        }

                        if (tempSpots.isEmpty()) {
                            player.sendMessage("§cKhông còn chỗ spawn trống!");
                            break;
                        }
                        Document spot = tempSpots.remove(random.nextInt(tempSpots.size()));
                        int x = spot.getInteger("x");
                        int y = spot.getInteger("y");
                        int z = spot.getInteger("z");
                        
                        Location targetLoc = new Location(world, x + 0.5, y, z + 0.5);
                        player.teleport(targetLoc);
                        player.sendMessage("§aBạn đã được teleport đến đấu trường!");
                        plugin.getLogger().info("Player " + player.getName() + " teleported to " + targetLoc.toVector());
                        EffectManager.showTeleportComplete(player);
                    }
                    future.complete(null);
                });
            })
            .exceptionally(ex -> {
                plugin.getLogger().log(Level.SEVERE, "Lỗi trong quá trình teleport players to arena: " + ex.getMessage(), ex);
                broadcast("§cĐã xảy ra lỗi nghiêm trọng khi chuẩn bị đấu trường. Vui lòng liên hệ quản trị viên.");
                PvpSessionManager.closeSession();
                future.completeExceptionally(ex);
                return null;
            });
        return future;
    }

    public Location getRandomLocationGround() {
        if (arenaMinLoc == null || arenaMaxLoc == null) {
            plugin.getLogger().warning("Không thể lấy vị trí ngẫu nhiên: Arena bounds chưa được đặt.");
            return null;
        }
        Random random = new Random();
        int minX = Math.min(arenaMinLoc.getBlockX(), arenaMaxLoc.getBlockX());
        int maxX = Math.max(arenaMinLoc.getBlockX(), arenaMaxLoc.getBlockX());
        int minZ = Math.min(arenaMinLoc.getBlockZ(), arenaMaxLoc.getBlockZ());
        int maxZ = Math.max(arenaMinLoc.getBlockZ(), arenaMaxLoc.getBlockZ());
        World world = arenaMinLoc.getWorld();

        int x = random.nextInt(maxX - minX + 1) + minX;
        int z = random.nextInt(maxZ - minZ + 1) + minZ;
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    public Location getRandomLocationInArena() {
        if (arenaMinLoc == null || arenaMaxLoc == null) {
            plugin.getLogger().warning("Không thể lấy vị trí ngẫu nhiên: Arena bounds chưa được đặt.");
            return null;
        }
        Random random = new Random();
        int minX = Math.min(arenaMinLoc.getBlockX(), arenaMaxLoc.getBlockX());
        int maxX = Math.max(arenaMinLoc.getBlockX(), arenaMaxLoc.getBlockX());
        int minZ = Math.min(arenaMinLoc.getBlockZ(), arenaMaxLoc.getBlockZ());
        int maxZ = Math.max(arenaMinLoc.getBlockZ(), arenaMaxLoc.getBlockZ());
        int minY = Math.min(arenaMinLoc.getBlockY(), arenaMaxLoc.getBlockY());
        int maxY = Math.max(arenaMinLoc.getBlockY(), arenaMaxLoc.getBlockY());

        int x = random.nextInt(maxX - minX + 1) + minX;
        int y = random.nextInt(maxY - minY + 1) + minY;
        int z = random.nextInt(maxZ - minZ + 1) + minZ;

        return new Location(arenaMinLoc.getWorld(), x + 0.5, y + 0.5, z + 0.5);
    }
    
    public void removeDroppedItemsInRegion(String regionId) {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            plugin.getLogger().warning("Thế giới 'world' không tồn tại.");
            return;
        }

        RegionManager regionManager = getRegionManager();
        if (regionManager == null) {
            plugin.getLogger().warning("Không thể lấy RegionManager.");
            return;
        }

        ProtectedRegion region = regionManager.getRegion(regionId);
        if (region == null) {
            plugin.getLogger().warning("Không tìm thấy region với ID: " + regionId);
            return;
        }

        int removed = 0;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Item) {
                Location loc = entity.getLocation();
                BlockVector3 vec = BukkitAdapter.asBlockVector(loc);

                if (region.contains(vec)) {
                    entity.remove();
                    removed++;
                }
            }
        }

        plugin.getLogger().info("[PvP] Đã xóa " + removed + " vật phẩm rơi trong region '" + regionId + "'");
    }


	public PvpSessionState getCurrentState() {
		// TODO Auto-generated method stub
		return currentState;
	}
}
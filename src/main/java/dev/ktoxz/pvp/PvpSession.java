package dev.ktoxz.pvp;

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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.domains.DefaultDomain; // Import DefaultDomain
import com.sk89q.worldedit.math.BlockVector3;

import java.io.IOException; // Import IOException
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
    private Document chosenArena = null;
    private String arenaRegionName;
    private Map<Player, Integer> playerTeamMap = new HashMap<>();
    private BukkitTask sessionTimeoutTask;

    private ProtectedRegion activeRegion; // Thêm biến này để lưu trữ region đang hoạt động
    private final Set<UUID> playersAddedToRegion = new HashSet<>(); // Để theo dõi những người đã được thêm vào WG region

    private static Location loc1, loc2; //

    private static final Set<Material> allowedMainhandItems = Set.of( //
        Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
        Material.BOW, Material.CROSSBOW, Material.TRIDENT,
        Material.FISHING_ROD, Material.FLINT_AND_STEEL, Material.SHEARS, Material.SNOWBALL, Material.MACE, Material.WIND_CHARGE
    ); //

    public PvpSession(Plugin plugin, Player owner, boolean isPublic, boolean includeSelf) { //
        this.plugin = plugin; //
        this.owner = owner; //
        this.publicRoom = isPublic; //

        this.started = false; //
        this.countdownPhase = false; //
        this.isOver = false; //

        if (includeSelf) { //
            this.players.add(owner); //
        }

        startSessionTimeout(); //
    }

    public PvpSession(Plugin plugin, Player owner, boolean isPublic) { //
        this(plugin, owner, isPublic, true); //
    }

    public Player getOwner() { return owner; } //
    public boolean isPublicRoom() { return publicRoom; } //
    public boolean isStarted() { return started; } //
    public boolean isCountdownPhase() { return countdownPhase; } //
    public boolean isOver() { return isOver; } //
    public Set<Player> getPlayers() { return players; } //
    public Set<Player> getInvitedPlayers() { return invitedPlayers; } //
    public void setStarted(boolean started) { this.started = started; } //
    public String getArenaRegionName() { return arenaRegionName; } //
    public void addInvited(Player player) { invitedPlayers.add(player); }


    // Modified addPlayer method
    public void addPlayer(Player player) {
        players.add(player); //

        // Nếu session đã bắt đầu và có activeRegion, thêm người chơi vào WorldGuard region
        if (isStarted() && activeRegion != null) {
        	RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        	World world = Bukkit.getWorld("world");
        	RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                plugin.getLogger().warning("Could not get RegionManager to add player " + player.getName() + " to region.");
                return;
            }

            DefaultDomain members = activeRegion.getMembers();
            if (!members.contains(player.getUniqueId())) {
                members.addPlayer(player.getUniqueId());
                playersAddedToRegion.add(player.getUniqueId());
                try {
					regionManager.save();
				} catch (StorageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				plugin.getLogger().info("Added " + player.getName() + " to WorldGuard region " + activeRegion.getId() + " upon joining session.");
            }
        }
    }

    // Modified removePlayer method
    public void removePlayer(Player player) {
        players.remove(player); //

        // Xóa người chơi khỏi WorldGuard region khi họ bị loại khỏi session
        if (activeRegion != null) {
            RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(Bukkit.getWorld("world")));
            if (regionManager == null) {
                plugin.getLogger().warning("Could not get RegionManager to remove player " + player.getName() + " from region.");
                return;
            }

            DefaultDomain members = activeRegion.getMembers();
            UUID playerUUID = player.getUniqueId();

            if (members.contains(playerUUID)) {
                members.removePlayer(playerUUID);
                playersAddedToRegion.remove(playerUUID); // Xóa khỏi danh sách theo dõi nội bộ
                plugin.getLogger().info("Removed player " + player.getName() + " from WorldGuard region " + activeRegion.getId());
                try {
					regionManager.save();
				} catch (StorageException e) {
					e.printStackTrace();
				}
            }
        }
    }

    public void broadcast(String message) { //
        for (Player p : players) { //
            p.sendMessage(message); //
        } //
    } //

    public void preparePlayersForBattle() { //
        teleportPlayersToArena(); //
        for (Player player : players) { //
            backupInventory(player); //
            clearInventoryExceptWhitelist(player); //
            spawnChest(player); //
        } //
    } //

    private void backupInventory(Player player) { //
        inventoryBackups.put(player.getUniqueId(), player.getInventory().getContents().clone()); //
        armorBackups.put(player.getUniqueId(), player.getInventory().getArmorContents().clone()); //
    } //

    private void assignTeams() { //
        List<Player> shuffledPlayers = new ArrayList<>(players); //
        Collections.shuffle(shuffledPlayers); //

        for (Player p: players) { //
            playerTeamMap.put(p, 1); //
        } //
    } //

    private void clearInventoryExceptWhitelist(Player player) { //
        List<ItemStack> keep = new ArrayList<>(); //

        for (int i = 0; i < 3; i++) { //
            ItemStack item = player.getInventory().getItem(i); //
            if (item != null && allowedMainhandItems.contains(item.getType())) { //
                keep.add(item.clone()); //
            } //
        } //

        ItemStack offhandItem = player.getInventory().getItemInOffHand(); //
        if (offhandItem != null && offhandItem.getType() != Material.AIR && allowedMainhandItems.contains(offhandItem.getType())) { //
            offhandItem = offhandItem.clone(); //
        } else { //
            offhandItem = null; //
        } //

        ItemStack[] armorContents = player.getInventory().getArmorContents(); //
        ItemStack[] savedArmor = new ItemStack[armorContents.length]; //
        for (int i = 0; i < armorContents.length; i++) { //
            if (armorContents[i] != null && armorContents[i].getType() != Material.AIR) { //
                savedArmor[i] = armorContents[i].clone(); //
            } else { //
                savedArmor[i] = null; //
            } //
        } //

        player.getInventory().clear(); //
        player.getInventory().setArmorContents(new ItemStack[4]); //
        player.getInventory().setItemInOffHand(null); //

        int slot = 0; //
        for (ItemStack item : keep) { //
            player.getInventory().setItem(slot++, item); //
        } //

        if (offhandItem != null) { //
            player.getInventory().setItemInOffHand(offhandItem); //
        } //

        player.getInventory().setArmorContents(savedArmor); //
    } //

    private void spawnChest(Player player) { //
        Location loc = player.getLocation().clone().add(player.getLocation().getDirection().setY(0).normalize().multiply(2)); //
        loc.setY(player.getWorld().getHighestBlockYAt(loc) + 1); //
        Block block = loc.getBlock(); //
        block.setType(Material.CHEST); //
        if (block.getState() instanceof Chest chest) { //
            ChestManager.fillChestWithItems(chest.getInventory()); //
        } //
        playerChestLocations.put(player, loc); //
    } //

    public void startCountdown() { //
        countdownPhase = true; //
        if (sessionTimeoutTask != null) { //
            sessionTimeoutTask.cancel(); //
            sessionTimeoutTask = null; //
        } //
        new BukkitRunnable() { //
            int countdown = 20; //
            boolean isHeal = false; //
            @Override //
            public void run() { //
                if (!PvpSessionManager.hasActiveSession() || PvpSessionManager.getActiveSession() != PvpSession.this) { //
                    cancel(); //
                    return; //
                } //
                if (!isHeal) { //
                    isHeal = true; //
                    for (Player p : players) { //
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 9)); //
                    } //
                } //

                if (countdown == 0) { //
                    countdownPhase = false; //
                    started = true; //
                    assignTeams(); //
                    destroyChests(); //
                    broadcast("§cBẮT ĐẦU PvP!"); //
                    startRandomEvents(); //
                    cancel(); //
                    return; //
                } //
                for (Player player : players) { //
                    if (player.isOnline()) { //
                        player.sendTitle("§eChuẩn bị!", "§f" + countdown + " giây", 0, 20, 0); //
                    } //
                } //
                countdown--; //
            } //
        }.runTaskTimer(plugin, 0L, 20L); //
    } //

    // Modified teleportPlayersToArena method
    private void teleportPlayersToArena() {
        if (arenaRegionName == null) { //
            chosenArena = dev.ktoxz.manager.TeleportManager.getRandomArena(); //

            if (chosenArena == null) { //
                broadcast("§cKhông tìm thấy Arena hợp lệ!"); //
                return; //
            }
            arenaRegionName = chosenArena.getString("_id"); //
        }

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer(); //
        World world = Bukkit.getWorld("world"); //
        if (world == null) { //
            broadcast("§cWorld không tồn tại."); //
            return; //
        }
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world)); //
        if (regionManager == null) { //
            broadcast("§cKhông tìm thấy RegionManager!"); //
            return; //
        }
        ProtectedRegion region = regionManager.getRegion(arenaRegionName); //
        if (region == null) { //
            broadcast("§cKhông tìm thấy WorldGuard region với tên: " + arenaRegionName); //
            return; //
        }

        this.activeRegion = region; // Lưu trữ region đang hoạt động

        List<Document> availableSpots = new ArrayList<>(); //
        if (chosenArena != null && chosenArena.containsKey("spots")) { //
            availableSpots.addAll(chosenArena.getList("spots", Document.class)); //
        } //

        if (availableSpots.isEmpty()) { //
            broadcast("§cArena không có chỗ spawn!"); //
            return; //
        } //

        Random random = new Random(); //

        for (Player player : players) { //
            if (availableSpots.isEmpty()) { //
                player.sendMessage("§cKhông còn chỗ spawn trống!"); //
                continue; //
            } //

            Document spot = availableSpots.remove(random.nextInt(availableSpots.size())); //

            int x = spot.getInteger("x"); //
            int y = spot.getInteger("y"); //
            int z = spot.getInteger("z"); //

            player.teleport(new Location(world, x + 0.5, y, z + 0.5)); //

            // THÊM NGƯỜI CHƠI VÀO WORLDGUARD REGION TẠI ĐÂY
            if (activeRegion != null) {
                DefaultDomain members = activeRegion.getMembers();
                if (!members.contains(player.getUniqueId())) {
                    members.addPlayer(player.getUniqueId());
                    playersAddedToRegion.add(player.getUniqueId()); // Thêm vào set theo dõi
                    try {
						regionManager.save();
					} catch (StorageException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					plugin.getLogger().info("Added " + player.getName() + " to WorldGuard region " + activeRegion.getId());
                }
            }
        }
    }

    void destroyChests() { //
        for (Location loc : playerChestLocations.values()) { //
            Block block = loc.getBlock(); //
            if (block.getType() == Material.CHEST) { //
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1f); //
                block.getWorld().spawnParticle(Particle.EXPLOSION, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.01); //
                block.setType(Material.AIR); //
            } //
        } //
        playerChestLocations.clear(); //
    } //

    public void startRandomEvents() { //
        new BukkitRunnable() { //
            @Override //
            public void run() { //
                if (players.size() <= 1 || isOver) { //
                    cancel(); //
                    return; //
                } //

                if (arenaRegionName == null || plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) { //
                    plugin.getLogger().warning("Không tìm thấy tên region hoặc WorldGuard! Không thể kích hoạt RandomEvent."); //
                    cancel(); //
                    return; //
                } //

                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer(); //
                World world = Bukkit.getWorld("world"); //
                if (world == null) { //
                    plugin.getLogger().warning("World không tồn tại! Không thể kích hoạt RandomEvent."); //
                    cancel(); //
                    return; //
                } //
                RegionManager regionManager = container.get(BukkitAdapter.adapt(world)); //
                if (regionManager == null) { //
                    plugin.getLogger().warning("Không tìm thấy RegionManager cho WorldGuard! Không thể kích hoạt RandomEvent."); //
                    cancel(); //
                    return; //
                } //

                ProtectedRegion region = regionManager.getRegion(arenaRegionName); //
                if (region == null) { //
                    plugin.getLogger().warning("Không tìm thấy WorldGuard region với ID: " + arenaRegionName + "! Không thể kích hoạt RandomEvent."); //
                    cancel(); //
                    return; //
                } //

                BlockVector3 minPoint = region.getMinimumPoint(); //
                BlockVector3 maxPoint = region.getMaximumPoint(); //

                loc1 = BukkitAdapter.adapt(world, minPoint); //
                loc2 = BukkitAdapter.adapt(world, maxPoint); //

                PvpEventManager.triggerRandomEvent(players); //
            } //
        }.runTaskTimer(plugin, 20 * 10L, 20 * 10L); //
    } //

    public void handleVictory() { //
        if (players.size() == 1 && !isOver) { //
            isOver = true; //
            // restoreAllInventories() và PvpSessionManager.closeSession() sẽ được gọi từ PvpSessionManager
            Player winner = players.iterator().next(); //
            win(winner); //
            PvpSessionManager.closeSession(); //
        } //
    } //

    public void win(Player winner) { //
        if (winner == null) return; //

        for (PotionEffect effect : winner.getActivePotionEffects()) { //
            winner.removePotionEffect(effect.getType()); //
        } //

        winner.getInventory().clear(); //

        UUID uuid = winner.getUniqueId(); //
        if (inventoryBackups.containsKey(uuid)) { //
            winner.getInventory().setContents(inventoryBackups.get(uuid)); //
        } //
        if (armorBackups.containsKey(uuid)) { //
            winner.getInventory().setArmorContents(armorBackups.get(uuid)); //
        } //

        winner.sendTitle("§6CHIẾN THẮNG!", "§eBạn là người sống sót cuối cùng!", 20, 100, 20); //
        Location loc = winner.getLocation(); //
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1); //
        spawnCelebrationFirework(loc); //
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f); //

        winner.giveExp(100); //

        Bukkit.broadcastMessage("§6[Winner] §f" + winner.getName() + " đã chiến thắng trận PvP!"); //
    } //

    private void spawnCelebrationFirework(Location loc) { //
        Firework firework = (Firework) loc.getWorld().spawn(loc, Firework.class); //
        FireworkMeta meta = firework.getFireworkMeta(); //
        meta.addEffect(FireworkEffect.builder() //
                .flicker(true) //
                .trail(true) //
                .withColor(Color.AQUA, Color.LIME, Color.YELLOW) //
                .withFade(Color.PURPLE) //
                .with(FireworkEffect.Type.BALL_LARGE) //
                .build()); //
        meta.setPower(1); //
        firework.setFireworkMeta(meta); //
    } //

    public void restoreAllInventories() { //
        for (UUID uuid : inventoryBackups.keySet()) { //
            Player player = Bukkit.getPlayer(uuid); //
            if (player != null && player.isOnline()) { //
                restoreInventory(player); //
            } //
        } //
    } //

    private void restoreInventory(Player player) { //
        UUID uuid = player.getUniqueId(); //
        if (inventoryBackups.containsKey(uuid)) { //
            player.getInventory().setContents(inventoryBackups.get(uuid)); //
        } //
        if (armorBackups.containsKey(uuid)) { //
            player.getInventory().setArmorContents(armorBackups.get(uuid)); //
        } //
    } //

    public void setCountdownPhase(boolean countdownPhase) { //
        this.countdownPhase = countdownPhase; //
    } //

    private void startSessionTimeout() { //
        sessionTimeoutTask = new BukkitRunnable() { //
            @Override //
            public void run() { //
                if (!started) { //
                    broadcast("§cPhiên PvP đã hết thời gian chờ và tự động đóng."); //
                    PvpSessionManager.closeSession(); //
                } //
            } //
        }.runTaskLater(plugin, 20L * 180); //
    } //

    public void cancelSessionTimeout() { //
        if (sessionTimeoutTask != null) { //
            sessionTimeoutTask.cancel(); //
            sessionTimeoutTask = null; //
        } //
    } //

    public Location getRandomLocationInArena() { //
    	Random random = new Random(); //
        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX()); //
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX()); //
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ()); //
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ()); //
        int y = loc1.getBlockY()+1; //

        int x = random.nextInt(maxX - minX + 1) + minX; //
        int z = random.nextInt(maxZ - minZ + 1) + minZ; //

        return new Location(loc1.getWorld(), x, y, z); //
    } //

    public Location getRandomLocationGround() { //
        Random random = new Random(); //
        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX()); //
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX()); //
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ()); //
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ()); //
        World world = loc1.getWorld(); //

        int x = random.nextInt(maxX - minX + 1) + minX; //
        int z = random.nextInt(maxZ - minZ + 1) + minZ; //

        int y = world.getHighestBlockYAt(x, z); //

        return new Location(world, x + 0.5, y, z + 0.5); //
    } //

}
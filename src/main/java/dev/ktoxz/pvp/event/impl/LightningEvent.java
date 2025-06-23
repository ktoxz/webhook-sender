package dev.ktoxz.pvp.event.impl;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import dev.ktoxz.pvp.PvpSessionManager;
import dev.ktoxz.pvp.event.PvpEvent;

public class LightningEvent extends PvpEvent{

    // Không cần activeTasks ở đây nữa, nó đã ở PvpEvent
    // private final List<BukkitTask> activeTasks = new ArrayList<>();

    public LightningEvent(Plugin plugin) {
        super("Sấm chớp", "Sấm chớp đùng đùng", plugin);
    }

    @Override
    public void trigger(Set<Player> players) {
    	showerLightning(players);
    }

    private static void showerLightning(Set<Player> players) {
    	 World world = PvpSessionManager.getActiveSession().getRandomLocationInArena().getWorld();

         WeatherType oldWeather = world.hasStorm() ? WeatherType.DOWNFALL : WeatherType.CLEAR;

         world.setStorm(true);
         broadcastActionBar(players, "[ENVIRONMENT] ⚡ Lightning Storm đang diễn ra!");

         BukkitTask task = new BukkitRunnable() {
             int count = 0;

             @Override
             public void run() {
                 if (count >= 10) {
                     world.setStorm(oldWeather == WeatherType.DOWNFALL);
                     broadcastActionBar(players, "[ENVIRONMENT] ☀ Trời đã quang trở lại.");
                     cancel();
                     return;
                 }

                 Location strikeLoc = PvpSessionManager.getActiveSession().getRandomLocationInArena();
                 world.strikeLightning(strikeLoc);
                 count++;
             }
         }.runTaskTimer(plugin, 0L, 20L);
         activeTasks.add(task);
    }
    
    // Không cần onEndMatch() nữa nếu không có logic cụ thể nào khác
    // @Override
    // public void onEndMatch() {
    //     super.onEndMatch(); // Gọi phương thức của lớp cha để hủy tác vụ
    // }
}
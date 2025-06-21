package dev.ktoxz.pvp.event.impl;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import dev.ktoxz.pvp.PvpSessionManager;
import dev.ktoxz.pvp.event.PvpEvent;

public class LightningEvent extends PvpEvent{

	public LightningEvent(Plugin plugin) {
		super("Sấm chớp", "Sấm chớp đùng đùng", plugin);
	}

	@Override
	public void trigger(Set<Player> players) {
		World world = PvpSessionManager.getActiveSession().getRandomLocationInArena().getWorld();
		
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

                Location strikeLoc = PvpSessionManager.getActiveSession().getRandomLocationInArena();
                world.strikeLightning(strikeLoc);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // 20 ticks = 1 giây		
	}

	@Override
	public void onEndMatch() {
		// TODO Auto-generated method stub
		
	}

}

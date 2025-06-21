package dev.ktoxz.pvp;

import dev.ktoxz.pvp.event.PvpEvent;
import dev.ktoxz.pvp.event.impl.DropEvent;
import dev.ktoxz.pvp.event.impl.EffectEvent;
import dev.ktoxz.pvp.event.impl.FunEvent;
import dev.ktoxz.pvp.event.impl.LightningEvent;
import dev.ktoxz.pvp.event.impl.PitfallTrapEvent;
import dev.ktoxz.pvp.event.impl.ShowerEvent;
import dev.ktoxz.pvp.event.impl.SummonEvent;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PvpEventManager {

    private static final List<PvpEvent> registeredEvents = new ArrayList<>();
    private static final Random random = new Random();
    private static Plugin plugin; // Reference to main plugin
    
    public static void init(Plugin pl) {
        plugin = pl;
//        registerEvent(new DropEvent(pl));
//        registerEvent(new EffectEvent(pl));
//        registerEvent(new FunEvent(pl));
        registerEvent(new LightningEvent(pl));
        registerEvent(new PitfallTrapEvent(pl));
        registerEvent(new ShowerEvent(pl));
        registerEvent(new SummonEvent(pl));

    }

    public static void registerEvent(PvpEvent event) {
        registeredEvents.add(event);
        plugin.getLogger().info("Registered PvP Event: " + event.getName());
    }

    public static void triggerRandomEvent(Set<Player> players) {
        if (registeredEvents.isEmpty()) {
            plugin.getLogger().warning("No PvP events registered! Cannot trigger random event.");
            return;
        }

        PvpEvent chosenEvent = registeredEvents.get(random.nextInt(registeredEvents.size()));
        plugin.getLogger().info("Triggering random PvP event: " + chosenEvent.getName());
        chosenEvent.trigger(players);
    }

    public static void clearEvents() {
        registeredEvents.clear();
    }
    
    
    //------------------------------------------------------------------//
}
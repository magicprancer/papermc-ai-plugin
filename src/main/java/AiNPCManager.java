package com.example.aiplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class AINPCManager {
    private final AIPlugin plugin;
    private final OpenAIClient client;
    private final Map<String, AINPC> npcs = new HashMap<>();

    public AINPCManager(AIPlugin plugin, OpenAIClient client) {
        this.plugin = plugin;
        this.client = client;
        loadFromConfig();
    }

    private void loadFromConfig() {
        if (!plugin.getConfig().isConfigurationSection("npcs")) return;
        for (String key : plugin.getConfig().getConfigurationSection("npcs").getKeys(false)) {
            String path = "npcs." + key;
            String name = plugin.getConfig().getString(path + ".name");
            String type = plugin.getConfig().getString(path + ".entity_type", "VILLAGER");
            String world = plugin.getConfig().getString(path + ".spawn.world", "world");
            double x = plugin.getConfig().getDouble(path + ".spawn.x", 0);
            double y = plugin.getConfig().getDouble(path + ".spawn.y", 65);
            double z = plugin.getConfig().getDouble(path + ".spawn.z", 0);
            String personality = plugin.getConfig().getString(path + ".personality", "friendly");
            boolean react = plugin.getConfig().getBoolean(path + ".react_to_player_chat", true);
            int prox = plugin.getConfig().getInt(path + ".proximity_trigger", 5);
            int memory = plugin.getConfig().getInt("npc_memory_size", 10);

            AINPC npc = new AINPC(key, name, EntityType.valueOf(type.toUpperCase()), personality, react, prox, plugin, client, memory);
            World w = Bukkit.getWorld(world);
            if (w != null) {
                Location loc = new Location(w, x, y, z);
                npc.spawnAt(loc);
                npcs.put(key, npc);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (AINPC n : npcs.values()) n.tick();
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    public void handleChat(Player sender, String message) {
        for (AINPC n : npcs.values()) {
            if (message.toLowerCase().contains(n.getName().toLowerCase())) n.onPlayerMentioned(sender, message);
            if (n.isReactToPlayerChat() && sender.getLocation().distance(n.getEntity().getLocation()) <= n.getProximityTrigger()) n.onPlayerNearby(sender);
        }
    }
}

package com.example.aiplugin;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class AINPC {
    private final String id;
    private final String name;
    private final EntityType type;
    private final String personality;
    private final boolean reactToPlayerChat;
    private final int proximityTrigger;
    private final AIPlugin plugin;
    private final OpenAIClient client;
    private final Queue<String> memory;
    private LivingEntity entity;
    private final int memorySize;

    public AINPC(String id, String name, EntityType type, String personality, boolean reactToPlayerChat, int proximityTrigger, AIPlugin plugin, OpenAIClient client, int memorySize) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.personality = personality;
        this.reactToPlayerChat = reactToPlayerChat;
        this.proximityTrigger = proximityTrigger;
        this.plugin = plugin;
        this.client = client;
        this.memorySize = memorySize;
        this.memory = new LinkedList<>();
    }

    public void spawnAt(Location loc) {
        this.entity = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        this.entity.setCustomName(name);
        this.entity.setCustomNameVisible(true);
    }

    public void tick() {
        if (entity == null || entity.isDead()) return;
    }

    public void onPlayerNearby(Player p) {
        p.sendMessage(name + ": Greetings, " + p.getName() + ".");
    }

    public void onPlayerMentioned(Player p, String message) {
        String userLine = "Player " + p.getName() + ": " + message;
        remember(userLine);
        p.sendMessage(name + ": Thinking...");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("You are %s, an NPC in a Minecraft server. Personality: %s. Respond concisely.", name, personality));
        sb.append("\nRecent context:\n");
        for (String m : memory) sb.append(m).append("\n");
        sb.append("Player says: ").append(message);
        String system = sb.toString();
        String prompt = message;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                AIResponse res = client.chat(system, prompt);
                if (res.getContent() != null) p.sendMessage(name + ": " + res.getContent());
                if (res.getFunctionCall() != null && plugin.getConfig().getBoolean("allow_function_calls", true)) {
                    // NPC can request actions, but route through admin manager and require permission or operator
                    plugin.getAdminManager().handleFunctionCall(p, res.getFunctionCall());
                }
            } catch (IOException e) {
                p.sendMessage(name + ": Sorry, I couldn't think right now.");
            }
        });
    }

    private void remember(String line) {
        memory.add(line);
        while (memory.size() > memorySize) memory.poll();
    }

    public String getName() { return name; }
    public LivingEntity getEntity() { return entity; }
    public boolean isReactToPlayerChat() { return reactToPlayerChat; }
    public int getProximityTrigger() { return proximityTrigger; }
}

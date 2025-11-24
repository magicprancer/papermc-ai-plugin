package com.example.aiplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.IOException;

public class AIEventListener implements Listener {
    private final AIPlugin plugin;
    private final OpenAIClient client;
    private final AINPCManager npcManager;

    public AIEventListener(AIPlugin plugin, OpenAIClient client, AINPCManager npcManager) {
        this.plugin = plugin;
        this.client = client;
        this.npcManager = npcManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String msg = event.getMessage();
        Player sender = event.getPlayer();

        if (msg.startsWith("@ai ") || msg.toLowerCase().contains("hey ai")) {
            String prompt = msg.replaceFirst("(?i)@?ai", "").trim();
            String system = "You are a helpful Minecraft server assistant.";
            try {
                AIResponse res = client.chat(system, prompt);
                if (res.getContent() != null) sender.sendMessage(res.getContent());
                if (res.getFunctionCall() != null) {
                    plugin.getAdminManager().handleFunctionCall(sender, res.getFunctionCall());
                }
            } catch (IOException e) {
                sender.sendMessage("§cAI request failed: " + e.getMessage());
            }
        }

        npcManager.handleChat(sender, msg);
    }
}

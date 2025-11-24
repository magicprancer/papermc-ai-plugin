package com.example.aiplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class AICommand implements CommandExecutor {
    private final AIPlugin plugin;
    private final OpenAIClient client;

    public AICommand(AIPlugin plugin, OpenAIClient client) {
        this.plugin = plugin;
        this.client = client;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /ai <message>");
            return true;
        }
        String prompt = String.join(" ", args);
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
        return true;
    }
}

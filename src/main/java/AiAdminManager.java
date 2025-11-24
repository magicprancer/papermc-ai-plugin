package com.example.aiplugin;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AIAdminManager {
    private final AIPlugin plugin;
    private final Map<UUID, RateWindow> rate = new HashMap<>();
    private final Path auditLog;

    public AIAdminManager(AIPlugin plugin) {
        this.plugin = plugin;
        this.auditLog = plugin.getDataFolder().toPath().resolve("audit.log");
        try { Files.createDirectories(plugin.getDataFolder().toPath()); } catch (IOException ignored) {}
    }

    public void handleFunctionCall(CommandSender caller, JsonObject functionCall) {
        if (functionCall == null) return;
        String name = functionCall.has("name") ? functionCall.get("name").getAsString() : null;
        JsonObject args = functionCall.has("arguments") ? functionCall.getAsJsonObject("arguments") : new JsonObject();

        // audit
        if (plugin.getConfig().getBoolean("ai_admin_audit_log", true)) {
            logAudit(caller.getName(), functionCall.toString());
        }

        if ("run_command".equals(name)) {
            String cmd = args.has("command") ? args.get("command").getAsString() : null;
            if (cmd == null) return;
            if (!(caller instanceof Player) || !caller.hasPermission(plugin.getConfig().getString("allowed_permission", "aiplugin.admin"))) {
                caller.sendMessage("§cYou do not have permission to run admin functions via AI.");
                return;
            }
            Player p = (Player) caller;
            if (!checkRate(p.getUniqueId())) {
                caller.sendMessage("§cAI admin rate limit exceeded.");
                return;
            }
            // Basic safety: do not allow commands that contain dangerous tokens unless server operator
            if (!isSafeCommand(cmd) && !p.isOp()) {
                caller.sendMessage("§cCommand rejected by safety filter.");
                return;
            }
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
            caller.sendMessage("§aExecuted command: " + cmd);
        } else if ("summon_mob".equals(name)) {
            if (!plugin.getConfig().getBoolean("summon_mobs_allowed", false)) {
                caller.sendMessage("§cSummoning mobs is not allowed.");
                return;
            }
            String type = args.has("type") ? args.get("type").getAsString() : "ZOMBIE";
            if (!(caller instanceof Player) || !caller.hasPermission(plugin.getConfig().getString("allowed_permission", "aiplugin.admin"))) {
                caller.sendMessage("§cYou do not have permission to summon mobs via AI.");
                return;
            }
            Player p = (Player) caller;
            try {
                p.getWorld().spawnEntity(p.getLocation(), org.bukkit.entity.EntityType.valueOf(type.toUpperCase()));
                caller.sendMessage("§aSummoned: " + type);
            } catch (IllegalArgumentException ex) {
                caller.sendMessage("§cUnknown entity type: " + type);
            }
        } else if ("admin_action".equals(name)) {
            String action = args.has("action") ? args.get("action").getAsString() : null;
            String target = args.has("target") ? args.get("target").getAsString() : null;
            if (action == null) return;
            if (!(caller instanceof Player) || !caller.hasPermission(plugin.getConfig().getString("allowed_permission", "aiplugin.admin"))) {
                caller.sendMessage("§cNo permission for admin actions.");
                return;
            }
            List<?> allowed = plugin.getConfig().getList("ai_admin_allowed_commands");
            if (allowed.contains(action)) {
                // whitelist check
                List<String> whitelist = plugin.getConfig().getStringList("ai_admin_whitelist_targets");
                if (target != null && !whitelist.isEmpty() && !whitelist.contains(target)) {
                    caller.sendMessage("§cTarget not whitelisted for AI actions.");
                    return;
                }
                String built = buildAdminCommand(action, target);
                if (!isSafeCommand(built) && !((Player) caller).isOp()) {
                    caller.sendMessage("§cAdmin action rejected by safety filter.");
                    return;
                }
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), built);
                caller.sendMessage("§aAdmin action executed: " + built);
            } else {
                caller.sendMessage("§cRequested admin action not allowed: " + action);
            }
        }
    }

    private boolean isSafeCommand(String cmd) {
        // Very simple blacklist of obviously dangerous patterns
        String lower = cmd.toLowerCase();
        if (lower.contains("stop") || lower.contains("shutdown") || lower.contains("op %") || lower.contains("save-all")) return false;
        return true;
    }

    private String buildAdminCommand(String action, String target) {
        switch (action) {
            case "kick": return "kick " + target + " You were removed by the AI admin";
            case "ban": return "ban " + target + " Banned by AI admin";
            case "mute": return "mute " + target;
            case "tp": return "tp " + target;
            case "time": return "time set day";
            case "weather": return "weather clear";
            default: return "";
        }
    }

    private boolean checkRate(UUID uuid) {
        RateWindow w = rate.computeIfAbsent(uuid, k -> new RateWindow());
        return w.tryConsume(plugin.getConfig().getInt("ai_admin_max_actions_per_minute", 5));
    }

    private void logAudit(String actor, String payload) {
        if (!plugin.getConfig().getBoolean("ai_admin_audit_log", true)) return;
        try (FileWriter fw = new FileWriter(auditLog.toFile(), true); PrintWriter pw = new PrintWriter(fw)) {
            pw.println(new Date().toString() + " | " + actor + " | " + payload);
        } catch (IOException ignored) {}
    }

    private static class RateWindow {
        private long windowStart = System.currentTimeMillis();
        private int count = 0;

        synchronized boolean tryConsume(int maxPerMinute) {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60_000) {
                windowStart = now;
                count = 0;
            }
            if (count >= maxPerMinute) return false;
            count++;
            return true;
        }
    }
}

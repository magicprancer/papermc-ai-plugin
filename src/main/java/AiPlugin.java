package com.example.aiplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class AIPlugin extends JavaPlugin {
    private OpenAIClient openAIClient;
    private AIAdminManager adminManager;
    private AINPCManager npcManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        String apiKey = cfg.getString("openai_api_key", "");
        String model = cfg.getString("model", "gpt-4o-mini");
        openAIClient = new OpenAIClient(apiKey, model, cfg.getBoolean("allow_function_calls", true), this);
        adminManager = new AIAdminManager(this);
        npcManager = new AINPCManager(this, openAIClient);
        getCommand("ai").setExecutor(new AICommand(this, openAIClient));
        getServer().getPluginManager().registerEvents(new AIEventListener(this, openAIClient, npcManager), this);
        getLogger().info("AIPlugin enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("AIPlugin disabled");
    }

    public AIAdminManager getAdminManager() { return adminManager; }
    public AINPCManager getNpcManager() { return npcManager; }
}

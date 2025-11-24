package com.example.aiplugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Small OpenAI client that includes function schema registration to improve safe function-calls.
 * The function schemas are basic and map to what AIAdminManager expects.
 */
public class OpenAIClient {
    private final String apiKey;
    private final String model;
    private final boolean allowFunctionCalls;
    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private final JavaPlugin plugin;

    public OpenAIClient(String apiKey, String model, boolean allowFunctionCalls, JavaPlugin plugin) {
        this.apiKey = (apiKey != null && !apiKey.isEmpty()) ? apiKey : System.getenv("OPENAI_API_KEY");
        this.model = model;
        this.allowFunctionCalls = allowFunctionCalls;
        this.plugin = plugin;
        this.http = new OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build();
    }

    public AIResponse chat(String systemPrompt, String userPrompt) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        JsonArray messages = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt == null ? "" : systemPrompt);
        messages.add(sys);
        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", userPrompt);
        messages.add(usr);
        payload.add("messages", messages);

        if (allowFunctionCalls) {
            // Register basic function schemas to help the model produce structured function calls
            JsonArray functions = new JsonArray();

            JsonObject runCommand = new JsonObject();
            runCommand.addProperty("name", "run_command");
            JsonObject runParams = new JsonObject();
            runParams.addProperty("type", "object");
            JsonObject runProps = new JsonObject();
            JsonObject cmd = new JsonObject(); cmd.addProperty("type", "string"); cmd.addProperty("description", "Command to run as console");
            runProps.add("command", cmd);
            runParams.add("properties", runProps);
            runCommand.add("parameters", runParams);
            functions.add(runCommand);

            JsonObject summon = new JsonObject();
            summon.addProperty("name", "summon_mob");
            JsonObject sumParams = new JsonObject();
            sumParams.addProperty("type", "object");
            JsonObject sumProps = new JsonObject();
            JsonObject typeProp = new JsonObject(); typeProp.addProperty("type", "string"); typeProp.addProperty("description", "Entity type (ZOMBIE, VILLAGER, etc.)");
            sumProps.add("type", typeProp);
            sumParams.add("properties", sumProps);
            summon.add("parameters", sumParams);
            functions.add(summon);

            JsonObject adminAction = new JsonObject();
            adminAction.addProperty("name", "admin_action");
            JsonObject adminParams = new JsonObject();
            adminParams.addProperty("type", "object");
            JsonObject adminProps = new JsonObject();
            JsonObject actionProp = new JsonObject(); actionProp.addProperty("type", "string"); actionProp.addProperty("description", "Action name (kick, ban, mute, tp, time, weather)");
            JsonObject targetProp = new JsonObject(); targetProp.addProperty("type", "string"); targetProp.addProperty("description", "Target player name (if applicable)");
            adminProps.add("action", actionProp); adminProps.add("target", targetProp);
            adminParams.add("properties", adminProps);
            adminAction.add("parameters", adminParams);
            functions.add(adminAction);

            payload.add("functions", functions);
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code() + " - " + response.message());
            String respBody = response.body().string();
            JsonObject json = gson.fromJson(respBody, JsonObject.class);
            JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
            JsonObject message = choice.getAsJsonObject("message");
            String content = message.has("content") ? message.get("content").getAsString() : null;
            JsonObject functionCall = message.has("function_call") ? message.getAsJsonObject("function_call") : null;
            return new AIResponse(content, functionCall);
        }
    }
}

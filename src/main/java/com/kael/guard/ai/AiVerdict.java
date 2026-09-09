package com.kael.guard.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class AiVerdict {

    private final boolean suspected;
    private final double confidence;
    private final List<String> categories;
    private final String reason;
    private final String recommendedAction;
    private final String memoryUpdate;

    public AiVerdict(boolean suspected, double confidence, List<String> categories,
                     String reason, String recommendedAction, String memoryUpdate) {
        this.suspected = suspected;
        this.confidence = confidence;
        this.categories = categories;
        this.reason = reason;
        this.recommendedAction = recommendedAction;
        this.memoryUpdate = memoryUpdate;
    }

    public static AiVerdict parse(JsonObject obj) {
        boolean suspected = obj.get("suspected") != null && obj.get("suspected").getAsBoolean();
        double confidence = obj.get("confidence") != null ? obj.get("confidence").getAsDouble() : 0;
        List<String> categories = new ArrayList<>();
        if (obj.get("categories") != null && obj.get("categories").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("categories");
            for (int i = 0; i < arr.size(); i++) categories.add(arr.get(i).getAsString());
        }
        String reason = str(obj, "reason");
        String action = str(obj, "recommended_action");
        String memory = str(obj, "memory_update");
        return new AiVerdict(suspected, confidence, categories, reason, action, memory);
    }

    private static String str(JsonObject obj, String key) {
        return obj.get(key) != null && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    public boolean suspected() { return suspected; }
    public double confidence() { return confidence; }
    public List<String> categories() { return categories; }
    public String reason() { return reason; }
    public String recommendedAction() { return recommendedAction; }
    public String memoryUpdate() { return memoryUpdate; }
}

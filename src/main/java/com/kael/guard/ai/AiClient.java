package com.kael.guard.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kael.guard.KaelorvynGuard;
import com.kael.guard.Settings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class AiClient {

    private final KaelorvynGuard plugin;
    private final HttpClient client;
    private static final Gson GSON = new Gson();

    public AiClient(KaelorvynGuard plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public CompletableFuture<AiVerdict> review(String snapshot) {
        Settings s = plugin.settings();
        JsonObject body = new JsonObject();
        body.addProperty("model", s.model());
        body.addProperty("max_tokens", s.maxTokens());
        JsonArray messages = new JsonArray();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", """
                你是 Kaelorvyn 服务器反作弊 AI 复核员。玩家可能被实时规则引擎标记。
                请根据快照判断是否真的可疑。只输出 JSON：
                {"suspected":true/false,"confidence":0-1,"categories":["FLIGHT"],"reason":"中文一句话","recommended_action":"alert|kick|ban|clear","memory_update":"值得记住的长期特征"}
                宁可漏报也不冤枉正常玩家。
                SLOW_FALL 与 STEP 属于易误报检测：刚出水、普通跳跃、爬楼梯/台阶都可能触发；
                只有持续数秒的缓降或客户端声称仍在地面时的持续异常上升才可疑。
                快照含 water/swimming/onGround/lastWaterMsAgo 时，优先按正常行为解释。""");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", snapshot);
        messages.add(user);
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(s.baseUrl() + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + s.apiKey())
                .timeout(Duration.ofSeconds(s.timeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parse(response.body()))
                .exceptionally(e -> {
                    plugin.getLogger().warning("Agnes AI 请求失败：" + e.getMessage());
                    return new AiVerdict(false, 0, java.util.List.of(), "", "", "");
                });
    }

    private AiVerdict parse(String body) {
        try {
            body = stripJsonFence(body);
            JsonObject root = GSON.fromJson(body, JsonObject.class);
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return new AiVerdict(false, 0, java.util.List.of(), "", "", "");
            String content = choices.get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
            JsonObject verdict = GSON.fromJson(content, JsonObject.class);
            return AiVerdict.parse(verdict);
        } catch (Exception e) {
            plugin.getLogger().warning("Agnes AI 响应解析失败：" + e.getMessage());
            return new AiVerdict(false, 0, java.util.List.of(), "", "", "");
        }
    }

    private static String stripJsonFence(String body) {
        if (body == null) {
            return "";
        }
        int start = body.indexOf('{');
        int end = body.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return body.substring(start, end + 1);
        }
        return body;
    }
}

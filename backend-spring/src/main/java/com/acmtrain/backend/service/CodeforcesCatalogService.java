package com.acmtrain.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CodeforcesCatalogService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private volatile Map<String, CatalogProblem> problemMap = Map.of();
    private volatile List<CatalogProblem> orderedProblems = List.of();
    private volatile Instant loadedAt = Instant.EPOCH;

    public CodeforcesCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Map<String, CatalogProblem> loadProblemMap() {
        ensureLoaded();
        return problemMap;
    }

    public List<CatalogProblem> loadProblems() {
        ensureLoaded();
        return orderedProblems;
    }

    private void ensureLoaded() {
        Instant now = Instant.now();
        if (!problemMap.isEmpty() && Duration.between(loadedAt, now).toHours() < 6) {
            return;
        }

        synchronized (this) {
            if (!problemMap.isEmpty() && Duration.between(loadedAt, now).toHours() < 6) {
                return;
            }

            Map<String, CatalogProblem> nextMap = fetchProblems();
            List<CatalogProblem> nextList = nextMap.values().stream()
                    .sorted(Comparator
                            .comparing(CatalogProblem::rating)
                            .thenComparing(CatalogProblem::problemCode))
                    .toList();
            problemMap = nextMap;
            orderedProblems = nextList;
            loadedAt = now;
        }
    }

    private Map<String, CatalogProblem> fetchProblems() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://codeforces.com/api/problemset.problems"))
                    .GET()
                    .header("User-Agent", "acm-train-problem-catalog/1.0")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codeforces 题库接口请求失败: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!"OK".equalsIgnoreCase(root.path("status").asText())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, root.path("comment").asText("Codeforces 题库信息获取失败"));
            }

            Map<String, CatalogProblem> loaded = new HashMap<>();
            for (JsonNode item : root.path("result").path("problems")) {
                String contestId = item.path("contestId").isMissingNode() ? "" : item.path("contestId").asText("");
                String index = item.path("index").asText("");
                String key = buildProblemKey(contestId, index);
                if (key.isBlank()) {
                    continue;
                }

                List<String> tags = new ArrayList<>();
                item.path("tags").forEach(tag -> {
                    String value = tag.asText("");
                    if (!value.isBlank()) {
                        tags.add(value);
                    }
                });
                int rating = item.path("rating").asInt(0);
                loaded.put(key, new CatalogProblem(
                        formatCodeforcesProblemCode(contestId, index),
                        item.path("name").asText("Codeforces Problem"),
                        rating,
                        tags.isEmpty() ? "Implementation" : tags.get(0),
                        tags,
                        "https://codeforces.com/problemset/problem/" + contestId + "/" + index
                ));
            }
            return loaded;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codeforces 题库信息解析失败");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codeforces 题库接口请求被中断");
        }
    }

    private String buildProblemKey(String contestId, String index) {
        if (contestId == null || contestId.isBlank() || index == null || index.isBlank()) {
            return "";
        }
        return contestId + ":" + index;
    }

    private String formatCodeforcesProblemCode(String contestId, String index) {
        return "CF " + contestId + index;
    }

    public record CatalogProblem(
            String problemCode,
            String title,
            int rating,
            String primaryTag,
            List<String> tags,
            String url
    ) {
    }
}

package com.acmtrain.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class OfficialOjAccountValidationService implements OjAccountValidationService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OfficialOjAccountValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public void validateCodeforcesHandle(String handle) {
        String normalized = normalizeRequired(handle, "Codeforces");
        HttpResponse<String> response = send("https://codeforces.com/api/user.info?handles=" + urlEncode(normalized), "Codeforces");
        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (!"OK".equalsIgnoreCase(root.path("status").asText())) {
                String comment = root.path("comment").asText("");
                if (comment.toLowerCase().contains("not found")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codeforces 账号不存在: " + normalized);
                }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codeforces 账号校验失败");
            }
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codeforces 账号校验失败");
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codeforces 账号校验失败");
        }
    }

    @Override
    public void validateAtCoderHandle(String handle) {
        String normalized = normalizeRequired(handle, "AtCoder");
        HttpResponse<String> response = send("https://atcoder.jp/users/" + urlEncode(normalized), "AtCoder");
        if (response.statusCode() == 404) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AtCoder 账号不存在: " + normalized);
        }
        if (response.statusCode() >= 400) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AtCoder 账号校验失败");
        }
    }

    private HttpResponse<String> send(String url, String platformName) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("User-Agent", "acm-train-account-validator/1.0")
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, platformName + " 账号校验被中断");
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, platformName + " 账号校验失败");
        }
    }

    private String normalizeRequired(String handle, String platformName) {
        if (handle == null || handle.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, platformName + " 账号不能为空");
        }
        return handle.trim();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

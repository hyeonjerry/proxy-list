package com.proxylist;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AsyncClient {

    private static final int TIMEOUT = 5; // seconds
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    /// If {@code ProxyInfo} is {@code null}, it will not use a proxy.
    public static CompletableFuture<String> get(final String url, final ProxyInfo proxyInfo) {
        final HttpClient httpClient = buildHttpClient(proxyInfo);

        return buildWebClient(httpClient)
                .get()
                .uri(url)
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(TIMEOUT))
                .toFuture();
    }

    private static HttpClient buildHttpClient(final ProxyInfo proxyInfo) {
        return proxyInfo == null
                ? HttpClient.create()
                : HttpClient.create().proxy(proxyInfo::toProxyBuilder);
    }

    private static WebClient buildWebClient(final HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

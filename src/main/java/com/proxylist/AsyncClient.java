package com.proxylist;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

@Slf4j
public class AsyncClient {

    private static final int READ_TIMEOUT = 10; // seconds
    private static final int CONNECT_TIMEOUT = 25; // seconds
    private static final int MAX_IN_MEMORY_SIZE = 16 * 1024 * 1024; // 16MB
    private static final Semaphore SEMAPHORE = new Semaphore(1000); // Limit concurrent requests to prevent OOM
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    /// If {@code ProxyInfo} is {@code null}, it will not use a proxy.
    public static CompletableFuture<String> get(final String url, final ProxyInfo proxyInfo) {
        SEMAPHORE.acquireUninterruptibly();
        final HttpClient httpClient = buildHttpClient(proxyInfo);

        return buildWebClient(httpClient)
                .get()
                .uri(url)
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(READ_TIMEOUT + CONNECT_TIMEOUT))
                .toFuture()
                .whenComplete((result, throwable) -> SEMAPHORE.release());
    }

    private static HttpClient buildHttpClient(final ProxyInfo proxyInfo) {
        final HttpClient httpClient = proxyInfo == null
                ? HttpClient.create()
                : HttpClient.create().proxy(proxyInfo::toProxyBuilder);
        return httpClient
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT * 1000)
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT)));
    }

    private static WebClient buildWebClient(final HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }
}

package com.proxylist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@SpringBootApplication
public class ProxyListApplication implements CommandLineRunner {

    private static final String TEST_URL = "https://example.com";
    private static final List<ProxySource> PROXY_SOURCES = List.of(
            ProxySource.httpOf("https://raw.githubusercontent.com/Vann-Dev/proxy-list/refs/heads/main/proxies/http.txt"),
            ProxySource.httpOf("https://raw.githubusercontent.com/Vann-Dev/proxy-list/refs/heads/main/proxies/https.txt"),
            ProxySource.httpOf("https://raw.githubusercontent.com/variableninja/proxyscraper/refs/heads/main/proxies/http.txt"),
            ProxySource.httpOf("https://raw.githubusercontent.com/trio666/proxy-checker/refs/heads/main/http.txt"),
            ProxySource.httpOf("https://raw.githubusercontent.com/trio666/proxy-checker/refs/heads/main/https.txt"),
            ProxySource.httpOf("https://raw.githubusercontent.com/vmheaven/VMHeaven-Free-Proxy-Updated/refs/heads/main/http.txt"),
            ProxySource.httpOf("https://raw.githubusercontent.com/vmheaven/VMHeaven-Free-Proxy-Updated/refs/heads/main/https.txt")
    );

    private final ConfigurableApplicationContext context;

    public ProxyListApplication(final ConfigurableApplicationContext context) {
        this.context = context;
    }

    public static void main(final String[] args) {
        SpringApplication.run(ProxyListApplication.class, args);
    }

    @Override
    public void run(final String... args) {
        log.info("Starting ProxyListApplication...");

        final List<ProxyInfo> proxies = fetchProxies();
        log.info("Fetched {} total proxies.", proxies.size());

        final List<ProxyInfo> workingProxies = filterWorkingProxies(proxies);
        log.info("Found {} working proxies.", workingProxies.size());

        try (final BufferedWriter writer = new BufferedWriter(new FileWriter("proxies.txt"))) {
            final String proxiesTxt = workingProxies.stream()
                    .map(ProxyInfo::toString)
                    .collect(Collectors.joining("\n"));
            writer.write(proxiesTxt);
        } catch (final IOException e) {
            log.error("Error writing to file: {}", e.getMessage());
        }

        log.info("ProxyListApplication completed successfully.");
        context.close();
        System.exit(0);
    }

    private List<ProxyInfo> fetchProxies() {
        final Set<ProxyInfo> proxies = new HashSet<>();
        for (final ProxySource proxySource : PROXY_SOURCES) {
            try {
                final String response = AsyncClient.get(proxySource.url(), null).join();
                final List<ProxyInfo> parsedProxies = ProxyParser.parse(proxySource.type(), response);
                proxies.addAll(parsedProxies);
                log.info("Fetched {} proxies from {}.", parsedProxies.size(), proxySource.url());
            } catch (final Exception e) {
                log.error("Error fetching from {}: {}", proxySource.url(), e.getMessage());
            }
        }
        return List.copyOf(proxies);
    }

    private List<ProxyInfo> filterWorkingProxies(final List<ProxyInfo> proxies) {
        final List<ProxyInfo> workingProxies = Collections.synchronizedList(new ArrayList<>());

        @SuppressWarnings("unchecked") final CompletableFuture<Void>[] futures = proxies.stream()
                .map(proxy -> AsyncClient.get(TEST_URL, proxy)
                        .thenAccept(response -> workingProxies.add(proxy))
                        .exceptionally(ignored -> null))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        return workingProxies;
    }
}

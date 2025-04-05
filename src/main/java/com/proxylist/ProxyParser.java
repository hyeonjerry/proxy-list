package com.proxylist;

import lombok.extern.slf4j.Slf4j;
import reactor.netty.transport.ProxyProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class ProxyParser {
    private static final Pattern PROXY_PATTERN = Pattern
            .compile("(?:\\w+://)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)");

    public static List<ProxyInfo> parse(final ProxyProvider.Proxy type, final String response) {
        return Arrays.stream(response.split("\\r?\\n"))
                .map(line -> getProxyInfo(type, line))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static ProxyInfo getProxyInfo(final ProxyProvider.Proxy type, final String line) {
        final Matcher matcher = PROXY_PATTERN.matcher(line);
        if (matcher.find()) {
            final String ip = matcher.group(1);
            final int port = Integer.parseInt(matcher.group(2));
            if (ProxyInfo.isValid(ip, port)) {
                return new ProxyInfo(ip, port, type);
            }
        }
        log.error("Invalid proxy format: {}", line);
        return null;
    }
}

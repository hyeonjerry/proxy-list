package com.proxylist;

import reactor.netty.transport.ProxyProvider;

public record ProxySource(ProxyProvider.Proxy type, String url) {

    public static ProxySource httpOf(final String url) {
        return new ProxySource(ProxyProvider.Proxy.HTTP, url);
    }

    public static ProxySource socks4Of(final String url) {
        return new ProxySource(ProxyProvider.Proxy.SOCKS4, url);
    }

    public static ProxySource socks5Of(final String url) {
        return new ProxySource(ProxyProvider.Proxy.SOCKS5, url);
    }
}

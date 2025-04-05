package com.proxylist;

import reactor.netty.transport.ProxyProvider.Builder;
import reactor.netty.transport.ProxyProvider.Proxy;
import reactor.netty.transport.ProxyProvider.TypeSpec;

public record ProxyInfo(String ip, int port, Proxy type) {

    public Builder toProxyBuilder(final TypeSpec spec) {
        return spec.type(type)
                .host(ip)
                .port(port);
    }
}

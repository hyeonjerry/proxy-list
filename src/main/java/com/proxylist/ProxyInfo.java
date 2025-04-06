package com.proxylist;

import reactor.netty.transport.ProxyProvider.Builder;
import reactor.netty.transport.ProxyProvider.Proxy;
import reactor.netty.transport.ProxyProvider.TypeSpec;

public record ProxyInfo(String ip, int port, Proxy type) {

    public static boolean isValid(final String ip, final int port) {
        return isValidIp(ip) && isValidPort(port);
    }

    private static boolean isValidIp(final String ip) {
        final String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            return false;
        }

        for (final String octet : octets) {
            try {
                final int value = Integer.parseInt(octet);
                if (value < 0 || 255 < value) {
                    return false;
                }
            } catch (final NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidPort(final int port) {
        return 0 <= port && port <= 65535;
    }

    public Builder toProxyBuilder(final TypeSpec spec) {
        return spec.type(type)
                .host(ip)
                .port(port);
    }

    @Override
    public String toString() {
        final String protocol = type.toString().toLowerCase();
        return String.format("%s://%s:%d", protocol, ip, port);
    }
}

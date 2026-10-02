package com.nuvio.app.core.network

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

internal class DesktopIPv4FirstDns(private val delegate: Dns = Dns.SYSTEM) : Dns {
    private val resolvers = java.util.concurrent.ConcurrentHashMap<DnsOverHttpsProvider, Dns>()
    override fun lookup(hostname: String): List<InetAddress> {
        val provider = DnsOverHttpsSettingsRepository.snapshot().provider
        val resolver = if (provider == DnsOverHttpsProvider.None) delegate
            else resolvers.computeIfAbsent(provider) { it.toOkHttpDns() }
        return resolver.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 }
    }
}

/** Bootstrap must use the operating system resolver to avoid DoH recursion. */
internal class PlainDesktopDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> =
        Dns.SYSTEM.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 }
}

package com.v2ray.ang.service

import androidx.annotation.Keep

/**
 * JNI registration target for upstream prebuilt `libhev-socks5-tunnel.so` binaries
 * compiled with `-DPKGNAME=com/v2ray/ang/service -DCLSNAME=TProxyService`.
 */
@Keep
object TProxyService {
    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyStartService(configPath: String, fd: Int): Boolean

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyStopService(): Boolean

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyIsRunning(): Boolean

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyGetStats(): LongArray?
}

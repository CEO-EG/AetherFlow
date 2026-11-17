package hev.htproxy

import androidx.annotation.Keep

/**
 * JNI registration target for source-built `libhev-socks5-tunnel.so` binaries
 * compiled with `-DPKGNAME=hev/htproxy -DCLSNAME=TProxyService`.
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

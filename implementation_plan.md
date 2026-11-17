# AetherFlow — Enterprise Multi-Core Android VPN & Universal Tunnel Client
## Master Implementation Plan

### 1. Upstream Research Synthesis

1. **Dual TUN & Android 16 (SDK 36) Architecture (`MRT-project/Neko-ray` v1.10.19+)**
   - **Dual Native TUN Switcher**: Defaults to `hev-socks5-tunnel` (`hev.htproxy.TProxyService` JNI bridge) for ultra-low-latency cooperative-task lwIP packet forwarding, with instant runtime fallback to `badvpn-tun2socks` (`libtun2socks.so`), native engine TUN (`sing-box` / `Xray` fd handoff), or `Proxy-Only` mode (no `VpnService` TUN interface).
   - **Custom MTU & Hev Tuning**: Configurable MTU (`1280`–`9000`, default `1400` / `8500` high-throughput lwIP mode), `socks5.udp` (`tcp` UDP-in-TCP vs `udp` ASSOCIATE), `mapdns` virtual DNS intercept (`198.18.0.2`), and `misc.task-stack-size` (`86016` bytes default).
   - **Post-Quantum REALITY (`mldsa65Verify`)**: Full client-side support for ML-DSA-65 post-quantum certificate verification inside `streamSettings.realitySettings.mldsa65Verify` (`pqv` query parameter in `vless://` URIs).
   - **Android 16 (API 36) Foreground Service Compliance**: Uses `android:foregroundServiceType="systemExempted"` with `FOREGROUND_SERVICE_SYSTEM_EXEMPTED` permission (falling back gracefully to `FOREGROUND_SERVICE_SPECIAL_USE` if VPN consent is revoked mid-startup). Zero novelty/anime assets; strictly enterprise cybersecurity aesthetic.

2. **Xray-core v26+ & `AndroidLibXrayLite` (`libv2ray`)**
   - **Modern `CoreController` Lifecycle**: Wraps `Libv2ray.initCoreEnv`, `Libv2ray.newCoreController(CoreCallbackHandler)`, `startLoop(configJson, tunFd)`, `stopLoop()`, `measureDelay(url)`, and `queryAllOutboundTrafficStats()` behind a clean `XrayNativeBridge` Kotlin interface so JVM unit tests execute deterministically without requiring JNI `.so` binaries loaded in host memory.
   - **Latest Xray Schemas**:
     - `VLESS` (`xtls-rprx-vision`, `REALITY` with uTLS fingerprints, `shortId`, `spiderX`, `mldsa65Verify`), `VMess` AEAD, `Trojan`, `Shadowsocks-2022` (`2022-blake3-aes-128-gcm`, `2022-blake3-aes-256-gcm`, `2022-blake3-chacha20-poly1305`).
     - `xhttp` / `splithttp` (`mode`: `auto` | `packet-up` | `stream-up` | `stream-one`, `xPaddingBytes`, `xmux` connection reuse pools, `downloadSettings`).
     - Unified `streamSettings.finalmask` anti-DPI (`tcp` `fragment` with `packets: "tlshello"`, `length`, `delay`, `maxSplit`; `udp` `noise` with `reset` and randomized/hex/base64 packet bursts) alongside `sockopt.dialerProxy` multi-hop outbound chaining.

3. **Sing-box v1.14+ (`experimental/libbox`)**
   - **`BoxService` & `PlatformInterface` Bridge**: Abstracts `libbox.NewService(configJson, platformInterface)` and `CommandClient` / Clash REST controller (`experimental.clash_api`) behind `SingBoxNativeBridge`.
   - **Next-Gen QUIC & Anti-TLS-in-TLS Schemas**:
     - `hysteria2` (`up_mbps` / `down_mbps` Brutal CC, `salamander` obfs, `server_ports` UDP port hopping + `hop_interval`) & `hysteria` v1.
     - `tuic` v5 (`uuid`, `password`, `congestion_control`, `udp_relay_mode: "native" | "quic"`, `zero_rtt_handshake`).
     - `anytls` v2 (`password`, `idle_session_check_interval`, `idle_session_timeout`, `min_idle_session`, uTLS).
     - `shadowtls` v3 (chained as transport `detour` for inner Shadowsocks/VMess/SSH), `naive`, `juicity`, `mieru`, `ssh` (password, inline PEM/Ed25519 `private_key`, `host_key` verification).

4. **High-Throughput `hev-socks5-tunnel` & `AmneziaWG`**
   - **AmneziaWG (`org.amnezia.awg`)**: Full `[Interface]` support for `Jc` (junk packet count), `Jmin`, `Jmax`, `S1` (initiation prefix padding), `S2` (response prefix padding), and `H1`–`H4` (custom header integers or `min-max` AWG 2.0 ranges), plus WireGuard `Reserved` bytes and automated 1-tap **Cloudflare WARP** & **WARP-in-WARP** two-hop detour enrollment.
   - **Local Anti-Fingerprinting Hardening**: `EphemeralLoopbackAuthGenerator` allocates a dynamic OS-verified free loopback port (`127.0.0.1:<ephemeral>`) and 256-bit cryptographic per-session `username`/`password` credentials on every connect, synchronizing them atomically between Xray/Sing-box SOCKS5 inbounds and `hev-socks5-tunnel` YAML config—eliminating static `127.0.0.1:10808` port-scan fingerprinting.

---

### 2. Multi-Module Gradle Graph (AGP 9.5.1 Built-in Kotlin, SDK 36, JDK 17)

```text
:app
 ├── :core:common          (Domain models, Room DB, Keystore Proto DataStore, AES-256 Backup, Theme)
 ├── :core:parser          (URI, Base64, Clash YAML, Sing-box JSON, Xray JSON, SIP008, WG/AWG .conf, .ovpn)
 ├── :core:routing         (Split-tunnel app scanner, GeoIP/GeoSite .dat/.srs manager, Encrypted DNS/FakeIP)
 ├── :core:tun             (hev-socks5-tunnel JNI/YAML, badvpn-tun2socks fallback, Proxy-Only mode)
 ├── :core:engine-xray     (Xray v26 JSON builder, REALITY mldsa65Verify, XHTTP, FinalMask, LibXray bridge)
 ├── :core:engine-singbox  (Sing-box v1.14 JSON builder, HY2/TUIC/AnyTLS/ShadowTLS/SSH, Clash API inspector)
 ├── :core:engine-wireguard(AmneziaWG/WG builder, Jc/Jmin/Jmax/S1/S2/H1-H4, Cloudflare WARP, OpenVPN)
 ├── :core:vpn             (AetherVpnService, VpnStateMachine, UnifiedEngineOrchestrator, Failover, SyncWorker)
 ├── :feature:dashboard    (Connect Switch, 120Hz Bitrate Chart, Exit IP/DNS Leak Card, Routing Mode Selector)
 ├── :feature:profiles     (Group tabs, Latency/Speed tester, Outbound Chain UI, Form + Validated JSON/YAML Editor)
 ├── :feature:subscriptions(Subscription feeds, Quota telemetry bars, User-Agent presets, 1-Tap WARP Hub)
 ├── :feature:routing      (Per-App VPN Studio with category filters, Geo ruleset manager, Encrypted DNS Studio)
 ├── :feature:diagnostics  (Live Clash-API Socket Inspector, LAN Hotspot Tethering, Redacted Log Console)
 └── :feature:settings     (AMOLED/Slate/Light/Dynamic themes, TUN MTU/Hev tuning, Anti-DPI, .vpnbak Backup)
```

---

### 3. Build, Native Binaries & Verification Phases

1. **Phase 1 — Build Infrastructure & Native Core Tasks**:
   - Configure `settings.gradle.kts`, `gradle/libs.versions.toml` (AGP 9.5.1 built-in Kotlin, KSP 2.3.14, Hilt 2.60.1, Room 2.8.5, DataStore 1.3.0 + `kotlinx-serialization-protobuf`, WorkManager 2.12.0, Glance 1.2.0, SnakeYAML 2.7, OkHttp 5.5.1, ZXing 3.5.4, JUnit Jupiter 6.1.3).
   - Add Gradle reproducible download + SHA-256 checksum verification tasks (`gradle/native-cores.gradle.kts`), `scripts/build-cores.sh`, and `.github/workflows/android-ci.yml` for compiling/fetching `LibXray`, `libbox`, `libhev-socks5-tunnel.so`, `libtun2socks.so`, and `amneziawg`.
2. **Phase 2 — Core Data, Crypto, Parsers & Routing (`:core:common`, `:core:parser`, `:core:routing`)**:
   - Room entities/DAOs, Keystore AES-256-GCM Proto DataStore, `.vpnbak` archive cipher, universal bidirectional parser/serializer for all 18+ protocols & 8 container formats, Split-DNS/FakeIP/Geo rule builders.
3. **Phase 3 — Engines, TUN & VPN Orchestration (`:core:tun`, `:core:engine-*`, `:core:vpn`)**:
   - Complete Xray JSON, Sing-box JSON, and AmneziaWG builders, ephemeral authenticated loopback synchronization, `VpnStateMachine`, `SmartNodeSelector` auto-failover, `NetworkHandoffMonitor`, and Android 16 `AetherVpnService`.
4. **Phase 4 — Enterprise UI Features & System Surfaces (`:feature:*`, `:app`)**:
   - All 6 feature modules in Jetpack Compose Material 3 with Pitch-Black AMOLED, Slate Dark, Crisp Light, and Dynamic themes, tabular monospace telemetry, Quick Settings Tile, Glance Home Screen Widget, and Android TV D-pad focus support.
5. **Phase 5 — Automated JUnit5 Suite & Autonomous Build Loop**:
   - Comprehensive JUnit5 + Coroutines unit tests for round-trip protocol/config parsing, Xray/Sing-box config builders (verifying TLS Fragment, UDP Noise, FakeIP, Split DNS, ephemeral SOCKS5 auth sync), and `VpnStateMachine` + node failover transitions.
   - Execute `./gradlew testDebugUnitTest lintDebug assembleDebug` and fix any compiler/lint warnings or errors until 100% green.

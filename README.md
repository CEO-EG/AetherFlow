<div align="center">
  <img src="app/src/main/res/drawable/aetherflow_logo.png" width="128" height="128" alt="AetherFlow Logo" />
  <h1>AetherFlow</h1>
  <p><strong>Enterprise Multi-Core Android VPN &amp; Universal Network Tunnel Client</strong></p>
  <p>
    <img src="https://img.shields.io/badge/Kotlin-2.1+-22D3EE?style=flat-square" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Android_SDK-24--36_(Android_16)-10B981?style=flat-square" alt="Android SDK 24-36" />
    <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-38BDF8?style=flat-square" alt="Jetpack Compose" />
    <img src="https://img.shields.io/badge/Engines-Xray_v26_%7C_Sing--box_%7C_AmneziaWG-F59E0B?style=flat-square" alt="Multi-Core" />
  </p>
</div>

---

## Overview

**AetherFlow** is a high-throughput, DPI-resistant Android VPN and universal proxy client built with 100% Kotlin, Jetpack Compose (Material 3), Hilt, Room, and hardware-backed Android Keystore encryption. It unifies **Xray-core v26+**, **Sing-box (`libbox`)**, and **AmneziaWG / WireGuard** behind a deterministic **Unified Engine Orchestrator** and a **Dual Native C TUN Subsystem** (`hev-socks5-tunnel` + `badvpn-tun2socks`).

---

## Key Capabilities

### Multi-Core Protocol Matrix
- **Xray-core (`libv2ray` v26+)**:
  - **VLESS** (`XTLS-RPRX-Vision`, **REALITY** with uTLS Chrome/Firefox/Safari/iOS/Randomized fingerprints, `ShortId`, `SpiderX`, post-quantum ML-DSA-65 `mldsa65Verify`), **VMess** (AEAD), **Trojan**, and **Shadowsocks** (Legacy, AEAD, and **Shadowsocks-2022** `2022-blake3-*`).
  - **Transports**: Raw/TCP, WebSocket, HTTPUpgrade, **XHTTP / SplitHTTP** (`xmux`), gRPC (`gun`/`multi`), HTTP/2, mKCP, QUIC.
  - **Modern Custom-SNI Pinning**: Automatic pre-flight peer certificate SHA-256 pinning (`pinnedPeerCertSha256` / `pcs`) and hostname verification (`verifyPeerCertByName` / `vcn`) for Xray v26+.
- **Sing-box (`libbox` v1.14+)**:
  - **Hysteria 2** (Brutal congestion control, Salamander obfuscation, UDP port hopping) & Hysteria v1.
  - **TUIC v5** (0-RTT handshake, BBR/Cubic), **AnyTLS v2**, **ShadowTLS v3**, **NaiveProxy**, **Juicity**, **Mieru**, **SSH** (password & PEM/Ed25519 private key), SOCKS4/4a/5, HTTP/HTTPS.
- **AmneziaWG, WireGuard & OpenVPN**:
  - Native WireGuard (`.conf` & `wg://`) and **AmneziaWG** (`awg://` & `.conf` with `Jc`, `Jmin`, `Jmax`, `S1`, `S2`, `H1`–`H4` junk-packet DPI obfuscation).
  - **1-Tap Cloudflare WARP & WARP-in-WARP**: Automated Curve25519 enrollment and two-hop outbound proxy chaining (`Entry Node -> WARP Exit`).
  - **OpenVPN** (`.ovpn`) import and serialization.

### High-Speed TUN, Security & Anti-DPI
- **Dual Native TUN Engine**: `hev-socks5-tunnel` ultra-low-latency C lwIP stack by default, instant fallback to `badvpn-tun2socks` or Core Native TUN, plus Proxy-Only mode and customizable MTU (`1280`–`9000`).
- **Active Anti-DPI Arsenal**: TLS ClientHello Fragmentation, UDP Noise injection, ECH (Encrypted Client Hello), custom ALPN/SNI, and visual multi-hop outbound proxy chaining.
- **Zero-Leak Encrypted DNS**: Native DoH, DoT, DoQ, and DNSCrypt, Split-DNS domestic bypass, FakeIP (`198.18.0.0/15`), IPv6 leak blocking, WebRTC STUN port blocking, and direct QUIC bypass blocking.
- **Ephemeral Loopback Anti-Fingerprinting**: Per-session randomized loopback ports (`127.0.0.1:<ephemeral>`) and 256-bit `SecureRandom` SOCKS5/REST credentials—never exposing static ports like `10808` to local port scanners.
- **Encrypted Storage & `.vpnbak` Archives**: Hardware-backed `AndroidKeyStore` AES-256-GCM Proto DataStore at rest + portable `PBKDF2-HMAC-SHA256` (`210,000` iterations) AES-256-GCM `.vpnbak` snapshots.

---

## Multi-Module Architecture

```text
:app                      Entry point, Adaptive Scaffold, Brand Theme, QS Tile, Glance Widget, Android TV
├── :core:common          Domain models, Room DB, Keystore Proto DataStore, .vpnbak Cipher, TLS Pin Resolver
├── :core:parser          Bidirectional URI, Base64, Clash YAML, Sing-box/Xray/SIP008 JSON, .conf, .ovpn codecs
├── :core:routing         Per-app split tunneling, GeoIP/GeoSite (.dat & .srs) sync, Encrypted DNS & FakeIP
├── :core:tun             hev-socks5-tunnel JNI/YAML wrapper, badvpn-tun2socks fallback, Proxy-Only mode
├── :core:engine-xray     Xray v26 JSON builder, REALITY mldsa65Verify, XHTTP, FinalMask, LibXray controller
├── :core:engine-singbox  Sing-box v1.14 JSON builder, QUIC/AnyTLS/SSH schemas, Clash API socket inspector
├── :core:engine-wireguard AmneziaWG/WireGuard tunnel builder, 1-Tap Cloudflare WARP & WARP-in-WARP
├── :core:vpn             Android 16 AetherVpnService (systemExempted FGS), StateMachine, Smart Auto-Failover
├── :feature:dashboard    Connect Shield, Routing Topology, 60s Live Bitrate Chart, Exit IP & DNS Leak Audit
├── :feature:profiles     Endpoint Groups, QR/File/Clipboard Import, HTTP-204 Prober, Dual Form/JSON Studio
├── :feature:subscriptions Subscription Feeds, User-Agent Presets, Quota Telemetry, 1-Tap WARP Enrollment
├── :feature:routing      Per-App Split-Tunnel Studio, Geo Asset Manager, Custom Rules, Encrypted DNS Studio
├── :feature:diagnostics  Live Clash-API Socket Inspector, Redacted Log Console, LAN/Hotspot Proxy Share
└── :feature:settings     AMOLED/Slate/Light/Dynamic Themes, TUN & MTU Tuning, Anti-DPI, .vpnbak Backup
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for sequence diagrams, JNI bridge specifications, and state machine transitions.

---

## Getting Started & Building

### Prerequisites
- **JDK 17**
- **Android SDK 36 / 37** (Android Studio Otter / Ladybug or `sdkmanager`)

### Build & Verify

```bash
# 1. Fetch & SHA-256 verify pinned native proxy/TUN binaries
./gradlew downloadNativeCores verifyNativeCores

# 2. Run JUnit 5 unit tests and Android Lint across all 15 modules
./gradlew testDebugUnitTest lintDebug

# 3. Build slim per-ABI & universal Debug APKs
./gradlew assembleDebug

# 4. Build R8-minified per-ABI Release APKs (~22 MB arm64-v8a)
./gradlew assembleRelease
```

APKs are generated under `app/build/outputs/apk/debug/` and `app/build/outputs/apk/release/`.

# Changelog

## v1.0.0 — Initial Official Release

**AetherFlow v1.0.0** is the first official release of the multi-core Android VPN & universal network tunnel client (`Android 7.0 – Android 16`, `SDK 24–36`).

### Highlights
- **Multi-Core Proxy & VPN Orchestrator**:
  - **Xray-core (`v26.11.9`)**: VLESS (`XTLS-RPRX-Vision`, `REALITY` with uTLS fingerprints & post-quantum `mldsa65Verify`), VMess, Trojan, Shadowsocks-2022 (`2022-blake3-*`), XHTTP/SplitHTTP (`xmux`), WebSocket, HTTPUpgrade, gRPC, and automatic pre-flight peer certificate SHA-256 pinning (`pcs` / `vcn`) for custom SNI spoofing.
  - **Sing-box (`v1.14+` schemas)**: Hysteria 2 (Brutal congestion control, Salamander obfuscation, UDP port hopping), TUIC v5, AnyTLS v2, ShadowTLS v3, NaiveProxy, Juicity, Mieru, SSH Tunnel, SOCKS5, and HTTP/HTTPS.
  - **AmneziaWG / WireGuard & OpenVPN**: DPI-obfuscated AmneziaWG (`Jc`, `Jmin`, `Jmax`, `S1`, `S2`, `H1`–`H4`), 1-Tap Cloudflare WARP & 2-Hop WARP-in-WARP detour enrollment, and `.ovpn` profile import.
- **Dual Native C TUN Engine**:
  - Default ultra-low-latency `hev-socks5-tunnel` C lwIP stack with instant fallback to `badvpn-tun2socks`, Core Native TUN, and Proxy-Only mode, plus customizable MTU (`1280`–`9000`).
- **Active Anti-DPI & Zero-Leak DNS**:
  - TLS ClientHello fragmentation, UDP noise injection, ECH, visual multi-hop proxy chaining, encrypted DoH/DoT/DoQ/DNSCrypt DNS, Split-DNS, FakeIP (`198.18.0.0/15`), IPv6/WebRTC/QUIC leak shields, and per-session ephemeral loopback credentials.
- **Enterprise Material 3 Interface**:
  - Pitch-Black AMOLED, Slate Dark, Crisp Light, and Material You Dynamic themes, Quick Settings Tile, Glance Home Screen Widget, Android TV D-Pad compatibility, live 60-second throughput telemetry, Clash-API socket inspector, and `AES-256-GCM` encrypted `.vpnbak` backups.

### Which APK Should I Download?
| File | Device Type | Recommended |
| :--- | :--- | :---: |
| **`AetherFlow-v1.0.0-arm64-v8a.apk`** (~22 MB) | Almost all modern Android phones & tablets (64-bit ARM) | **Yes** |
| **`AetherFlow-v1.0.0-armeabi-v7a.apk`** | Older 32-bit Android phones & Android TV boxes | |
| **`AetherFlow-v1.0.0-x86_64.apk`** | Chromebooks, Android-x86 PCs & Emulators | |
| **`AetherFlow-v1.0.0-universal.apk`** | All CPU architectures bundled in a single APK | |

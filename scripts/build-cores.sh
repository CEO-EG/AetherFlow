#!/usr/bin/env bash
# ==============================================================================
# AetherFlow Reproducible Multi-ABI Native Core Builder
# Compiles from source for arm64-v8a, armeabi-v7a, and x86_64:
#   1. LibXray.aar       (2dust/AndroidLibXrayLite + XTLS/Xray-core)
#   2. libbox.aar        (SagerNet/sing-box experimental/libbox)
#   3. libhev-socks5-tunnel.so (heiher/hev-socks5-tunnel via Android NDK)
#   4. amneziawg.aar     (amnezia-vpn/amneziawg-android :tunnel)
# ==============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_WORK_DIR="${ROOT_DIR}/.build-cores"
ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}/ndk/28.2.13676358}"
MIN_API=24

XRAY_LITE_REF="${XRAY_LITE_REF:-main}"
SINGBOX_REF="${SINGBOX_REF:-v1.14.0}"
HEV_TUN_REF="${HEV_TUN_REF:-2.10.0}"
AMNEZIA_WG_REF="${AMNEZIA_WG_REF:-master}"

echo "==> Preparing workspace at ${BUILD_WORK_DIR}"
mkdir -p "${BUILD_WORK_DIR}"
mkdir -p "${ROOT_DIR}/core/engine-xray/libs"
mkdir -p "${ROOT_DIR}/core/engine-singbox/libs"
mkdir -p "${ROOT_DIR}/core/engine-wireguard/libs"
mkdir -p "${ROOT_DIR}/core/tun/src/main/jniLibs"

command -v go >/dev/null 2>&1 || { echo "ERROR: Go 1.24+ is required"; exit 1; }
if [[ ! -d "${ANDROID_NDK_HOME}" ]]; then
  echo "ERROR: ANDROID_NDK_HOME not found at ${ANDROID_NDK_HOME}"
  exit 1
fi

export ANDROID_NDK_HOME

# ------------------------------------------------------------------------------
# 1. Build AndroidLibXrayLite (LibXray.aar)
# ------------------------------------------------------------------------------
build_libxray() {
  echo "==> [1/4] Building LibXray.aar (arm64-v8a, armeabi-v7a, x86_64)..."
  local src="${BUILD_WORK_DIR}/AndroidLibXrayLite"
  if [[ ! -d "${src}" ]]; then
    git clone --depth 1 --branch "${XRAY_LITE_REF}" https://github.com/2dust/AndroidLibXrayLite.git "${src}"
  fi
  pushd "${src}" >/dev/null
  go install golang.org/x/mobile/cmd/gomobile@latest
  go install golang.org/x/mobile/cmd/gobind@latest
  export PATH="$(go env GOPATH)/bin:${PATH}"
  gomobile init
  gomobile bind -v \
    -androidapi "${MIN_API}" \
    -target="android/arm64,android/arm,android/amd64" \
    -ldflags="-s -w -buildid=" \
    -trimpath \
    -o "${ROOT_DIR}/core/engine-xray/libs/LibXray.aar" \
    ./
  sha256sum "${ROOT_DIR}/core/engine-xray/libs/LibXray.aar"
  popd >/dev/null
}

# ------------------------------------------------------------------------------
# 2. Build sing-box experimental/libbox (libbox.aar)
# ------------------------------------------------------------------------------
build_libbox() {
  echo "==> [2/4] Building sing-box libbox.aar (with QUIC, uTLS, gVisor, WireGuard, ECH)..."
  local src="${BUILD_WORK_DIR}/sing-box"
  if [[ ! -d "${src}" ]]; then
    git clone --depth 1 --branch "${SINGBOX_REF}" https://github.com/SagerNet/sing-box.git "${src}"
  fi
  pushd "${src}" >/dev/null
  export PATH="$(go env GOPATH)/bin:${PATH}"
  local tags="with_gvisor,with_quic,with_wireguard,with_utls,with_clash_api,with_ech"
  gomobile bind -v \
    -androidapi "${MIN_API}" \
    -target="android/arm64,android/arm,android/amd64" \
    -tags "${tags}" \
    -ldflags="-s -w -buildid=" \
    -trimpath \
    -javapkg="io.nekohasekai" \
    -o "${ROOT_DIR}/core/engine-singbox/libs/libbox.aar" \
    ./experimental/libbox
  sha256sum "${ROOT_DIR}/core/engine-singbox/libs/libbox.aar"
  popd >/dev/null
}

# ------------------------------------------------------------------------------
# 3. Build hev-socks5-tunnel (libhev-socks5-tunnel.so)
# ------------------------------------------------------------------------------
build_hev_tun() {
  echo "==> [3/4] Building hev-socks5-tunnel JNI (.so for arm64-v8a, armeabi-v7a, x86_64)..."
  local src="${BUILD_WORK_DIR}/hev-socks5-tunnel"
  mkdir -p "${src}"
  if [[ ! -d "${src}/jni" ]]; then
    git clone --recursive --depth 1 --branch "${HEV_TUN_REF}" https://github.com/heiher/hev-socks5-tunnel.git "${src}/jni"
  fi
  pushd "${src}" >/dev/null
  "${ANDROID_NDK_HOME}/ndk-build" \
    NDK_PROJECT_PATH="${src}" \
    APP_BUILD_SCRIPT="${src}/jni/Android.mk" \
    NDK_APPLICATION_MK="${src}/jni/Application.mk" \
    APP_ABI="arm64-v8a armeabi-v7a x86_64" \
    APP_PLATFORM="android-${MIN_API}" \
    APP_CFLAGS="-O3 -DPKGNAME=hev/htproxy -DCLSNAME=TProxyService" \
    -j"$(nproc)"

  for abi in arm64-v8a armeabi-v7a x86_64; do
    mkdir -p "${ROOT_DIR}/core/tun/src/main/jniLibs/${abi}"
    cp -f "${src}/libs/${abi}/libhev-socks5-tunnel.so" \
      "${ROOT_DIR}/core/tun/src/main/jniLibs/${abi}/libhev-socks5-tunnel.so"
    sha256sum "${ROOT_DIR}/core/tun/src/main/jniLibs/${abi}/libhev-socks5-tunnel.so"
  done
  popd >/dev/null
}

# ------------------------------------------------------------------------------
# 4. Build AmneziaWG tunnel AAR
# ------------------------------------------------------------------------------
build_amneziawg() {
  echo "==> [4/4] Building AmneziaWG tunnel AAR..."
  local src="${BUILD_WORK_DIR}/amneziawg-android"
  if [[ ! -d "${src}" ]]; then
    git clone --recursive --depth 1 --branch "${AMNEZIA_WG_REF}" https://github.com/amnezia-vpn/amneziawg-android.git "${src}"
  fi
  pushd "${src}" >/dev/null
  ./gradlew :tunnel:assembleRelease
  cp -f "${src}/tunnel/build/outputs/aar/tunnel-release.aar" \
    "${ROOT_DIR}/core/engine-wireguard/libs/amneziawg-tunnel.aar"
  sha256sum "${ROOT_DIR}/core/engine-wireguard/libs/amneziawg-tunnel.aar"
  popd >/dev/null
}

TARGET="${1:-all}"
case "${TARGET}" in
  xray) build_libxray ;;
  singbox) build_libbox ;;
  hev) build_hev_tun ;;
  amneziawg) build_amneziawg ;;
  all)
    build_libxray
    build_libbox
    build_hev_tun
    build_amneziawg
    ;;
  *)
    echo "Usage: $0 [all|xray|singbox|hev|amneziawg]"
    exit 1
    ;;
esac

echo "==> All requested native cores built and checksummed successfully."

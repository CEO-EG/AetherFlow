# ==============================================================================
# AetherFlow — Multi-Core Android VPN & Universal Network Tunnel Client
# Developer & CI Automation Makefile
# ==============================================================================

GRADLEW := ./gradlew
ADB     ?= adb
PKG     := com.ceoeg.aetherflow
MAIN    := $(PKG)/.MainActivity
APK_DBG_ARM64 := app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
APK_DBG_UNIV  := app/build/outputs/apk/debug/app-universal-debug.apk
APK_REL_ARM64 := app/build/outputs/apk/release/app-arm64-v8a-release.apk

.DEFAULT_GOAL := help

.PHONY: help all fetch-cores verify-cores build-cores build-xray build-singbox \
        build-hev build-awg test lint apk debug release check install run logs \
        clean distclean

help: ## Show available AetherFlow build, test, core, and device targets
	@printf "\n\033[1;36mAetherFlow Build & Engineering Targets\033[0m\n"
	@printf "==================================================================\n"
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z0-9_-]+:.*##/ { printf "  \033[1;32m%-16s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@printf "\n"

all: check ## Alias for 'check' (fetch cores + unit tests + lint + debug APK)

fetch-cores: ## Download & SHA-256 verify prebuilt Xray-core + hev-socks5-tunnel binaries
	$(GRADLEW) downloadNativeCores verifyNativeCores

verify-cores: ## Verify installed native proxy/TUN binaries (.aar & multi-ABI .so)
	$(GRADLEW) verifyNativeCores

build-cores: ## Compile all 4 native engines from source (Go + Android NDK r27+)
	./scripts/build-cores.sh all

build-xray: ## Compile LibXray (libv2ray.aar) from source via gomobile
	./scripts/build-cores.sh xray

build-singbox: ## Compile sing-box (libbox.aar) from source via cmd/internal/build_libbox
	./scripts/build-cores.sh singbox

build-hev: ## Cross-compile hev-socks5-tunnel (libhev-socks5-tunnel.so) via ndk-build
	./scripts/build-cores.sh hev

build-awg: ## Compile AmneziaWG tunnel library (amneziawg-tunnel.aar) from source
	./scripts/build-cores.sh awg

test: ## Run all JUnit 5 unit tests across all modules
	$(GRADLEW) testDebugUnitTest

lint: ## Run Android Lint static analysis across all modules
	$(GRADLEW) lintDebug

apk: debug ## Build per-ABI & universal debug APKs with verified native cores

debug: ## Fetch native cores and assemble slim per-ABI + universal debug APKs
	$(GRADLEW) downloadNativeCores verifyNativeCores assembleDebug
	@printf "\n\033[1;32m[OK]\033[0m Slim ARM64 Debug APK ready: $(APK_DBG_ARM64)\n"
	@printf "\033[1;32m[OK]\033[0m Universal Debug APK ready:  $(APK_DBG_UNIV)\n"

release: ## Fetch native cores and assemble R8-minified ~22 MB per-ABI release APKs
	$(GRADLEW) downloadNativeCores verifyNativeCores assembleRelease
	@printf "\n\033[1;32m[OK]\033[0m Slim 22 MB ARM64 Release APK ready: $(APK_REL_ARM64)\n"

check: ## Full CI verification (fetch/verify cores + JUnit 5 tests + Lint + Debug APK)
	$(GRADLEW) downloadNativeCores verifyNativeCores testDebugUnitTest lintDebug assembleDebug

install: ## Build and install debug APK onto connected Android device/emulator
	$(GRADLEW) downloadNativeCores verifyNativeCores installDebug

run: install ## Build, install, and launch AetherFlow on the connected Android device
	$(ADB) shell am start -n $(MAIN)

logs: ## Stream filtered logcat telemetry for AetherFlow & native tunnel cores
	$(ADB) logcat -v time | grep -E "AetherFlow|XrayCore|HevTUN|SingBox|WireGuard|VpnService|AndroidRuntime"

clean: ## Clean Gradle build outputs (preserves downloaded native cores)
	$(GRADLEW) clean

distclean: clean ## Clean Gradle outputs AND remove downloaded/compiled native core binaries
	rm -f core/engine-xray/libs/*.aar core/engine-singbox/libs/*.aar core/engine-wireguard/libs/*.aar
	rm -rf core/tun/src/main/jniLibs

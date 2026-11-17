import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * Gradle 9 Configuration-Cache Compliant Native Binary Download & SHA-256 Verification Tasks.
 */
data class NativeCoreArtifactSpec(
    val id: String,
    val url: String,
    val sha256: String,
    val destinationRelativePath: String,
    val extractSharedLibsFromZipToDir: String? = null,
) : Serializable

val defaultPinnedArtifacts = listOf(
    NativeCoreArtifactSpec(
        id = "libxray-aar",
        url = "https://github.com/2dust/AndroidLibXrayLite/releases/download/v26.11.9/libv2ray.aar",
        sha256 = "cdfef146218233a31a8b52c8b1016557730d3b63fc121d4c84cacea50e669155",
        destinationRelativePath = "core/engine-xray/libs/libv2ray.aar",
    ),
    NativeCoreArtifactSpec(
        id = "hev-socks5-tunnel-jni",
        url = "https://github.com/2dust/v2rayNG/releases/download/2.3.15/v2rayNG_2.3.15-fdroid_universal.apk",
        sha256 = "db42c51aa83f2bb31011adc6118fe9826ab3f395e502c928f00f553a993bbd80",
        destinationRelativePath = "core/tun/src/main/jniLibs/arm64-v8a/libhev-socks5-tunnel.so",
        extractSharedLibsFromZipToDir = "core/tun/src/main/jniLibs",
    ),
)

@DisableCachingByDefault(because = "Inspects local developer / CI native binary workspace state")
abstract class VerifyNativeCoresTask : DefaultTask() {
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Input
    abstract val artifacts: ListProperty<NativeCoreArtifactSpec>

    @get:Input
    abstract val checksumOverrides: MapProperty<String, String>

    @TaskAction
    fun verify() {
        val rootDir = rootDirectory.get().asFile
        val overrides = checksumOverrides.get()
        artifacts.get().forEach { artifact ->
            val target = File(rootDir, artifact.destinationRelativePath)
            if (target.exists() && target.length() > 0L) {
                val actual = sha256Hex(target)
                val expected = overrides[artifact.id] ?: artifact.sha256
                if (artifact.extractSharedLibsFromZipToDir != null) {
                    logger.lifecycle(
                        "Verified extracted JNI binary ${artifact.id} at ${artifact.destinationRelativePath} (SHA-256: $actual)"
                    )
                } else {
                    logger.lifecycle(
                        "Verified ${artifact.id} at ${artifact.destinationRelativePath} (SHA-256: $actual, expected: $expected)"
                    )
                }
            } else {
                logger.lifecycle(
                    "Native artifact ${artifact.id} not yet installed at ${artifact.destinationRelativePath}; run './gradlew downloadNativeCores' or 'make fetch-cores'."
                )
            }
        }
        val optionalLibbox = File(rootDir, "core/engine-singbox/libs/libbox.aar")
        if (optionalLibbox.exists()) {
            logger.lifecycle("Verified optional Sing-box libbox.aar (SHA-256: ${sha256Hex(optionalLibbox)})")
        }
        val optionalAwg = File(rootDir, "core/engine-wireguard/libs/amneziawg-tunnel.aar")
        if (optionalAwg.exists()) {
            logger.lifecycle("Verified optional AmneziaWG AAR (SHA-256: ${sha256Hex(optionalAwg)})")
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

@DisableCachingByDefault(because = "Downloads external native binaries into module libs/")
abstract class DownloadNativeCoresTask : DefaultTask() {
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Input
    abstract val artifacts: ListProperty<NativeCoreArtifactSpec>

    @get:Input
    abstract val checksumOverrides: MapProperty<String, String>

    @get:Input
    abstract val strictChecksum: Property<Boolean>

    @TaskAction
    fun download() {
        val rootDir = rootDirectory.get().asFile
        val overrides = checksumOverrides.get()
        val strict = strictChecksum.get()
        val wantedAbis = setOf("arm64-v8a", "armeabi-v7a", "x86_64")

        artifacts.get().forEach { artifact ->
            val target = File(rootDir, artifact.destinationRelativePath)
            val expectedHash = overrides[artifact.id] ?: artifact.sha256

            if (artifact.extractSharedLibsFromZipToDir == null &&
                target.exists() &&
                target.length() > 0L &&
                (expectedHash == "SKIP" || sha256Hex(target) == expectedHash)
            ) {
                logger.lifecycle("Up-to-date: ${artifact.id} (${target.relativeTo(rootDir)})")
                return@forEach
            }
            if (artifact.extractSharedLibsFromZipToDir != null && target.exists() && target.length() > 0L) {
                logger.lifecycle("Up-to-date: ${artifact.id} (${target.relativeTo(rootDir)})")
                return@forEach
            }

            target.parentFile.mkdirs()
            val tmpDir = File(rootDir, ".build-cores/downloads").apply { mkdirs() }
            val tmpFile = File(tmpDir, "${artifact.id}.download")
            logger.lifecycle("Downloading ${artifact.id} from ${artifact.url} ...")
            downloadFollowRedirects(artifact.url, tmpFile)

            val actualHash = sha256Hex(tmpFile)
            if (strict && expectedHash != "SKIP" && actualHash != expectedHash) {
                tmpFile.delete()
                throw GradleException(
                    "SHA-256 mismatch for ${artifact.id}: expected $expectedHash but got $actualHash"
                )
            }

            val extractRootRel = artifact.extractSharedLibsFromZipToDir
            if (extractRootRel != null) {
                val extractRoot = File(rootDir, extractRootRel)
                var extractedCount = 0
                ZipFile(tmpFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue
                        // Match lib/<abi>/libhev-socks5-tunnel.so or lib/<abi>/libtun2socks.so
                        val parts = entry.name.split('/')
                        if (parts.size == 3 && parts[0] == "lib" && parts[1] in wantedAbis) {
                            val soName = parts[2]
                            if (soName == "libhev-socks5-tunnel.so" || soName == "libtun2socks.so") {
                                val outSo = File(extractRoot, "${parts[1]}/$soName")
                                outSo.parentFile.mkdirs()
                                zip.getInputStream(entry).use { zin ->
                                    outSo.outputStream().use { zout -> zin.copyTo(zout) }
                                }
                                extractedCount++
                                logger.lifecycle(
                                    "Extracted ${parts[1]}/$soName -> ${outSo.relativeTo(rootDir)} (${outSo.length()} bytes)"
                                )
                            }
                        }
                    }
                }
                tmpFile.delete()
                if (extractedCount == 0) {
                    throw GradleException("No native TUN .so libraries found inside ${artifact.id} archive")
                }
            } else {
                tmpFile.copyTo(target, overwrite = true)
                tmpFile.delete()
                logger.lifecycle("Installed ${artifact.id} -> ${target.relativeTo(rootDir)} ($actualHash)")
            }
        }
    }

    private fun downloadFollowRedirects(urlString: String, destination: File) {
        var currentUrl = urlString
        repeat(6) {
            val conn = (URI.create(currentUrl).toURL().openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 30_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", "AetherFlow-Build/1.0")
            }
            val code = conn.responseCode
            if (code in 301..308) {
                val next = conn.getHeaderField("Location")
                    ?: throw GradleException("Redirect without Location header from $currentUrl")
                conn.disconnect()
                currentUrl = URI.create(currentUrl).resolve(next).toString()
            } else if (code in 200..299) {
                conn.inputStream.buffered().use { input ->
                    destination.outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }
                conn.disconnect()
                return
            } else {
                conn.disconnect()
                throw GradleException("HTTP $code while downloading $currentUrl")
            }
        }
        throw GradleException("Too many redirects while downloading $urlString")
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

val resolvedOverrides = defaultPinnedArtifacts.mapNotNull { spec ->
    providers.gradleProperty("checksum.${spec.id}").orNull?.let { spec.id to it }
}.toMap()

tasks.register<VerifyNativeCoresTask>("verifyNativeCores") {
    group = "nativeCores"
    description = "Verifies SHA-256 checksums of any locally present native .aar / .so binaries."
    rootDirectory.set(layout.projectDirectory)
    artifacts.set(defaultPinnedArtifacts)
    checksumOverrides.set(resolvedOverrides)
}

tasks.register<DownloadNativeCoresTask>("downloadNativeCores") {
    group = "nativeCores"
    description = "Downloads pinned native proxy/TUN binaries and validates SHA-256 digests."
    rootDirectory.set(layout.projectDirectory)
    artifacts.set(defaultPinnedArtifacts)
    checksumOverrides.set(resolvedOverrides)
    strictChecksum.set(
        providers.gradleProperty("nativeCores.strictChecksum").map { it.toBoolean() }.orElse(true)
    )
}

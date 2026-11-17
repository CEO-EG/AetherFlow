# Preserve JNI & Gomobile native bindings
-keep class hev.htproxy.** { *; }
-keep class com.v2ray.ang.service.TProxyService { *; }
-keep class libv2ray.** { *; }
-keep class go.** { *; }
-keep class io.nekohasekai.libbox.** { *; }
-keep class org.amnezia.awg.** { *; }
-dontwarn hev.htproxy.**
-dontwarn com.v2ray.ang.service.**
-dontwarn libv2ray.**
-dontwarn io.nekohasekai.libbox.**
-dontwarn org.amnezia.awg.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.ceoeg.aetherflow.**$$serializer { *; }

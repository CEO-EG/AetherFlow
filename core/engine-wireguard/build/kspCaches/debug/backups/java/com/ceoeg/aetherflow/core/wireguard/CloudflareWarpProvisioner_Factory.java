package com.ceoeg.aetherflow.core.wireguard;

import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class CloudflareWarpProvisioner_Factory implements Factory<CloudflareWarpProvisioner> {
  private final Provider<DiagnosticLogger> loggerProvider;

  private CloudflareWarpProvisioner_Factory(Provider<DiagnosticLogger> loggerProvider) {
    this.loggerProvider = loggerProvider;
  }

  @Override
  public CloudflareWarpProvisioner get() {
    return newInstance(loggerProvider.get());
  }

  public static CloudflareWarpProvisioner_Factory create(
      Provider<DiagnosticLogger> loggerProvider) {
    return new CloudflareWarpProvisioner_Factory(loggerProvider);
  }

  public static CloudflareWarpProvisioner newInstance(DiagnosticLogger logger) {
    return new CloudflareWarpProvisioner(logger);
  }
}

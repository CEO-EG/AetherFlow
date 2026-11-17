package com.ceoeg.aetherflow.core.tun;

import android.content.Context;
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ProcessBadVpnNativeBridge_Factory implements Factory<ProcessBadVpnNativeBridge> {
  private final Provider<Context> contextProvider;

  private final Provider<DiagnosticLogger> loggerProvider;

  private ProcessBadVpnNativeBridge_Factory(Provider<Context> contextProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    this.contextProvider = contextProvider;
    this.loggerProvider = loggerProvider;
  }

  @Override
  public ProcessBadVpnNativeBridge get() {
    return newInstance(contextProvider.get(), loggerProvider.get());
  }

  public static ProcessBadVpnNativeBridge_Factory create(Provider<Context> contextProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    return new ProcessBadVpnNativeBridge_Factory(contextProvider, loggerProvider);
  }

  public static ProcessBadVpnNativeBridge newInstance(Context context, DiagnosticLogger logger) {
    return new ProcessBadVpnNativeBridge(context, logger);
  }
}

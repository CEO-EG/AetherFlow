package com.ceoeg.aetherflow.core.vpn;

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
public final class NetworkHandoffMonitor_Factory implements Factory<NetworkHandoffMonitor> {
  private final Provider<Context> contextProvider;

  private final Provider<DiagnosticLogger> loggerProvider;

  private NetworkHandoffMonitor_Factory(Provider<Context> contextProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    this.contextProvider = contextProvider;
    this.loggerProvider = loggerProvider;
  }

  @Override
  public NetworkHandoffMonitor get() {
    return newInstance(contextProvider.get(), loggerProvider.get());
  }

  public static NetworkHandoffMonitor_Factory create(Provider<Context> contextProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    return new NetworkHandoffMonitor_Factory(contextProvider, loggerProvider);
  }

  public static NetworkHandoffMonitor newInstance(Context context, DiagnosticLogger logger) {
    return new NetworkHandoffMonitor(context, logger);
  }
}

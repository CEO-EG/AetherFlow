package com.ceoeg.aetherflow.core.singbox;

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
public final class GomobileLibBoxBridge_Factory implements Factory<GomobileLibBoxBridge> {
  private final Provider<DiagnosticLogger> loggerProvider;

  private GomobileLibBoxBridge_Factory(Provider<DiagnosticLogger> loggerProvider) {
    this.loggerProvider = loggerProvider;
  }

  @Override
  public GomobileLibBoxBridge get() {
    return newInstance(loggerProvider.get());
  }

  public static GomobileLibBoxBridge_Factory create(Provider<DiagnosticLogger> loggerProvider) {
    return new GomobileLibBoxBridge_Factory(loggerProvider);
  }

  public static GomobileLibBoxBridge newInstance(DiagnosticLogger logger) {
    return new GomobileLibBoxBridge(logger);
  }
}

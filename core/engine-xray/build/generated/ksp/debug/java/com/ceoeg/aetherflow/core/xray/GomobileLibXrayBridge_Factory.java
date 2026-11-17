package com.ceoeg.aetherflow.core.xray;

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
public final class GomobileLibXrayBridge_Factory implements Factory<GomobileLibXrayBridge> {
  private final Provider<DiagnosticLogger> loggerProvider;

  private GomobileLibXrayBridge_Factory(Provider<DiagnosticLogger> loggerProvider) {
    this.loggerProvider = loggerProvider;
  }

  @Override
  public GomobileLibXrayBridge get() {
    return newInstance(loggerProvider.get());
  }

  public static GomobileLibXrayBridge_Factory create(Provider<DiagnosticLogger> loggerProvider) {
    return new GomobileLibXrayBridge_Factory(loggerProvider);
  }

  public static GomobileLibXrayBridge newInstance(DiagnosticLogger logger) {
    return new GomobileLibXrayBridge(logger);
  }
}

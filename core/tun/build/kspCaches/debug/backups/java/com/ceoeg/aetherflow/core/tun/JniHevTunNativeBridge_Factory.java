package com.ceoeg.aetherflow.core.tun;

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
public final class JniHevTunNativeBridge_Factory implements Factory<JniHevTunNativeBridge> {
  private final Provider<DiagnosticLogger> loggerProvider;

  private JniHevTunNativeBridge_Factory(Provider<DiagnosticLogger> loggerProvider) {
    this.loggerProvider = loggerProvider;
  }

  @Override
  public JniHevTunNativeBridge get() {
    return newInstance(loggerProvider.get());
  }

  public static JniHevTunNativeBridge_Factory create(Provider<DiagnosticLogger> loggerProvider) {
    return new JniHevTunNativeBridge_Factory(loggerProvider);
  }

  public static JniHevTunNativeBridge newInstance(DiagnosticLogger logger) {
    return new JniHevTunNativeBridge(logger);
  }
}

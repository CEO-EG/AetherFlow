package com.ceoeg.aetherflow.core.common.logging;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class DiagnosticLogger_Factory implements Factory<DiagnosticLogger> {
  @Override
  public DiagnosticLogger get() {
    return newInstance();
  }

  public static DiagnosticLogger_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DiagnosticLogger newInstance() {
    return new DiagnosticLogger();
  }

  private static final class InstanceHolder {
    static final DiagnosticLogger_Factory INSTANCE = new DiagnosticLogger_Factory();
  }
}

package com.ceoeg.aetherflow.core.parser;

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
public final class UniversalConfigParser_Factory implements Factory<UniversalConfigParser> {
  @Override
  public UniversalConfigParser get() {
    return newInstance();
  }

  public static UniversalConfigParser_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static UniversalConfigParser newInstance() {
    return new UniversalConfigParser();
  }

  private static final class InstanceHolder {
    static final UniversalConfigParser_Factory INSTANCE = new UniversalConfigParser_Factory();
  }
}

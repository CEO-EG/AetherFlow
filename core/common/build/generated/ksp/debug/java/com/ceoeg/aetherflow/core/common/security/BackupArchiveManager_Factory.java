package com.ceoeg.aetherflow.core.common.security;

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
public final class BackupArchiveManager_Factory implements Factory<BackupArchiveManager> {
  @Override
  public BackupArchiveManager get() {
    return newInstance();
  }

  public static BackupArchiveManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BackupArchiveManager newInstance() {
    return new BackupArchiveManager();
  }

  private static final class InstanceHolder {
    static final BackupArchiveManager_Factory INSTANCE = new BackupArchiveManager_Factory();
  }
}

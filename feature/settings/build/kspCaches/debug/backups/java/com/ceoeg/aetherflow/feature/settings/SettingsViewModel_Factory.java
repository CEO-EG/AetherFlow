package com.ceoeg.aetherflow.feature.settings;

import com.ceoeg.aetherflow.core.common.database.TunnelRepository;
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger;
import com.ceoeg.aetherflow.core.common.security.BackupArchiveManager;
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<EncryptedSettingsStore> settingsStoreProvider;

  private final Provider<TunnelRepository> repositoryProvider;

  private final Provider<BackupArchiveManager> backupManagerProvider;

  private final Provider<DiagnosticLogger> loggerProvider;

  private SettingsViewModel_Factory(Provider<EncryptedSettingsStore> settingsStoreProvider,
      Provider<TunnelRepository> repositoryProvider,
      Provider<BackupArchiveManager> backupManagerProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    this.settingsStoreProvider = settingsStoreProvider;
    this.repositoryProvider = repositoryProvider;
    this.backupManagerProvider = backupManagerProvider;
    this.loggerProvider = loggerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsStoreProvider.get(), repositoryProvider.get(), backupManagerProvider.get(), loggerProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<EncryptedSettingsStore> settingsStoreProvider,
      Provider<TunnelRepository> repositoryProvider,
      Provider<BackupArchiveManager> backupManagerProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    return new SettingsViewModel_Factory(settingsStoreProvider, repositoryProvider, backupManagerProvider, loggerProvider);
  }

  public static SettingsViewModel newInstance(EncryptedSettingsStore settingsStore,
      TunnelRepository repository, BackupArchiveManager backupManager, DiagnosticLogger logger) {
    return new SettingsViewModel(settingsStore, repository, backupManager, logger);
  }
}

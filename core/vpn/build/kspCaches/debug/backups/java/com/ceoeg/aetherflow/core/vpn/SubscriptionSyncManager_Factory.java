package com.ceoeg.aetherflow.core.vpn;

import android.content.Context;
import com.ceoeg.aetherflow.core.common.database.TunnelRepository;
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger;
import com.ceoeg.aetherflow.core.parser.UniversalConfigParser;
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
public final class SubscriptionSyncManager_Factory implements Factory<SubscriptionSyncManager> {
  private final Provider<Context> contextProvider;

  private final Provider<TunnelRepository> repositoryProvider;

  private final Provider<UniversalConfigParser> parserProvider;

  private final Provider<DiagnosticLogger> loggerProvider;

  private SubscriptionSyncManager_Factory(Provider<Context> contextProvider,
      Provider<TunnelRepository> repositoryProvider, Provider<UniversalConfigParser> parserProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    this.contextProvider = contextProvider;
    this.repositoryProvider = repositoryProvider;
    this.parserProvider = parserProvider;
    this.loggerProvider = loggerProvider;
  }

  @Override
  public SubscriptionSyncManager get() {
    return newInstance(contextProvider.get(), repositoryProvider.get(), parserProvider.get(), loggerProvider.get());
  }

  public static SubscriptionSyncManager_Factory create(Provider<Context> contextProvider,
      Provider<TunnelRepository> repositoryProvider, Provider<UniversalConfigParser> parserProvider,
      Provider<DiagnosticLogger> loggerProvider) {
    return new SubscriptionSyncManager_Factory(contextProvider, repositoryProvider, parserProvider, loggerProvider);
  }

  public static SubscriptionSyncManager newInstance(Context context, TunnelRepository repository,
      UniversalConfigParser parser, DiagnosticLogger logger) {
    return new SubscriptionSyncManager(context, repository, parser, logger);
  }
}

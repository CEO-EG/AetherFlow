package com.ceoeg.aetherflow.feature.subscriptions;

import com.ceoeg.aetherflow.core.common.database.TunnelRepository;
import com.ceoeg.aetherflow.core.vpn.SubscriptionSyncManager;
import com.ceoeg.aetherflow.core.wireguard.CloudflareWarpProvisioner;
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
public final class SubscriptionsViewModel_Factory implements Factory<SubscriptionsViewModel> {
  private final Provider<TunnelRepository> repositoryProvider;

  private final Provider<SubscriptionSyncManager> syncManagerProvider;

  private final Provider<CloudflareWarpProvisioner> warpProvisionerProvider;

  private SubscriptionsViewModel_Factory(Provider<TunnelRepository> repositoryProvider,
      Provider<SubscriptionSyncManager> syncManagerProvider,
      Provider<CloudflareWarpProvisioner> warpProvisionerProvider) {
    this.repositoryProvider = repositoryProvider;
    this.syncManagerProvider = syncManagerProvider;
    this.warpProvisionerProvider = warpProvisionerProvider;
  }

  @Override
  public SubscriptionsViewModel get() {
    return newInstance(repositoryProvider.get(), syncManagerProvider.get(), warpProvisionerProvider.get());
  }

  public static SubscriptionsViewModel_Factory create(Provider<TunnelRepository> repositoryProvider,
      Provider<SubscriptionSyncManager> syncManagerProvider,
      Provider<CloudflareWarpProvisioner> warpProvisionerProvider) {
    return new SubscriptionsViewModel_Factory(repositoryProvider, syncManagerProvider, warpProvisionerProvider);
  }

  public static SubscriptionsViewModel newInstance(TunnelRepository repository,
      SubscriptionSyncManager syncManager, CloudflareWarpProvisioner warpProvisioner) {
    return new SubscriptionsViewModel(repository, syncManager, warpProvisioner);
  }
}

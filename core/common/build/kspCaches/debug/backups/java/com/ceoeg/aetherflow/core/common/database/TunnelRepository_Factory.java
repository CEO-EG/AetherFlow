package com.ceoeg.aetherflow.core.common.database;

import com.ceoeg.aetherflow.core.common.security.CredentialCipher;
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
public final class TunnelRepository_Factory implements Factory<TunnelRepository> {
  private final Provider<ProfileDao> profileDaoProvider;

  private final Provider<SubscriptionDao> subscriptionDaoProvider;

  private final Provider<RoutingRuleDao> routingRuleDaoProvider;

  private final Provider<CredentialCipher> cipherProvider;

  private TunnelRepository_Factory(Provider<ProfileDao> profileDaoProvider,
      Provider<SubscriptionDao> subscriptionDaoProvider,
      Provider<RoutingRuleDao> routingRuleDaoProvider, Provider<CredentialCipher> cipherProvider) {
    this.profileDaoProvider = profileDaoProvider;
    this.subscriptionDaoProvider = subscriptionDaoProvider;
    this.routingRuleDaoProvider = routingRuleDaoProvider;
    this.cipherProvider = cipherProvider;
  }

  @Override
  public TunnelRepository get() {
    return newInstance(profileDaoProvider.get(), subscriptionDaoProvider.get(), routingRuleDaoProvider.get(), cipherProvider.get());
  }

  public static TunnelRepository_Factory create(Provider<ProfileDao> profileDaoProvider,
      Provider<SubscriptionDao> subscriptionDaoProvider,
      Provider<RoutingRuleDao> routingRuleDaoProvider, Provider<CredentialCipher> cipherProvider) {
    return new TunnelRepository_Factory(profileDaoProvider, subscriptionDaoProvider, routingRuleDaoProvider, cipherProvider);
  }

  public static TunnelRepository newInstance(ProfileDao profileDao, SubscriptionDao subscriptionDao,
      RoutingRuleDao routingRuleDao, CredentialCipher cipher) {
    return new TunnelRepository(profileDao, subscriptionDao, routingRuleDao, cipher);
  }
}

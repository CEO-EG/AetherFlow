package com.ceoeg.aetherflow.core.common.di;

import com.ceoeg.aetherflow.core.common.database.AetherFlowDatabase;
import com.ceoeg.aetherflow.core.common.database.SubscriptionDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class CommonModule_ProvideSubscriptionDaoFactory implements Factory<SubscriptionDao> {
  private final Provider<AetherFlowDatabase> dbProvider;

  private CommonModule_ProvideSubscriptionDaoFactory(Provider<AetherFlowDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SubscriptionDao get() {
    return provideSubscriptionDao(dbProvider.get());
  }

  public static CommonModule_ProvideSubscriptionDaoFactory create(
      Provider<AetherFlowDatabase> dbProvider) {
    return new CommonModule_ProvideSubscriptionDaoFactory(dbProvider);
  }

  public static SubscriptionDao provideSubscriptionDao(AetherFlowDatabase db) {
    return Preconditions.checkNotNullFromProvides(CommonModule.INSTANCE.provideSubscriptionDao(db));
  }
}

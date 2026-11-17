package com.ceoeg.aetherflow.core.common.di;

import com.ceoeg.aetherflow.core.common.database.AetherFlowDatabase;
import com.ceoeg.aetherflow.core.common.database.RoutingRuleDao;
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
public final class CommonModule_ProvideRoutingRuleDaoFactory implements Factory<RoutingRuleDao> {
  private final Provider<AetherFlowDatabase> dbProvider;

  private CommonModule_ProvideRoutingRuleDaoFactory(Provider<AetherFlowDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public RoutingRuleDao get() {
    return provideRoutingRuleDao(dbProvider.get());
  }

  public static CommonModule_ProvideRoutingRuleDaoFactory create(
      Provider<AetherFlowDatabase> dbProvider) {
    return new CommonModule_ProvideRoutingRuleDaoFactory(dbProvider);
  }

  public static RoutingRuleDao provideRoutingRuleDao(AetherFlowDatabase db) {
    return Preconditions.checkNotNullFromProvides(CommonModule.INSTANCE.provideRoutingRuleDao(db));
  }
}

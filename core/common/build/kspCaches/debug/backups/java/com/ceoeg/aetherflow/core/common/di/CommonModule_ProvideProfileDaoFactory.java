package com.ceoeg.aetherflow.core.common.di;

import com.ceoeg.aetherflow.core.common.database.AetherFlowDatabase;
import com.ceoeg.aetherflow.core.common.database.ProfileDao;
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
public final class CommonModule_ProvideProfileDaoFactory implements Factory<ProfileDao> {
  private final Provider<AetherFlowDatabase> dbProvider;

  private CommonModule_ProvideProfileDaoFactory(Provider<AetherFlowDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ProfileDao get() {
    return provideProfileDao(dbProvider.get());
  }

  public static CommonModule_ProvideProfileDaoFactory create(
      Provider<AetherFlowDatabase> dbProvider) {
    return new CommonModule_ProvideProfileDaoFactory(dbProvider);
  }

  public static ProfileDao provideProfileDao(AetherFlowDatabase db) {
    return Preconditions.checkNotNullFromProvides(CommonModule.INSTANCE.provideProfileDao(db));
  }
}

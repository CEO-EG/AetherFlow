package com.ceoeg.aetherflow.core.common.di;

import android.content.Context;
import com.ceoeg.aetherflow.core.common.database.AetherFlowDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class CommonModule_ProvideDatabaseFactory implements Factory<AetherFlowDatabase> {
  private final Provider<Context> contextProvider;

  private CommonModule_ProvideDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AetherFlowDatabase get() {
    return provideDatabase(contextProvider.get());
  }

  public static CommonModule_ProvideDatabaseFactory create(Provider<Context> contextProvider) {
    return new CommonModule_ProvideDatabaseFactory(contextProvider);
  }

  public static AetherFlowDatabase provideDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(CommonModule.INSTANCE.provideDatabase(context));
  }
}

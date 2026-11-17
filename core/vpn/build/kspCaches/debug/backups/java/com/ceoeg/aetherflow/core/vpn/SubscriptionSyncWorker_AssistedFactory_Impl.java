package com.ceoeg.aetherflow.core.vpn;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class SubscriptionSyncWorker_AssistedFactory_Impl implements SubscriptionSyncWorker_AssistedFactory {
  private final SubscriptionSyncWorker_Factory delegateFactory;

  SubscriptionSyncWorker_AssistedFactory_Impl(SubscriptionSyncWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public SubscriptionSyncWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<SubscriptionSyncWorker_AssistedFactory> create(
      SubscriptionSyncWorker_Factory delegateFactory) {
    return InstanceFactory.create(new SubscriptionSyncWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<SubscriptionSyncWorker_AssistedFactory> createFactoryProvider(
      SubscriptionSyncWorker_Factory delegateFactory) {
    return InstanceFactory.create(new SubscriptionSyncWorker_AssistedFactory_Impl(delegateFactory));
  }
}

package com.ceoeg.aetherflow.core.vpn;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
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
public final class SubscriptionSyncWorker_Factory {
  private final Provider<SubscriptionSyncManager> syncManagerProvider;

  private SubscriptionSyncWorker_Factory(Provider<SubscriptionSyncManager> syncManagerProvider) {
    this.syncManagerProvider = syncManagerProvider;
  }

  public SubscriptionSyncWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, syncManagerProvider.get());
  }

  public static SubscriptionSyncWorker_Factory create(
      Provider<SubscriptionSyncManager> syncManagerProvider) {
    return new SubscriptionSyncWorker_Factory(syncManagerProvider);
  }

  public static SubscriptionSyncWorker newInstance(Context appContext,
      WorkerParameters workerParams, SubscriptionSyncManager syncManager) {
    return new SubscriptionSyncWorker(appContext, workerParams, syncManager);
  }
}

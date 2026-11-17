package com.ceoeg.aetherflow.core.vpn;

import androidx.hilt.work.WorkerAssistedFactory;
import androidx.work.ListenableWorker;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.annotation.processing.Generated;

@Generated("androidx.hilt.AndroidXHiltProcessor")
@Module
@InstallIn(SingletonComponent.class)
@OriginatingElement(
    topLevelClass = SubscriptionSyncWorker.class
)
public interface SubscriptionSyncWorker_HiltModule {
  @Binds
  @IntoMap
  @StringKey("com.ceoeg.aetherflow.core.vpn.SubscriptionSyncWorker")
  WorkerAssistedFactory<? extends ListenableWorker> bind(
      SubscriptionSyncWorker_AssistedFactory factory);
}

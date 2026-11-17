package com.ceoeg.aetherflow.core.common.security;

import android.content.Context;
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
public final class EncryptedSettingsStore_Factory implements Factory<EncryptedSettingsStore> {
  private final Provider<Context> contextProvider;

  private final Provider<CredentialCipher> cipherProvider;

  private EncryptedSettingsStore_Factory(Provider<Context> contextProvider,
      Provider<CredentialCipher> cipherProvider) {
    this.contextProvider = contextProvider;
    this.cipherProvider = cipherProvider;
  }

  @Override
  public EncryptedSettingsStore get() {
    return newInstance(contextProvider.get(), cipherProvider.get());
  }

  public static EncryptedSettingsStore_Factory create(Provider<Context> contextProvider,
      Provider<CredentialCipher> cipherProvider) {
    return new EncryptedSettingsStore_Factory(contextProvider, cipherProvider);
  }

  public static EncryptedSettingsStore newInstance(Context context, CredentialCipher cipher) {
    return new EncryptedSettingsStore(context, cipher);
  }
}

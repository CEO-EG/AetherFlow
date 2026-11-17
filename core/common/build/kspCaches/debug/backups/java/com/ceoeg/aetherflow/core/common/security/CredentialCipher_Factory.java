package com.ceoeg.aetherflow.core.common.security;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class CredentialCipher_Factory implements Factory<CredentialCipher> {
  @Override
  public CredentialCipher get() {
    return newInstance();
  }

  public static CredentialCipher_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static CredentialCipher newInstance() {
    return new CredentialCipher();
  }

  private static final class InstanceHolder {
    static final CredentialCipher_Factory INSTANCE = new CredentialCipher_Factory();
  }
}

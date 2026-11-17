package com.ceoeg.aetherflow.core.routing;

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
public final class DnsAndRoutingCompiler_Factory implements Factory<DnsAndRoutingCompiler> {
  @Override
  public DnsAndRoutingCompiler get() {
    return newInstance();
  }

  public static DnsAndRoutingCompiler_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DnsAndRoutingCompiler newInstance() {
    return new DnsAndRoutingCompiler();
  }

  private static final class InstanceHolder {
    static final DnsAndRoutingCompiler_Factory INSTANCE = new DnsAndRoutingCompiler_Factory();
  }
}

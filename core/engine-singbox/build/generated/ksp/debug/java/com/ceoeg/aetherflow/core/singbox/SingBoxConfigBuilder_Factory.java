package com.ceoeg.aetherflow.core.singbox;

import com.ceoeg.aetherflow.core.routing.DnsAndRoutingCompiler;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
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
public final class SingBoxConfigBuilder_Factory implements Factory<SingBoxConfigBuilder> {
  private final Provider<DnsAndRoutingCompiler> routingCompilerProvider;

  private SingBoxConfigBuilder_Factory(Provider<DnsAndRoutingCompiler> routingCompilerProvider) {
    this.routingCompilerProvider = routingCompilerProvider;
  }

  @Override
  public SingBoxConfigBuilder get() {
    return newInstance(routingCompilerProvider.get());
  }

  public static SingBoxConfigBuilder_Factory create(
      Provider<DnsAndRoutingCompiler> routingCompilerProvider) {
    return new SingBoxConfigBuilder_Factory(routingCompilerProvider);
  }

  public static SingBoxConfigBuilder newInstance(DnsAndRoutingCompiler routingCompiler) {
    return new SingBoxConfigBuilder(routingCompiler);
  }
}

package com.ceoeg.aetherflow.core.xray;

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
public final class XrayConfigBuilder_Factory implements Factory<XrayConfigBuilder> {
  private final Provider<DnsAndRoutingCompiler> routingCompilerProvider;

  private XrayConfigBuilder_Factory(Provider<DnsAndRoutingCompiler> routingCompilerProvider) {
    this.routingCompilerProvider = routingCompilerProvider;
  }

  @Override
  public XrayConfigBuilder get() {
    return newInstance(routingCompilerProvider.get());
  }

  public static XrayConfigBuilder_Factory create(
      Provider<DnsAndRoutingCompiler> routingCompilerProvider) {
    return new XrayConfigBuilder_Factory(routingCompilerProvider);
  }

  public static XrayConfigBuilder newInstance(DnsAndRoutingCompiler routingCompiler) {
    return new XrayConfigBuilder(routingCompiler);
  }
}

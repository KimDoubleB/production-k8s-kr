package com.example.demo.config;

import dev.openfeature.contrib.providers.flagd.FlagdOptions;
import dev.openfeature.contrib.providers.flagd.FlagdProvider;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import dev.openfeature.sdk.OpenFeatureAPI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FlagdProperties.class)
public class OpenFeatureConfiguration {

	@Bean
	public Client openFeatureClient(final FlagdProperties properties) {
		OpenFeatureAPI instance = OpenFeatureAPI.getInstance();
		instance.setProvider(pluggableFlagdProvider(properties));

		return instance.getClient();
	}

	private FeatureProvider pluggableFlagdProvider(FlagdProperties properties) {
		return new FlagdProvider(FlagdOptions.builder()
				.host(properties.host())
				.port(properties.port())
				.tls(properties.tls())
				.build());
	}
}

package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("feature-flag")
public record FlagdProperties(String host, int port, boolean tls) {
}

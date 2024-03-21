package bio.terra.iffy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("iffy.flagd")
public record FlagdConfiguration(String host, int port, int timeoutMilliseconds) {}

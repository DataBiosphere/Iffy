package bio.terra.iffy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iffy.sam")
public record SamConfiguration(String basePath) {}

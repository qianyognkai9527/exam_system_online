package com.joker.ai.exam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kimi.api")
@Data
public class KimiProperties {

    private String model;
    private String baseUrl;
    private String apiKey;
    private Integer maxTokens;
    private Double temperature;
}
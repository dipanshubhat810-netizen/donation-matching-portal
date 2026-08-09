package com.sevasahayog.donationmatching.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class RenderDataSourceEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DB_URL = "DB_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty(DB_URL);
        if (dbUrl == null || dbUrl.startsWith("jdbc:")) {
            return;
        }
        String jdbcUrl;
        if (dbUrl.startsWith("postgres://")) {
            jdbcUrl = "jdbc:postgresql://" + dbUrl.substring("postgres://".length());
        } else if (dbUrl.startsWith("postgresql://")) {
            jdbcUrl = "jdbc:postgresql://" + dbUrl.substring("postgresql://".length());
        } else {
            return;
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", jdbcUrl);
        environment.getPropertySources().addFirst(new MapPropertySource("renderDataSourceUrl", properties));
    }
}

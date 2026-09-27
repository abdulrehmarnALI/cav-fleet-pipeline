package com.sunshine.cavfleet;

public class Config {
    public static final String NATS_URL = getEnvOrDefault("NATS_URL", "nats://localhost:4222");
    public static final String JDBC_URL = getEnvOrDefault("JDBC_URL", "jdbc:postgresql://127.0.0.1:5434/cavfleet");
    public static final String JDBC_USER = getEnvOrDefault("JDBC_USER", "cavfleet");
    public static final String JDBC_PASSWORD = getEnvOrDefault("JDBC_PASSWORD", "cavfleet");
    private static String getEnvOrDefault(String envKey, String defaultValue) {
        String value = System.getenv(envKey);
        return value != null ? value : defaultValue;
    }
}

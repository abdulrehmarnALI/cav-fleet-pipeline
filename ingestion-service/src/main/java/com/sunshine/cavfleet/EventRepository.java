package com.sunshine.cavfleet;

import java.sql.*;

public class EventRepository {
    private final String jdbcUrl;
    private final String user;
    private final String password;

    public EventRepository(String jdbcUrl, String user, String password) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
    }

    public void upsertTelemetry(String msgId, String route, TelemetryEvent event) throws SQLException {
        String sql = """
            INSERT INTO telemetry_events (msg_id, route, vehicle_id, speed_kph, lat, lon, occurred_at)
            VALUES (?, ?, ?, ?, ?, ?, to_timestamp(?))
            ON CONFLICT (msg_id) DO NOTHING
            """;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, msgId);
            ps.setString(2, route);
            ps.setString(3, event.getVehicleId());
            ps.setDouble(4, event.getSpeedKph());
            ps.setDouble(5, event.getLat());
            ps.setDouble(6, event.getLon());
            ps.setDouble(7, event.getOccurredAt());
            int rowsAffected = ps.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected + " (0 means it was already there - dedup working)");
        }
    }

    public void upsertDisengagement(String msgId, String route, DisengagementEvent event) throws SQLException {
    String sql = """
        INSERT INTO disengagement_events (msg_id, route, vehicle_id, reason, occurred_at)
        VALUES (?, ?, ?, ?, to_timestamp(?))
        ON CONFLICT (msg_id) DO NOTHING
        """;

    try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, msgId);
        ps.setString(2, route);
        ps.setString(3, event.getVehicleId());
        ps.setString(4, event.getReason());
        ps.setDouble(5, event.getOccurredAt());
        ps.executeUpdate();
    }
}
}
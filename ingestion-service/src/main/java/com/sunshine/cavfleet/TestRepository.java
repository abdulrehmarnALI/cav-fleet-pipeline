package com.sunshine.cavfleet;

public class TestRepository {
    public static void main(String[] args) throws Exception {
        EventRepository repo = new EventRepository(
            "jdbc:postgresql://127.0.0.1:5434/cavfleet", "cavfleet", "cavfleet"
        );

        TelemetryEvent fakeEvent = new TelemetryEvent("car99", 52.5, -1.9, 42.0, 1790440000.0);

        repo.upsertTelemetry("test-msg-001", "route1_motorway", fakeEvent);
        System.out.println("First insert done - check Postgres for a row with msg_id=test-msg-001");

        repo.upsertTelemetry("test-msg-001", "route1_motorway", fakeEvent);
        System.out.println("Second insert with same msg_id done - should show 0 rows affected");
    }
}
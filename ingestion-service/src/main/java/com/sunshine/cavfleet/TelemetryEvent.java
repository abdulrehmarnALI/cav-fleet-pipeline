package com.sunshine.cavfleet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TelemetryEvent {
    private final String vehicleId;
    private final double lat;
    private final double lon;
    private final double speedKph;
    private final double occurredAt;

    @JsonCreator
    public TelemetryEvent(
            @JsonProperty("vehicle_id") String vehicleId,
            @JsonProperty("lat") double lat,
            @JsonProperty("lon") double lon,
            @JsonProperty("speed_kph") double speedKph,
            @JsonProperty("occurred_at") double occurredAt) {
        this.vehicleId = vehicleId;
        this.lat = lat;
        this.lon = lon;
        this.speedKph = speedKph;
        this.occurredAt = occurredAt;
    }

    public String getVehicleId() { return vehicleId; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public double getSpeedKph() { return speedKph; }
    public double getOccurredAt() { return occurredAt; }
}
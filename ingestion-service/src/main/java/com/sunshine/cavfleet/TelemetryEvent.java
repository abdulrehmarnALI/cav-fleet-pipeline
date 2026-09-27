package com.sunshine.cavfleet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TelemetryEvent {
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
    private final String vehicleId;
    private final Double lat;
    private final Double lon;
    private final Double speedKph;
    private final Double occurredAt;

    @JsonCreator
    public TelemetryEvent(
            @JsonProperty("vehicle_id") String vehicleId,
            @JsonProperty("lat") Double lat,
            @JsonProperty("lon") Double lon,
            @JsonProperty("speed_kph") Double speedKph,
            @JsonProperty("occurred_at") Double occurredAt) {
        this.vehicleId = vehicleId;
        this.lat = lat;
        this.lon = lon;
        this.speedKph = speedKph;
        this.occurredAt = occurredAt;
    }

    public void validate() {
        require(vehicleId != null, "vehicleId must be non-null");
        require(lat != null, "lat must be non-null");
        require(lon != null, "lon must be non-null");
        require(speedKph != null, "speedKph must be non-null");
        require(occurredAt != null, "occurredAt must be non-null");
    }

    public String getVehicleId() { return vehicleId; }
    public Double getLat() { return lat; }
    public Double getLon() { return lon; }
    public Double getSpeedKph() { return speedKph; }
    public Double getOccurredAt() { return occurredAt; }
}
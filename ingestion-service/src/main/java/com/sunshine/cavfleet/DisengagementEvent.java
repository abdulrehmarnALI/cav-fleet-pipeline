package com.sunshine.cavfleet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DisengagementEvent {
    private final String vehicleId;
    private final String reason;
    private final double occurredAt;

    @JsonCreator
    public DisengagementEvent(
            @JsonProperty("vehicle_id") String vehicleId,
            @JsonProperty("reason") String reason,
            @JsonProperty("occurred_at") double occurredAt) {
        this.vehicleId = vehicleId;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public String getVehicleId() { return vehicleId; }
    public String getReason() { return reason; }
    public double getOccurredAt() { return occurredAt; }
}
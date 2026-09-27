package com.sunshine.cavfleet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DisengagementEvent {
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private final String vehicleId;
    private final String reason;
    private final Double occurredAt;

    @JsonCreator
    public DisengagementEvent(
            @JsonProperty("vehicle_id") String vehicleId,
            @JsonProperty("reason") String reason,
            @JsonProperty("occurred_at") Double occurredAt) {
        this.vehicleId = vehicleId;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public void validate() {
        require(vehicleId != null, "vehicleId must be non-null");
        require(reason != null, "reason must be non-null");
        require(occurredAt != null, "occurredAt must be non-null");
    }

    public String getVehicleId() { return vehicleId; }
    public String getReason() { return reason; }
    public Double getOccurredAt() { return occurredAt; }
}
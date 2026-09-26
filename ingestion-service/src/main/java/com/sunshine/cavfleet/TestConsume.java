package com.sunshine.cavfleet;

import io.nats.client.*;
import io.nats.client.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

public class TestConsume {
    public static void main(String[] args) throws Exception {
        Connection nc = Nats.connect("nats://localhost:4222");

        JetStreamManagement jsm = nc.jetStreamManagement();
        ConsumerConfiguration cc = ConsumerConfiguration.builder()
            .durable("test-consumer")
            .ackPolicy(AckPolicy.Explicit)
            .filterSubject("fleet.>")
            .build();
        jsm.addOrUpdateConsumer("FLEET", cc);

        JetStream js = nc.jetStream();
        PullSubscribeOptions pso = PullSubscribeOptions.bind("FLEET", "test-consumer");
        JetStreamSubscription sub = js.subscribe("fleet.>", pso);

        List<Message> messages = sub.fetch(1, Duration.ofSeconds(5));

        ObjectMapper mapper = new ObjectMapper();

        for (Message msg : messages) {
            System.out.println("Subject: " + msg.getSubject());
            System.out.println("Raw JSON: " + new String(msg.getData()));

            TelemetryEvent event = mapper.readValue(msg.getData(), TelemetryEvent.class);
            System.out.println("Parsed: vehicleId=" + event.getVehicleId()
                + " speed=" + event.getSpeedKph()
                + " lat=" + event.getLat()
                + " lon=" + event.getLon()
                + " occurredAt=" + event.getOccurredAt());

            msg.ack();
        }

        if (messages.isEmpty()) {
            System.out.println("No messages received in the last fetch — is the simulator running / did messages land in FLEET?");
        }

        nc.close();
    }
}
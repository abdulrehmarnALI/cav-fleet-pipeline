package com.sunshine.cavfleet;

import io.nats.client.*;
import io.nats.client.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

public class JetStreamIngestConsumer {
    public static void main(String[] args) throws Exception {
        Connection nc = Nats.connect(Config.NATS_URL);

        JetStreamManagement jsm = nc.jetStreamManagement();
        ConsumerConfiguration cc = ConsumerConfiguration.builder()
            .durable("ingest-consumer")
            .ackPolicy(AckPolicy.Explicit)
            .ackWait(Duration.ofSeconds(30))
            .maxDeliver(5)
            .filterSubject("fleet.>")
            .build();
        jsm.addOrUpdateConsumer("FLEET", cc);

        JetStream js = nc.jetStream();
        PullSubscribeOptions pso = PullSubscribeOptions.bind("FLEET", "ingest-consumer");
        JetStreamSubscription sub = js.subscribe("fleet.>", pso);

        ObjectMapper mapper = new ObjectMapper();
        EventRepository repo = new EventRepository(
            Config.JDBC_URL, Config.JDBC_USER, Config.JDBC_PASSWORD
        );

        System.out.println("Ingestion consumer running - Ctrl+C to stop");

        while (true) {
            List<Message> batch = sub.fetch(20, Duration.ofSeconds(5));

            for (Message msg : batch) {
                String subject = msg.getSubject();
                String msgId = msg.getHeaders() != null ? msg.getHeaders().getFirst("Nats-Msg-Id") : null;
                String[] parts = subject.split("\\.");
                String route = parts[1];

                try {
                    if (subject.endsWith(".telemetry")) {
                        TelemetryEvent event = mapper.readValue(msg.getData(), TelemetryEvent.class);
                        repo.upsertTelemetry(msgId, route, event);
                    } else if (subject.endsWith(".disengagement")) {
                        DisengagementEvent event = mapper.readValue(msg.getData(), DisengagementEvent.class);
                        repo.upsertDisengagement(msgId, route, event);
                    }
                    msg.ack();
                    System.out.println("Ingested: " + subject);
                } catch (Exception e) {
                    System.out.println("Failed to process " + subject + ": " + e.getMessage());
                    msg.nak();
                }
            }
        }
    }
}
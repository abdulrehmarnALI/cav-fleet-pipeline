package com.sunshine.cavfleet;

import io.nats.client.*;
import io.nats.client.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.sql.SQLException;
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
                        event.validate();
                        repo.upsertTelemetry(msgId, route, event);
                    } else if (subject.endsWith(".disengagement")) {
                        DisengagementEvent event = mapper.readValue(msg.getData(), DisengagementEvent.class);
                        event.validate();
                        repo.upsertDisengagement(msgId, route, event);
                    } else {
                        throw new IllegalArgumentException("Unsupported subject: " + subject);
                    }
                    msg.ack();
                    System.out.println("Ingested: " + subject);
                } catch (JsonProcessingException | IllegalArgumentException e) {
                    // bad JSON or a validation error - never going to succeed on retry
                    // write to dead-letter and terminate the message
                    try {
                        repo.writeDeadLetter(msgId, subject, new String(msg.getData()), e.getMessage(), route);
                    } catch (SQLException ex) {
                        System.out.println("Failed to write to dead-letter: " + subject + " - " + ex.getMessage());
                    }
                    msg.term();
                    System.out.println("Dead-lettered - Failed to ingest: " + subject + " - " + e.getMessage());
                } catch (SQLException e) {
                    // database error - might succeed on retry
                    // this doesn't write to dead-letter because the error might be transient
                    msg.nak();
                    System.out.println("Nak'd - Failed to ingest due to SQL error: " + subject + " - " + e.getMessage());
                }
            }
        }
    }
}
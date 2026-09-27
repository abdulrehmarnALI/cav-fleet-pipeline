package com.sunshine.cavfleet;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nats.client.*;
import io.nats.client.api.*;

public class RedeliveryDemo {
    public static void main(String[] args) throws Exception {
        Connection nc = Nats.connect(Config.NATS_URL);
        JetStreamManagement jsm = nc.jetStreamManagement();

        // separate consumer name for redelivery demo so it doesn't interfere with the
        // main ingestion consumer
        ConsumerConfiguration cc = ConsumerConfiguration.builder()
                .durable("redelivery-demo-consumer")
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(Duration.ofSeconds(3)) // short ack wait for demo purposes
                .maxDeliver(5)
                .filterSubject("fleet.>")
                .deliverPolicy(DeliverPolicy.New) // start with new messages so it doesn't pick up old backlog already
                                                  // ingested by real consumer
                .build();
        jsm.addOrUpdateConsumer("FLEET", cc);

        JetStream js = nc.jetStream();
        // subscribe to the FLEET stream with the redelivery demo consumer
        PullSubscribeOptions pso = PullSubscribeOptions.bind("FLEET", "redelivery-demo-consumer");
        // create the subscription
        JetStreamSubscription sub = js.subscribe("fleet.>", pso);

        ObjectMapper mapper = new ObjectMapper();
        EventRepository repo = new EventRepository(Config.JDBC_URL, Config.JDBC_USER, Config.JDBC_PASSWORD);

        System.out.println("Redelivery demo consumer is set up and ready to receive messages.");
        System.out.println("Fetching a message but not acknowledging it to demonstrate redelivery.");
        List<Message> first = sub.fetch(1, Duration.ofSeconds(5)); // fetch a single message without acknowledging it
        if (first.isEmpty()) {
            System.out.println("No messages received.");
            return;
        }
        Message msg = first.get(0);
        String subject = msg.getSubject();
        String data = new String(msg.getData());
        String deliveryCount = String.valueOf(msg.metaData().deliveredCount());
        String msgId = msg.getHeaders().getFirst("Nats-Msg-Id"); // get the message ID from the headers
        String[] parts = subject.split("\\.");
        String route = parts[1]; // the route is the second part of the subject
        System.out.println("Received message: " + data);
        System.out.println("Delivery count: " + deliveryCount);
        if (subject.endsWith(".telemetry")) {
            TelemetryEvent telemetryEvent = mapper.readValue(data, TelemetryEvent.class);
            repo.upsertTelemetry(msgId, route, telemetryEvent);
        }
        if (subject.endsWith(".disengagement")) {
            DisengagementEvent disengagementEvent = mapper.readValue(data, DisengagementEvent.class);
            repo.upsertDisengagement(msgId, route, disengagementEvent);
        }
        System.out.println("Sleeping past the ack wait duration to demonstrate JetStream redelivery.");
        Thread.sleep(4000); // longer than the 3s ack wait

        System.out.println(
                "Finished sleeping. Fetching again - this should be the same message, redelivered by JetStream.");
        List<Message> redelivered = sub.fetch(1, Duration.ofSeconds(5));
        if (redelivered.isEmpty()) {
            System.out.println("No redelivered message received.");
            return; // exit the method if no redelivered message is received
        }
        Message redeliveredMsg = redelivered.get(0);
        String subjectRedelivered = redeliveredMsg.getSubject();
        String dataRedelivered = new String(redeliveredMsg.getData());
        String redeliveredMsgId = redeliveredMsg.getHeaders().getFirst("Nats-Msg-Id");
        String[] partsRedelivered = subjectRedelivered.split("\\.");
        String routeRedelivered = partsRedelivered[1]; // the route is the second part of the subject
        String deliveryCountRedelivered = String.valueOf(redeliveredMsg.metaData().deliveredCount());
        System.out.println("Redelivered message: " + dataRedelivered);
        System.out.println("Delivery count: " + deliveryCountRedelivered);
        if (subjectRedelivered.endsWith(".telemetry")) {
            TelemetryEvent telemetryEventRedelivered = mapper.readValue(dataRedelivered, TelemetryEvent.class);
            repo.upsertTelemetry(redeliveredMsgId, routeRedelivered, telemetryEventRedelivered);
            redeliveredMsg.ack(); // acknowledge the redelivered message
        }
        if (subjectRedelivered.endsWith(".disengagement")) {
            DisengagementEvent disengagementEventRedelivered = mapper.readValue(dataRedelivered,
                    DisengagementEvent.class);
            repo.upsertDisengagement(redeliveredMsgId, routeRedelivered, disengagementEventRedelivered);
            redeliveredMsg.ack(); // acknowledge the redelivered message
        }
        nc.close(); // close the NATS connection
    }
}

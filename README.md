# CAV Fleet Telemetry Pipeline

A small NATS JetStream event-ingestion pipeline (Java), built to get hands-on
with durable pull consumers, idempotent writes, and dead-letter handling -
patterns used in message-driven architectures generally, not specific to
this toy domain.

**Status: core pipeline working and verified end to end.** Telemetry and
disengagement ingestion, idempotent writes, dead-letter handling, and
redelivery-driven idempotency have all been proven against real running
data, not just reasoned about. Not yet done: a read API, the aggregate
analytics job, and some smaller polish items - see "What's left" below.

## What it does

A simulated fleet of autonomous vehicles publishes two kinds of events to a
NATS JetStream stream:

- `fleet.<route>.<vehicle_id>.telemetry` - speed, lat/lon
- `fleet.<route>.<vehicle_id>.disengagement` - safety events (autonomy
  handing control back to a human), with a realistic distribution of causes

A Java consumer durably pulls these off the stream, validates and writes
them idempotently to Postgres, and routes anything malformed to a
dead-letter table instead of either crashing, looping forever, or silently
inserting bad data as if it were valid.

## Architecture

```
publisher.py --> NATS JetStream (FLEET stream) --> JetStreamIngestConsumer
                                                          |
                                    parse --> validate --> write
                                                          |
                              success: Postgres (telemetry_events /
                                                  disengagement_events)
                              failure (bad data):    dead_letters
                              failure (DB issue):    nak() + retry
```

The consumer creates the `FLEET` stream itself on first run if it's
missing - no manual NATS CLI setup step required, even after a full
`docker compose down -v`.

## Why these design choices

- **Pull consumers, not push** - the client controls backpressure by
  fetching batches on demand, rather than the server pushing faster than
  the consumer can handle.
- **At-least-once delivery + idempotent writes, not exactly-once** -
  JetStream redelivers if a consumer crashes before acking. Every write is
  `ON CONFLICT (msg_id) DO NOTHING`, so redelivery can never create a
  duplicate row. Proven with a standalone demo (`RedeliveryDemo.java`) that
  forces a real JetStream redelivery via a short `ackWait` and shows the
  second write is a genuine no-op, not just a manual double-insert test.
- **Dead-letter split by failure type** - a message with bad JSON or a
  missing required field will never succeed on retry, so it's written to
  `dead_letters` and `term()`'d immediately. A database connection issue
  might resolve itself, so those are `nak()`'d for redelivery instead of
  discarded.
- **`Double` (boxed) over `double` (primitive) for required numeric
  fields** - a primitive can't be `null`, so a missing JSON field would
  otherwise silently become `0.0` instead of a detectable, catchable error.

## Running it locally

```bash
docker compose up -d                            # NATS + Postgres
cd simulator && python publisher.py              # start generating events
cd ingestion-service && mvn compile exec:java    # start the consumer
```

## Proving it works

```sql
-- real data landing
SELECT count(*) FROM telemetry_events;

-- malformed messages correctly dead-lettered, not silently corrupted
SELECT error, count(*) FROM dead_letters GROUP BY error;

-- confirms no malformed data leaked through as fake-valid zero-speed rows
SELECT count(*) FROM telemetry_events WHERE speed_kph = 0;
```

Redelivery + idempotency demo (no manual kill/restart needed):

```bash
mvn compile exec:java -Dexec.mainClass="com.sunshine.cavfleet.RedeliveryDemo"
```

## What's left

- A small read API over the Postgres tables
- A disengagements-per-100km aggregate (the actual "so what" of the data)
- Connection pooling, graceful shutdown, null-`msgId` fallback to stream sequence
- Only the "missing required field" dead-letter path has fired against real
  traffic so far; unparseable-JSON and unrecognized-subject paths are built
  and reasoned through but unverified against real messages

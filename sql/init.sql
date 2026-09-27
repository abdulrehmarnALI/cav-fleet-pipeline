CREATE TABLE
    telemetry_events (
        msg_id      TEXT PRIMARY KEY,
        route       TEXT NOT NULL,
        vehicle_id  TEXT NOT NULL,
        speed_kph   NUMERIC,
        lat         NUMERIC,
        lon         NUMERIC,
        occurred_at TIMESTAMPTZ NOT NULL,
        ingested_at TIMESTAMPTZ NOT NULL DEFAULT now ()
    );

CREATE TABLE
    disengagement_events (
        msg_id      TEXT PRIMARY KEY,
        route       TEXT NOT NULL,
        vehicle_id  TEXT NOT NULL,
        reason      TEXT NOT NULL,
        occurred_at TIMESTAMPTZ NOT NULL,
        ingested_at TIMESTAMPTZ NOT NULL DEFAULT now ()
    );

CREATE TABLE
    dead_letters (
        id          BIGSERIAL PRIMARY KEY,
        msg_id      TEXT,
        subject     TEXT NOT NULL,
        raw_payload TEXT NOT NULL,
        error       TEXT NOT NULL,
        route       TEXT NOT NULL,
        ingested_at TIMESTAMPTZ NOT NULL DEFAULT now ()
    );
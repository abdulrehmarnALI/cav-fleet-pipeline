CREATE TABLE telemetry_events (
    msg_id TEXT PRIMARY KEY,
    route TEXT NOT NULL,
    vehicle_id TEXT NOT NULL,
    speed_kph NUMERIC,
    lat NUMERIC,
    lon NUMERIC,
    occured_at TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE disengagement_events (
    msg_id TEXT PRIMARY KEY,
    route TEXT NOT NULL,
    vehicle_id TEXT NOT NULL,
    reason TEXT NOT NULL,
    occured_at TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE dead_letters (
    msg_id TEXT PRIMARY KEY,
    route TEXT NOT NULL,
    vehicle_id TEXT NOT NULL,
    reason TEXT NOT NULL,
    occured_at TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
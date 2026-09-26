

import asyncio
import json
import random
import time
import uuid

import nats


ROUTES = {
    "route1_motorway": {"vehicles": ["car1", "car2"], "speed_range": (85, 115)},
    "route2_urban": {"vehicles": ["car3", "car4"], "speed_range": (30, 50)},
    "route3_suburban": {"vehicles": ["car5", "car6"], "speed_range": (50, 70)}
}

DISENGAGEMENT_REASONS_WEIGHTED =  [
    ("edge_case_pedestrian", 0.35),
    ("lane_ambiguity", 0.25),
    ("construction_zone", 0.15),
    ("weather_conditions", 0.12),
    ("sensor_fault", 0.07),
    ("manual_override", 0.06),
]

def weighted_choice(choices):
    reasons, weights = zip(*choices)
    return random.choices(reasons, weights=weights, k=1)[0]

# rough starting coordingates per rroute (fictional, just needs to look like real drift)
ROUTE_STARTING_COORDINATES = {
    "route1_motorway": (52.4862, -1.8904),
    "route2_urban": (52.4895, -1.8985),
    "route3_suburban": (52.4920, -1.9020)
}

vehicle_positions = {}

def next_vehicle_position(route, vehicle_id):
    key = (route, vehicle_id)
    if key not in vehicle_positions:
        vehicle_positions[key] = ROUTE_STARTING_COORDINATES[route]
    lat, lon = vehicle_positions[key]
    # simulate some random drift
    lat += (random.random() - 0.5) * 0.0001
    lon += (random.random() - 0.5) * 0.0001
    vehicle_positions[key] = (lat, lon)
    return vehicle_positions[key]

async def publish_telemetry(js, route, vehicle_id, speed_range):
    lat, lon = next_vehicle_position(route, vehicle_id)
    payload = {
        "vehicle_id": vehicle_id,
        "speed_kph": round(random.uniform(*speed_range)),
        "lat": round(lat, 6),
        "lon": round(lon, 6),
        "occurred_at": round(time.time(), 6)
    }
    subject = f"fleet.{route}.{vehicle_id}.telemetry"
    msg_id = str(uuid.uuid4())
    headers = {"Nats-Msg-Id": msg_id}

    await js.publish(subject, json.dumps(payload).encode(), headers=headers)
    print(f"telemetry  {subject}  speed={payload['speed_kph']}kph")

    # ~8% of the time, resend the exact same message/id to simulate duplicate delivery / network retry
    if random.random() < 0.08:
        await js.publish(subject, json.dumps(payload).encode(), headers=headers)
        print(f"Resent duplicate message for {subject} with msg_id {msg_id}")

async def publish_disengagement(js, route, vehicle_id):
    reason = weighted_choice(DISENGAGEMENT_REASONS_WEIGHTED)
    payload = {
        "vehicle_id": vehicle_id,
        "reason": reason,
        "occurred_at": round(time.time(), 6)
    }
    subject = f"fleet.{route}.{vehicle_id}.disengagement"
    msg_id = str(uuid.uuid4())
    headers = {"Nats-Msg-Id": msg_id}

    await js.publish(subject, json.dumps(payload).encode(), headers=headers)
    print(f"disengagement  {subject}  reason={reason}")

async def publish_malformed(js, route, vehicle_id):
    # missing 'speed_kph' field to simulate malformed message
    # this is what should up in the dead-letter table
    payload = {
        "vehicle_id": vehicle_id,
        "lat": round(next_vehicle_position(route, vehicle_id)[0], 6),
        "lon": round(next_vehicle_position(route, vehicle_id)[1], 6),
        "occurred_at": round(time.time(), 6)
    }
    subject = f"fleet.{route}.{vehicle_id}.telemetry"
    msg_id = str(uuid.uuid4())
    headers = {"Nats-Msg-Id": msg_id}

    await js.publish(subject, json.dumps(payload).encode(), headers=headers)
    print(f"!! malformed (missing speed_kph)  {subject}")

async def main():
    nc = await nats.connect("nats://localhost:4222")
    js = nc.jetstream()

    try:
        while True:
            route, cfg = random.choice(list(ROUTES.items()))
            vehicle_id = random.choice(cfg["vehicles"])

            roll = random.random()
            if roll < 0.82:
                await publish_telemetry(js, route, vehicle_id, cfg["speed_range"])
            elif roll < 0.96:
                await publish_disengagement(js, route, vehicle_id)
            else:
                await publish_malformed(js, route, vehicle_id)

            await asyncio.sleep(0.3)  # small delay to prevent overwhelming the NATS server
    except KeyboardInterrupt:
        pass
    finally:
        await nc.drain()
        await nc.close()

asyncio.run(main())
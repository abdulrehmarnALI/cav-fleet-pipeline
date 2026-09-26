import asyncio, json, time, uuid
import nats

async def main():
    nc = await nats.connect("nats://localhost:4222")
    js = nc.jetstream()

    for i in range(5):
        payload = {
            "resident_placeholder": "ignore",  # will replace once full schema is set
            "value": i,
            "occurred_at": time.time(),
        }
        subject = "fleet.route1.car1.telemetry"
        msg_id = str(uuid.uuid4())
        ack = await js.publish(subject, json.dumps(payload).encode(), headers={"Nats-Msg-Id": msg_id})
        print(f"published seq={ack.seq} to {subject}")

    await nc.close()

asyncio.run(main())
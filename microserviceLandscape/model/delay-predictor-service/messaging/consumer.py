import json
import pika
import pandas as pd
from collections import defaultdict

from messaging.training import start_training_for_routing_key, convert_keys_to_snake_case

batch_storage = defaultdict(list)

def start_consuming():
    try:
        connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
        channel = connection.channel()

        channel.exchange_declare(exchange='dataResponses', exchange_type='topic', durable=True, passive=True)

        queue_name = 'dataResponses.dataResponsesGroup'
        channel.queue_declare(queue=queue_name, durable=True, passive=True)
        channel.queue_bind(exchange='dataResponses', queue=queue_name, routing_key='#')

        print(f"🟢 Listening for responses on queue: {queue_name}")

        def on_message(ch, method, properties, body):
            try:
                message = json.loads(body)
            except json.JSONDecodeError as ex:
                print("JSON decode error:", ex)
                return

            routing_key = message.get("key")
            event_type = message.get("eventType")
            data = message.get("data", [])

            if not routing_key:
                print("Missing routing key — skipping.")
                return

            if event_type == "DATA_TRANSFER":
                print(f"Received batch for key {routing_key} with {len(data)} records.")
                batch_storage[routing_key].extend(data)

            elif event_type == "COMPLETE":
                print(f"COMPLETE event for key {routing_key}")
                records = batch_storage.pop(routing_key, [])
                if not records:
                    print("No records collected.")
                    return
                df = pd.DataFrame(convert_keys_to_snake_case(records))
                start_training_for_routing_key(df)

            else:
                print(f"ℹ️ Unknown eventType: {event_type}")

        channel.basic_consume(
            queue=queue_name,
            on_message_callback=on_message,
            auto_ack=True
        )

        channel.start_consuming()

    except Exception as e:
        print("Error in consumer:", e)
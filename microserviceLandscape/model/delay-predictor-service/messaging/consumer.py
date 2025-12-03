import json
import os
import re

import pika
import pandas as pd
from collections import defaultdict

from spring_config import ClientConfigurationBuilder
from spring_config.client import SpringConfigClient

from messaging.training import start_training_for_routing_key, convert_keys_to_snake_case

batch_storage = defaultdict(list)

ENV_VAR_PATTERN = re.compile(r'^\$\{([A-Z_][A-Z0-9_]*)\}$')

def resolve_config_property(flat_key, config):
    keys = flat_key.lower().split('_')

    current = lower_keys(config)
    for key in keys:
        if not isinstance(current, dict) or key not in current:
            return ""
        current = current[key]

    if isinstance(current, str):
        match = ENV_VAR_PATTERN.match(current)
        if match:
            return os.getenv(match.group(1), "")
    return current


def lower_keys(d):
    if isinstance(d, dict):
        return {k.lower(): lower_keys(v) for k, v in d.items()}
    elif isinstance(d, list):
        return [lower_keys(item) for item in d]
    else:
        return d

def start_consuming():
    try:
        print("Getting configurations from Spring Cloud config...")
        config = (
            ClientConfigurationBuilder()
            .app_name("delay-predictor-service")
            .address(os.getenv("CONFIG_SERVER_URL", "http://localhost:8888"))
            .profile(os.getenv("CONFIG_PROFILE", "default"))
            .authentication((os.getenv('CONFIG_USERNAME', 'admin'), os.getenv('CONFIG_PASSWORD', 'admin')))
            .build()
        )
        config_client = SpringConfigClient(config)
        config = config_client.get_config()


        connection = pika.BlockingConnection(pika.ConnectionParameters(host=resolve_config_property('RABBITMQ_HOST', config)))
        channel = connection.channel()

        channel.exchange_declare(exchange='dataResponses', exchange_type='topic', durable=True, passive=True)

        queue_name = 'dataResponses.dataResponsesGroup'
        channel.queue_declare(queue=queue_name, durable=True, passive=False)
        channel.queue_bind(exchange='dataResponses', queue=queue_name, routing_key='#')

        print(f"Listening for responses on queue: {queue_name}")

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
                print(f"Unknown eventType: {event_type}")

        channel.basic_consume(
            queue=queue_name,
            on_message_callback=on_message,
            auto_ack=True
        )

        channel.start_consuming()

    except Exception as e:
        print("Error in consumer:", e)
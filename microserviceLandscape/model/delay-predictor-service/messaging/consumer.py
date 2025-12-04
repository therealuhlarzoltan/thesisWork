import json
import os
import re
import time

import pika
import pandas as pd
from collections import defaultdict

from spring_config import ClientConfigurationBuilder
from spring_config.client import SpringConfigClient

from messaging.training import start_training_for_routing_key

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


def show_mem(df, label):
    mem_gb = df.memory_usage(deep=True).sum() / 1024**3
    print(f"{label}: {mem_gb:.2f} GB")


RECONNECT_DELAY_SEC = 5

def start_consuming():
    while True:
        connection = None
        stop = False

        try:
            print("Getting configurations from Spring Cloud config...")
            config = (
                ClientConfigurationBuilder()
                .app_name("delay-predictor-service")
                .address(os.getenv("CONFIG_SERVER_URL", "http://localhost:8888"))
                .profile(os.getenv("CONFIG_PROFILE", "default"))
                .authentication(
                    (
                        os.getenv("CONFIG_USERNAME", "admin"),
                        os.getenv("CONFIG_PASSWORD", "admin"),
                    )
                )
                .build()
            )
            config_client = SpringConfigClient(config)
            config = config_client.get_config()

            rabbit_host = resolve_config_property("RABBITMQ_HOST", config)
            print(f"Connecting to RabbitMQ at {rabbit_host}...")

            connection = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=rabbit_host,
                    heartbeat=60,
                    blocked_connection_timeout=300,
                )
            )
            channel = connection.channel()

            channel.exchange_declare(
                exchange='dataResponses',
                exchange_type='topic',
                durable=True,
                passive=True,
            )

            queue_name = 'dataResponses.dataResponsesGroup'
            channel.queue_declare(
                queue=queue_name,
                durable=True,
                passive=True,
            )
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
                    df = pd.DataFrame(records)
                    show_mem(df, label="Received DataFrame's size")
                    print("Received number of records: ", len(df))
                    start_training_for_routing_key(df)

                else:
                    print(f"Unknown eventType: {event_type}")

            channel.basic_consume(
                queue=queue_name,
                on_message_callback=on_message,
                auto_ack=True
            )

            channel.start_consuming()

        except KeyboardInterrupt:
            print("Consumer interrupted by application shutdown...")
            stop = True
        except Exception as e:
            print(f"Error in consumer: {e}")
        finally:
            if connection is not None and not connection.is_closed:
                try:
                    connection.close()
                except Exception:
                    pass

        if stop:
            break

        print(f"Consumer reconnecting in {RECONNECT_DELAY_SEC} seconds...")
        time.sleep(RECONNECT_DELAY_SEC)
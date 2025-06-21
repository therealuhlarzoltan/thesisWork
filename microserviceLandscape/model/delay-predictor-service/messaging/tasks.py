import json
import os
import re
import uuid
import pika
from celery import shared_task
from spring_config import ClientConfigurationBuilder
from spring_config.client import SpringConfigClient

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

@shared_task
def publish_initial_batch_request():
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

        channel.exchange_declare(exchange='dataRequests', exchange_type='direct', passive=True)

        body = {
            "type": "DataTransferEvent",
            "key": str(uuid.uuid4()),
            "eventType": "REQUEST",
            "data": []
        }

        props = pika.BasicProperties(
            content_type='application/json',
            headers={
                "__TypeId__": "hu.uni_obuda.thesis.railways.data.event.DataTransferEvent",
                "__ContentTypeId__": "hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.DelayRecord"
            },
            delivery_mode=2
        )

        channel.basic_publish(exchange='dataRequests', routing_key='', body=json.dumps(body), properties=props)
        print(f"▶️ Sent DataTransferEvent<List<DelayRecord>> to dataRequests")
        connection.close()
    except Exception as e:
        print("Failed to publish batch request:", e)

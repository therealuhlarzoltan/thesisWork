import json
import uuid
import pika
from celery import shared_task

@shared_task
def publish_initial_batch_request():
    try:
        connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
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

from django.apps import AppConfig

import os
import threading
import json
import uuid
import time
import pika

class MessagingConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'messaging'

    def ready(self):

        if os.environ.get('RUN_MAIN') != 'true':
            return

        t1 = threading.Thread(target=self._publish_initial_batch_request, daemon=True)
        t1.start()

        t2 = threading.Thread(target=self._consume_batch_responses, daemon=True)
        t2.start()

    def _publish_initial_batch_request(self):
        try:
            connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
            channel = connection.channel()

            channel.exchange_declare(exchange='dataRequests', exchange_type='direct', passive=True)

            body = {
                "type": "DataTransferEvent",  # <-- must match @JsonSubTypes name
                "key": "myUniqueRoutingKey123",  # <-- will be passed into getBatches(...)
                "eventType": "REQUEST",  # <-- enum name (REQUEST/DATA_TRANSFER/COMPLETE)
                "data": [
                    {
                        "stationCode": "BUD",
                        "stationLatitude": 47.497913,
                        "stationLongitude": 19.040236,
                        "trainNumber": "4478",
                        "scheduledArrival": "2025-06-03T11:55:00",
                        "scheduledDeparture": "2025-06-03T12:00:00",
                        "actualArrival": "2025-06-03T12:05:00",
                        "actualDeparture": "2025-06-03T12:07:00",
                        "arrivalDelay": 5,
                        "departureDelay": 7,
                        "date": "2025-06-03",
                        "weather": {
                            "time": "2025-06-03T12:00:00",
                            "address": "Budapest, Hungary",
                            "latitude": 47.497913,
                            "longitude": 19.040236,
                            "temperature": 22.5,
                            "relativeHumidity": 0.65,
                            "windSpeedAt10m": 3.2,
                            "windSpeedAt80m": 5.8,
                            "isSnowing": False,
                            "snowFall": 0.0,
                            "snowDepth": 0.0,
                            "isRaining": False,
                            "precipitation": 0.0,
                            "rain": 0.0,
                            "showers": 0.0,
                            "visibilityInMeters": 10000,
                            "cloudCoverPercentage": 20
                        }
                    },
                    {
                        "stationCode": "PST",
                        "stationLatitude": 46.069945,
                        "stationLongitude": 18.232265,
                        "trainNumber": "1234",
                        "scheduledArrival": "2025-06-03T12:10:00",
                        "scheduledDeparture": "2025-06-03T12:15:00",
                        "actualArrival": "2025-06-03T12:20:00",
                        "actualDeparture": "2025-06-03T12:22:00",
                        "arrivalDelay": 10,
                        "departureDelay": 7,
                        "date": "2025-06-03",
                        "weather": {
                            "time": "2025-06-03T12:15:00",
                            "address": "Pécs, Hungary",
                            "latitude": 46.069945,
                            "longitude": 18.232265,
                            "temperature": 18.2,
                            "relativeHumidity": 0.72,
                            "windSpeedAt10m": 2.5,
                            "windSpeedAt80m": 4.1,
                            "isSnowing": False,
                            "snowFall": 0.0,
                            "snowDepth": 0.0,
                            "isRaining": True,
                            "precipitation": 0.3,
                            "rain": 0.3,
                            "showers": 0.0,
                            "visibilityInMeters": 8000,
                            "cloudCoverPercentage": 60
                        }
                    }
                ]
            }

            props = pika.BasicProperties(
                content_type='application/json',
                headers={
                    "__TypeId__": "hu.uni_obuda.thesis.railways.data.event.DataTransferEvent",
                    "__ContentTypeId__": "hu.uni_obuda.thesis.railways.data.delaydatacollector.dto.DelayRecord"
                },
                delivery_mode=2
            )

            channel.basic_publish(
                exchange='dataRequests',
                routing_key='',  # Spring is bound to "" by default
                body=json.dumps(body),
                properties=props
            )

            print(f"▶️ Sent DataTransferEvent<DelayRecord> to dataRequests")
            connection.close()
        except Exception as e:
            # If RabbitMQ isn't up yet, we might fail. You could retry or just log.
            print("❌ Failed to publish batch request on startup:", e)

    def _consume_batch_responses(self):
        """
        Connect to RabbitMQ, passively declare the existing queue "dataResponses.auditGroup",
        then consume messages. Spring has already created and bound this queue.
        """
        try:
            connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
            channel = connection.channel()


            # Passive‐declare the queue that Spring’s binder already created
            #channel.queue_declare(
            #    queue='dataResponses.auditGroup',
            #    passive=True,  # do not create it if it doesn't exist
            #    durable=True  # must match Spring’s durability
            #)

            channel.exchange_declare(exchange='dataResponses', exchange_type='topic', durable=True)

            # Create a temporary queue and bind it to the exchange
            result = channel.queue_declare(queue='', exclusive=True, durable=True, passive=True)
            queue_name = result.method.queue
            channel.queue_bind(exchange='dataResponses', queue=queue_name, routing_key='#')

            print(f"🟢 Listening for responses on 'dataResponses.{queue_name}'…")

            def on_message(ch, method, properties, body):
                try:
                    message = json.loads(body)
                except json.JSONDecodeError as ex:
                    print("❌ JSON parse error:", ex, "raw body:", body)
                    return

                print("📬 Received data-response on dataResponses.auditGroup:")
                print(json.dumps(message, indent=2, sort_keys=True))
                print("―――――――――――――――――――――――――――――")

            channel.basic_consume(
                queue='dataResponses.auditGroup',
                on_message_callback=on_message,
                auto_ack=True
            )
            channel.start_consuming()

        except Exception as e:
            print("❌ Failed to start consuming from dataResponses.auditGroup:", e)

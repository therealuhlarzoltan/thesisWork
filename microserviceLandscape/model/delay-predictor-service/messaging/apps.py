import os
import json
import re
import threading
import time

import pika
import pandas as pd
import uuid

from collections import defaultdict
from django.apps import AppConfig
from sklearn.model_selection import train_test_split


# Avoid importing Django models or app-dependent pipelines at module level
# Delay import inside methods
class MessagingConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'messaging'

    def ready(self):
        try:
            if os.environ.get("RUN_MAIN") != "true":
                return

            self.batch_storage = defaultdict(list)

            t1 = threading.Thread(target=self._publish_initial_batch_request, daemon=True)
            t1.start()

            t2 = threading.Thread(target=self._consume_batch_responses, daemon=False)  # non-daemon!
            t2.start()

            threading.Thread(target=lambda: threading.Event().wait(), daemon=True).start()
        except Exception as e:
            import traceback
            print("❌ Messaging app failed to start:", str(e))
            print(traceback.format_exc())

    def _publish_initial_batch_request(self):
        try:
            connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
            channel = connection.channel()

            channel.exchange_declare(exchange='dataRequests', exchange_type='direct', passive=True)

            body = {
                "type": "DataTransferEvent",
                "key":  str(uuid.uuid4()),
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


            #channel.basic_publish(
            #    exchange='dataRequests',
            #    routing_key='',
            #    body=json.dumps(body),
            #    properties=props
            #)

            print(f"▶️ Sent DataTransferEvent<List<DelayRecord>> to dataRequests")
            connection.close()

        except Exception as e:
            print("❌ Failed to publish batch request on startup:", e)

    def _consume_batch_responses(self):
        try:
            connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
            channel = connection.channel()

            channel.exchange_declare(exchange='dataResponses', exchange_type='topic', durable=True, passive=True)

            queue_name = 'dataResponses.dataResponsesGroup'
            queue_result = channel.queue_declare(queue=queue_name, durable=True, passive=True)
            print(f"✅ Queue declared: {queue_result.method.queue}")
            channel.queue_bind(exchange='dataResponses', queue=queue_name, routing_key='#')
            print("✅ Queue bound successfully")

            print(f"🟢 Listening for responses on {queue_name}…")

            def on_message(ch, method, properties, body):
                try:
                    message = json.loads(body)
                except json.JSONDecodeError as ex:
                    print("❌ JSON parse error:", ex, "raw body:", body)
                    return

                event_type = message.get("eventType")
                routing_key = message.get("key")
                data = message.get("data", [])

                if not routing_key:
                    print("⚠️ Missing routing key — skipping message.")
                    return

                if event_type == "DATA_TRANSFER":
                    print(f"📦 Received batch for key {routing_key} with {len(data)} records.")
                    self.batch_storage[routing_key].extend(data)

                elif event_type == "COMPLETE":
                    print(f"✅ COMPLETE event received for key {routing_key}. Training model…")
                    records = self.batch_storage.pop(routing_key, [])
                    if not records:
                        print("⚠️ No data was collected for this key.")
                        return

                    try:
                        df = pd.DataFrame(self.convert_keys_to_snake_case(records))
                        self._start_training_arrival_in_background(df)
                        self._start_training_departure_in_background(df)
                    except Exception as e:
                        print("❌ Failed to train or save model:", e)

                else:
                    print(f"ℹ️ Unknown eventType: {event_type}")

            channel.basic_consume(
                queue=queue_name,
                on_message_callback=on_message,
                auto_ack=True
            )
            channel.start_consuming()

        except Exception as e:
            print(f"❌ Failed to start consuming from {queue_name}:", e)

    def _train_arrival_model_with_logging(self, data_frame):
        try:
            from model.pipelines.predict import arrival_delay_pipeline
            from model.utils import save_prediction_model

            print("🔧 Cleaning...")
            df_cleaned = arrival_delay_pipeline.named_steps['cleaning'].fit_transform(data_frame)
            y = df_cleaned.pop('arrival_delay')
            print("🔄 Preprocessing...")
            df_preprocessed = arrival_delay_pipeline.named_steps['preprocessing'].fit_transform(df_cleaned)

            X = df_preprocessed
            X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

            print("🚀 Training...")
            arrival_delay_pipeline.named_steps['predicting'].named_steps['xgb'].fit(
                X_train, y_train, eval_set=[(X_test, y_test)]
            )

            y_pred = arrival_delay_pipeline.named_steps['predicting'].named_steps['xgb'].predict(X_test)

            from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
            import numpy as np
            import pandas as pd

            mae = mean_absolute_error(y_test, y_pred)
            mse = mean_squared_error(y_test, y_pred)
            rmse = np.sqrt(mse)
            r2 = r2_score(y_test, y_pred)

            metrics_df = pd.DataFrame({
                'MAE': [mae],
                'MSE': [mse],
                'RMSE': [rmse],
                'R2': [r2],
            })
            print(metrics_df)
            save_prediction_model('arrival', arrival_delay_pipeline, metrics_df)
            print("✅ Arrival delay model training complete.")

        except Exception as e:
            import traceback
            print("❌ Training failed:", str(e))
            print(traceback.format_exc())


    def _start_training_arrival_in_background(self, df):
        threading.Thread(
            target=self._train_arrival_model_with_logging,
            args=(df,),
            daemon=True
        ).start()

    def _train_departure_model_with_logging(self, data_frame):
        try:
            from model.pipelines.predict import departure_delay_pipeline
            from model.utils import save_prediction_model

            print("🔧 Cleaning...")
            df_cleaned = departure_delay_pipeline.named_steps['cleaning'].fit_transform(data_frame)
            y = df_cleaned.pop('departure_delay')
            print("🔄 Preprocessing...")
            df_preprocessed = departure_delay_pipeline.named_steps['preprocessing'].fit_transform(df_cleaned)

            X = df_preprocessed
            X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

            print("🚀 Training...")
            departure_delay_pipeline.named_steps['predicting'].named_steps['xgb'].fit(
                X_train, y_train, eval_set=[(X_test, y_test)]
            )

            y_pred = departure_delay_pipeline.named_steps['predicting'].named_steps['xgb'].predict(X_test)

            from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
            import numpy as np
            import pandas as pd

            mae = mean_absolute_error(y_test, y_pred)
            mse = mean_squared_error(y_test, y_pred)
            rmse = np.sqrt(mse)
            r2 = r2_score(y_test, y_pred)

            metrics_df = pd.DataFrame({
                'MAE': [mae],
                'MSE': [mse],
                'RMSE': [rmse],
                'R2': [r2],
            })
            print(metrics_df)
            save_prediction_model('departure', departure_delay_pipeline, metrics_df)
            print("✅ Departure delay model training complete.")

        except Exception as e:
            import traceback
            print("❌ Training failed:", str(e))
            print(traceback.format_exc())


    def _start_training_departure_in_background(self, df):
        threading.Thread(
            target=self._train_departure_model_with_logging,
            args=(df,),
            daemon=True
        ).start()

    def camel_to_snake(self, name):
        s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
        return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

    def convert_keys_to_snake_case(self, obj):
        if isinstance(obj, dict):
            return {self.camel_to_snake(k): self.convert_keys_to_snake_case(v) for k, v in obj.items()}
        elif isinstance(obj, list):
            return [self.convert_keys_to_snake_case(item) for item in obj]
        else:
            return obj


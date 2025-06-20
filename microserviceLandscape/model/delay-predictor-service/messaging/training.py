import threading
import re

def convert_keys_to_snake_case(obj):
    def camel_to_snake(name):
        s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
        return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

    if isinstance(obj, dict):
        return {camel_to_snake(k): convert_keys_to_snake_case(v) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [convert_keys_to_snake_case(item) for item in obj]
    return obj


def start_training_for_routing_key(df):
    threading.Thread(target=_train_arrival_model, args=(df,), daemon=False).start()
    threading.Thread(target=_train_departure_model, args=(df,), daemon=False).start()


def _train_arrival_model(df):
    try:
        from model.pipelines.predict import arrival_delay_pipeline
        from model.utils import save_prediction_model
        from sklearn.model_selection import train_test_split
        from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
        import numpy as np
        import pandas as pd

        df_cleaned = arrival_delay_pipeline.named_steps['cleaning'].fit_transform(df)
        y = df_cleaned.pop('arrival_delay')
        X = arrival_delay_pipeline.named_steps['preprocessing'].fit_transform(df_cleaned)
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)

        arrival_delay_pipeline.named_steps['predicting'].named_steps['xgb'].fit(
            X_train, y_train, eval_set=[(X_test, y_test)]
        )

        y_pred = arrival_delay_pipeline.named_steps['predicting'].named_steps['xgb'].predict(X_test)
        metrics_df = pd.DataFrame({
            'MAE': [mean_absolute_error(y_test, y_pred)],
            'MSE': [mean_squared_error(y_test, y_pred)],
            'RMSE': [np.sqrt(mean_squared_error(y_test, y_pred))],
            'R2': [r2_score(y_test, y_pred)],
        })

        save_prediction_model('arrival', arrival_delay_pipeline, metrics_df)
        print("✅ Arrival model trained and saved.")
    except Exception as e:
        import traceback
        print("❌ Arrival training error:", e)
        print(traceback.format_exc())


def _train_departure_model(df):
    try:
        from model.pipelines.predict import departure_delay_pipeline
        from model.utils import save_prediction_model
        from sklearn.model_selection import train_test_split
        from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
        import numpy as np
        import pandas as pd

        df_cleaned = departure_delay_pipeline.named_steps['cleaning'].fit_transform(df)
        y = df_cleaned.pop('departure_delay')
        X = departure_delay_pipeline.named_steps['preprocessing'].fit_transform(df_cleaned)
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)

        departure_delay_pipeline.named_steps['predicting'].named_steps['xgb'].fit(
            X_train, y_train, eval_set=[(X_test, y_test)]
        )

        y_pred = departure_delay_pipeline.named_steps['predicting'].named_steps['xgb'].predict(X_test)
        metrics_df = pd.DataFrame({
            'MAE': [mean_absolute_error(y_test, y_pred)],
            'MSE': [mean_squared_error(y_test, y_pred)],
            'RMSE': [np.sqrt(mean_squared_error(y_test, y_pred))],
            'R2': [r2_score(y_test, y_pred)],
        })

        save_prediction_model('departure', departure_delay_pipeline, metrics_df)
        print("✅ Departure model trained and saved.")
    except Exception as e:
        import traceback
        print("❌ Departure training error:", e)
        print(traceback.format_exc())
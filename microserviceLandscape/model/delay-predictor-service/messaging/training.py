import threading
import re
from cluster_making import apply_cluster_quantile_mask

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

        cleaned = arrival_delay_pipeline.named_steps["cleaning"].fit_transform(df)
        if "arrival_delay" not in cleaned.columns:
            raise ValueError("arrival_delay column missing after cleaning")

        cleaned_masked = apply_cluster_quantile_mask(
            cleaned,
            target_col="arrival_delay",
            cluster_col="line_service_cluster",
            upper_q=0.98,
            min_delay=-5.0,
        )

        y = cleaned_masked["arrival_delay"].copy()
        X = cleaned_masked.drop(columns=["arrival_delay"])

        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42
        )

        preproc = arrival_delay_pipeline.named_steps["preprocessing"]
        X_train_proc = preproc.fit_transform(X_train)
        X_test_proc = preproc.transform(X_test)

        xgb = arrival_delay_pipeline.named_steps["predicting"].named_steps["xgb"]
        xgb.fit(
            X_train_proc,
            y_train,
            eval_set=[(X_train_proc, y_train), (X_test_proc, y_test)],
            verbose=True,
        )

        y_pred = xgb.predict(X_test_proc)
        mae = mean_absolute_error(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)
        rmse = np.sqrt(mse)
        r2 = r2_score(y_test, y_pred)

        metrics_df = pd.DataFrame([{
            "MAE": mae,
            "MSE": mse,
            "RMSE": rmse,
            "R2": r2,
        }])

        from model.utils import save_prediction_model
        save_prediction_model("arrival", arrival_delay_pipeline, metrics_df)

        print("Arrival delay pipeline created and saved")
    except Exception as e:
        import traceback
        print("Arrival training error:", e)
        print(traceback.format_exc())


def _train_departure_model(df):
    try:
        from model.pipelines.predict import departure_delay_pipeline
        from model.utils import save_prediction_model
        from sklearn.model_selection import train_test_split
        from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
        import numpy as np
        import pandas as pd

        cleaned = departure_delay_pipeline.named_steps["cleaning"].fit_transform(df)
        if "departure_delay" not in cleaned.columns:
            raise ValueError("departure_delay column missing after cleaning")

        cleaned_masked = apply_cluster_quantile_mask(
            cleaned,
            target_col="departure_delay",
            cluster_col="line_service_cluster",
            upper_q=0.98,
            min_delay=-5.0,
        )

        y = cleaned_masked["departure_delay"].copy()
        X = cleaned_masked.drop(columns=["departure_delay"])

        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42
        )

        preproc = departure_delay_pipeline.named_steps["preprocessing"]
        X_train_proc = preproc.fit_transform(X_train)
        X_test_proc = preproc.transform(X_test)

        xgb = departure_delay_pipeline.named_steps["predicting"].named_steps["xgb"]
        xgb.fit(
            X_train_proc,
            y_train,
            eval_set=[(X_train_proc, y_train), (X_test_proc, y_test)],
            verbose=True,
        )

        y_pred = xgb.predict(X_test_proc)
        mae = mean_absolute_error(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)
        rmse = np.sqrt(mse)
        r2 = r2_score(y_test, y_pred)

        metrics_df = pd.DataFrame([{
            "MAE": mae,
            "MSE": mse,
            "RMSE": rmse,
            "R2": r2,
        }])

        save_prediction_model("departure", departure_delay_pipeline, metrics_df)
        print("Departure delay pipeline created and saved.")
    except Exception as e:
        import traceback
        print("Departure training error:", e)
        print(traceback.format_exc())
import pickle

from .models import DelayPredictionModel


def save_prediction_model(type, model, metrics):
    import pickle
    serialized_pipeline = pickle.dumps(model)

    mae = metrics.at[0, 'MAE']
    mse = metrics.at[0, 'MSE']
    rmse = metrics.at[0, 'RMSE']
    r2 = metrics.at[0, 'R2']

    print("Saving model...")
    obj = DelayPredictionModel.objects.create(
        delay_type=type,
        pipeline_binary=serialized_pipeline,
        mae=mae,
        mse=mse,
        rmse=rmse,
        r2=r2
    )
    print("Model saved.")
    return obj

def load_prediction_model(type, created_at):
    obj = DelayPredictionModel.objects.filter(
        delay_type=type, created_at=created_at).first()
    return pickle.loads(obj.pipeline_binary)


def load_latest_prediction_model():
    obj = DelayPredictionModel.objects.order_by('-created_at').first()
    return pickle.loads(obj.pipeline_binary)
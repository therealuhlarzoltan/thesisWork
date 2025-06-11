import pickle
from datetime import datetime

from .models import XGBRegressorDatabaseModel

def save_xgb_model(name, model):
    serialized_model = pickle.dumps(model)
    obj, created = XGBRegressorDatabaseModel.objects.update_or_create(
        name=name,
        defaults={'model_binary': serialized_model}
    )
    return obj


def save_xgb_regressor_model(model):
    serialized_model = pickle.dumps(model)
    obj, created = XGBRegressorDatabaseModel.objects.update_or_create(
        name="XGBRegressor:" + datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        defaults={'model_binary': serialized_model}
    )
    return obj


def load_xgb_model(name):
    obj = XGBRegressorDatabaseModel.objects.get(name=name)
    return pickle.loads(obj.model_binary)


def load_latest_xgb_regressor_model():
    obj = XGBRegressorDatabaseModel.objects.order_by('-created_at').first()
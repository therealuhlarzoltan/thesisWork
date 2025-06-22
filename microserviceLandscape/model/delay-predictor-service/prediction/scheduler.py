import pickle
from datetime import timedelta

from django.utils import timezone

from model.models import DelayPredictionModel
from prediction import model_cache


def reload_models():
    print("APScheduler: Reloading ML models from DB...")

    def load_model(category):
        two_weeks_ago = timezone.now() - timedelta(weeks=2)
        db_model = (
            DelayPredictionModel.objects
            .filter(delay_type=category, created_at__gte=two_weeks_ago)
            .order_by('rmse', '-r2')
            .first()
        )
        return pickle.loads(db_model.pipeline_binary) if db_model else None

    model_cache.arrival_model = load_model('arrival')
    model_cache.departure_model = load_model('departure')

    print("APScheduler: Models reloaded.")
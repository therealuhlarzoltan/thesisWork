import pickle
from datetime import timedelta
from django.utils import timezone
from model.models import DelayPredictionModel
from prediction import model_cache

def reload_models():
    print("APScheduler: Reloading ML models from DB...")

    def load_model(category):
        one_month_ago = timezone.now() - timedelta(weeks=1)
        recent_qs = (
            DelayPredictionModel.objects
            .filter(delay_type=category, created_at__gte=one_month_ago)
            .order_by('rmse', '-r2')
        )
        db_model = recent_qs.first()

        if db_model is None:
            fallback_qs = (
                DelayPredictionModel.objects
                .filter(delay_type=category)
                .order_by('-created_at')
                .filter(created_at__gte=timezone.now() - timedelta(weeks=1))
            )
            db_model = fallback_qs.first()
        return pickle.loads(db_model.pipeline_binary) if db_model else None

    model_cache.arrival_model = load_model('arrival')
    model_cache.departure_model = load_model('departure')

    print("APScheduler: Models reloaded.")
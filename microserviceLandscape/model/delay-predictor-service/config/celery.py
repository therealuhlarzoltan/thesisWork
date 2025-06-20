import os
from celery import Celery
from celery.schedules import crontab

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')

app = Celery('config')
app.config_from_object('django.conf:settings', namespace='CELERY')
app.autodiscover_tasks()

app.conf.beat_schedule = {
    'publish-batch-request-every-2-hours': {
        'task': 'messaging.tasks.publish_initial_batch_request',
        'schedule': crontab(minute=0, hour='*/2'),
    },
    'reload-delay-predictor-models-2-hours': {
        'task': 'prediction.tasks.reload_models',
        'schedule': crontab(minute=0, hour='*/2'),
    },
}
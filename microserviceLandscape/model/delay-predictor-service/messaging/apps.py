import atexit

from django.apps import AppConfig

class MessagingConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'messaging'

    def ready(self):
        import sys
        if 'runserver' not in sys.argv:
            return
        
        from apscheduler.schedulers.background import BackgroundScheduler
        from apscheduler.triggers.interval import IntervalTrigger
        from django_apscheduler.jobstores import DjangoJobStore
        from .scheduler import publish_initial_batch_request
        scheduler = BackgroundScheduler()
        scheduler.add_jobstore(DjangoJobStore(), "default")

        scheduler.add_job(
            publish_initial_batch_request,
            trigger=IntervalTrigger(hours=12),
            id="publish_initial_batch_request",
            name="Initiate data fetch every 12 hours",
            replace_existing=True,
        )

        scheduler.start()
        print("APScheduler started")

        atexit.register(lambda: scheduler.shutdown())


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
        print('Sending batch request...')
        scheduler = BackgroundScheduler()
        scheduler.add_jobstore(DjangoJobStore(), "default")

        scheduler.add_job(
            publish_initial_batch_request,
            trigger=IntervalTrigger(hours=2),
            id="publish_initial_batch_request",
            name="Initiate data fetch ever hour",
            replace_existing=True,
        )

        scheduler.start()
        print("APScheduler started")

        print("Running publish_initial_batch_request() immediately at startup")
        publish_initial_batch_request()

        atexit.register(lambda: scheduler.shutdown())


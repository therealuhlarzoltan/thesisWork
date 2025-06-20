import os
import socket
import atexit
import signal
import sys
from django.apps import AppConfig
import py_eureka_client.eureka_client as eureka_client

eureka_user = os.getenv('EUREKA_USERNAME', 'admin')
eureka_password = os.getenv('EUREKA_PASSWORD', 'admin')

class PredictionConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'prediction'

    def ready(self):
        # Avoid running during management commands like migrate or collectstatic
        import sys
        if 'runserver' not in sys.argv and 'celery' not in sys.argv:
            return

        hostname = socket.gethostname()
        ip = socket.gethostbyname(hostname)

        print('Registering app to eureka server...')
        eureka_client.init(
            eureka_server="localhost:8761",
            eureka_protocol="http",
            eureka_basic_auth_user=eureka_user,
            eureka_basic_auth_password=eureka_password,
            eureka_context="/eureka/",
            app_name="delay-predictor-service",
            instance_port=7000,
            instance_ip=ip,
            instance_host=hostname
        )

        # Graceful shutdown hooks
        def eureka_shutdown_handler(signum=None, frame=None):
            print("Shutting down and deregistering from Eureka...")
            eureka_client.stop()
            sys.exit(0)


        atexit.register(eureka_shutdown_handler)
        signal.signal(signal.SIGINT, eureka_shutdown_handler)
        signal.signal(signal.SIGTERM, eureka_shutdown_handler)

        from apscheduler.schedulers.background import BackgroundScheduler
        from apscheduler.triggers.interval import IntervalTrigger
        from django_apscheduler.jobstores import DjangoJobStore
        from prediction.scheduler import reload_models
        print('Loading models...')
        scheduler = BackgroundScheduler()
        scheduler.add_jobstore(DjangoJobStore(), "default")

        scheduler.add_job(
            reload_models,
            trigger=IntervalTrigger(hours=2),
            id="reload_models",
            name="Reload ML models from DB every 2 hours",
            replace_existing=True,
        )

        scheduler.start()
        print("📅 APScheduler started")

        print("🚀 Running reload_models() immediately at startup")
        reload_models()

        atexit.register(lambda: scheduler.shutdown())
import os
import re
import socket
import atexit
import signal
import sys
from django.apps import AppConfig
import py_eureka_client.eureka_client as eureka_client
from spring_config import ClientConfigurationBuilder
from spring_config.client import SpringConfigClient



CONFIG_USERNAME = os.getenv("CONFIG_USERNAME", "admin")
CONFIG_PASSWORD = os.getenv("CONFIG_PASSWORD", "admin")

ENV_VAR_PATTERN = re.compile(r'^\$\{([A-Z_][A-Z0-9_]*)\}$')

class PredictionConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'prediction'

    def ready(self):
        # Avoid running during management commands like migrate or collectstatic
        import sys
        if 'runserver' not in sys.argv and 'celery' not in sys.argv:
            return

        print("Getting configurations from Spring Cloud config...")
        config = (
            ClientConfigurationBuilder()
            .app_name("delay-predictor-service")
            .address(os.getenv("CONFIG_SERVER_URL", "http://localhost:8888"))
            .profile(os.getenv("CONFIG_PROFILE", "default"))
            .authentication((os.getenv('CONFIG_USERNAME', 'admin'), os.getenv('CONFIG_PASSWORD', 'admin')))
            .build()
        )
        config_client = SpringConfigClient(config)
        config = config_client.get_config()

        hostname = socket.gethostname()
        ip = socket.gethostbyname(hostname)

        print('Registering app to eureka server...')
        eureka_client.init(
            eureka_server=self.extract_eureka_server(self.resolve_config_property("APP_EUREKA_SERVER", config), self.resolve_config_property("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", config)),
            eureka_protocol="http",
            eureka_basic_auth_user=self.resolve_config_property("APP_EUREKA_USERNAME", config),
            eureka_basic_auth_password=self.resolve_config_property("APP_EUREKA_PASSWORD", config),
            eureka_context=self.extract_eureka_context(self.resolve_config_property("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", config)),
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


    def extract_eureka_server(self, eureka_server, eureka_url):
        match = re.search(r':(\d+)/', eureka_url)
        port = match.group(1) if match else ""
        return eureka_server + ":" + port


    def extract_eureka_context(self, eureka_url):
        match = re.search(r'@.+?(/[^"]*)', eureka_url)
        return match.group(1) if match else ""


    def resolve_config_property(self, flat_key, config):
        keys = flat_key.lower().split('_')


        current = self.lower_keys(config)
        for key in keys:
            if not isinstance(current, dict) or key not in current:
                return ""
            current = current[key]

        if isinstance(current, str):
            match = ENV_VAR_PATTERN.match(current)
            if match:
                return os.getenv(match.group(1), "")
        return current

    def lower_keys(self, d):
        if isinstance(d, dict):
            return {k.lower(): self.lower_keys(v) for k, v in d.items()}
        elif isinstance(d, list):
            return [self.lower_keys(item) for item in d]
        else:
            return d
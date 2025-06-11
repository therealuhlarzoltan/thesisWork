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
        hostname = socket.gethostname()
        ip = socket.gethostbyname(hostname)

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
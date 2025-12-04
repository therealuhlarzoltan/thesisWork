import os

from django.core.management.base import BaseCommand
from messaging.consumer import start_consuming

def is_training_instance() -> bool:
    val = os.getenv("IS_TRAINING_INSTANCE", "true").strip().lower()
    return val in ("1", "true", "yes", "y", "on")


class Command(BaseCommand):
    help = 'Start RabbitMQ consumer for batch responses'

    def handle(self, *args, **kwargs):
        if is_training_instance():
            self.stdout.write(self.style.SUCCESS("Starting RabbitMQ response listener..."))
            start_consuming()
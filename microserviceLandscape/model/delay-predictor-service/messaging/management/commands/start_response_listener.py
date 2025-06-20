from django.core.management.base import BaseCommand
from messaging.consumer import start_consuming  # move your _consume_batch_responses logic here

class Command(BaseCommand):
    help = 'Start RabbitMQ consumer for batch responses'

    def handle(self, *args, **kwargs):
        self.stdout.write(self.style.SUCCESS("Starting RabbitMQ response listener..."))
        start_consuming()
#!/bin/bash
set -e

#echo "Applying database migrations..."
#python manage.py migrate

echo "Starting Celery worker..."
celery -A config worker --loglevel=info --pool=solo &

echo "Starting Celery beat..."
celery -A config beat --loglevel=info &

echo "Starting RabbitMQ listeners..."
python manage.py start_response_listener &

echo "Starting Django development server..."
python manage.py runserver --noreload 0.0.0.0:8000
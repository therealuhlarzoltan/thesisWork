#!/bin/bash
set -e

echo "Starting RabbitMQ listeners..."
python manage.py start_response_listener &

echo "Starting Django development server..."
python manage.py runserver --noreload 0.0.0.0:8000
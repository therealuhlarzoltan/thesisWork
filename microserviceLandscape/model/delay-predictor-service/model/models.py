from django.db import models

class DelayPredictionModel(models.Model):
    DELAY_TYPE_CHOICES = [
        ('arrival', 'Arrival Delay'),
        ('departure', 'Departure Delay'),
    ]

    created_at = models.DateTimeField(auto_now_add=True)
    pipeline_binary = models.BinaryField()

    mae = models.FloatField()
    mse = models.FloatField()
    rmse = models.FloatField()
    r2 = models.FloatField()

    delay_type = models.CharField(max_length=10, choices=DELAY_TYPE_CHOICES)

    def __str__(self):
        return f"{self.get_delay_type_display()} model created at {self.created_at.strftime('%Y-%m-%d %H:%M:%S')}"
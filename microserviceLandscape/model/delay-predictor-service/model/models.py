from django.db import models

class XGBRegressorDatabaseModel(models.Model):
    name = models.CharField(max_length=255, unique=True)
    created_at = models.DateTimeField(auto_now_add=True)
    model_binary = models.BinaryField()

    def __str__(self):
        return self.name

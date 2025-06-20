from django.urls import path

from prediction.views import ArrivalDelayPredictorView, DepartureDelayPredictorView

urlpatterns = [
    path("delay/arrival", ArrivalDelayPredictorView.as_view(), name="predict_arrival_delay"),
    path("delay/departure", DepartureDelayPredictorView.as_view(), name="predict_departure_delay"),
]
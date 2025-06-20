import json

import pandas as pd
from django.core.exceptions import BadRequest
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from prediction.serializers import DelayPredictionRequestSerializer

from prediction import model_cache




class ArrivalDelayPredictorView(APIView):
    http_method_names = ['post']

    def post(self, request, *args, **kwargs):
        print("Got arrival delay prediction request:\n" + json.dumps(request.data, indent=2))
        serializer = DelayPredictionRequestSerializer(data=request.data)
        if serializer.is_valid():
            data = serializer.validated_data
            arrival_model = model_cache.arrival_model
            if arrival_model is None:
                print("Arrival model not found")
                return Response({'message': 'No available ML model'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

            df = pd.DataFrame([data])
            arrival_delay = int(round(arrival_model.predict(df)[0]))
            print("Predicted arrival delay: " + str(arrival_delay))
            return Response({'trainNumber': data['train_number'], 'stationCode':data['station_code'], 'predictedDelay': arrival_delay}, status=status.HTTP_200_OK)
        else:
            raise BadRequest(serializer.errors)



class DepartureDelayPredictorView(APIView):
    http_method_names = ['post']

    def post(self, request, *args, **kwargs):
        print("📩 Got departure delay prediction request:\n" + json.dumps(request.data, indent=2))
        serializer = DelayPredictionRequestSerializer(data=request.data)
        if serializer.is_valid():
            data = serializer.validated_data
            departure_model = model_cache.departure_model
            if departure_model is None:
                print("Departure model not found")
                return Response({'message': 'No available ML model'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

            df = pd.DataFrame([data])
            departure_delay = int(round(departure_model.predict(df)[0]))
            print("Departure arrival delay: " + str(departure_delay))
            return Response({'trainNumber': data['train_number'], 'stationCode': data['station_code'],
                             'predictedDelay': departure_delay}, status=status.HTTP_200_OK)
        else:
            raise BadRequest(serializer.errors)
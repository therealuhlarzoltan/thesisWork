import json
import pickle
from datetime import timedelta

import pandas as pd
from django.core.exceptions import BadRequest
from django.utils import timezone
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from model.models import DelayPredictionModel
from prediction.serializers import DelayPredictionRequestSerializer


class ArrivalDelayPredictorView(APIView):
    http_method_names = ['post']

    def post(self, request, *args, **kwargs):
        print("📩 Got arrival delay prediction request:\n" + json.dumps(request.data, indent=2))
        serializer = DelayPredictionRequestSerializer(data=request.data)
        if serializer.is_valid():
            data = serializer.validated_data
            db_model = self.__retrieve_arrival_delay_predictor()
            if db_model == None:
                print("Model was None")
                return Response({'message': 'No available ML model'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
            ml_model = self.__load_prediction_model(db_model)
            df = pd.DataFrame([data])
            print('Dataframe: ' + str(df.head()))
            cleaner = None
            preprocessor =  None
            #df_cleaned = cleaner.transform(df)
            #df_cleaned.drop(columns=['departure_delay', 'arrival_delay', 'actual_arrival', 'actual_departure'], axis=1, inplace=True, errors='ignore')
            #print("Arrival delay data cleaned: " + str(df_cleaned.head()))
            #df_preprocessed = preprocessor.transform(df_cleaned)
            #print("Arrival delay data preprocessed")
            for col in ['actual_departure', 'actual_arrival']:
                if col not in df.columns:
                    df[col] = None
            cleaner = ml_model.named_steps["cleaning"]
            preprocessor = ml_model.named_steps["preprocessing"]
            predictor = ml_model.named_steps["predicting"]
            df_cl = cleaner.transform(df)
            df_pr = preprocessor.transform(df_cl)
            arrival_delay = predictor.predict(df_pr)
            #arrival_delay = 5
            print("Predicted arrival delay: " + str(arrival_delay))
            return Response({'trainNumber': data['trainNumber'], 'stationCode':data['stationCode'], 'predictedDelay': arrival_delay}, status=status.HTTP_200_OK)
        else:
            raise BadRequest(serializer.errors)


    def __retrieve_arrival_delay_predictor(self):
        two_weeks_ago = timezone.now() - timedelta(weeks=2)

        return (
            DelayPredictionModel.objects
            .filter(delay_type='arrival', created_at__gte=two_weeks_ago)
            .order_by('rmse', '-r2')
            .first()
        )


    def __load_prediction_model(self, database_model):
        return pickle.loads(database_model.pipeline_binary)


class DepartureDelayPredictorView(APIView):
    http_method_names = ['post']

    def post(self, request, *args, **kwargs):
        print("📩 Got departure delay prediction request:\n" + json.dumps(request.data, indent=2))
        serializer = DelayPredictionRequestSerializer(data=request.data)
        if serializer.is_valid():
            data = serializer.validated_data
            db_model = self.__retrieve_departure_delay_predictor()
            if db_model == None:
                print("Model was None")
                return Response({'message': 'No available ML model'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
            ml_model = self.__load_prediction_model(db_model)
            df = pd.DataFrame([data])
            print('Dataframe: ' + str(df.head()))
            cleaner =  None
            preprocessor =  None
            #df_cleaned = cleaner.transform(df)
            #df_cleaned.drop(columns=['departure_delay', 'arrival_delay', 'actual_arrival', 'actual_departure'], axis=1, inplace=True, errors='ignore')
            #print("Departure delay data cleaned: " + str(df_cleaned.head()))
            #df_preprocessed = preprocessor.transform(df_cleaned)
            #print("Departure delay data preprocessed")
            for col in ['actual_departure', 'actual_arrival']:
                if col not in df.columns:
                    df[col] = None
            departure_delay = ml_model.predict(df)
            print("Predicted arrival delay: " + str(departure_delay))
            #departure_delay = 3
            return Response({'trainNumber': data['trainNumber'], 'stationCode': data['stationCode'],
                             'predictedDelay': departure_delay}, status=status.HTTP_200_OK)
        else:
            raise BadRequest(serializer.errors)


    def __retrieve_departure_delay_predictor(self):
        two_weeks_ago = timezone.now() - timedelta(weeks=2)

        return (
            DelayPredictionModel.objects
            .filter(delay_type='departure', created_at__gte=two_weeks_ago)
            .order_by('rmse', '-r2')
            .first()
        )

    def __load_prediction_model(self, database_model):
        return pickle.loads(database_model.pipeline_binary)
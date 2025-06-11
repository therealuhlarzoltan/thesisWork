from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView


class ArrivalDelayPredictorView(APIView):
    http_method_names = ['post']

    def post(self, request, *args, **kwargs):
        return Response({'predictedDelay': 5}, status=status.HTTP_200_OK)


class DepartureDelayPredictorView(APIView):
    http_method_names = ['post']

    def post(self, request, *args, **kwargs):
        return Response({'predictedDelay': 4}, status=status.HTTP_200_OK)
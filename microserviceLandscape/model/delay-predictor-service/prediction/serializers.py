from rest_framework import serializers

class WeatherInfoSerializer(serializers.Serializer):
    time = serializers.DateTimeField(required=True)
    address = serializers.CharField(required=True)
    latitude = serializers.FloatField(required=True)
    longitude = serializers.FloatField(required=True)

    temperature = serializers.FloatField(required=False, allow_null=True)
    relative_humidity = serializers.FloatField(required=False, allow_null=True)

    wind_speed_at10m = serializers.FloatField(required=False, allow_null=True)
    wind_speed_at80m = serializers.FloatField(required=False, allow_null=True)

    is_snowing = serializers.BooleanField(required=False, allow_null=True)
    snow_fall = serializers.FloatField(required=False, allow_null=True)
    snow_depth = serializers.FloatField(required=False, allow_null=True)

    is_raining = serializers.BooleanField(required=False, allow_null=True)
    precipitation = serializers.FloatField(required=False, allow_null=True)
    rain = serializers.FloatField(required=False, allow_null=True)
    showers = serializers.FloatField(required=False, allow_null=True)

    visibility_in_meters = serializers.IntegerField(required=False, allow_null=True)
    cloud_cover_percentage = serializers.IntegerField(required=False, allow_null=True)


class DelayPredictionRequestSerializer(serializers.Serializer):
    station_code = serializers.CharField(required=True)
    train_number = serializers.CharField(required=True)
    scheduled_departure = serializers.DateTimeField(required=False, allow_null=True)
    scheduled_arrival = serializers.DateTimeField(required=False, allow_null=True)
    date = serializers.DateField(required=True)
    station_latitude = serializers.FloatField(required=True)
    station_longitude = serializers.FloatField(required=True)
    weather = WeatherInfoSerializer()
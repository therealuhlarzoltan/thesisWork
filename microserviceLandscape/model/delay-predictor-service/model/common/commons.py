import re

ALLOWED_DT_PARTS = ("month", "day", "hour")

WEATHER_NUM_COLS = [
    "rain", "showers", "snow_fall", "snow_depth", "temperature",
    "precipitation", "wind_speed_at_10m", "wind_speed_at_80m",
    "relative_humidity", "visibility_in_meters", "cloud_cover_percentage",
]

WEATHER_BOOL_COLS = ["is_raining", "is_snowing"]


def camel_to_snake(name: str) -> str:
    s1 = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    s2 = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s1)
    s3 = re.sub(r"([a-zA-Z])([0-9]+)", r"\1_\2", s2)
    return s3.lower()

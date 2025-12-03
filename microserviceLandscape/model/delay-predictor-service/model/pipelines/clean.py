# model/pipelines/clean_v2.py
from sklearn.pipeline import Pipeline

from ..cleaning.transformers import (
    DropIdAndUrls,
    EnsureCoordinates,
    OriginTerminusAndScheduleFixer,
    DateFlagsAdder,
    WeatherFlattener,
    CamelToSnakeRenamer,
    WeatherImputer,
    StopIndexAdder,
    StationClusterer,
    LineServiceFeatures,
    DateTimeDecomposer,
)


arrival_delay_cleaner_pipeline = Pipeline(steps=[
    ("drop_id_urls", DropIdAndUrls()),
    ("ensure_coords", EnsureCoordinates()),
    ("origin_terminus_and_sched", OriginTerminusAndScheduleFixer()),
    ("date_flags", DateFlagsAdder()),
    ("flatten_weather", WeatherFlattener()),
    ("rename_camel_to_snake", CamelToSnakeRenamer()),
    ("impute_weather", WeatherImputer()),
    ("stop_index", StopIndexAdder()),
    ("station_cluster", StationClusterer()),
    ("line_service_features", LineServiceFeatures()),
    ("decompose_dt", DateTimeDecomposer()),
])
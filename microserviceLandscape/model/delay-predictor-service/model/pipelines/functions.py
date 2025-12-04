from typing import List, Tuple

from sklearn.base import TransformerMixin
from sklearn.pipeline import Pipeline

from model.cleaning.transformers import (
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
    DropInvalidDelayRecords,
    DropArrivalDelayColumn,
    DropDepartureDelayColumn,
    DropZeroDelayTrains,
)

def make_cleaning_pipeline(delay_type: str) -> Pipeline:
    steps: List[Tuple[str, TransformerMixin]] = [
        ("rename_camel_to_snake_columns", CamelToSnakeRenamer()),
        ("drop_measurement_errors", DropZeroDelayTrains())
    ]

    if delay_type == "arrival":
        steps.append(("drop_departure_delay_col", DropDepartureDelayColumn()))
    elif delay_type == "departure":
        steps.append(("drop_arrival_delay_col", DropArrivalDelayColumn()))
    else:
        raise ValueError(f"Unknown delay_type {delay_type!r}, expected 'arrival' or 'departure'.")

    steps.extend([
        ("drop_null_delays", DropInvalidDelayRecords()),
        ("drop_id_urls", DropIdAndUrls()),
        ("ensure_coords", EnsureCoordinates()),
        ("origin_terminus_and_sched", OriginTerminusAndScheduleFixer()),
        ("date_flags", DateFlagsAdder()),
        ("flatten_weather", WeatherFlattener()),
        ("rename_camel_to_snake_weather", CamelToSnakeRenamer()),
        ("impute_weather", WeatherImputer()),
        ("stop_index", StopIndexAdder()),
        ("station_cluster", StationClusterer()),
        ("line_service_features", LineServiceFeatures()),
        ("decompose_dt", DateTimeDecomposer()),
    ])

    return Pipeline(steps=steps)

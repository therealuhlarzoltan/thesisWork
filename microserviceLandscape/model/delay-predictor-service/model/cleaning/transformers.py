from sklearn.preprocessing import FunctionTransformer

from .functions import *
from .functions import (
    _impute_weather,
    _flatten_weather,
    drop_id_func,
    drop_url_cols_func,
    drop_null_weather_func,
    drop_null_coordinates_func,
    fill_delay_nans_func,
    drop_null_actual_func,
    rename_weather_cols_func,
    drop_actuals_arrival_func,
    drop_actuals_departure_func,
    drop_huge_arrivals,
    drop_huge_departures
)

drop_id = FunctionTransformer(drop_id_func, validate=False)

drop_url_cols = FunctionTransformer(drop_url_cols_func, validate=False)

drop_null_weather = FunctionTransformer(drop_null_weather_func, validate=False)

drop_null_coordinates = FunctionTransformer(drop_null_coordinates_func, validate=False)

make_flags = FunctionTransformer(create_flags, validate=False)

fix_sched = FunctionTransformer(fix_scheduled, validate=False)

fix_act = FunctionTransformer(fix_actual, validate=False)

fill_delay_nans = FunctionTransformer(fill_delay_nans_func, validate=False)

drop_null_actual = FunctionTransformer(drop_null_actual_func, validate=False)

add_date_feats = FunctionTransformer(add_date_features, validate=False)

flatten_weather = FunctionTransformer(_flatten_weather, validate=False)

rename_weather_cols = FunctionTransformer(rename_weather_cols_func, validate=False)

impute_weather = FunctionTransformer(_impute_weather, validate=False)

decompose_dt = FunctionTransformer(decompose_datetimes, validate=False)

drop_actuals_arrival = FunctionTransformer(drop_actuals_arrival_func, validate=False)

drop_actuals_departure = FunctionTransformer(drop_actuals_departure_func, validate=False)

drop_outliers_arrival = FunctionTransformer(drop_huge_arrivals, validate=False)

drop_outliers_departure = FunctionTransformer(drop_huge_departures, validate=False)

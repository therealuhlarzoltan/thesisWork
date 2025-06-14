from ..cleaning.transformers import *

from sklearn.pipeline import Pipeline

arrival_delay_cleaner_pipeline = Pipeline([
    ('drop_id', drop_id),
    ('drop_bad_weather', drop_null_weather),
    ('drop_bad_coordinates', drop_null_coordinates),
    ('make_flags', make_flags),
    ('fix_scheduled', fix_sched),
    ('fix_actual', fix_act),
    ('drop_null_actual', drop_null_actual),
    ('add_date_feats', add_date_feats),
    ('impute_delays', fill_delay_nans),
    ('flatten_weather', flatten_weather),
    ('rename_weather', rename_weather_cols),
    ('impute_weather', impute_weather),
    ('decompose_dt', decompose_dt),
    ('drop_actuals', drop_actuals_arrival),
    ('drop_outliers', drop_outliers_arrival)
])

departure_delay_cleaner_pipeline = Pipeline([
    ('drop_id', drop_id),
    ('drop_bad_weather', drop_null_weather),
    ('drop_bad_coordinates', drop_null_coordinates),
    ('make_flags', make_flags),
    ('fix_scheduled', fix_sched),
    ('fix_actual', fix_act),
    ('drop_null_actual', drop_null_actual),
    ('add_date_feats', add_date_feats),
    ('impute_delays', fill_delay_nans),
    ('flatten_weather', flatten_weather),
    ('rename_weather', rename_weather_cols),
    ('impute_weather', impute_weather),
    ('decompose_dt', decompose_dt),
    ('drop_actuals', drop_actuals_departure),
    ('drop_outliers', drop_outliers_departure)
])
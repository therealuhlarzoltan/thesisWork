from ..cleaning.transformers import *

from sklearn.pipeline import Pipeline

cleaning_pipeline = Pipeline([
    ('drop_urls', drop_url_cols),
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
    ('drop_actuals', drop_actuals)
])
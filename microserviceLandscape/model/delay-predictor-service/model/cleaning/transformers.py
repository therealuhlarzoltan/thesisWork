from sklearn.preprocessing import FunctionTransformer

from .functions import  *
from .functions import _impute_weather, _flatten_weather

drop_url_cols = FunctionTransformer(
    lambda df: df.drop(columns=['third_party_station_url',
                                'official_station_url']),
    validate=False
)


drop_null_weather = FunctionTransformer(
    lambda df: df[df['weather'].notna()].reset_index(drop=True),
    validate=False
)


drop_null_coordinates = FunctionTransformer(
    lambda df: df[df['latitude'].notna() & df['longitude'].notna()].reset_index(drop=True),
    validate=False
)

make_flags = FunctionTransformer(create_flags, validate=False)


fix_sched = FunctionTransformer(fix_scheduled, validate=False)


fix_act = FunctionTransformer(fix_actual, validate=False)


fill_delay_nans = FunctionTransformer(
    lambda df: df.fillna({'arrival_delay': 0, 'departure_delay': 0}),
    validate=False
)


drop_null_actual = FunctionTransformer(
    lambda df: df[df[['actual_departure', 'actual_arrival']].notna().all(axis=1)]
    .reset_index(drop=True),
    validate=False
)

add_date_feats = FunctionTransformer(add_date_features, validate=False)


flatten_weather = FunctionTransformer(_flatten_weather, validate=False)


rename_weather_cols = FunctionTransformer(
    lambda df: df.rename(columns=camel_to_snake),
    validate=False
)


impute_weather = FunctionTransformer(_impute_weather, validate=False)


decompose_dt = FunctionTransformer(decompose_datetimes, validate=False)


drop_actuals = FunctionTransformer(
    lambda df: df.drop(columns=['actual_departure', 'actual_arrival']),
    validate=False
)
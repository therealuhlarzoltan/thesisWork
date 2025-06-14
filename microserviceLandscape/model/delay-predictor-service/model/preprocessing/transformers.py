from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder

dt_feats = [
    f'{col}_{part}'
    for col in ['date','scheduled_departure','scheduled_arrival']
    for part in ('year','month','day','hour','minute')
]

weather_num = [
    'rain','showers','snow_fall','snow_depth','temperature',
    'precipitation','wind_speed_at_10m','wind_speed_at_80m',
    'relative_humidity','visibility_in_meters','cloud_cover_percentage'
]

numeric_features = weather_num + dt_feats + ["longitude", "latitude"]

categorical_features = [
    'station_code','train_number',
    'is_origin','is_terminus','is_weekend','is_holiday'
]

arrival_preprocessor = ColumnTransformer([
    ('num', StandardScaler(), numeric_features),
    ('cat', OneHotEncoder(handle_unknown='ignore'),
           categorical_features),
])

departure_preprocessor = ColumnTransformer([
    ('num', StandardScaler(), numeric_features),
    ('cat', OneHotEncoder(handle_unknown='ignore'),
           categorical_features),
])
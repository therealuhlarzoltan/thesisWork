from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler, OrdinalEncoder

from ..common.commons import (
    WEATHER_NUM_COLS,
    WEATHER_BOOL_COLS,
    ALLOWED_DT_PARTS,
)

dt_feats = [
    f"{col}_{part}"
    for col in ["scheduled_departure", "scheduled_arrival"]
    for part in ALLOWED_DT_PARTS
]

numeric_cols = WEATHER_NUM_COLS + dt_feats + [
    "max_daily_trains",
    "mean_stops_per_run",
    "stop_index",
]

categorical_cols = + WEATHER_BOOL_COLS + [
    "station_cluster",
    "line_service_cluster",
    "line_number",
    "is_origin",
    "is_terminus",
    "is_weekend",
    "is_holiday",
]

numeric_pipe = Pipeline(steps=[
    ("imputer", SimpleImputer(strategy="median")),
    ("scaler", StandardScaler()),
])

categorical_pipe = Pipeline(steps=[
    ("imputer", SimpleImputer(strategy="most_frequent")),
    ("encoder", OrdinalEncoder(handle_unknown="use_encoded_value", unknown_value=-1)),
])

arrival_preprocessor = ColumnTransformer(
    transformers=[
        ("num", numeric_pipe, numeric_cols),
        ("cat", categorical_pipe, categorical_cols),
    ],
    remainder="drop",
)
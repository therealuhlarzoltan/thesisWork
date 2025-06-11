import holidays
import pandas as pd
import re


def create_flags(df):
    df2 = df.copy()
    df2['is_origin'] = df2['scheduled_arrival'].isna()
    df2['is_terminus'] = df2['scheduled_departure'].isna()
    return df2


def fix_scheduled(df):
    df2 = df.copy()
    # origins: no arrival → copy departure
    mask_o = df2['is_origin']
    df2.loc[mask_o, 'scheduled_arrival'] = df2.loc[mask_o, 'scheduled_departure']
    # termini: no departure → copy arrival
    mask_t = df2['is_terminus']
    df2.loc[mask_t, 'scheduled_departure'] = df2.loc[mask_t, 'scheduled_arrival']
    return df2


def fix_actual(df):
    df2 = df.copy()
    mask_o = df2['is_origin']
    mask_t = df2['is_terminus']
    # origins: no actual_arrival → copy actual_departure
    df2.loc[mask_o, 'actual_arrival'] = df2.loc[mask_o, 'actual_departure']
    # termini: no actual_departure → copy actual_arrival
    df2.loc[mask_t, 'actual_departure'] = df2.loc[mask_t, 'actual_arrival']
    return df2


def add_date_features(df):
    df2 = df.copy()
    # ensure datetime
    df2['date'] = pd.to_datetime(df2['date'])
    # weekend flag
    df2['is_weekend'] = df2['date'].dt.weekday >= 5
    # holiday flag
    hu_hols = holidays.Hungary(years=df2['date'].dt.year.unique())
    df2['is_holiday'] = df2['date'].apply(lambda d: d in hu_hols)
    return df2


def _flatten_weather(df):
    df2 = df.copy()
    weather_df = pd.json_normalize(df2.pop('weather'))
    weather_df = weather_df.drop(columns=['time'], errors='ignore')
    return pd.concat([df2.reset_index(drop=True),
                      weather_df.reset_index(drop=True)],
                     axis=1)


def camel_to_snake(name: str) -> str:
    # 1) FooBar → Foo_Bar
    s1 = re.sub(r'(.)([A-Z][a-z]+)', r'\1_\2', name)
    # 2) fooBar → foo_Bar
    s2 = re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1)
    # 3) at10m → at_10m  (digits stay together)
    s3 = re.sub(r'([a-zA-Z])([0-9]+)', r'\1_\2', s2)
    return s3.lower()


def _impute_weather(df):
    df2 = df.copy()
    # 1) per-train lat/lon via transform (preserves index)
    df2[['latitude', 'longitude']] = (
        df2.groupby('train_number')[['latitude', 'longitude']]
        .transform(lambda grp: grp.ffill().bfill())
    )

    weather_num = ['rain', 'showers', 'snow_fall', 'snow_depth', 'temperature',
                   'precipitation', 'wind_speed_at_10m', 'wind_speed_at_80m',
                   'relative_humidity', 'visibility_in_meters', 'cloud_cover_percentage']
    for col in weather_num:
        df2[col] = df2[col].fillna(df2[col].mean())

    weather_bool = ['is_raining', 'is_snowing']
    for col in weather_bool:
        mode = df2[col].mode()
        fill = mode.iloc[0] if not mode.empty else False
        df2[col] = df2[col].fillna(fill)

    return df2


def decompose_datetimes(df):
    df2 = df.copy()
    datetime_cols = [
        'date',
        'scheduled_departure', 'scheduled_arrival',
    ]
    for col in datetime_cols:
        # ensure it’s a datetime
        dt = pd.to_datetime(df2[col])
        # extract parts
        df2[f'{col}_year'] = dt.dt.year
        df2[f'{col}_month'] = dt.dt.month
        df2[f'{col}_day'] = dt.dt.day
        df2[f'{col}_hour'] = dt.dt.hour
        df2[f'{col}_minute'] = dt.dt.minute
    # now drop the raw datetime columns
    return df2.drop(columns=datetime_cols)
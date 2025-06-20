import holidays
import pandas as pd
import re


def drop_id_func(df):
    return df.drop(columns=['id'], axis=1, errors='ignore')

def drop_url_cols_func(df):
    return df.drop(columns=['third_party_station_url', 'official_station_url'], axis=1, errors='ignore')

def drop_null_weather_func(df):
    return df[df['weather'].notna()].reset_index(drop=True)

def drop_null_coordinates_func(df):
    return df[df['station_latitude'].notna() & df['station_longitude'].notna()].reset_index(drop=True)

def fill_delay_nans_func(df):
    return df.fillna({'arrival_delay': 0, 'departure_delay': 0})


def rename_weather_cols_func(df):
    return df.rename(columns=camel_to_snake)

def drop_actuals_arrival_func(df):
    return df.drop(columns=['actual_departure', 'actual_arrival', 'departure_delay'], axis=1, errors='ignore')

def drop_actuals_departure_func(df):
    return df.drop(columns=['actual_departure', 'actual_arrival', 'arrival_delay'], axis=1, errors='ignore')

def create_flags(df):
    df2 = df.copy()
    df2['is_origin'] = df2['scheduled_arrival'].isna()
    df2['is_terminus'] = df2['scheduled_departure'].isna()
    return df2

def fix_scheduled(df):
    df2 = df.copy()
    mask_o = df2['is_origin']
    df2.loc[mask_o, 'scheduled_arrival'] = df2.loc[mask_o, 'scheduled_departure']
    mask_t = df2['is_terminus']
    df2.loc[mask_t, 'scheduled_departure'] = df2.loc[mask_t, 'scheduled_arrival']
    return df2


def add_date_features(df):
    df2 = df.copy()
    df2['date'] = pd.to_datetime(df2['date'])
    df2['is_weekend'] = df2['date'].dt.weekday >= 5
    hu_hols = holidays.Hungary(years=df2['date'].dt.year.unique())
    df2['is_holiday'] = df2['date'].apply(lambda d: d in hu_hols)
    return df2

def _flatten_weather(df):
    df2 = df.copy()
    weather_df = pd.json_normalize(df2.pop('weather'))
    weather_df = weather_df.drop(columns=['time'], errors='ignore')
    return pd.concat([df2.reset_index(drop=True), weather_df.reset_index(drop=True)], axis=1)

def camel_to_snake(name: str) -> str:
    s1 = re.sub(r'(.)([A-Z][a-z]+)', r'\1_\2', name)
    s2 = re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1)
    s3 = re.sub(r'([a-zA-Z])([0-9]+)', r'\1_\2', s2)
    return s3.lower()

def _impute_weather(df):
    df2 = df.copy()
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
    datetime_cols = ['date', 'scheduled_departure', 'scheduled_arrival']
    for col in datetime_cols:
        dt = pd.to_datetime(df2[col])
        df2[f'{col}_year'] = dt.dt.year
        df2[f'{col}_month'] = dt.dt.month
        df2[f'{col}_day'] = dt.dt.day
        df2[f'{col}_hour'] = dt.dt.hour
        df2[f'{col}_minute'] = dt.dt.minute
    return df2.drop(columns=datetime_cols)

def drop_huge_arrivals(df):
    if 'arrival_delay' not in df.columns:
        return df
    df2 = df.copy()
    threshold = df2['arrival_delay'].quantile(0.999)
    mask = df2['arrival_delay'] <= threshold
    return df2[mask].reset_index(drop=True)

def drop_huge_departures(df):
    if 'departure_delay' not in df.columns:
        return df
    df2 = df.copy()
    threshold = df2['departure_delay'].quantile(0.999)
    mask = df2['departure_delay'] <= threshold
    return df2[mask].reset_index(drop=True)

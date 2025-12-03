from __future__ import annotations

from typing import Iterable, Sequence, Optional, Dict

import holidays
import pandas as pd
from sklearn.base import BaseEstimator, TransformerMixin
from sklearn.cluster import KMeans
from sklearn.metrics import silhouette_score
from sklearn.preprocessing import StandardScaler

from ..common.commons import (
    camel_to_snake,
    ALLOWED_DT_PARTS,
    WEATHER_NUM_COLS,
    WEATHER_BOOL_COLS,
)


class DropIdAndUrls(BaseEstimator, TransformerMixin):
    """
    Drop purely technical columns that are never needed for modeling.
    Does NOT drop station_code, train_number, line_number, etc.
    """

    def __init__(self,
                 id_cols: Sequence[str] = ("id",),
                 url_cols: Sequence[str] = ("third_party_station_url", "official_station_url")):
        self.id_cols = list(id_cols)
        self.url_cols = list(url_cols)

    def fit(self, X, y=None):
        return self

    def transform(self, X):
        df = X.copy()
        drop_cols = [c for c in (self.id_cols + self.url_cols) if c in df.columns]
        if drop_cols:
            df = df.drop(columns=drop_cols, errors="ignore")
        return df


class EnsureCoordinates(BaseEstimator, TransformerMixin):
    """
    Make sure we always have 'latitude' and 'longitude' columns.
    If only 'station_latitude' / 'station_longitude' exist, copy them.
    """

    def fit(self, X, y=None):
        return self

    def transform(self, X):
        df = X.copy()
        if "latitude" not in df.columns and "station_latitude" in df.columns:
            df["latitude"] = df["station_latitude"]
        if "longitude" not in df.columns and "station_longitude" in df.columns:
            df["longitude"] = df["station_longitude"]
        return df


class OriginTerminusAndScheduleFixer(BaseEstimator, TransformerMixin):
    """
    Set is_origin / is_terminus flags and fill missing scheduled edges:
      - at origin: use scheduled_departure for scheduled_arrival
      - at terminus: use scheduled_arrival for scheduled_departure
    Works even if some columns are missing (no crash).
    """

    def fit(self, X, y=None):
        return self

    def transform(self, X):
        df = X.copy()

        if "scheduled_arrival" not in df.columns or "scheduled_departure" not in df.columns:
            # Nothing to do
            return df

        df["is_origin"] = df["scheduled_arrival"].isna()
        df["is_terminus"] = df["scheduled_departure"].isna()

        mask_o = df["is_origin"]
        df.loc[mask_o, "scheduled_arrival"] = df.loc[mask_o, "scheduled_departure"]

        mask_t = df["is_terminus"]
        df.loc[mask_t, "scheduled_departure"] = df.loc[mask_t, "scheduled_arrival"]

        return df


class DateFlagsAdder(BaseEstimator, TransformerMixin):
    """
    Ensure 'date' is datetime and add is_weekend / is_holiday flags.
    """

    def __init__(self, country="HU"):
        self.country = country

    def fit(self, X, y=None):
        # We don't need training data; holidays lib can be called in transform.
        return self

    def transform(self, X):
        df = X.copy()
        if "date" not in df.columns:
            return df

        df["date"] = pd.to_datetime(df["date"])
        df["is_weekend"] = df["date"].dt.weekday >= 5

        years = df["date"].dt.year.unique()
        if self.country == "HU":
            hu_holidays = holidays.Hungary(years=years)
        else:
            # Fallback; you can extend with other countries if you want.
            hu_holidays = holidays.country_holidays(self.country, years=years)

        df["is_holiday"] = df["date"].apply(lambda d: d in hu_holidays)
        return df


class WeatherFlattener(BaseEstimator, TransformerMixin):
    """
    Flatten 'weather' JSON/dict column into top-level columns.

    Assumes each row has either a dict-like object or a JSON that pandas/json_normalize
    can understand. Drops the nested 'time' field if present.
    """

    def __init__(self, weather_col: str = "weather"):
        self.weather_col = weather_col

    def fit(self, X, y=None):
        return self

    def transform(self, X):
        df = X.copy()
        if self.weather_col not in df.columns:
            return df

        weather_series = df.pop(self.weather_col)
        weather_df = pd.json_normalize(weather_series)
        weather_df = weather_df.drop(columns=["time"], errors="ignore")

        weather_df = weather_df.reset_index(drop=True)
        df = df.reset_index(drop=True)
        return pd.concat([df, weather_df], axis=1)


class CamelToSnakeRenamer(BaseEstimator, TransformerMixin):
    """
    Apply camel_to_snake to all columns.
    Useful after WeatherFlattener.
    """

    def fit(self, X, y=None):
        return self

    def transform(self, X):
        df = X.copy()
        df.columns = [camel_to_snake(c) for c in df.columns]
        return df


class WeatherImputer(BaseEstimator, TransformerMixin):
    """
    Impute:
      - latitude / longitude per train_number via ffill/bfill
      - numeric weather cols with mean
      - boolean weather cols with mode (or False fallback)
    """

    def __init__(self,
                 train_col: str = "train_number",
                 num_cols: Sequence[str] = WEATHER_NUM_COLS,
                 bool_cols: Sequence[str] = WEATHER_BOOL_COLS):
        self.train_col = train_col
        self.num_cols = list(num_cols)
        self.bool_cols = list(bool_cols)

    def fit(self, X, y=None):
        df = X.copy()

        # Store global means / modes
        self.num_means_: Dict[str, float] = {}
        for col in self.num_cols:
            if col in df.columns:
                self.num_means_[col] = df[col].astype(float).mean()

        self.bool_modes_: Dict[str, bool] = {}
        for col in self.bool_cols:
            if col in df.columns:
                mode = df[col].mode()
                self.bool_modes_[col] = bool(mode.iloc[0]) if not mode.empty else False

        return self

    def transform(self, X):
        df = X.copy()

        # Lat/lon per train_number: fill forward/backward
        if self.train_col in df.columns and {"latitude", "longitude"} <= set(df.columns):
            df[["latitude", "longitude"]] = (
                df.groupby(self.train_col)[["latitude", "longitude"]]
                .transform(lambda grp: grp.ffill().bfill())
            )

        # Numeric weather means
        for col, mean_val in self.num_means_.items():
            if col in df.columns:
                df[col] = df[col].astype(float).fillna(mean_val)

        # Boolean weather modes
        for col in self.bool_cols:
            if col in df.columns:
                fill_val = self.bool_modes_.get(col, False)
                df[col] = df[col].fillna(fill_val)

        return df


class StopIndexAdder(BaseEstimator, TransformerMixin):
    """
    Add 'stop_index' within (date, train_number), ordered by scheduled_arrival with a 6h buffer
    for overnight trains:

      - Convert scheduled_arrival to datetime
      - If hour < buffer_hours, treat it as 'next day' → add 1 day
      - Rank within (date, train_number) by this adjusted time.

    Requirements:
      - columns: 'date', 'train_number', 'scheduled_arrival'
    """

    def __init__(self,
                 date_col: str = "date",
                 train_col: str = "train_number",
                 time_col: str = "scheduled_arrival",
                 buffer_hours: int = 6):
        self.date_col = date_col
        self.train_col = train_col
        self.time_col = time_col
        self.buffer_hours = buffer_hours

    def fit(self, X, y=None):
        # purely deterministic, no learned state
        return self

    def transform(self, X):
        df = X.copy()
        required = {self.date_col, self.train_col, self.time_col}
        if not required.issubset(df.columns):
            return df

        dt = pd.to_datetime(df[self.time_col])
        df["_order_time"] = dt

        # If scheduled_arrival is earlier than buffer_hours, assume it's past midnight → +1 day
        hours = df["_order_time"].dt.hour
        add_day = (hours < self.buffer_hours).astype("int32")
        df["_order_time"] = df["_order_time"] + pd.to_timedelta(add_day, unit="D")

        df["stop_index"] = (
            df.groupby([self.date_col, self.train_col])["_order_time"]
              .rank(method="first")
              .astype("int32") - 1
        )

        df = df.drop(columns=["_order_time"])
        return df


class StationClusterer(BaseEstimator, TransformerMixin):
    """
    Cluster stations by latitude/longitude; attach station_cluster.
    The clustering is learned at fit-time and reused at prediction time.

    Uses KMeans on distinct (station_code, latitude, longitude) rows.
    """

    def __init__(self,
                 n_clusters: int = 50,
                 random_state: int = 42,
                 station_col: str = "station_code"):
        self.n_clusters = n_clusters
        self.random_state = random_state
        self.station_col = station_col

    def fit(self, X, y=None):
        df = X.copy()
        required = {self.station_col, "latitude", "longitude"}
        if not required.issubset(df.columns):
            self.station_cluster_map_ = {}
            self.n_clusters_ = 0
            return self

        coords = (
            df[[self.station_col, "latitude", "longitude"]]
            .dropna()
            .drop_duplicates(subset=[self.station_col])
        )

        if coords.empty:
            self.station_cluster_map_ = {}
            self.n_clusters_ = 0
            return self

        n_clusters = min(self.n_clusters, len(coords))
        self.n_clusters_ = n_clusters

        km = KMeans(n_clusters=n_clusters, random_state=self.random_state)
        labels = km.fit_predict(coords[["latitude", "longitude"]])

        self.kmeans_ = km
        self.station_cluster_map_ = dict(zip(coords[self.station_col], labels))

        return self

    def transform(self, X):
        df = X.copy()
        if not hasattr(self, "station_cluster_map_") or self.n_clusters_ == 0:
            df["station_cluster"] = -1
            return df

        df["station_cluster"] = (
            df.get(self.station_col)
              .map(self.station_cluster_map_)
              .fillna(-1)
              .astype("int32")
        )
        return df


class LineServiceFeatures(BaseEstimator, TransformerMixin):
    """
    Compute line-level frequency & stop pattern stats and cluster lines into
    line_service_cluster groups. Also attaches:
      - max_daily_trains
      - mean_stops_per_run

    This is the sklearn version of your notebook's line_freq + KMeans logic.
    """

    def __init__(self,
                 line_col: str = "line_number",
                 date_col: str = "date",
                 train_col: str = "train_number",
                 station_col: str = "station_code",
                 min_k: int = 3,
                 max_k: int = 10,
                 random_state: int = 42):
        self.line_col = line_col
        self.date_col = date_col
        self.train_col = train_col
        self.station_col = station_col
        self.min_k = min_k
        self.max_k = max_k
        self.random_state = random_state

    def fit(self, X, y=None):
        df = X.copy()
        required = {self.line_col, self.date_col, self.train_col, self.station_col}
        if not required.issubset(df.columns):
            # Not enough info: fall back to "no-op" mode
            self.line_freq_ = None
            self.global_max_daily_trains_ = 0.0
            self.global_mean_stops_per_run_ = 0.0
            return self

        df_line = df[[self.date_col, self.line_col, self.train_col, self.station_col]].dropna(
            subset=[self.line_col, self.train_col]
        ).copy()
        df_line[self.date_col] = pd.to_datetime(df_line[self.date_col])

        # (line, date, train) -> stops_per_run
        train_runs = (
            df_line
            .groupby([self.line_col, self.date_col, self.train_col])[self.station_col]
            .nunique()
            .reset_index(name="stops_per_run")
        )

        # (line, date) -> trains_per_day
        line_date_counts = (
            train_runs
            .groupby([self.line_col, self.date_col])[self.train_col]
            .nunique()
            .reset_index(name="trains_per_day")
        )

        # Aggregate daily frequency features per line
        line_freq = (
            line_date_counts
            .groupby(self.line_col)["trains_per_day"]
            .agg(
                mean_daily_trains="mean",
                median_daily_trains="median",
                max_daily_trains="max",
                std_daily_trains="std",
            )
        )

        # Weekday / weekend split
        line_date_counts["is_weekend"] = line_date_counts[self.date_col].dt.weekday >= 5

        weekday_means = (
            line_date_counts[~line_date_counts["is_weekend"]]
            .groupby(self.line_col)["trains_per_day"]
            .mean()
            .rename("weekday_trains")
        )

        weekend_means = (
            line_date_counts[line_date_counts["is_weekend"]]
            .groupby(self.line_col)["trains_per_day"]
            .mean()
            .rename("weekend_trains")
        )

        line_freq = line_freq.join(weekday_means).join(weekend_means)

        # Stops-per-run stats per line
        stops_agg = (
            train_runs
            .groupby(self.line_col)["stops_per_run"]
            .agg(
                mean_stops_per_run="mean",
                median_stops_per_run="median",
                max_stops_per_run="max",
                std_stops_per_run="std",
            )
        )

        line_freq = line_freq.join(stops_agg)

        # Fill gaps (e.g. lines with only weekday runs)
        line_freq = line_freq.fillna(0.0)

        line_freq["weekend_trains"] = line_freq["weekend_trains"].fillna(0.0)
        line_freq["weekday_trains"] = line_freq["weekday_trains"].fillna(0.0)
        line_freq["weekend_weekday_ratio"] = (
            line_freq["weekend_trains"] / (line_freq["weekday_trains"] + 1e-3)
        )

        # Choose k using silhouette, roughly matching your notebook
        X_service = line_freq[[
            "mean_daily_trains",
            "median_daily_trains",
            "max_daily_trains",
            "weekday_trains",
            "weekend_trains",
        ]].values

        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X_service)

        n_lines = len(line_freq)
        if n_lines <= 2:
            best_k = n_lines
        else:
            min_k = max(2, min(self.min_k, n_lines))
            max_k = min(self.max_k, n_lines)
            best_k = min_k
            best_score = -1.0

            for k in range(min_k, max_k + 1):
                km = KMeans(n_clusters=k, random_state=self.random_state)
                labels = km.fit_predict(X_scaled)
                try:
                    score = silhouette_score(X_scaled, labels)
                except ValueError:
                    continue
                if score > best_score:
                    best_score = score
                    best_k = k

        self.scaler_ = scaler
        self.kmeans_ = KMeans(n_clusters=best_k, random_state=self.random_state)
        self.kmeans_.fit(X_scaled)

        labels = self.kmeans_.predict(X_scaled)
        line_freq["line_service_cluster"] = labels

        self.line_freq_ = line_freq
        self.global_max_daily_trains_ = float(line_freq["max_daily_trains"].mean())
        self.global_mean_stops_per_run_ = float(line_freq["mean_stops_per_run"].mean())
        self.default_cluster_ = -1

        return self

    def transform(self, X):
        df = X.copy()

        if self.line_freq_ is None or self.line_col not in df.columns:
            # Attach default values if we couldn't compute line_freq during fit
            df["line_service_cluster"] = -1
            df["max_daily_trains"] = self.global_max_daily_trains_
            df["mean_stops_per_run"] = self.global_mean_stops_per_run_
            return df

        line_freq_small = self.line_freq_[[
            "line_service_cluster",
            "max_daily_trains",
            "mean_stops_per_run",
        ]]

        df = df.merge(
            line_freq_small,
            left_on=self.line_col,
            right_index=True,
            how="left",
        )

        df["line_service_cluster"] = (
            df["line_service_cluster"]
            .fillna(self.default_cluster_)
            .astype("Int64")
        )
        df["max_daily_trains"] = df["max_daily_trains"].fillna(self.global_max_daily_trains_)
        df["mean_stops_per_run"] = df["mean_stops_per_run"].fillna(self.global_mean_stops_per_run_)

        return df


class DateTimeDecomposer(BaseEstimator, TransformerMixin):
    """
    Add dt parts for date, scheduled_departure, scheduled_arrival
    according to ALLOWED_DT_PARTS, but **do not** drop the original
    datetime columns (ColumnTransformer will ignore them).
    """

    def __init__(self,
                 datetime_cols: Sequence[str] = ("date", "scheduled_departure", "scheduled_arrival"),
                 parts: Sequence[str] = ALLOWED_DT_PARTS):
        self.datetime_cols = list(datetime_cols)
        self.parts = list(parts)

    def fit(self, X, y=None):
        return self

    def transform(self, X):
        df = X.copy()
        for col in self.datetime_cols:
            if col not in df.columns:
                continue
            dt = pd.to_datetime(df[col])
            for part in self.parts:
                df[f"{col}_{part}"] = getattr(dt.dt, part)
        return df

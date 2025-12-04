from __future__ import annotations

import pandas as pd

def apply_cluster_quantile_mask(
    df: pd.DataFrame,
    target_col: str,
    cluster_col: str = "line_service_cluster",
    upper_q: float = 0.98,
    min_delay: float = -5.0,
) -> pd.DataFrame:

    if target_col not in df.columns:
        raise ValueError(f"{target_col} not found in dataframe")

    if cluster_col not in df.columns:
        global_q = df[target_col].quantile(upper_q)
        mask = (df[target_col] <= global_q) & (df[target_col] >= min_delay)
        return df.loc[mask].reset_index(drop=True)

    per_cluster_q = (
        df.groupby(cluster_col)[target_col]
           .transform(lambda s: s.quantile(upper_q))
    )

    global_q = df[target_col].quantile(upper_q)
    per_cluster_q = per_cluster_q.fillna(global_q)

    mask = (df[target_col] <= per_cluster_q) & (df[target_col] >= min_delay)

    return df.loc[mask].reset_index(drop=True)

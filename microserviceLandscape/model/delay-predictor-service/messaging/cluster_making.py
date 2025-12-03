from __future__ import annotations

import pandas as pd


def apply_cluster_quantile_mask(
    df: pd.DataFrame,
    target_col: str,
    cluster_col: str = "line_service_cluster",
    upper_q: float = 0.98,
    min_delay: float = -5.0,
) -> pd.DataFrame:
    """
    Training-only helper.

    For each cluster in cluster_col, compute upper_q quantile of target_col,
    then keep only rows where:
        min_delay <= target <= cluster_q(cluster)

    If cluster_q is NaN for some rows, falls back to global quantile.

    Returns a filtered copy of df.
    """

    if target_col not in df.columns:
        raise ValueError(f"{target_col} not found in dataframe")

    if cluster_col not in df.columns:
        # no cluster info → fall back to global quantile only
        global_q = df[target_col].quantile(upper_q)
        mask = (df[target_col] <= global_q) & (df[target_col] >= min_delay)
        return df.loc[mask].reset_index(drop=True)

    df2 = df.copy()

    # per-cluster quantiles
    per_cluster_q = (
        df2.groupby(cluster_col)[target_col]
           .transform(lambda s: s.quantile(upper_q))
    )

    # global fallback
    global_q = df2[target_col].quantile(upper_q)
    per_cluster_q = per_cluster_q.fillna(global_q)

    mask = (df2[target_col] <= per_cluster_q) & (df2[target_col] >= min_delay)

    return df2.loc[mask].reset_index(drop=True)

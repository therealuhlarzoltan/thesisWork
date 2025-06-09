from sklearn.pipeline import Pipeline

from model.predictor.xg_boost import xgb_regressor

xgb_pipeline = Pipeline([
    ('xgb', xgb_regressor)
])
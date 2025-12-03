from sklearn.pipeline import Pipeline

from model.pipelines.clean import arrival_delay_cleaner_pipeline
from model.pipelines.preprocess import arrival_delay_preprocessor_pipeline
from model.predictor.xg_boost import arrival_delay_xgb_regressor
from model.pipelines.clean import departure_delay_cleaner_pipeline
from model.pipelines.preprocess import departure_delay_preprocessor_pipeline
from model.predictor.xg_boost import departure_delay_xgb_regressor

arrival_delay_predictor_pipeline = Pipeline(steps=[
    ("xgb", arrival_delay_xgb_regressor),
])

arrival_delay_pipeline = Pipeline(steps=[
    ("cleaning", arrival_delay_cleaner_pipeline),
    ("preprocessing", arrival_delay_preprocessor_pipeline),
    ("predicting", arrival_delay_predictor_pipeline),
])

departure_delay_predictor_pipeline = Pipeline(steps=[
    ("xgb", departure_delay_xgb_regressor),
])

departure_delay_pipeline = Pipeline(steps=[
    ('cleaning', departure_delay_cleaner_pipeline),
    ('preprocessing', departure_delay_preprocessor_pipeline),
    ('predicting', departure_delay_predictor_pipeline)
])
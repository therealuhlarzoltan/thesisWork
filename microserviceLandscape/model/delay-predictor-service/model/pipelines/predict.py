from sklearn.pipeline import Pipeline

from model.pipelines.clean import arrival_delay_cleaner_pipeline
from model.pipelines.preprocess import arrival_preprocessor
from model.predictor.xg_boost import arrival_xgb_regressor


arrival_delay_predictor_pipeline = Pipeline(steps=[
    ("xgb", arrival_xgb_regressor),
])

arrival_delay_pipeline = Pipeline(steps=[
    ("cleaning", arrival_delay_cleaner_pipeline),
    ("preprocessing", arrival_preprocessor),
    ("predicting", arrival_delay_predictor_pipeline),
])

departure_delay_pipeline = Pipeline(steps=[
    ('cleaning', departure_delay_cleaner_pipeline),
    ('preprocessing', departure_delay_processor_pipeline),
    ('predicting', departure_delay_predictor_pipeline)
])
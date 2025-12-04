from sklearn.pipeline import Pipeline

from model.preprocessing.transformers import arrival_delay_preprocessor_pipeline, departure_delay_preprocessor_pipeline

arrival_delay_processor_pipeline = Pipeline([
    ('preproc', arrival_delay_preprocessor_pipeline),
])

departure_delay_processor_pipeline = Pipeline([
    ('preproc', departure_delay_preprocessor_pipeline),
])
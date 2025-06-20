from sklearn.pipeline import Pipeline

from model.preprocessing.transformers import arrival_preprocessor, departure_preprocessor

arrival_delay_processor_pipeline = Pipeline([
    ('preproc', arrival_preprocessor),
])

departure_delay_processor_pipeline = Pipeline([
    ('preproc', departure_preprocessor),
])
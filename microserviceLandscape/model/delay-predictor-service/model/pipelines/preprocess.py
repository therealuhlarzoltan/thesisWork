from sklearn.pipeline import Pipeline

from model.preprocessing.transformers import preprocessor

processor_pipeline = Pipeline([
    ('preproc', preprocessor),
])
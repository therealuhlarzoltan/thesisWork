from .functions import make_cleaning_pipeline

arrival_delay_cleaner_pipeline = make_cleaning_pipeline("arrival")
departure_delay_cleaner_pipeline = make_cleaning_pipeline("departure")
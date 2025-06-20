from xgboost import XGBRegressor

arrival_xgb_regressor = XGBRegressor(
             n_estimators=8000,
             learning_rate=0.070,
             max_depth=9,
             random_state=42,
             tree_method='hist',
             verbosity=2,
            subsample=0.85,
            colsample_bytree=0.75,
            early_stopping_rounds=25,
            alpha=0.5,
            gamma=0.5,
            n_jobs=-1
             )

departure_xgb_regressor = XGBRegressor(
             n_estimators=8000,
             learning_rate=0.070,
             max_depth=9,
             random_state=42,
             tree_method='hist',
             verbosity=3,
            subsample=0.85,
            colsample_bytree=0.75,
            early_stopping_rounds=25,
            alpha=0.5,
            gamma=0.5,
            n_jobs=-1
             )
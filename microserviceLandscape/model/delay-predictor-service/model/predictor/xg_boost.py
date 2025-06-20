from xgboost import XGBRegressor

xgb_regressor = XGBRegressor(
             n_estimators=3000,
             learning_rate=0.085,
             max_depth=17,
             random_state=42,
             tree_method='gpu_hist',
             verbosity=3,
            subsample=0.85,
        colsample_bytree=0.75,
            n_jobs=-1
             )
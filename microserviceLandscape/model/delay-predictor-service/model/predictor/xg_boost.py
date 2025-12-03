from xgboost import XGBRegressor


arrival_xgb_regressor_v2 = XGBRegressor(
    objective="reg:squarederror",
    n_estimators=10000,
    learning_rate=0.095,
    max_depth=9,
    min_child_weight=1,
    subsample=0.8783137597380328,
    colsample_bytree=0.6384706204365683,
    reg_alpha=2.81432414754274,
    reg_lambda=6.082895947655132,
    gamma=0.5,
    tree_method="hist",
    random_state=42,
    eval_metric="rmse",
    n_jobs=-1,
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
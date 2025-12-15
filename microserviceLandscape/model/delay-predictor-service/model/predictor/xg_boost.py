from xgboost import XGBRegressor

arrival_delay_xgb_regressor = XGBRegressor(
    objective='reg:squarederror',
    n_estimators=20000,
    learning_rate=0.08,
    max_depth=9,
    min_child_weight=1,
    subsample=0.87,
    colsample_bytree=0.7,
    reg_alpha=0.04,
    reg_lambda=1.08,
    gamma=0.1,
    tree_method='hist',
    random_state=42,
    eval_metric='rmse',
    verbose=3,
    early_stopping_rounds=75,
    n_jobs=-1,
)

departure_delay_xgb_regressor =  XGBRegressor(
    objective='reg:squarederror',
    n_estimators=20000,
    learning_rate=0.08,
    max_depth=9,
    min_child_weight=1,
    subsample=0.87,
    colsample_bytree=0.7,
    reg_alpha=0.04,
    reg_lambda=1.08,
    gamma=0.1,
    tree_method='hist',
    random_state=42,
    eval_metric='rmse',
    verbose=3,
    early_stopping_rounds=75,
    n_jobs=-1,
)
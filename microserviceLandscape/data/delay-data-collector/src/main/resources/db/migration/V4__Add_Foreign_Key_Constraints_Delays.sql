ALTER TABLE delays
ADD CONSTRAINT fk_station_code
FOREIGN KEY (station_code)
REFERENCES stations(station_code)
ON DELETE SET NULL;

ALTER TABLE delays
ADD CONSTRAINT fk_train_number
FOREIGN KEY (train_number)
REFERENCES trains(train_number)
ON DELETE CASCADE;
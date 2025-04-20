CREATE TABLE IF NOT EXISTS delays (
    id BIGSERIAL PRIMARY KEY,
    station_code VARCHAR(100),
    third_party_station_url VARCHAR(1024),
    official_station_url VARCHAR(1024),
    train_number VARCHAR(16) NOT NULL,
    scheduled_departure TIMESTAMP,
    actual_departure TIMESTAMP,
    scheduled_arrival TIMESTAMP,
    actual_arrival TIMESTAMP,
    arrival_delay INTEGER,
    departure_delay INTEGER,
    date DATE,
    weather JSONB
);

CREATE TABLE IF NOT EXISTS trains (
    train_number VARCHAR(16) PRIMARY KEY CHECK (char_length(train_number) BETWEEN 1 AND 16),
    line_number VARCHAR(16) NOT NULL CHECK (char_length(line_number) BETWEEN 1 AND 16),
    start_station VARCHAR(64) NOT NULL CHECK (char_length(start_station) BETWEEN 2 AND 64),
    end_station VARCHAR(64) NOT NULL CHECK (char_length(end_station) BETWEEN 2 AND 64)
);

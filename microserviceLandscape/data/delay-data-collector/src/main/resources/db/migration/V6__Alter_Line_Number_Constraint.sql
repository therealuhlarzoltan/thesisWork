ALTER TABLE trains
ALTER COLUMN line_number TYPE VARCHAR(64),
DROP CONSTRAINT IF EXISTS trains_line_number_check,
ADD CONSTRAINT trains_line_number_check CHECK (char_length(line_number) BETWEEN 1 AND 64);

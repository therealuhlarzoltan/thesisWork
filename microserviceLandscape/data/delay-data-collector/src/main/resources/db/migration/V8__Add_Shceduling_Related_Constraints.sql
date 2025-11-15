ALTER TABLE scheduled_jobs
    ADD CONSTRAINT pk_scheduled_jobs PRIMARY KEY (id);

ALTER TABLE scheduled_dates
    ADD CONSTRAINT pk_scheduled_dates PRIMARY KEY (id);

ALTER TABLE scheduled_intervals
    ADD CONSTRAINT pk_scheduled_intervals PRIMARY KEY (id);

ALTER TABLE scheduled_jobs
    ALTER COLUMN job_name SET NOT NULL;

ALTER TABLE scheduled_dates
    ALTER COLUMN job_id SET NOT NULL,
ALTER COLUMN cron_expression SET NOT NULL;

ALTER TABLE scheduled_intervals
    ALTER COLUMN job_id SET NOT NULL,
ALTER COLUMN interval_in_millis SET NOT NULL;

ALTER TABLE scheduled_jobs
    ADD CONSTRAINT uq_scheduled_jobs_job_name UNIQUE (job_name);

ALTER TABLE scheduled_jobs
    ADD CONSTRAINT chk_scheduled_jobs_job_name_not_blank
        CHECK (length(trim(job_name)) > 0);

ALTER TABLE scheduled_dates
    ADD CONSTRAINT chk_scheduled_dates_cron_not_blank
        CHECK (length(trim(cron_expression)) > 0);

ALTER TABLE scheduled_intervals
    ADD CONSTRAINT chk_scheduled_intervals_positive
        CHECK (interval_in_millis > 0);

ALTER TABLE scheduled_dates
    ADD CONSTRAINT fk_scheduled_dates_job
        FOREIGN KEY (job_id)
            REFERENCES scheduled_jobs (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT;

ALTER TABLE scheduled_intervals
    ADD CONSTRAINT fk_scheduled_intervals_job
        FOREIGN KEY (job_id)
            REFERENCES scheduled_jobs (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_scheduled_dates_job_id
    ON scheduled_dates (job_id);

CREATE INDEX IF NOT EXISTS idx_scheduled_intervals_job_id
    ON scheduled_intervals (job_id);
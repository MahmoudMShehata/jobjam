CREATE TABLE jobs (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    requirements TEXT,
    benefits     TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

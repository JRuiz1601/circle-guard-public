CREATE TABLE system_settings (
    id BIGSERIAL PRIMARY KEY,
    unconfirmed_fencing_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auto_threshold_seconds BIGINT NOT NULL DEFAULT 3600
);

INSERT INTO system_settings (unconfirmed_fencing_enabled, auto_threshold_seconds)
SELECT TRUE, 3600
WHERE NOT EXISTS (SELECT 1 FROM system_settings);

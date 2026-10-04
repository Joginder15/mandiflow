--liquibase formatted sql

--changeset mandiflow:002-outbox
CREATE TABLE outbox_events (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  aggregate_id CHAR(36)     NOT NULL,
  topic        VARCHAR(100) NOT NULL,
  event_type   VARCHAR(200) NOT NULL,
  payload      JSON         NOT NULL,
  created_at   TIMESTAMP(6) NOT NULL,
  published_at TIMESTAMP(6) NULL,
  INDEX idx_pending (published_at, id)
);
--rollback DROP TABLE outbox_events;

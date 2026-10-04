--liquibase formatted sql

--changeset mandiflow:001-notifications
CREATE TABLE notifications (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id  CHAR(36)     NOT NULL,
  event_id  CHAR(36)     NOT NULL,
  channel   VARCHAR(20)  NOT NULL,   -- EMAIL
  recipient VARCHAR(200) NOT NULL,
  subject   VARCHAR(200) NOT NULL,
  sent_at   TIMESTAMP(6) NOT NULL,
  INDEX idx_notifications_order (order_id)
);
--rollback DROP TABLE notifications;

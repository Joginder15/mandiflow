--liquibase formatted sql

--changeset mandiflow:001-payments
CREATE TABLE payments (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id   CHAR(36)      NOT NULL,
  amount     DECIMAL(10,2) NOT NULL,
  status     VARCHAR(20)   NOT NULL,   -- PAID, DECLINED
  created_at TIMESTAMP(6)  NOT NULL,
  -- Last line of defence: one payment per order, even if an event arrives twice.
  CONSTRAINT uk_payments_order UNIQUE (order_id)
);
--rollback DROP TABLE payments;

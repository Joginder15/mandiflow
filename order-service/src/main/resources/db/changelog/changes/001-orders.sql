--liquibase formatted sql

--changeset mandiflow:001-orders
CREATE TABLE orders (
  id            CHAR(36)      PRIMARY KEY,
  customer_id   BIGINT        NOT NULL,
  status        VARCHAR(20)   NOT NULL,   -- PENDING, CONFIRMED, REJECTED, CANCELLED
  status_reason VARCHAR(255)  NULL,
  total_amount  DECIMAL(10,2) NOT NULL,
  version       BIGINT        NOT NULL DEFAULT 0,
  created_at    TIMESTAMP(6)  NOT NULL,
  INDEX idx_orders_customer (customer_id, created_at)
);

CREATE TABLE order_items (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id   CHAR(36)      NOT NULL,
  product_id BIGINT        NOT NULL,
  quantity   INT           NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
--rollback DROP TABLE order_items; DROP TABLE orders;

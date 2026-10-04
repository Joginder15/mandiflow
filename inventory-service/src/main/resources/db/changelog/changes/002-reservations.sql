--liquibase formatted sql

--changeset mandiflow:002-reservations
CREATE TABLE reservations (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id   CHAR(36)     NOT NULL,
  status     VARCHAR(20)  NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT uk_reservations_order UNIQUE (order_id)
);

CREATE TABLE reservation_items (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  reservation_id BIGINT NOT NULL,
  product_id     BIGINT NOT NULL,
  quantity       INT    NOT NULL,
  CONSTRAINT fk_res_items_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
);
--rollback DROP TABLE reservation_items; DROP TABLE reservations;

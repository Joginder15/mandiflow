--liquibase formatted sql

--changeset mandiflow:001-products
CREATE TABLE products (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(100)  NOT NULL,
  unit           VARCHAR(20)   NOT NULL,
  price_per_unit DECIMAL(10,2) NOT NULL,
  available_qty  INT           NOT NULL,
  reserved_qty   INT           NOT NULL DEFAULT 0,
  version        BIGINT        NOT NULL DEFAULT 0
);
--rollback DROP TABLE products;

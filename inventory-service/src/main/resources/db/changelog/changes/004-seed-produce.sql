--liquibase formatted sql

--changeset mandiflow:004-seed-produce
INSERT INTO products (id, name, unit, price_per_unit, available_qty) VALUES
  (1, 'Tomatoes',        'kg',    40.00, 120),
  (2, 'Kinnow',          'dozen', 60.00,  40),
  (3, 'Sarson saag',     'bunch', 30.00,  35),
  (4, 'Basmati rice',    'kg',   120.00, 100),
  (5, 'Onions',          'kg',    35.00, 150),
  (6, 'Fresh coriander', 'bunch', 10.00,  60);
--rollback DELETE FROM products WHERE id BETWEEN 1 AND 6;

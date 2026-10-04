-- Runs once, the first time the MySQL volume is created.
-- One database per service: a service may only touch its own.
CREATE DATABASE IF NOT EXISTS mandi_orders;
CREATE DATABASE IF NOT EXISTS mandi_inventory;
CREATE DATABASE IF NOT EXISTS mandi_payments;
CREATE DATABASE IF NOT EXISTS mandi_notifications;

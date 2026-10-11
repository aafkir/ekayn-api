CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), email varchar(320) NOT NULL UNIQUE, password_hash varchar(255) NOT NULL,
  first_name varchar(120), last_name varchar(120), phone varchar(80), email_verified boolean NOT NULL DEFAULT false,
  verification_token_hash varchar(64), verification_token_expires_at timestamp with time zone,
  reset_token_hash varchar(64), reset_token_expires_at timestamp with time zone, session_version bigint NOT NULL DEFAULT 0,
  role varchar(20) NOT NULL DEFAULT 'CUSTOMER', created_at timestamp with time zone NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS addresses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES users(id), label varchar(120) NOT NULL,
  line1 varchar(255) NOT NULL, line2 varchar(255), city varchar(120) NOT NULL, postal_code varchar(30) NOT NULL,
  country varchar(2) NOT NULL, default_address boolean NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX IF NOT EXISTS one_default_address_per_user ON addresses(user_id) WHERE default_address;
CREATE TABLE IF NOT EXISTS orders (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES users(id), payment_status varchar(30) NOT NULL DEFAULT 'UNPAID',
  delivery_status varchar(30) NOT NULL DEFAULT 'CONFIRMED', subtotal numeric(12,2) NOT NULL DEFAULT 0, shipping_cost numeric(12,2) NOT NULL DEFAULT 0,
  total numeric(12,2) NOT NULL, currency varchar(3) NOT NULL DEFAULT 'EUR', stripe_payment_intent_id varchar(255),
  stripe_checkout_session_id varchar(255) UNIQUE, stripe_checkout_url varchar(1000), checkout_idempotency_key varchar(100),
  carrier varchar(120), tracking_number varchar(255), created_at timestamp with time zone NOT NULL DEFAULT now(), updated_at timestamp with time zone NOT NULL DEFAULT now(),
  shipping_name varchar(255), shipping_line1 varchar(255), shipping_line2 varchar(255), shipping_city varchar(120), shipping_postal_code varchar(30), shipping_country varchar(2),
  UNIQUE(user_id, checkout_idempotency_key)
);
CREATE TABLE IF NOT EXISTS order_item (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_id uuid NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id varchar(100) NOT NULL, product_name varchar(255) NOT NULL, variant varchar(255), quantity integer NOT NULL, unit_price numeric(12,2) NOT NULL);
CREATE TABLE IF NOT EXISTS revoked_token (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), token_id varchar(255) NOT NULL UNIQUE, expires_at timestamp with time zone NOT NULL);
CREATE TABLE IF NOT EXISTS stripe_events (event_id varchar(255) PRIMARY KEY, processed_at timestamp with time zone NOT NULL DEFAULT now());

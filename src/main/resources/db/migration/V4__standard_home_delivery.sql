UPDATE orders SET shipping_method = 'STANDARD_HOME_DELIVERY', shipping_method_label = 'Livraison à domicile avec suivi';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_address_snapshot text;
UPDATE orders SET shipping_address_snapshot = concat_ws(E'\n', shipping_name, shipping_line1, shipping_line2, shipping_postal_code || ' ' || shipping_city, shipping_country) WHERE shipping_address_snapshot IS NULL;
ALTER TABLE orders DROP COLUMN IF EXISTS relay_id;
ALTER TABLE orders DROP COLUMN IF EXISTS relay_name;
ALTER TABLE orders DROP COLUMN IF EXISTS relay_address;
ALTER TABLE orders DROP COLUMN IF EXISTS relay_postal_code;
ALTER TABLE orders DROP COLUMN IF EXISTS relay_city;

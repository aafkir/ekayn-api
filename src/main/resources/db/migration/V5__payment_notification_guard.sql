ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_notification_sent boolean NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS service_reviews (
  id         BIGSERIAL PRIMARY KEY,
  booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
  username   varchar(255) NOT NULL,
  service    varchar(255) NOT NULL,
  barber     varchar(255),
  rating     smallint NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment    text,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now(),
  UNIQUE (booking_id)   -- 1 การจอง รีวิวได้ครั้งเดียว
);

CREATE INDEX IF NOT EXISTS ix_reviews_service  ON service_reviews(service);
CREATE INDEX IF NOT EXISTS ix_reviews_barber   ON service_reviews(barber);
CREATE INDEX IF NOT EXISTS ix_reviews_created  ON service_reviews(created_at DESC);

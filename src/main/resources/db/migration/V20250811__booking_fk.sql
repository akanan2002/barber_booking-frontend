ALTER TABLE bookings
  ADD CONSTRAINT IF NOT EXISTS fk_bookings_user
  FOREIGN KEY (username) REFERENCES users(username)
  ON DELETE CASCADE;

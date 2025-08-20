-- ==========================================
-- ✅ V2__init_bookings.sql
-- Booking table linked with users
-- ==========================================

CREATE TABLE IF NOT EXISTS bookings (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    service VARCHAR(100) NOT NULL,
    booking_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    CONSTRAINT fk_bookings_user FOREIGN KEY (username)
        REFERENCES users(username)
        ON DELETE CASCADE
);

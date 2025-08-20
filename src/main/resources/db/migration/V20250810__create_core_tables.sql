-- Users table
CREATE TABLE IF NOT EXISTS users (
  id          BIGSERIAL PRIMARY KEY,
  username    VARCHAR(100)  NOT NULL UNIQUE,
  password    VARCHAR(255)  NOT NULL,
  full_name   VARCHAR(255),
  email       VARCHAR(255),
  avatar_url  VARCHAR(512),
  role        VARCHAR(50)   NOT NULL,
  created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- Bookings table
CREATE TABLE IF NOT EXISTS bookings (
  id        BIGSERIAL PRIMARY KEY,
  username  VARCHAR(100) NOT NULL,
  service   VARCHAR(100) NOT NULL,
  barber    VARCHAR(100),
  date      DATE         NOT NULL,
  time      VARCHAR(20)  NOT NULL,
  status    VARCHAR(50)  NOT NULL,
  note      TEXT
);

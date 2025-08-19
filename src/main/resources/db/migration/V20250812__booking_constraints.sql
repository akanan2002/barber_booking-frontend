-- Default ของ status
ALTER TABLE bookings
    ALTER COLUMN status SET DEFAULT 'รอดำเนินการ';

-- กันจองชนกัน: ช่างคนเดียว ช่องเวลาเดียวกัน ห้ามซ้ำ
CREATE UNIQUE INDEX IF NOT EXISTS ux_bookings_barber_slot
    ON bookings (date, time, barber);

-- ดัชนีช่วยค้นหาแดชบอร์ด
CREATE INDEX IF NOT EXISTS ix_bookings_status ON bookings (status);
CREATE INDEX IF NOT EXISTS ix_bookings_barber ON bookings (barber);
CREATE INDEX IF NOT EXISTS ix_bookings_date   ON bookings (date);

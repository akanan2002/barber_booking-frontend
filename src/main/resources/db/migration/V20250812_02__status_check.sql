-- ล็อกค่าคอลัมน์ status ให้มีได้เฉพาะ 4 ค่านี้ (รันซ้ำได้ ปลอดภัย)
ALTER TABLE IF EXISTS bookings
  DROP CONSTRAINT IF EXISTS ck_bookings_status;

ALTER TABLE IF EXISTS bookings
  ADD CONSTRAINT ck_bookings_status
  CHECK (status IN ('รอดำเนินการ','ยืนยันแล้ว','ยกเลิก','เสร็จสิ้น'));

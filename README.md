# Barber Booking — Frontend

UI สำหรับจองคิวร้านตัดผม: เลือกบริการ/ช่าง ดูตารางคิว และยืนยันนัดหมาย  
> Demo (ออนไลน์เป็นช่วง ๆ ผ่าน ngrok): https://936be72361c8.ngrok-free.app

## Screenshots
![Home](docs/screenshot-home.png)
![Calendar](docs/screenshot-calendar.png)



## Features
- เลือกบริการและช่าง
- ปฏิทินเลือกช่วงเวลาจอง (responsive)
- ดู/ยืนยัน/ยกเลิกนัด
- [WIP] แจ้งเตือนพื้นฐาน

## Tech Stack
- **React 18** + **Vite**
- **Tailwind CSS**
- ติดต่อ **REST API** จาก Backend (Java/Spring Boot)

## Requirements
- Node.js ≥ 18, npm ≥ 9

## Environment
สร้างไฟล์ `.env` จากตัวอย่างด้านล่าง
```env
VITE_API_BASE_URL=http://localhost:8080

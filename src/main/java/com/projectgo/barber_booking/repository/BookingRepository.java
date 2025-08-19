package com.projectgo.barber_booking.repository;

import com.projectgo.barber_booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    // ฝั่งผู้ใช้
    List<Booking> findAllByUsername(String username);
    long countByDate(LocalDate date);
    long countByStatusAndDate(String status, LocalDate date);
    boolean existsByBarberAndDateAndTime(String barber, LocalDate d, LocalTime t);

    @Query("select distinct b.service from Booking b order by b.service asc")
    List<String> distinctServices();


}

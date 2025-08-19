package com.projectgo.barber_booking.dto;

import java.time.LocalDate;

public class AdminStatsDTO {
    private LocalDate date;
    private long total;
    private long pending;
    private long confirmed;
    private long done;
    private long canceled;

    public AdminStatsDTO(LocalDate date, long total, long pending, long confirmed, long done, long canceled) {
        this.date = date;
        this.total = total;
        this.pending = pending;
        this.confirmed = confirmed;
        this.done = done;
        this.canceled = canceled;
    }
    public LocalDate getDate() { return date; }
    public long getTotal() { return total; }
    public long getPending() { return pending; }
    public long getConfirmed() { return confirmed; }
    public long getDone() { return done; }
    public long getCanceled() { return canceled; }
}

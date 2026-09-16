package com.mentora.common;

import java.time.LocalDate;

public interface Schedulable {

    LocalDate getScheduledDate();

    void reschedule(LocalDate newDate);
}

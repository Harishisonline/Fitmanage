package com.ironpump.fitmanage.model;

import java.time.LocalDate;

/** One calendar day and how many check-ins landed on it. */
public record DayCount(LocalDate day, int count) {}

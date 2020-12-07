package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

public class JSTemporalDurationObject extends JSNonProxyObject {

    private final long years;
    private final long months;
    private final long weeks;
    private final long days;
    private final long hours;
    private final long minutes;
    private final long seconds;
    private final long milliseconds;
    private final long microseconds;
    private final long nanoseconds;

    protected JSTemporalDurationObject(Shape shape) {
        this(shape, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public JSTemporalDurationObject(Shape shape, long years, long months, long weeks, long days, long hours,
                                    long minutes, long seconds, long milliseconds, long microseconds, long nanoseconds) {
        super(shape);
        this.years = years;
        this.months = months;
        this.weeks = weeks;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds;
        this.microseconds = microseconds;
        this.nanoseconds = nanoseconds;
    }

    public long getYears() {
        return years;
    }

    public long getMonths() {
        return months;
    }

    public long getWeeks() {
        return weeks;
    }

    public long getDays() {
        return days;
    }

    public long getHours() {
        return hours;
    }

    public long getMinutes() {
        return minutes;
    }

    public long getSeconds() {
        return seconds;
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    public long getMicroseconds() {
        return microseconds;
    }

    public long getNanoseconds() {
        return nanoseconds;
    }
}

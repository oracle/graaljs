package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

public class JSTemporalTimeObject extends JSNonProxyObject {

    private final int hours;
    private final int minutes;
    private final int seconds;
    private final int milliseconds;
    private final int microseconds;
    private final int nanoseconds;

    protected JSTemporalTimeObject(Shape shape) {
        super(shape);
        this.hours = 0;
        this.minutes = 0;
        this.seconds = 0;
        this.milliseconds = 0;
        this.microseconds = 0;
        this.nanoseconds = 0;
    }

    protected JSTemporalTimeObject(Shape shape, int hours, int minutes, int seconds, int milliseconds, int microseconds,
                                   int nanoseconds) {
        super(shape);
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds;
        this.microseconds = microseconds;
        this.nanoseconds = nanoseconds;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getMilliseconds() {
        return milliseconds;
    }

    public int getMicroseconds() {
        return microseconds;
    }

    public int getNanoseconds() {
        return nanoseconds;
    }
}

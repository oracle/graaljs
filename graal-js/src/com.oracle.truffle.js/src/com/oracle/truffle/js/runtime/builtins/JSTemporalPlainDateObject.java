package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

public class JSTemporalPlainDateObject extends JSNonProxyObject {

    private final int year;
    private final int month;
    private final int day;
    // TODO: Add a calendar object.

    protected JSTemporalPlainDateObject(Shape shape) {
        super(shape);
        this.year = 0;
        this.month = 0;
        this.day = 0;
    }

    public JSTemporalPlainDateObject(Shape shape, int year, int month, int day) {
        super(shape);
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }
}

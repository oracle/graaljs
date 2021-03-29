package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

public class JSTemporalPlainMonthDayObject extends JSNonProxyObject {

    private final long isoMonth;
    private final long isoDay;
    private final JSTemporalCalendarObject calendar;
    private final long isoYear;

    protected JSTemporalPlainMonthDayObject(Shape shape, long isoMonth, long isoDay, JSTemporalCalendarObject calendar,
                                            long isoYear) {
        super(shape);
        this.isoMonth = isoMonth;
        this.isoDay = isoDay;
        this.calendar = calendar;
        this.isoYear = isoYear;
    }
}

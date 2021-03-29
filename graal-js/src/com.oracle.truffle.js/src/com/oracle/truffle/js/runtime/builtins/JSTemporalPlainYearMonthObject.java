package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

public class JSTemporalPlainYearMonthObject extends JSNonProxyObject {

    private final long isoYear;
    private final long isoMonth;
    private final long isoDay;
    private final JSTemporalCalendarObject calendar;

    protected JSTemporalPlainYearMonthObject(Shape shape, long isoYear, long isoMonth, long isoDay,
                                             JSTemporalCalendarObject calendar) {
        super(shape);
        this.isoYear = isoYear;
        this.isoMonth = isoMonth;
        this.isoDay = isoDay;
        this.calendar = calendar;
    }

    public long getIsoYear() {
        return isoYear;
    }

    public long getIsoMonth() {
        return isoMonth;
    }

    public long getIsoDay() {
        return isoDay;
    }

    public JSTemporalCalendarObject getCalendar() {
        return calendar;
    }
}

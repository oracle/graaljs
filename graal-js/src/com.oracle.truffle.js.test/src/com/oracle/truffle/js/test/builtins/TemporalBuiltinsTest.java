package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TemporalBuiltinsTest extends JSTest {

    private Context getJSContext() {
        return JSTest.newContextBuilder(ID).option("js.ecmascript-version", "2022").build();
    }

    private void validatePlainTime(Context ctx, long hour, long minute, long second, long millisecond, long microsecond,
                                   long nanosecond) {
        final Value hourValue = ctx.eval(ID, "plainTime.hour");
        final Value minuteValue = ctx.eval(ID, "plainTime.minute");
        final Value secondValue = ctx.eval(ID, "plainTime.second");
        final Value millisecondValue = ctx.eval(ID, "plainTime.millisecond");
        final Value microsecondValue = ctx.eval(ID, "plainTime.microsecond");
        final Value nanosecondValue = ctx.eval(ID, "plainTime.nanosecond");

        assertEquals(hour, hourValue.asLong());
        assertEquals(minute, minuteValue.asLong());
        assertEquals(second, secondValue.asLong());
        assertEquals(millisecond, millisecondValue.asLong());
        assertEquals(microsecond, microsecondValue.asLong());
        assertEquals(nanosecond, nanosecondValue.asLong());
    }

    @Test
    public void testPlainTimeCreation() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(12, 45, 35, 520, 450, 860);");
            validatePlainTime(ctx, 12, 45, 35, 520, 450, 860);
        }
    }

    @Test
    public void testPlainTimeToLocaleString() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime(10, 25, 5, 500, 400, 760);");
            Value toString = ctx.eval(ID, "plainTime.toLocaleString();");
            assertEquals("10:25:05.50040076", toString.asString());
        }
    }

    @Test
    public void testPlainTimeToValueOf() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainTime = new Temporal.PlainTime()");
            ctx.eval(ID, "plainTime.valueOf();");
        } catch (PolyglotException e) {
            assertEquals(e.getMessage(), "TypeError: Not supported.");
        }
    }

    private void validatePlainYearMonth(Context ctx, long year, long month, String monthCode, long daysInYear,
                                        long daysInMonth, long monthsInYear, boolean inLeapYear) {
        final Value yearValue = ctx.eval(ID, "plainYearMonth.year");
        final Value monthValue = ctx.eval(ID, "plainYearMonth.month");
        final Value monthCodeValue = ctx.eval(ID, "plainYearMonth.monthCode");
        final Value daysInYearValue = ctx.eval(ID, "plainYearMonth.daysInYear");
        final Value daysInMonthValue = ctx.eval(ID, "plainYearMonth.daysInMonth");
        final Value monthsInYearValue = ctx.eval(ID, "plainYearMonth.monthsInYear");
        final Value inLeapYearValue = ctx.eval(ID, "plainYearMonth.inLeapYear");

        assertEquals(year, yearValue.asLong());
        assertEquals(month, monthValue.asLong());
        assertEquals(monthCode, monthCodeValue.asString());
        assertEquals(daysInYear, daysInYearValue.asLong());
        assertEquals(daysInMonth, daysInMonthValue.asLong());
        assertEquals(monthsInYear, monthsInYearValue.asLong());
        assertEquals(inLeapYear, inLeapYearValue.asBoolean());
    }

    @Test
    public void testPlainYearMonthCreation() {
        try (Context ctx = getJSContext()) {
            ctx.eval(ID, "let plainYearMonth = new Temporal.PlainYearMonth(2021, 4)");
            validatePlainYearMonth(ctx, 2021, 4, "M04", 365, 30, 12, false);
        }
    }
}

/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.builtins.intl;

import java.util.Set;

import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleBaseNameAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleCalendarAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleCalendarsAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleCaseFirstAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleCollationAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleCollationsAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleHourCycleAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleHourCyclesAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleNumericAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleNumberingSystemAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleNumberingSystemsAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleLanguageAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleScriptAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleRegionAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleTextInfoAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleTimeZonesAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleWeekInfoAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleMaximizeNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleMinimizeNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleToStringNodeGen;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocale;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

public final class LocalePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<LocalePrototypeBuiltins.LocalePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new LocalePrototypeBuiltins();

    protected LocalePrototypeBuiltins() {
        super(JSLocale.PROTOTYPE_NAME, LocalePrototype.class);
    }

    public enum LocalePrototype implements BuiltinEnum<LocalePrototype> {
        maximize(0),
        minimize(0),
        toString(0),

        // getters
        baseName(0),
        calendar(0),
        caseFirst(0),
        collation(0),
        hourCycle(0),
        numeric(0),
        numberingSystem(0),
        language(0),
        script(0),
        region(0),

        calendars(0),
        collations(0),
        hourCycles(0),
        numberingSystems(0),
        timeZones(0),
        textInfo(0),
        weekInfo(0);

        private final int length;

        LocalePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return baseName.ordinal() <= ordinal();
        }

    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, LocalePrototype builtinEnum) {
        switch (builtinEnum) {
            case maximize:
                return JSLocaleMaximizeNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case minimize:
                return JSLocaleMinimizeNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toString:
                return JSLocaleToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case baseName:
                return JSLocaleBaseNameAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case calendar:
                return JSLocaleCalendarAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case caseFirst:
                return JSLocaleCaseFirstAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case collation:
                return JSLocaleCollationAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case hourCycle:
                return JSLocaleHourCycleAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case numeric:
                return JSLocaleNumericAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case numberingSystem:
                return JSLocaleNumberingSystemAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case language:
                return JSLocaleLanguageAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case script:
                return JSLocaleScriptAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case region:
                return JSLocaleRegionAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case calendars:
                return JSLocaleCalendarsAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case collations:
                return JSLocaleCollationsAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case hourCycles:
                return JSLocaleHourCyclesAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case numberingSystems:
                return JSLocaleNumberingSystemsAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case timeZones:
                return JSLocaleTimeZonesAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case textInfo:
                return JSLocaleTextInfoAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case weekInfo:
                return JSLocaleWeekInfoAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSLocaleMaximizeNode extends JSBuiltinNode {

        public JSLocaleMaximizeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            String maximizedLocale = JSLocale.getInternalState(localeObject).maximize();
            return JSFunction.construct(getRealm().getLocaleConstructor(), new Object[]{maximizedLocale});
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }
    }

    public abstract static class JSLocaleMinimizeNode extends JSBuiltinNode {

        public JSLocaleMinimizeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            String minimizedLocale = JSLocale.getInternalState(localeObject).minimize();
            return JSFunction.construct(getRealm().getLocaleConstructor(), new Object[]{minimizedLocale});
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }
    }

    public abstract static class JSLocaleToStringNode extends JSBuiltinNode {

        public JSLocaleToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public String doLocale(DynamicObject localeObject) {
            return JSLocale.getInternalState(localeObject).getLocale();
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public String doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }
    }

    public abstract static class JSLocaleBaseNameAccessor extends JSBuiltinNode {

        public JSLocaleBaseNameAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public String doLocale(DynamicObject localeObject) {
            return JSLocale.getInternalState(localeObject).getBaseName();
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public String doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleCalendarAccessor extends JSBuiltinNode {

        public JSLocaleCalendarAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            return JSRuntime.nullToUndefined(JSLocale.getInternalState(localeObject).getCalendar());
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleCaseFirstAccessor extends JSBuiltinNode {

        public JSLocaleCaseFirstAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            return JSRuntime.nullToUndefined(JSLocale.getInternalState(localeObject).getCaseFirst());
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleCollationAccessor extends JSBuiltinNode {

        public JSLocaleCollationAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            return JSRuntime.nullToUndefined(JSLocale.getInternalState(localeObject).getCollation());
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleHourCycleAccessor extends JSBuiltinNode {

        public JSLocaleHourCycleAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            return JSRuntime.nullToUndefined(JSLocale.getInternalState(localeObject).getHourCycle());
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleNumericAccessor extends JSBuiltinNode {

        public JSLocaleNumericAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public boolean doLocale(DynamicObject localeObject) {
            return JSLocale.getInternalState(localeObject).getNumeric();
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public boolean doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleNumberingSystemAccessor extends JSBuiltinNode {

        public JSLocaleNumberingSystemAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            return JSRuntime.nullToUndefined(JSLocale.getInternalState(localeObject).getNumberingSystem());
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleLanguageAccessor extends JSBuiltinNode {

        public JSLocaleLanguageAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            String language = JSLocale.getInternalState(localeObject).getLanguage();
            return language.isEmpty() ? Undefined.instance : language;
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleScriptAccessor extends JSBuiltinNode {

        public JSLocaleScriptAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            String script = JSLocale.getInternalState(localeObject).getScript();
            return script.isEmpty() ? Undefined.instance : script;
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleRegionAccessor extends JSBuiltinNode {

        public JSLocaleRegionAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            String region = JSLocale.getInternalState(localeObject).getRegion();
            return region.isEmpty() ? Undefined.instance : region;
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleCalendarsAccessor extends JSBuiltinNode {

        public JSLocaleCalendarsAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            ULocale locale = JSLocale.getInternalState(localeObject).getULocale();
            String calendar = locale.getUnicodeLocaleType("ca");
            String[] calendars;
            if (calendar == null) {
                calendars = IntlUtil.availableCalendars(locale, true);
            } else {
                calendars = new String[]{calendar};
            }
            return JSArray.createConstantObjectArray(getContext(), getRealm(), calendars);
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleCollationsAccessor extends JSBuiltinNode {

        public JSLocaleCollationsAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            ULocale locale = JSLocale.getInternalState(localeObject).getULocale();
            String collation = locale.getUnicodeLocaleType("co");
            String[] collations;
            if (collation == null) {
                collations = IntlUtil.availableCollations(locale, true);
            } else {
                collations = new String[]{collation};
            }
            return JSArray.createConstantObjectArray(getContext(), getRealm(), collations);
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleHourCyclesAccessor extends JSBuiltinNode {

        public JSLocaleHourCyclesAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            ULocale locale = JSLocale.getInternalState(localeObject).getULocale();
            String hourCycle = locale.getUnicodeLocaleType("hc");
            if (hourCycle == null) {
                DateTimePatternGenerator patternGenerator = DateTimePatternGenerator.getInstance(locale);
                hourCycle = IntlUtil.toJSHourCycle(patternGenerator.getDefaultHourCycle());
            }
            return JSArray.createConstantObjectArray(getContext(), getRealm(), new Object[]{hourCycle});
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleNumberingSystemsAccessor extends JSBuiltinNode {

        public JSLocaleNumberingSystemsAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            ULocale locale = JSLocale.getInternalState(localeObject).getULocale();
            String numberingSystem = locale.getUnicodeLocaleType("nu");
            if (numberingSystem == null) {
                numberingSystem = NumberingSystem.getInstance(locale).getName();
            }
            return JSArray.createConstantObjectArray(getContext(), getRealm(), new Object[]{numberingSystem});
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleTimeZonesAccessor extends JSBuiltinNode {

        public JSLocaleTimeZonesAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            ULocale locale = JSLocale.getInternalState(localeObject).getULocale();
            String region = locale.getCountry();
            if (region.isEmpty()) {
                return Undefined.instance;
            } else {
                Set<String> timeZoneSet = TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL, region, null);
                Object[] timeZones = timeZoneSet.toArray(new Object[timeZoneSet.size()]);
                return JSArray.createConstantObjectArray(getContext(), getRealm(), timeZones);
            }
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleTextInfoAccessor extends JSBuiltinNode {
        @Child CreateDataPropertyNode createDirectionNode = CreateDataPropertyNode.create(getContext(), IntlUtil.DIRECTION);

        public JSLocaleTextInfoAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            DynamicObject textInfo = JSOrdinary.create(getContext(), getRealm());
            createDirectionNode.executeVoid(textInfo, direction(localeObject));
            return textInfo;
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

        @TruffleBoundary
        private static String direction(DynamicObject localeObject) {
            ULocale locale = JSLocale.getInternalState(localeObject).getULocale();
            String orientation = locale.getCharacterOrientation();
            return "right-to-left".equals(orientation) ? IntlUtil.RTL : IntlUtil.LTR;
        }

    }

    public abstract static class JSLocaleWeekInfoAccessor extends JSBuiltinNode {
        @Child CreateDataPropertyNode createFirstDayNode = CreateDataPropertyNode.create(getContext(), IntlUtil.FIRST_DAY);
        @Child CreateDataPropertyNode createWeekendNode = CreateDataPropertyNode.create(getContext(), IntlUtil.WEEKEND);
        @Child CreateDataPropertyNode createMinimalDaysNode = CreateDataPropertyNode.create(getContext(), IntlUtil.MINIMAL_DAYS);

        public JSLocaleWeekInfoAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSLocale(localeObject)")
        public Object doLocale(DynamicObject localeObject) {
            Calendar.WeekData weekData = weekData(localeObject);

            int firstDay = calendarToECMAScriptDay(weekData.firstDayOfWeek);
            int minimalDays = weekData.minimalDaysInFirstWeek;

            SimpleArrayList<Integer> weekendList = new SimpleArrayList<>(7);
            int weekendCease = weekData.weekendCease;
            int day = weekData.weekendOnset;
            while (true) {
                weekendList.add(calendarToECMAScriptDay(day), null);
                if (day == weekendCease) {
                    break;
                }
                if (day == Calendar.SATURDAY) {
                    day = Calendar.SUNDAY;
                } else {
                    day++;
                }
            }
            JSContext context = getContext();
            JSRealm realm = getRealm();
            Object weekend = JSArray.createConstantObjectArray(context, realm, weekendList.toArray());

            DynamicObject weekInfo = JSOrdinary.create(context, realm);
            createFirstDayNode.executeVoid(weekInfo, firstDay);
            createWeekendNode.executeVoid(weekInfo, weekend);
            createMinimalDaysNode.executeVoid(weekInfo, minimalDays);
            return weekInfo;
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

        @TruffleBoundary
        private static Calendar.WeekData weekData(DynamicObject localeObject) {
            ULocale locale = JSLocale.getInternalState(localeObject).getULocale();
            return Calendar.getInstance(locale).getWeekData();
        }

        private static int calendarToECMAScriptDay(int day) {
            return (day == Calendar.SUNDAY) ? 7 : (day - 1);
        }

    }

}

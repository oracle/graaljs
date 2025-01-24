/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.shadowed.com.ibm.icu.text.DateTimePatternGenerator;
import org.graalvm.shadowed.com.ibm.icu.text.NumberingSystem;
import org.graalvm.shadowed.com.ibm.icu.util.Calendar;
import org.graalvm.shadowed.com.ibm.icu.util.TimeZone;
import org.graalvm.shadowed.com.ibm.icu.util.ULocale;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleBaseNameAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleCalendarAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleCaseFirstAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleCollationAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleFirstDayOfWeekAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleGetCalendarsNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleGetCollationsNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleGetHourCyclesNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleGetNumberingSystemsNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleGetTextInfoNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleGetTimeZonesNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleGetWeekInfoNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleHourCycleAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleLanguageAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleMaximizeNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleMinimizeNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleNumberingSystemAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleNumericAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleRegionAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleScriptAccessorNodeGen;
import com.oracle.truffle.js.builtins.intl.LocalePrototypeBuiltinsFactory.JSLocaleToStringNodeGen;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.InitializeLocaleNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocale;
import com.oracle.truffle.js.runtime.builtins.intl.JSLocaleObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
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
        firstDayOfWeek(0),
        hourCycle(0),
        numeric(0),
        numberingSystem(0),
        language(0),
        script(0),
        region(0),

        getCalendars(0),
        getCollations(0),
        getHourCycles(0),
        getNumberingSystems(0),
        getTimeZones(0),
        getTextInfo(0),
        getWeekInfo(0);

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
            return baseName.ordinal() <= ordinal() && ordinal() <= region.ordinal();
        }

        @Override
        public int getECMAScriptVersion() {
            if (getCalendars.ordinal() <= ordinal()) {
                return JSConfig.StagingECMAScriptVersion;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
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
            case firstDayOfWeek:
                return JSLocaleFirstDayOfWeekAccessorNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
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
            case getCalendars:
                return JSLocaleGetCalendarsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case getCollations:
                return JSLocaleGetCollationsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case getHourCycles:
                return JSLocaleGetHourCyclesNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case getNumberingSystems:
                return JSLocaleGetNumberingSystemsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case getTimeZones:
                return JSLocaleGetTimeZonesNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case getTextInfo:
                return JSLocaleGetTextInfoNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case getWeekInfo:
                return JSLocaleGetWeekInfoNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSLocaleMaximizeNode extends JSBuiltinNode {

        public JSLocaleMaximizeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            String maximizedLocale = localeObject.getInternalState().maximize();
            return JSFunction.construct(getRealm().getLocaleConstructor(), new Object[]{Strings.fromJavaString(maximizedLocale)});
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

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            String minimizedLocale = localeObject.getInternalState().minimize();
            return JSFunction.construct(getRealm().getLocaleConstructor(), new Object[]{Strings.fromJavaString(minimizedLocale)});
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

        @Specialization
        public TruffleString doLocale(JSLocaleObject localeObject) {
            return Strings.fromJavaString(localeObject.getInternalState().getLocale());
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public TruffleString doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }
    }

    public abstract static class JSLocaleBaseNameAccessor extends JSBuiltinNode {

        public JSLocaleBaseNameAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public TruffleString doLocale(JSLocaleObject localeObject) {
            return Strings.fromJavaString(localeObject.getInternalState().getBaseName());
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

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            String calendar = localeObject.getInternalState().getCalendar();
            if (calendar == null) {
                return Undefined.instance;
            } else {
                return Strings.fromJavaString(calendar);
            }
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

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            String caseFirst = localeObject.getInternalState().getCaseFirst();
            if (caseFirst == null) {
                return Undefined.instance;
            } else {
                return Strings.fromJavaString(caseFirst);
            }
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

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            String collation = localeObject.getInternalState().getCollation();
            if (collation == null) {
                return Undefined.instance;
            } else {
                return Strings.fromJavaString(collation);
            }
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleFirstDayOfWeekAccessor extends JSBuiltinNode {

        public JSLocaleFirstDayOfWeekAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doLocale(JSLocaleObject localeObject,
                        @Cached TruffleString.FromJavaStringNode fromJavaScriptNode) {
            String fw = localeObject.getInternalState().getFirstDayOfWeek();
            return (fw == null) ? Undefined.instance : Strings.fromJavaString(fromJavaScriptNode, fw);
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

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            String hourCycle = localeObject.getInternalState().getHourCycle();
            if (hourCycle == null) {
                return Undefined.instance;
            } else {
                return Strings.fromJavaString(hourCycle);
            }
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

        @Specialization
        public boolean doLocale(JSLocaleObject localeObject) {
            return localeObject.getInternalState().getNumeric();
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

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            String numberingSystem = localeObject.getInternalState().getNumberingSystem();
            if (numberingSystem == null) {
                return Undefined.instance;
            } else {
                return Strings.fromJavaString(numberingSystem);
            }
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

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            String language = localeObject.getInternalState().getLanguage();
            return language.isEmpty() ? Undefined.instance : Strings.fromJavaString(language);
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

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            String script = localeObject.getInternalState().getScript();
            return script.isEmpty() ? Undefined.instance : Strings.fromJavaString(script);
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

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            String region = localeObject.getInternalState().getRegion();
            return region.isEmpty() ? Undefined.instance : Strings.fromJavaString(region);
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleGetCalendarsNode extends JSBuiltinNode {

        public JSLocaleGetCalendarsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            ULocale locale = localeObject.getInternalState().getULocale();
            String calendar = locale.getUnicodeLocaleType("ca");
            String[] calendars;
            if (calendar == null) {
                calendars = IntlUtil.availableCalendars(locale, true);
            } else {
                calendars = new String[]{calendar};
            }
            return JSArray.createConstantObjectArray(getContext(), getRealm(), Strings.convertJavaStringArray(calendars));
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleGetCollationsNode extends JSBuiltinNode {

        public JSLocaleGetCollationsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            ULocale locale = localeObject.getInternalState().getULocale();
            String collation = locale.getUnicodeLocaleType("co");
            String[] collations;
            if (collation == null) {
                collations = IntlUtil.availableCollations(locale, true);
            } else {
                collations = new String[]{collation};
            }
            return JSArray.createConstantObjectArray(getContext(), getRealm(), Strings.convertJavaStringArray(collations));
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleGetHourCyclesNode extends JSBuiltinNode {

        public JSLocaleGetHourCyclesNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            ULocale locale = localeObject.getInternalState().getULocale();
            String hourCycle = locale.getUnicodeLocaleType("hc");
            if (hourCycle == null) {
                DateTimePatternGenerator patternGenerator = DateTimePatternGenerator.getInstance(locale);
                hourCycle = IntlUtil.toJSHourCycle(patternGenerator.getDefaultHourCycle());
            }
            return JSArray.createConstantObjectArray(getContext(), getRealm(), new Object[]{Strings.fromJavaString(hourCycle)});
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleGetNumberingSystemsNode extends JSBuiltinNode {

        public JSLocaleGetNumberingSystemsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            ULocale locale = localeObject.getInternalState().getULocale();
            String numberingSystem = locale.getUnicodeLocaleType("nu");
            if (numberingSystem == null) {
                numberingSystem = NumberingSystem.getInstance(locale).getName();
            }
            return JSArray.createConstantObjectArray(getContext(), getRealm(), new Object[]{Strings.fromJavaString(numberingSystem)});
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleGetTimeZonesNode extends JSBuiltinNode {

        public JSLocaleGetTimeZonesNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            ULocale locale = localeObject.getInternalState().getULocale();
            String region = locale.getCountry();
            if (region.isEmpty()) {
                return Undefined.instance;
            } else {
                Set<String> timeZoneSet = TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL, region, null);
                Object[] timeZones = new Object[timeZoneSet.size()];
                int i = 0;
                for (String timeZone : timeZoneSet) {
                    timeZones[i++] = Strings.fromJavaString(timeZone);
                }
                return JSArray.createConstantObjectArray(getContext(), getRealm(), timeZones);
            }
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

    }

    public abstract static class JSLocaleGetTextInfoNode extends JSBuiltinNode {
        @Child CreateDataPropertyNode createDirectionNode;

        public JSLocaleGetTextInfoNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createDirectionNode = CreateDataPropertyNode.create(context, IntlUtil.KEY_DIRECTION);
        }

        @Specialization
        public Object doLocale(JSLocaleObject localeObject) {
            JSDynamicObject textInfo = JSOrdinary.create(getContext(), getRealm());
            createDirectionNode.executeVoid(textInfo, direction(localeObject));
            return textInfo;
        }

        @Specialization(guards = "!isJSLocale(bummer)")
        public Object doOther(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorLocaleExpected();
        }

        @TruffleBoundary
        private static TruffleString direction(JSLocaleObject localeObject) {
            ULocale locale = localeObject.getInternalState().getULocale();
            return locale.isRightToLeft() ? IntlUtil.KEY_RTL : IntlUtil.KEY_LTR;
        }

    }

    public abstract static class JSLocaleGetWeekInfoNode extends JSBuiltinNode {
        @Child CreateDataPropertyNode createFirstDayNode;
        @Child CreateDataPropertyNode createWeekendNode;
        @Child CreateDataPropertyNode createMinimalDaysNode;

        public JSLocaleGetWeekInfoNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createFirstDayNode = CreateDataPropertyNode.create(context, IntlUtil.KEY_FIRST_DAY);
            this.createWeekendNode = CreateDataPropertyNode.create(context, IntlUtil.KEY_WEEKEND);
            this.createMinimalDaysNode = CreateDataPropertyNode.create(context, IntlUtil.KEY_MINIMAL_DAYS);
        }

        @Specialization
        public Object doLocale(JSLocaleObject localeObject,
                        @Cached InlinedBranchProfile growProfile) {
            Calendar.WeekData weekData = weekData(localeObject);

            int fw = InitializeLocaleNode.stringToWeekdayValue(localeObject.getInternalState().getFirstDayOfWeek());
            int firstDay;
            if (fw == -1) {
                firstDay = calendarToECMAScriptDay(weekData.firstDayOfWeek);
            } else {
                firstDay = fw;
            }
            int minimalDays = weekData.minimalDaysInFirstWeek;

            SimpleArrayList<Integer> weekendList = new SimpleArrayList<>(7);
            int weekendCease = weekData.weekendCease;
            int day = weekData.weekendOnset;
            while (true) {
                weekendList.add(calendarToECMAScriptDay(day), this, growProfile);
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

            JSObject weekInfo = JSOrdinary.create(context, realm);
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
        private static Calendar.WeekData weekData(JSLocaleObject localeObject) {
            ULocale locale = localeObject.getInternalState().getULocale();
            return Calendar.getInstance(locale).getWeekData();
        }

        private static int calendarToECMAScriptDay(int day) {
            return (day == Calendar.SUNDAY) ? 7 : (day - 1);
        }

    }

}

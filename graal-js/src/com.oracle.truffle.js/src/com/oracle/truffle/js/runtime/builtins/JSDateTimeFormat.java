/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.UnmodifiableEconomicMap;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;
import com.oracle.truffle.js.runtime.util.LazyValue;

public final class JSDateTimeFormat extends JSBuiltinObject implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "DateTimeFormat";
    public static final String PROTOTYPE_NAME = "DateTimeFormat.prototype";

    private static final HiddenKey INTERNAL_STATE_ID = new HiddenKey("_internalState");
    private static final Property INTERNAL_STATE_PROPERTY;

    static final HiddenKey BOUND_OBJECT_KEY = new HiddenKey(CLASS_NAME);

    public static final JSDateTimeFormat INSTANCE = new JSDateTimeFormat();

    /**
     * Maps the upper-case version of a supported time zone to the corresponding case-regularized
     * canonical ID.
     */
    private static final LazyValue<UnmodifiableEconomicMap<String, String>> canonicalTimeZoneIDMap = new LazyValue<>(JSDateTimeFormat::initCanonicalTimeZoneIDMap);

    static {
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        INTERNAL_STATE_PROPERTY = JSObjectUtil.makeHiddenProperty(INTERNAL_STATE_ID, allocator.locationForType(InternalState.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)));
    }

    private JSDateTimeFormat() {
    }

    public static boolean isJSDateTimeFormat(Object obj) {
        return JSObject.isDynamicObject(obj) && isJSDateTimeFormat((DynamicObject) obj);
    }

    public static boolean isJSDateTimeFormat(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject numberFormatPrototype = JSObject.createInit(realm, realm.getObjectPrototype(), JSUserObject.INSTANCE);
        JSObjectUtil.putConstructorProperty(ctx, numberFormatPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, numberFormatPrototype, PROTOTYPE_NAME);
        JSObjectUtil.putConstantAccessorProperty(ctx, numberFormatPrototype, "format", createFormatFunctionGetter(realm, ctx), Undefined.instance);
        JSObjectUtil.putDataProperty(ctx, numberFormatPrototype, Symbol.SYMBOL_TO_STRING_TAG, "Object", JSAttributes.configurableNotEnumerableNotWritable());
        return numberFormatPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
        initialShape = initialShape.addProperty(INTERNAL_STATE_PROPERTY);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static DynamicObject create(JSContext context) {
        InternalState state = new InternalState();
        DynamicObject result = JSObject.create(context, context.getDateTimeFormatFactory(), state);
        assert isJSDateTimeFormat(result);
        return result;
    }

    @TruffleBoundary
    public static void setupInternalDateTimeFormat(
                    JSContext ctx,
                    InternalState state, String[] locales,
                    String weekdayOpt,
                    String eraOpt,
                    String yearOpt,
                    String monthOpt,
                    String dayOpt,
                    String hourOpt,
                    String hcOpt,
                    Boolean hour12Opt,
                    String minuteOpt,
                    String secondOpt,
                    String tzNameOpt,
                    TimeZone timeZone) {
        String selectedTag = IntlUtil.selectedLocale(ctx, locales);
        Locale selectedLocale = selectedTag != null ? Locale.forLanguageTag(selectedTag) : ctx.getLocale();
        Locale strippedLocale = selectedLocale.stripExtensions();
        String skeleton = makeSkeleton(weekdayOpt, eraOpt, yearOpt, monthOpt, dayOpt, hourOpt, hcOpt, hour12Opt, minuteOpt, secondOpt, tzNameOpt);

        DateTimePatternGenerator patternGenerator = DateTimePatternGenerator.getInstance(strippedLocale);
        String bestPattern = patternGenerator.getBestPattern(skeleton);
        String baseSkeleton = patternGenerator.getBaseSkeleton(bestPattern);

        if (containsOneOf(baseSkeleton, "eEc")) {
            state.weekday = weekdayOpt;
        }

        if (baseSkeleton.contains("G")) {
            state.era = eraOpt;
        }

        if (containsOneOf(baseSkeleton, "YyUu")) {
            state.year = yearOpt;
        }

        if (containsOneOf(baseSkeleton, "ML")) {
            state.month = monthOpt;
        }

        if (containsOneOf(baseSkeleton, "dDFg")) {
            state.day = dayOpt;
        }

        if (containsOneOf(baseSkeleton, "hHKk")) {
            state.hour = hourOpt;
            state.hourCycle = hcOpt;
            state.hour12 = containsOneOf(baseSkeleton, "hK");
        }

        if (baseSkeleton.contains("m")) {
            state.minute = minuteOpt;
        }

        if (containsOneOf(baseSkeleton, "sSA")) {
            state.second = secondOpt;
        }

        state.initialized = true;

        SimpleDateFormat dateFormat = new SimpleDateFormat(bestPattern, strippedLocale);
        state.dateFormat = dateFormat;
        state.locale = strippedLocale.toLanguageTag();

        state.calendar = Calendar.getInstance(strippedLocale).getCalendarType();
        if ("gregory".equals(state.calendar)) {
            // Ensure that Gregorian calendar is used for all dates.
            // GregorianCalendar used by SimpleDateFormat is using
            // Julian calendar for dates before 1582 otherwise.
            com.ibm.icu.util.Calendar calendar = dateFormat.getCalendar();
            if (!(calendar instanceof GregorianCalendar)) {
                calendar = new GregorianCalendar(strippedLocale);
                dateFormat.setCalendar(calendar);
            }
            ((GregorianCalendar) calendar).setGregorianChange(new Date(Long.MIN_VALUE));
        }

        if (tzNameOpt != null && !tzNameOpt.isEmpty()) {
            state.timeZoneName = tzNameOpt;
        }

        state.dateFormat.setTimeZone(timeZone);
        state.timeZone = timeZone.getID();
    }

    private static String weekdayOptToSkeleton(String weekdayOpt) {
        if (weekdayOpt == null) {
            return "";
        }
        switch (weekdayOpt) {
            case IntlUtil.NARROW:
                return "eeeee";
            case IntlUtil.SHORT:
                return "eee";
            case IntlUtil.LONG:
                return "eeee";
        }
        return "";
    }

    private static String eraOptToSkeleton(String eraOpt) {
        if (eraOpt == null) {
            return "";
        }
        switch (eraOpt) {
            case IntlUtil.NARROW:
                return "GGGGG";
            case IntlUtil.SHORT:
                return "GGG";
            case IntlUtil.LONG:
                return "GGGG";
        }
        return "";
    }

    private static String yearOptToSkeleton(String yearOpt) {
        if (yearOpt == null) {
            return "";
        }
        switch (yearOpt) {
            case IntlUtil._2_DIGIT:
                return "yy";
            case IntlUtil.NUMERIC:
                return "y";
        }
        return "";
    }

    private static String monthOptToSkeleton(String monthOpt) {
        if (monthOpt == null) {
            return "";
        }
        switch (monthOpt) {
            case IntlUtil._2_DIGIT:
                return "MM";
            case IntlUtil.NUMERIC:
                return "M";
            case IntlUtil.NARROW:
                return "MMMMM";
            case IntlUtil.SHORT:
                return "MMM";
            case IntlUtil.LONG:
                return "MMMM";
        }
        return "";
    }

    private static String dayOptToSkeleton(String dayOpt) {
        if (dayOpt == null) {
            return "";
        }
        switch (dayOpt) {
            case IntlUtil._2_DIGIT:
                return "dd";
            case IntlUtil.NUMERIC:
                return "d";
        }
        return "";
    }

    private static String hourOptToSkeleton(String hourOpt, String hcOpt, Boolean hour12Opt) {
        if (hourOpt == null) {
            return "";
        }
        switch (hourOpt) {
            case IntlUtil._2_DIGIT:
                if (hcOpt == null) {
                    if (hour12Opt != null) {
                        if (hour12Opt) {
                            return "KK";
                        } else {
                            return "HH";
                        }
                    } else {
                        return "jj";
                    }
                } else {
                    switch (hcOpt) {
                        case IntlUtil.H11:
                            return "KK";
                        case IntlUtil.H12:
                            return "hh";
                        case IntlUtil.H23:
                            return "HH";
                        case IntlUtil.H24:
                            return "kk";
                    }
                }
                break;
            case IntlUtil.NUMERIC:
                if (hcOpt == null) {
                    if (hour12Opt != null) {
                        if (hour12Opt) {
                            return "K";
                        } else {
                            return "H";
                        }
                    } else {
                        return "j";
                    }
                } else {
                    switch (hcOpt) {
                        case IntlUtil.H11:
                            return "K";
                        case IntlUtil.H12:
                            return "h";
                        case IntlUtil.H23:
                            return "H";
                        case IntlUtil.H24:
                            return "k";
                    }
                }
        }
        return "";
    }

    private static String minuteOptToSkeleton(String minuteOpt) {
        if (minuteOpt == null) {
            return "";
        }
        switch (minuteOpt) {
            case IntlUtil._2_DIGIT:
                return "mm";
            case IntlUtil.NUMERIC:
                return "m";
        }
        return "";
    }

    private static String secondOptToSkeleton(String secondOpt) {
        if (secondOpt == null) {
            return "";
        }
        switch (secondOpt) {
            case IntlUtil._2_DIGIT:
                return "ss";
            case IntlUtil.NUMERIC:
                return "s";
        }
        return "";
    }

    private static String timeZoneNameOptToSkeleton(String timeZoneNameOpt) {
        if (timeZoneNameOpt == null) {
            return "";
        }
        switch (timeZoneNameOpt) {
            case IntlUtil.SHORT:
                return "v";
            case IntlUtil.LONG:
                return "vvvv";
        }
        return "";
    }

    private static String makeSkeleton(String weekdayOpt, String eraOpt, String yearOpt, String monthOpt, String dayOpt, String hourOpt, String hcOpt, Boolean hour12Opt, String minuteOpt,
                    String secondOpt, String timeZoneNameOpt) {
        return weekdayOptToSkeleton(weekdayOpt) + eraOptToSkeleton(eraOpt) + yearOptToSkeleton(yearOpt) + monthOptToSkeleton(monthOpt) + dayOptToSkeleton(dayOpt) +
                        hourOptToSkeleton(hourOpt, hcOpt, hour12Opt) +
                        minuteOptToSkeleton(minuteOpt) + secondOptToSkeleton(secondOpt) + timeZoneNameOptToSkeleton(timeZoneNameOpt);
    }

    private static UnmodifiableEconomicMap<String, String> initCanonicalTimeZoneIDMap() {
        CompilerAsserts.neverPartOfCompilation();
        EconomicMap<String, String> map = EconomicMap.create();
        for (String available : TimeZone.getAvailableIDs()) {
            map.put(IntlUtil.toUpperCase(available), TimeZone.getCanonicalID(available));
        }
        return map;
    }

    @TruffleBoundary
    public static TimeZone toTimeZone(Object tzVal) {
        if (tzVal != Undefined.instance) {
            String tzId = JSDateTimeFormat.canonicalizeTimeZone(JSRuntime.toString(tzVal));
            if (tzId != null) {
                return TimeZone.getTimeZone(tzId);
            } else {
                throw Errors.createRangeError(String.format("Invalid time zone %s", tzVal));
            }
        } else {
            return TimeZone.getDefault();
        }
    }

    @TruffleBoundary
    // https://tc39.github.io/ecma402/#sec-canonicalizetimezonename
    private static String canonicalizeTimeZone(String tzId) {
        String ucTzId = IntlUtil.toUpperCase(tzId);
        String canTzId = canonicalTimeZoneIDMap.get().get(ucTzId);
        if (canTzId == null) {
            return null;
        }
        if (canTzId.equals("Etc/UTC") || canTzId.equals("Etc/GMT")) {
            return "UTC";
        } else {
            return canTzId;
        }
    }

    private static boolean containsOneOf(String suspect, String containees) {
        for (int c : containees.getBytes()) {
            if (suspect.indexOf(c) > -1) {
                return true;
            }
        }
        return false;
    }

    public static DateFormat getDateFormatProperty(DynamicObject obj) {
        return getInternalState(obj).dateFormat;
    }

    @TruffleBoundary
    public static String format(JSContext context, DynamicObject numberFormatObj, Object n) {
        DateFormat dateFormat = getDateFormatProperty(numberFormatObj);
        return dateFormat.format(timeClip(context, n));
    }

    private static double timeClip(JSContext context, Object n) {
        double x;
        if (n == Undefined.instance) {
            x = context.getRealm().currentTimeMillis();
        } else {
            x = JSDate.timeClip(JSRuntime.toDouble(n));
            if (Double.isNaN(x)) {
                throwDateOutOfRange();
            }
        }
        return x;
    }

    private static void throwDateOutOfRange() throws JSException {
        throw Errors.createRangeError("Provided date is not in valid range.");
    }

    private static final LazyValue<UnmodifiableEconomicMap<DateFormat.Field, String>> fieldToTypeMap = new LazyValue<>(JSDateTimeFormat::initializeFieldToTypeMap);

    private static UnmodifiableEconomicMap<DateFormat.Field, String> initializeFieldToTypeMap() {
        CompilerAsserts.neverPartOfCompilation();
        EconomicMap<DateFormat.Field, String> map = EconomicMap.create(14);
        map.put(DateFormat.Field.AM_PM, "dayPeriod");
        map.put(DateFormat.Field.ERA, "era");
        map.put(DateFormat.Field.YEAR, "year");
        map.put(DateFormat.Field.MONTH, "month");
        map.put(DateFormat.Field.DOW_LOCAL, "weekday");
        map.put(DateFormat.Field.DAY_OF_WEEK, "weekday");
        map.put(DateFormat.Field.DAY_OF_MONTH, "day");
        map.put(DateFormat.Field.HOUR0, "hour");
        map.put(DateFormat.Field.HOUR1, "hour");
        map.put(DateFormat.Field.HOUR_OF_DAY0, "hour");
        map.put(DateFormat.Field.HOUR_OF_DAY1, "hour");
        map.put(DateFormat.Field.MINUTE, "minute");
        map.put(DateFormat.Field.SECOND, "second");
        map.put(DateFormat.Field.TIME_ZONE, "timeZoneName");
        return map;
    }

    private static String fieldToType(DateFormat.Field field) {
        return fieldToTypeMap.get().get(field);
    }

    @TruffleBoundary
    public static DynamicObject formatToParts(JSContext context, DynamicObject numberFormatObj, Object n) {

        DateFormat dateFormat = getDateFormatProperty(numberFormatObj);

        double x = timeClip(context, n);

        List<Object> resultParts = new ArrayList<>();
        AttributedCharacterIterator fit = dateFormat.formatToCharacterIterator(x);
        String formatted = dateFormat.format(x);
        int i = fit.getBeginIndex();
        while (i < fit.getEndIndex()) {
            fit.setIndex(i);
            Map<AttributedCharacterIterator.Attribute, Object> attributes = fit.getAttributes();
            Set<AttributedCharacterIterator.Attribute> attKeySet = attributes.keySet();
            if (!attKeySet.isEmpty()) {
                for (AttributedCharacterIterator.Attribute a : attKeySet) {
                    if (a instanceof DateFormat.Field) {
                        String value = formatted.substring(fit.getRunStart(), fit.getRunLimit());
                        String type = fieldToType((DateFormat.Field) a);
                        assert type != null : a;
                        resultParts.add(makePart(context, type, value));
                        i = fit.getRunLimit();
                        break;
                    } else {
                        throw Errors.shouldNotReachHere();
                    }
                }
            } else {
                String value = formatted.substring(fit.getRunStart(), fit.getRunLimit());
                resultParts.add(makePart(context, IntlUtil.LITERAL, value));
                i = fit.getRunLimit();
            }
        }
        return JSArray.createConstant(context, resultParts.toArray());
    }

    private static Object makePart(JSContext context, String type, String value) {
        DynamicObject p = JSUserObject.create(context);
        JSObject.set(p, IntlUtil.TYPE, type);
        JSObject.set(p, IntlUtil.VALUE, value);
        return p;
    }

    public static class InternalState {

        private boolean initialized = false;
        private DateFormat dateFormat;

        private DynamicObject boundFormatFunction = null;

        private String locale;
        private String calendar;
        private String numberingSystem = "latn";

        private String weekday;
        private String era;
        private String year;
        private String month;
        private String day;
        private String hour;
        private String minute;
        private String second;

        private Boolean hour12;
        private String hourCycle;

        private String timeZoneName;
        private String timeZone;

        DynamicObject toResolvedOptionsObject(JSContext context) {
            DynamicObject result = JSUserObject.create(context);
            JSObjectUtil.defineDataProperty(result, IntlUtil.LOCALE, locale, JSAttributes.getDefault());
            if (calendar != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.CALENDAR, calendar, JSAttributes.getDefault());
            }
            JSObjectUtil.defineDataProperty(result, IntlUtil.NUMBERING_SYSTEM, numberingSystem, JSAttributes.getDefault());
            if (timeZone != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.TIME_ZONE, timeZone, JSAttributes.getDefault());
            }
            if (hourCycle != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.HOUR_CYCLE, hourCycle, JSAttributes.getDefault());
            }
            if (hour12 != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.HOUR12, hour12, JSAttributes.getDefault());
            }
            if (weekday != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.WEEKDAY, weekday, JSAttributes.getDefault());
            }
            if (era != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.ERA, era, JSAttributes.getDefault());
            }
            if (year != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.YEAR, year, JSAttributes.getDefault());
            }
            if (month != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.MONTH, month, JSAttributes.getDefault());
            }
            if (day != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.DAY, day, JSAttributes.getDefault());
            }
            if (hour != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.HOUR, hour, JSAttributes.getDefault());
            }
            if (minute != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.MINUTE, minute, JSAttributes.getDefault());
            }
            if (second != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.SECOND, second, JSAttributes.getDefault());
            }
            if (timeZoneName != null) {
                JSObjectUtil.defineDataProperty(result, IntlUtil.TIME_ZONE_NAME, timeZoneName, JSAttributes.getDefault());
            }
            return result;
        }
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, DynamicObject numberFormatObj) {
        InternalState state = getInternalState(numberFormatObj);
        return state.toResolvedOptionsObject(context);
    }

    public static InternalState getInternalState(DynamicObject numberFormatObj) {
        return (InternalState) INTERNAL_STATE_PROPERTY.get(numberFormatObj, isJSDateTimeFormat(numberFormatObj));
    }

    private static CallTarget createGetFormatCallTarget(JSContext context) {
        return Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            private final BranchProfile errorBranch = BranchProfile.create();
            @CompilationFinal private ContextReference<JSRealm> realmRef;
            @Child private PropertySetNode setBoundObjectNode = PropertySetNode.createSetHidden(BOUND_OBJECT_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {

                Object[] frameArgs = frame.getArguments();
                Object dateTimeFormatObj = JSArguments.getThisObject(frameArgs);

                if (isJSDateTimeFormat(dateTimeFormatObj)) {

                    InternalState state = getInternalState((DynamicObject) dateTimeFormatObj);

                    if (state == null || !state.initialized) {
                        errorBranch.enter();
                        throw Errors.createTypeErrorMethodCalledOnNonObjectOrWrongType("format");
                    }

                    if (state.boundFormatFunction == null) {
                        if (realmRef == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            realmRef = lookupContextReference(JavaScriptLanguage.class);
                        }
                        JSFunctionData formatFunctionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.DateTimeFormatFormat, c -> createFormatFunctionData(c));
                        DynamicObject formatFn = JSFunction.create(realmRef.get(), formatFunctionData);
                        setBoundObjectNode.setValue(formatFn, dateTimeFormatObj);
                        state.boundFormatFunction = formatFn;
                    }

                    return state.boundFormatFunction;
                }
                errorBranch.enter();
                throw Errors.createTypeErrorTypeXExpected(CLASS_NAME);
            }
        });
    }

    private static JSFunctionData createFormatFunctionData(JSContext context) {
        return JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private PropertyGetNode getBoundObjectNode = PropertyGetNode.createGetHidden(BOUND_OBJECT_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {
                Object[] arguments = frame.getArguments();
                DynamicObject thisObj = (DynamicObject) getBoundObjectNode.getValue(JSArguments.getFunctionObject(arguments));
                assert isJSDateTimeFormat(thisObj);
                Object n = JSArguments.getUserArgumentCount(arguments) > 0 ? JSArguments.getUserArgument(arguments, 0) : Undefined.instance;
                return format(context, thisObj, n);
            }
        }), 1, "");
    }

    private static DynamicObject createFormatFunctionGetter(JSRealm realm, JSContext context) {
        JSFunctionData fd = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.DateTimeFormatGetFormat, (c) -> {
            CallTarget ct = createGetFormatCallTarget(context);
            return JSFunctionData.create(context, ct, ct, 0, "get format", false, false, false, true);
        });
        return JSFunction.create(realm, fd);
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getDateTimeFormatPrototype();
    }
}

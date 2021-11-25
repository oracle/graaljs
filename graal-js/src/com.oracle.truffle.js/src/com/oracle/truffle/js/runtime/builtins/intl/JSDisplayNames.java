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
package com.oracle.truffle.js.runtime.builtins.intl;

import java.util.Locale;

import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DisplayContext;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.util.ULocale;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.intl.DisplayNamesFunctionBuiltins;
import com.oracle.truffle.js.builtins.intl.DisplayNamesPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.IntlUtil;

public final class JSDisplayNames extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final String CLASS_NAME = "DisplayNames";
    public static final String PROTOTYPE_NAME = "DisplayNames.prototype";

    public static final JSDisplayNames INSTANCE = new JSDisplayNames();

    private JSDisplayNames() {
    }

    public static boolean isJSDisplayNames(Object obj) {
        return obj instanceof JSDisplayNamesObject;
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
        DynamicObject displayNamesPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, displayNamesPrototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, displayNamesPrototype, DisplayNamesPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(displayNamesPrototype, "Intl.DisplayNames");
        return displayNamesPrototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, DisplayNamesFunctionBuiltins.BUILTINS);
    }

    public static DynamicObject create(JSContext context, JSRealm realm) {
        InternalState state = new InternalState();
        JSObjectFactory factory = context.getDisplayNamesFactory();
        JSDisplayNamesObject obj = new JSDisplayNamesObject(factory.getShape(realm), state);
        factory.initProto(obj, realm);
        assert isJSDisplayNames(obj);
        return obj;
    }

    public static class InternalState {
        String locale;
        String style;
        String type;
        String fallback;
        String languageDisplay;
        LocaleDisplayNames displayNames;
        DateTimePatternGenerator dateTimePatternGenerator;
        DateTimePatternGenerator.DisplayWidth displayWidth;

        DynamicObject toResolvedOptionsObject(JSContext context, JSRealm realm) {
            DynamicObject result = JSOrdinary.create(context, realm);
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.LOCALE, locale, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.STYLE, style, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.TYPE, type, JSAttributes.getDefault());
            JSObjectUtil.defineDataProperty(context, result, IntlUtil.FALLBACK, fallback, JSAttributes.getDefault());
            if (languageDisplay != null) {
                JSObjectUtil.defineDataProperty(context, result, IntlUtil.LANGUAGE_DISPLAY, languageDisplay, JSAttributes.getDefault());
            }
            return result;
        }
    }

    @TruffleBoundary
    public static void setupInternalState(JSContext ctx, InternalState state, String[] locales, String optStyle, String optType, String optFallback, String optLanguageDisplay) {
        Locale selectedLocale = IntlUtil.selectedLocale(ctx, locales);
        Locale strippedLocale = selectedLocale.stripExtensions();
        if (strippedLocale.toLanguageTag().equals(IntlUtil.UND)) {
            selectedLocale = ctx.getLocale();
            strippedLocale = selectedLocale.stripExtensions();
        }
        state.locale = strippedLocale.toLanguageTag();
        state.style = optStyle;
        state.type = optType;
        state.fallback = optFallback;
        state.languageDisplay = IntlUtil.LANGUAGE.equals(optType) ? optLanguageDisplay : null;
        if (IntlUtil.DATE_TIME_FIELD.equals(optType)) {
            state.dateTimePatternGenerator = DateTimePatternGenerator.getInstance(strippedLocale);
            state.displayWidth = styleDisplayWidth(optStyle);
        } else {
            DisplayContext fallbackCtx = fallbackDisplayContext(optFallback);
            DisplayContext styleCtx = styleDisplayContext(optStyle);
            DisplayContext languageDisplayCtx = languageDisplayContext(optLanguageDisplay);
            state.displayNames = LocaleDisplayNames.getInstance(convertOldISOCodes(strippedLocale), styleCtx, fallbackCtx, languageDisplayCtx);
        }
    }

    private static DisplayContext fallbackDisplayContext(String optFallback) {
        return IntlUtil.NONE.equals(optFallback) ? DisplayContext.NO_SUBSTITUTE : DisplayContext.SUBSTITUTE;
    }

    private static DisplayContext styleDisplayContext(String optStyle) {
        return IntlUtil.LONG.equals(optStyle) ? DisplayContext.LENGTH_FULL : DisplayContext.LENGTH_SHORT;
    }

    private static DisplayContext languageDisplayContext(String optLanguageDisplay) {
        return IntlUtil.DIALECT.equals(optLanguageDisplay) ? DisplayContext.DIALECT_NAMES : DisplayContext.STANDARD_NAMES;
    }

    private static DateTimePatternGenerator.DisplayWidth styleDisplayWidth(String optStyle) {
        DateTimePatternGenerator.DisplayWidth displayWidth;
        switch (optStyle) {
            case IntlUtil.LONG:
                displayWidth = DateTimePatternGenerator.DisplayWidth.WIDE;
                break;
            case IntlUtil.SHORT:
                displayWidth = DateTimePatternGenerator.DisplayWidth.ABBREVIATED;
                break;
            case IntlUtil.NARROW:
                displayWidth = DateTimePatternGenerator.DisplayWidth.NARROW;
                break;
            default:
                throw Errors.shouldNotReachHere(optStyle);
        }
        return displayWidth;
    }

    /**
     * Converts any legacy language codes found in j.u.Locale to the new standard language codes.
     * Using the legacy language codes can lead to MissingResourceExceptions in ICU when attempting
     * to load display name data. This should no longer be necessary after ICU-21742 is fixed.
     *
     * <p>
     * <a href="https://unicode-org.atlassian.net/browse/ICU-21742">ICU-21742</a>
     * </p>
     */
    private static ULocale convertOldISOCodes(Locale locale) {
        ULocale.Builder builder = new ULocale.Builder();
        builder.setLocale(ULocale.forLocale(locale));
        switch (locale.getLanguage()) {
            case "iw":
                builder.setLanguage("he");
                break;
            case "ji":
                builder.setLanguage("yi");
                break;
            case "in":
                builder.setLanguage("id");
                break;
        }
        return builder.build();
    }

    @TruffleBoundary
    public static DynamicObject resolvedOptions(JSContext context, JSRealm realm, DynamicObject displayNamesObject) {
        InternalState state = getInternalState(displayNamesObject);
        return state.toResolvedOptionsObject(context, realm);
    }

    @TruffleBoundary
    public static Object of(DynamicObject displayNamesObject, String code) {
        InternalState state = getInternalState(displayNamesObject);
        String type = state.type;
        LocaleDisplayNames displayNames = state.displayNames;
        String result;
        switch (type) {
            case IntlUtil.LANGUAGE:
                result = displayNames.localeDisplayName(IntlUtil.validateAndCanonicalizeLanguageTag(code));
                break;
            case IntlUtil.REGION:
                IntlUtil.ensureIsStructurallyValidRegionSubtag(code);
                result = displayNames.regionDisplayName(code.toUpperCase());
                break;
            case IntlUtil.SCRIPT:
                IntlUtil.ensureIsStructurallyValidScriptSubtag(code);
                String canonicalScript = Character.toUpperCase(code.charAt(0)) + code.substring(1).toLowerCase();
                result = displayNames.scriptDisplayName(canonicalScript);
                break;
            case IntlUtil.CALENDAR:
                IntlUtil.ensureIsStructurallyValidCalendar(code);
                result = displayNames.keyValueDisplayName(IntlUtil.CALENDAR, displayNamesFriendlyCalendar(code.toLowerCase()));
                break;
            case IntlUtil.DATE_TIME_FIELD:
                result = state.dateTimePatternGenerator.getFieldDisplayName(toDateTimeFieldCode(code), state.displayWidth);
                break;
            case IntlUtil.CURRENCY:
                IntlUtil.ensureIsWellFormedCurrencyCode(code);
                String upperCaseCode = code.toUpperCase();
                result = displayNames.keyValueDisplayName(IntlUtil.CURRENCY, upperCaseCode);
                if (IntlUtil.NONE.equals(state.fallback) && upperCaseCode.equals(result)) {
                    // ICU4J seems to ignore DisplayContext.NO_SUBSTITUTE for currencies
                    result = null;
                }
                break;
            default:
                throw Errors.shouldNotReachHere(type);
        }
        return (result == null) ? Undefined.instance : result;
    }

    public static InternalState getInternalState(DynamicObject displayNamesObject) {
        assert isJSDisplayNames(displayNamesObject);
        return ((JSDisplayNamesObject) displayNamesObject).getInternalState();
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getDisplayNamesPrototype();
    }

    private static int toDateTimeFieldCode(String code) {
        int fieldCode;
        switch (code) {
            case IntlUtil.ERA:
                fieldCode = DateTimePatternGenerator.ERA;
                break;
            case IntlUtil.YEAR:
                fieldCode = DateTimePatternGenerator.YEAR;
                break;
            case IntlUtil.QUARTER:
                fieldCode = DateTimePatternGenerator.QUARTER;
                break;
            case IntlUtil.MONTH:
                fieldCode = DateTimePatternGenerator.MONTH;
                break;
            case IntlUtil.WEEK_OF_YEAR:
                fieldCode = DateTimePatternGenerator.WEEK_OF_YEAR;
                break;
            case IntlUtil.WEEKDAY:
                fieldCode = DateTimePatternGenerator.WEEKDAY;
                break;
            case IntlUtil.DAY:
                fieldCode = DateTimePatternGenerator.DAY;
                break;
            case IntlUtil.DAY_PERIOD:
                fieldCode = DateTimePatternGenerator.DAYPERIOD;
                break;
            case IntlUtil.HOUR:
                fieldCode = DateTimePatternGenerator.HOUR;
                break;
            case IntlUtil.MINUTE:
                fieldCode = DateTimePatternGenerator.MINUTE;
                break;
            case IntlUtil.SECOND:
                fieldCode = DateTimePatternGenerator.SECOND;
                break;
            case IntlUtil.TIME_ZONE_NAME:
                fieldCode = DateTimePatternGenerator.ZONE;
                break;
            default:
                throw Errors.createRangeErrorInvalidDateTimeField(code);
        }
        return fieldCode;
    }

    private static String displayNamesFriendlyCalendar(String calendar) {
        // DisplayNames.keyValueDisplayName("calendar", ...) handles some
        // preferred calendars worse than their aliases
        // => using non-preferred aliases in some special cases
        if ("gregory".equals(calendar)) {
            return "gregorian";
        } else if ("ethioaa".equals(calendar)) {
            return "ethiopic-amete-alem";
        }
        return calendar;
    }

}

/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetRegexResult;

import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.RegExpFunctionBuiltins;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.dyn.LazyRegexResultIndicesArray;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.util.TRegexUtil;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InteropReadIntArrayMemberNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InteropReadMemberNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InteropReadStringMemberNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.InvokeGetGroupBoundariesMethodNode;
import com.oracle.truffle.js.runtime.util.TRegexUtil.TRegexMaterializeResult;

public final class JSRegExp extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    static final TruffleString BRACKET_REG_EXP_SPC = Strings.constant("[RegExp ");

    public static final JSRegExp INSTANCE = new JSRegExp();

    public static final TruffleString CLASS_NAME = Strings.constant("RegExp");
    public static final TruffleString PROTOTYPE_NAME = Strings.concat(CLASS_NAME, Strings.DOT_PROTOTYPE);
    public static final TruffleString MULTILINE = Strings.constant("multiline");
    public static final TruffleString GLOBAL = Strings.GLOBAL;
    public static final TruffleString IGNORE_CASE = Strings.constant("ignoreCase");
    public static final TruffleString STICKY = Strings.constant("sticky");
    public static final TruffleString UNICODE = Strings.constant("unicode");
    public static final TruffleString DOT_ALL = Strings.constant("dotAll");
    public static final TruffleString SOURCE = Strings.SOURCE;
    public static final TruffleString FLAGS = Strings.constant("flags");
    public static final TruffleString LAST_INDEX = Strings.constant("lastIndex");
    public static final TruffleString INPUT = Strings.INPUT;
    public static final TruffleString GROUPS = Strings.constant("groups");
    public static final TruffleString INDEX = Strings.INDEX;
    public static final TruffleString INDICES = Strings.constant("indices");
    public static final TruffleString HAS_INDICES = Strings.constant("hasIndices");
    public static final TruffleString UNICODE_SETS = Strings.constant("unicodeSets");

    public static final TruffleString EMPTY_REGEX = Strings.constant("(?:)");
    public static final TruffleString LAST_MATCH = Strings.constant("lastMatch");
    public static final TruffleString LAST_PAREN = Strings.constant("lastParen");
    public static final TruffleString LEFT_CONTEXT = Strings.constant("leftContext");
    public static final TruffleString RIGHT_CONTEXT = Strings.constant("rightContext");
    public static final TruffleString $_ = Strings.constant("$_");
    public static final TruffleString $_AMPERSAND = Strings.constant("$&");
    public static final TruffleString $_PLUS = Strings.constant("$+");
    public static final TruffleString $_BACKTICK = Strings.constant("$`");
    public static final TruffleString $_SQUOT = Strings.constant("$'");
    public static final TruffleString $_1 = Strings.constant("$1");
    public static final TruffleString $_2 = Strings.constant("$2");
    public static final TruffleString $_3 = Strings.constant("$3");
    public static final TruffleString $_4 = Strings.constant("$4");
    public static final TruffleString $_5 = Strings.constant("$5");
    public static final TruffleString $_6 = Strings.constant("$6");
    public static final TruffleString $_7 = Strings.constant("$7");
    public static final TruffleString $_8 = Strings.constant("$8");
    public static final TruffleString $_9 = Strings.constant("$9");

    public static final PropertyProxy LAZY_INDEX_PROXY = new LazyRegexResultIndexProxyProperty();
    public static final HiddenKey GROUPS_RESULT_ID = new HiddenKey("regexResult");

    public static final int MAX_FLAGS_LENGTH = 8; // "dgimsuvy"

    /**
     * Since we cannot use nodes here, access to this property is special-cased in
     * {@code com.oracle.truffle.js.nodes.access.PropertyGetNode.LazyRegexResultIndexPropertyGetNode}
     * .
     */
    public static final class LazyRegexResultIndexProxyProperty extends PropertyProxy {

        @Override
        public Object get(JSDynamicObject object) {
            Object regexResult = arrayGetRegexResult(object, DynamicObjectLibrary.getUncached());
            return InvokeGetGroupBoundariesMethodNode.getUncached().execute(null, regexResult, TRegexUtil.Props.RegexResult.GET_START, 0);
        }

        @TruffleBoundary
        @Override
        public boolean set(JSDynamicObject object, Object value) {
            JSObjectUtil.defineDataProperty(object, JSRegExp.INDEX, value, JSAttributes.getDefault());
            return true;
        }
    }

    public static final class LazyNamedCaptureGroupProperty extends PropertyProxy {

        private final int[] groupIndices;
        private final TruffleString groupName;

        public LazyNamedCaptureGroupProperty(TruffleString groupName, int[] groupIndices) {
            this.groupIndices = groupIndices;
            this.groupName = groupName;
        }

        public int[] getGroupIndices() {
            return groupIndices;
        }

        @Override
        public Object get(JSDynamicObject object) {
            JSRegExpGroupsObject groups = (JSRegExpGroupsObject) object;
            Object regexResult = groups.getRegexResult();
            if (groups.isIndices()) {
                return LazyRegexResultIndicesArray.getIntIndicesArray(JavaScriptLanguage.getCurrentLanguage().getJSContext(), regexResult, groupIndices,
                                null, InvokeGetGroupBoundariesMethodNode.getUncached(), InvokeGetGroupBoundariesMethodNode.getUncached());
            } else {
                TruffleString input = groups.getInputString();
                return TRegexMaterializeResult.materializeGroupUncached(regexResult, groupIndices, input);
            }
        }

        @TruffleBoundary
        @Override
        public boolean set(JSDynamicObject object, Object value) {
            JSObjectUtil.defineDataProperty(object, groupName, value, JSAttributes.getDefault());
            return true;
        }
    }

    private JSRegExp() {
    }

    @NeverDefault
    public static Object getCompiledRegex(JSRegExpObject thisObj) {
        return thisObj.getCompiledRegex();
    }

    public static JSObjectFactory getGroupsFactory(JSRegExpObject thisObj) {
        return thisObj.getGroupsFactory();
    }

    public static Object getRealm(JSRegExpObject thisObj) {
        return thisObj.getRealm();
    }

    public static boolean getLegacyFeaturesEnabled(JSRegExpObject thisObj) {
        return thisObj.getLegacyFeaturesEnabled();
    }

    /**
     * Creates a new JavaScript RegExp object (with a {@code lastIndex} of 0).
     * <p>
     * This overload incurs hitting a {@link TruffleBoundary} when having to examine the
     * {@code compiledRegex} for information about named capture groups. In order to avoid a
     * {@link TruffleBoundary} in cases when your regular expression has no named capture groups,
     * consider using the {@code com.oracle.truffle.js.nodes.intl.CreateRegExpNode}.
     */
    public static JSRegExpObject create(JSContext ctx, JSRealm realm, Object compiledRegex) {
        JSObjectFactory groupsFactory = computeGroupsFactory(ctx, compiledRegex);
        JSRegExpObject obj = create(ctx, realm, compiledRegex, groupsFactory);
        JSObjectUtil.putDataProperty(obj, LAST_INDEX, 0, JSAttributes.notConfigurableNotEnumerableWritable());
        return obj;
    }

    /**
     * Creates a new JavaScript RegExp object <em>without</em> a {@code lastIndex} property.
     */
    public static JSRegExpObject create(JSContext context, JSRealm realm, Object compiledRegex, JSObjectFactory groupsFactory) {
        return create(context, realm, compiledRegex, groupsFactory, true);
    }

    /**
     * Creates a new JavaScript RegExp object <em>without</em> a {@code lastIndex} property.
     */
    public static JSRegExpObject create(JSContext context, JSRealm realm, JSDynamicObject proto, Object compiledRegex, JSObjectFactory groupsFactory, boolean legacyFeaturesEnabled) {
        JSObjectFactory factory = context.getRegExpFactory();
        return create(factory, realm, proto, compiledRegex, groupsFactory, legacyFeaturesEnabled);
    }

    /**
     * Creates a new JavaScript RegExp object <em>without</em> a {@code lastIndex} property.
     */
    public static JSRegExpObject create(JSContext context, JSRealm realm, Object compiledRegex, JSObjectFactory groupsFactory, boolean legacyFeaturesEnabled) {
        JSObjectFactory factory = context.getRegExpFactory();
        return create(factory, realm, factory.getPrototype(realm), compiledRegex, groupsFactory, legacyFeaturesEnabled);
    }

    private static JSRegExpObject create(JSObjectFactory factory, JSRealm realm, JSDynamicObject proto, Object compiledRegex, JSObjectFactory groupsFactory, boolean legacyFeaturesEnabled) {
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSRegExpObject(shape, proto, compiledRegex, groupsFactory, realm, legacyFeaturesEnabled), realm, proto);
        return factory.trackAllocation(newObj);
    }

    private static void initialize(JSContext ctx, JSRegExpObject regExp, Object regex) {
        regExp.setCompiledRegex(regex);
        regExp.setGroupsFactory(computeGroupsFactory(ctx, regex));
    }

    public static void updateCompilation(JSContext ctx, JSRegExpObject thisObj, Object regex) {
        assert regex != null;
        initialize(ctx, thisObj, regex);
    }

    public static JSDynamicObject createGroupsObject(JSRealm realm, JSObjectFactory groupsFactory, Object regexResult, TruffleString input, boolean isIndices) {
        var proto = groupsFactory.getPrototype(realm);
        var shape = groupsFactory.getShape(realm, proto);
        var newObj = groupsFactory.initProto(new JSRegExpGroupsObject(shape, proto, regexResult, input, isIndices), realm, proto);
        return groupsFactory.trackAllocation(newObj);
    }

    @TruffleBoundary
    private static JSObjectFactory computeGroupsFactory(JSContext ctx, Object compiledRegex) {
        Object namedCaptureGroups = InteropReadMemberNode.getUncached().execute(null, compiledRegex, TRegexUtil.Props.CompiledRegex.GROUPS);
        if (InteropLibrary.getUncached().isNull(namedCaptureGroups)) {
            return null;
        } else {
            return buildGroupsFactory(ctx, namedCaptureGroups);
        }
    }

    @TruffleBoundary
    public static JSObjectFactory buildGroupsFactory(JSContext ctx, Object namedCaptureGroups) {
        try {
            Shape groupsShape = ctx.getRegExpGroupsEmptyShape();
            Shape.DerivedBuilder builder = Shape.newBuilder(groupsShape);
            List<Object> keys = JSInteropUtil.keys(namedCaptureGroups);
            int firstIndexOfLastGroup = 0;
            for (Object key : keys) {
                int[] groupIndices = InteropReadIntArrayMemberNode.getUncached().execute(null, namedCaptureGroups, InteropLibrary.getUncached().asString(key));
                assert groupIndices[0] > firstIndexOfLastGroup;
                firstIndexOfLastGroup = groupIndices[0];
                TruffleString groupName = Strings.interopAsTruffleString(key);
                builder.addConstantProperty(groupName, new LazyNamedCaptureGroupProperty(groupName, groupIndices), JSAttributes.getDefault() | JSProperty.PROXY);
            }
            groupsShape = builder.build();
            return JSObjectFactory.createBound(ctx, Null.instance, groupsShape);
        } catch (UnsupportedMessageException e) {
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    /**
     * Format: '/' pattern '/' flags. Flags may be none, one, or more of 'dgimsuy', in that order.
     * <p>
     * Example: <code>/ab*c/gi</code>
     */
    @TruffleBoundary
    public static TruffleString prototypeToString(JSRegExpObject thisObj) {
        Object regex = getCompiledRegex(thisObj);
        InteropReadStringMemberNode readString = InteropReadStringMemberNode.getUncached();
        TruffleString pattern = readString.execute(null, regex, TRegexUtil.Props.CompiledRegex.PATTERN);
        if (Strings.length(pattern) == 0) {
            pattern = EMPTY_REGEX;
        }
        Object regexFlags = TRegexUtil.InteropReadMemberNode.getUncached().execute(null, regex, TRegexUtil.Props.CompiledRegex.FLAGS);
        TruffleString flags = readString.execute(null, regexFlags, TRegexUtil.Props.Flags.SOURCE);
        return Strings.concatAll(Strings.SLASH, pattern, Strings.SLASH, flags);
    }

    // non-standard according to ES2015, 7.2.8 IsRegExp (@@match check missing)
    public static boolean isJSRegExp(Object obj) {
        return obj instanceof JSRegExpObject;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSContext ctx = realm.getContext();
        JSDynamicObject prototype;
        if (ctx.getEcmaScriptVersion() < 6) {
            Shape shape = JSShape.createPrototypeShape(realm.getContext(), INSTANCE, realm.getObjectPrototype());
            prototype = JSRegExpObject.create(shape, realm.getObjectPrototype(), es5GetEmptyRegexEarly(realm), realm);
            JSObjectUtil.setOrVerifyPrototype(ctx, prototype, realm.getObjectPrototype());
            JSObjectUtil.putDataProperty(prototype, LAST_INDEX, 0, JSAttributes.notConfigurableNotEnumerableWritable());
        } else {
            prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        }

        putRegExpPropertyAccessor(realm, prototype, SOURCE);
        putRegExpPropertyAccessor(realm, prototype, FLAGS);
        putRegExpPropertyAccessor(realm, prototype, MULTILINE);
        putRegExpPropertyAccessor(realm, prototype, GLOBAL);
        putRegExpPropertyAccessor(realm, prototype, IGNORE_CASE);
        if (ctx.getEcmaScriptVersion() >= 6) {
            putRegExpPropertyAccessor(realm, prototype, STICKY);
            putRegExpPropertyAccessor(realm, prototype, UNICODE);
        }
        if (ctx.getEcmaScriptVersion() >= JSConfig.ECMAScript2018) {
            putRegExpPropertyAccessor(realm, prototype, DOT_ALL);
        }
        if (ctx.isOptionRegexpMatchIndices()) {
            putRegExpPropertyAccessor(realm, prototype, HAS_INDICES);
        }
        if (ctx.isOptionRegexpUnicodeSets()) {
            putRegExpPropertyAccessor(realm, prototype, UNICODE_SETS);
        }
        // ctor and functions
        JSObjectUtil.putConstructorProperty(prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, RegExpPrototypeBuiltins.BUILTINS);
        return prototype;
    }

    private static void putRegExpPropertyAccessor(JSRealm realm, JSDynamicObject prototype, TruffleString name) {
        JSObjectUtil.putBuiltinAccessorProperty(prototype, name, realm.lookupAccessor(RegExpPrototypeBuiltins.BUILTINS, name));
    }

    private static Object es5GetEmptyRegexEarly(JSRealm realm) {
        return realm.getEnv().parseInternal(Source.newBuilder("regex", "//", "//").mimeType("application/tregex").internal(true).build()).call();
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject thisObj) {
        return JSObjectUtil.getProtoChildShape(thisObj, INSTANCE, ctx);
    }

    public static Shape makeInitialGroupsObjectShape(JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        return JSShape.createRootWithNullProto(context, JSOrdinary.BARE_INSTANCE);
    }

    @Override
    public void fillConstructor(JSRealm realm, JSDynamicObject constructor) {
        putConstructorSpeciesGetter(realm, constructor);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, RegExpFunctionBuiltins.BUILTINS);
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getRegExpPrototype();
    }

    @TruffleBoundary
    public static TruffleString escapeRegExpPattern(TruffleString pattern) {
        if (Strings.length(pattern) == 0) {
            return EMPTY_REGEX;
        }
        int extraChars = escapeRegExpExtraCharCount(pattern);
        if (extraChars == 0) {
            return pattern;
        } else {
            return escapeRegExpPattern(pattern, extraChars);
        }
    }

    /**
     * Returns the number of extra characters that need to be inserted into {@code pattern} in order
     * for it to be correctly escaped for use in a RegExp literal (according to the requirements of
     * EscapeRegExpPattern).
     *
     * This method satisfies the following property: if its return value is 0, the pattern does not
     * need to be modified by EscapeRegExpPattern. In order to satisfy this property, this method
     * can sometimes return a result that is 1 higher than the advertised value. This is the case
     * when the pattern needs escaping but none of the escapes actually prolong the pattern, as in
     * {@code "\\\n"}, which is escaped as {@code "\\n"} and where both the original and the escaped
     * pattern are of length 2.
     */
    private static int escapeRegExpExtraCharCount(TruffleString pattern) {
        // The body of this method mirrors that of escapeRegExpPattern. However, instead of actually
        // allocating and filling a new StringBuilder, it only scans the input pattern and takes
        // note of any characters that will need to be escaped.
        int extraChars = 0;
        boolean insideCharClass = false;
        int i = 0;
        while (i < Strings.length(pattern)) {
            switch (Strings.charAt(pattern, i)) {
                case '\\':
                    assert i + 1 < Strings.length(pattern);
                    i++;
                    switch (Strings.charAt(pattern, i)) {
                        case '\n':
                        case '\r':
                            // We are replacing "\\\n" with "\\n" or "\\\r" with "\\r". We are not
                            // adding any extra characters but we are still modifying the pattern.
                            // Therefore, we make sure that resulting value extraChars is at least
                            // 1.
                            extraChars = Math.max(extraChars, 1);
                            break;
                        case '\u2028':
                        case '\u2029':
                            extraChars += 4;
                            break;
                    }
                    break;
                case '\n':
                case '\r':
                    extraChars += 1;
                    break;
                case '\u2028':
                case '\u2029':
                    extraChars += 5;
                    break;
                case '/':
                    if (!insideCharClass) {
                        extraChars += 1;
                    }
                    break;
                case '[':
                    insideCharClass = true;
                    break;
                case ']':
                    insideCharClass = false;
                    break;
            }
            i++;
        }
        return extraChars;
    }

    /**
     * Implements the EscapeRegExpPattern abstract operation from the ECMAScript spec.
     *
     * @param pattern the input pattern, which is assumed to be non-empty
     * @param extraChars an estimate on the difference of sizes between the original pattern and the
     *            escaped pattern
     * @return the escaped pattern
     */
    @TruffleBoundary
    private static TruffleString escapeRegExpPattern(TruffleString pattern, int extraChars) {
        StringBuilder sb = new StringBuilder(Strings.length(pattern) + extraChars);
        boolean insideCharClass = false;
        int i = 0;
        while (i < Strings.length(pattern)) {
            char c = Strings.charAt(pattern, i);
            switch (c) {
                case '\\':
                    assert i + 1 < Strings.length(pattern);
                    sb.append(c);
                    i++;
                    c = Strings.charAt(pattern, i);
                    // The patterns used in RegExp objects can not only have literal LineTerminators
                    // (e.g. RegExp("\n")), they can also have identity escapes of literal
                    // LineTerminators (e.g. RegExp("\\\n")) (note that this is only valid when the
                    // Unicode flag is not present). Since LineTerminators are not allowed in RegExp
                    // literals, we have to replace these identity escapes with other escapes.
                    switch (c) {
                        case '\n':
                            sb.append('n');
                            break;
                        case '\r':
                            sb.append('r');
                            break;
                        case '\u2028':
                            sb.append("u2028");
                            break;
                        case '\u2029':
                            sb.append("u2029");
                            break;
                        default:
                            sb.append(c);
                    }
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\u2028':
                    sb.append("\\u2028");
                    break;
                case '\u2029':
                    sb.append("\\u2029");
                    break;
                case '/':
                    // According to the syntax of RegularExpressionLiterals, forward slashes are
                    // allowed inside character classes and therefore do not have to be escaped.
                    if (!insideCharClass) {
                        sb.append("\\/");
                    } else {
                        sb.append('/');
                    }
                    break;
                case '[':
                    insideCharClass = true;
                    sb.append(c);
                    break;
                case ']':
                    insideCharClass = false;
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
            }
            i++;
        }
        return Strings.fromJavaString(sb.toString());
    }
}

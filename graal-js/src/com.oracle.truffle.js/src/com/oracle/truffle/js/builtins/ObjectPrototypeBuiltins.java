/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins;

import java.util.EnumSet;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.FormatCacheNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.GetBuiltinToStringTagNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeDefineGetterOrSetterNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeHasOwnPropertyNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeIsPrototypeOfNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeLookupGetterOrSetterNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypePropertyIsEnumerableNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeToStringNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeValueOfNodeGen;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSGetOwnPropertyNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Record;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for Object.prototype.
 */
public final class ObjectPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ObjectPrototypeBuiltins.ObjectPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new ObjectPrototypeBuiltins();

    protected ObjectPrototypeBuiltins() {
        super(JSOrdinary.PROTOTYPE_NAME, ObjectPrototype.class);
    }

    public enum ObjectPrototype implements BuiltinEnum<ObjectPrototype> {
        hasOwnProperty(1),
        isPrototypeOf(1),
        propertyIsEnumerable(1),
        toLocaleString(0),
        toString(0),
        valueOf(0),

        // Annex B
        __defineGetter__(2),
        __defineSetter__(2),
        __lookupGetter__(1),
        __lookupSetter__(1);

        private final int length;

        ObjectPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isAnnexB() {
            return EnumSet.of(__defineGetter__, __defineSetter__, __lookupGetter__, __lookupSetter__).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ObjectPrototype builtinEnum) {
        switch (builtinEnum) {
            case hasOwnProperty:
                return ObjectPrototypeHasOwnPropertyNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case isPrototypeOf:
                return ObjectPrototypeIsPrototypeOfNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case propertyIsEnumerable:
                return ObjectPrototypePropertyIsEnumerableNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case toLocaleString:
                return ObjectPrototypeToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case toString:
                return ObjectPrototypeToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return ObjectPrototypeValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));

            case __defineGetter__:
            case __defineSetter__:
                return ObjectPrototypeDefineGetterOrSetterNodeGen.create(context, builtin, builtinEnum == ObjectPrototype.__defineGetter__,
                                args().withThis().fixedArgs(2).createArgumentNodes(context));
            case __lookupGetter__:
            case __lookupSetter__:
                return ObjectPrototypeLookupGetterOrSetterNodeGen.create(context, builtin, builtinEnum == ObjectPrototype.__lookupGetter__,
                                args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class ObjectOperation extends JSBuiltinNode {

        public ObjectOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToObjectNode toObjectNode;

        private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();
        private final BranchProfile notAJSObjectBranch = BranchProfile.create();

        /**
         * Convert to a DynamicObject that is a JavaScript object.
         */
        protected final DynamicObject toJSObject(Object target) {
            return JSRuntime.expectJSObject(toObject(target), notAJSObjectBranch);
        }

        /**
         * Convert to a TruffleObject.
         */
        protected final Object toObject(Object target) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
            }
            return toObjectNode.execute(target);
        }

        /**
         * Coerce to Object or throw TypeError. Must be the first statement (evaluation order!) and
         * executed only once.
         */
        protected final DynamicObject asJSObject(Object object) {
            if (isObject.profile(JSRuntime.isObject(object))) {
                return (DynamicObject) object;
            } else {
                throw createTypeErrorCalledOnNonObject(object);
            }
        }

        protected final DynamicObject toOrAsJSObject(Object thisObj) {
            if (getContext().getEcmaScriptVersion() >= 6) {
                return toJSObject(thisObj);
            } else {
                return asJSObject(thisObj); // ES5
            }
        }

        @TruffleBoundary
        protected final JSException createTypeErrorCalledOnNonObject(Object value) {
            assert !JSRuntime.isObject(value);
            return Errors.createTypeErrorFormat("Object.%s called on non-object", getBuiltin().getName());
        }
    }

    @ImportStatic({JSConfig.class})
    public abstract static class ObjectPrototypeValueOfNode extends ObjectOperation {

        public ObjectPrototypeValueOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSDynamicObject(thisObj)")
        protected DynamicObject valueOfJSObject(DynamicObject thisObj) {
            return toJSObject(thisObj);
        }

        @Specialization
        protected DynamicObject valueOfSymbol(Symbol thisObj) {
            return toJSObject(thisObj);
        }

        @Specialization
        protected DynamicObject valueOfLazyString(JSLazyString thisObj) {
            return toJSObject(thisObj);
        }

        @Specialization
        protected DynamicObject valueOfSafeInteger(SafeInteger thisObj) {
            return toJSObject(thisObj);
        }

        @Specialization
        protected DynamicObject valueOfBigInt(BigInt thisObj) {
            return toJSObject(thisObj);
        }

        @Specialization
        protected DynamicObject valueOfRecord(Record thisObj) {
            return toJSObject(thisObj);
        }

        @Specialization
        protected DynamicObject valueOfTuple(Tuple thisObj) {
            return toJSObject(thisObj);
        }

        @Specialization(guards = "!isTruffleObject(thisObj)")
        protected DynamicObject valueOfOther(Object thisObj) {
            return toJSObject(thisObj);
        }

        @Specialization(guards = "isForeignObject(thisObj)")
        protected Object valueOfForeign(Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            if (interop.isNull(thisObj)) {
                throw Errors.createTypeErrorNotObjectCoercible(thisObj, null, getContext());
            }
            return thisObj;
        }
    }

    public abstract static class ObjectPrototypeToStringNode extends ObjectOperation {
        @Child private PropertyGetNode getStringTagNode;
        @Child private FormatCacheNode formatCacheNode;

        public ObjectPrototypeToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.getStringTagNode = PropertyGetNode.create(Symbol.SYMBOL_TO_STRING_TAG, false, context);
        }

        private String formatString(String name) {
            if (formatCacheNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                formatCacheNode = insert(FormatCacheNode.create());
            }
            return formatCacheNode.execute(name);
        }

        private String getToStringTag(DynamicObject thisObj) {
            if (getContext().getEcmaScriptVersion() >= 6) {
                Object toStringTag = getStringTagNode.getValue(thisObj);
                if (JSRuntime.isString(toStringTag)) {
                    return JSRuntime.toStringIsString(toStringTag);
                }
            }
            return null;
        }

        @Specialization(guards = {"isJSObject(thisObj)", "!isJSProxy(thisObj)"})
        protected String doJSObject(DynamicObject thisObj,
                        @Shared("builtinTag") @Cached GetBuiltinToStringTagNode getBuiltinToStringTagNode) {
            String toString = getToStringTag(thisObj);
            if (toString == null) {
                if (getContext().getEcmaScriptVersion() >= 6) {
                    toString = getBuiltinToStringTagNode.execute(thisObj);
                } else {
                    toString = JSObject.getClassName(thisObj);
                }
            }
            return formatString(toString);
        }

        @Specialization(guards = "isJSProxy(thisObj)")
        protected String doJSProxy(DynamicObject thisObj,
                        @Shared("builtinTag") @Cached("create()") GetBuiltinToStringTagNode getBuiltinToStringTagNode) {
            // builtinTag must be read before tag because the latter may revoke the proxy
            String builtinTag = getBuiltinToStringTagNode.execute(thisObj);
            String tag = getToStringTag(thisObj);
            if (tag == null) {
                tag = builtinTag;
            }
            return formatString(tag);
        }

        @Specialization(guards = "isJSNull(thisObj)")
        protected String doNull(@SuppressWarnings("unused") Object thisObj) {
            return "[object Null]";
        }

        @Specialization(guards = "isUndefined(thisObj)")
        protected String doUndefined(@SuppressWarnings("unused") Object thisObj) {
            return "[object Undefined]";
        }

        @Specialization(guards = "isForeignObject(thisObj)", limit = "1")
        @TruffleBoundary
        protected String doForeignObject(Object thisObj,
                        @CachedLibrary("thisObj") InteropLibrary interop) {
            if (interop.isNull(thisObj)) {
                return "[object Null]";
            } else if (interop.hasArrayElements(thisObj)) {
                return "[object Array]";
            } else if (interop.isExecutable(thisObj) || interop.isInstantiable(thisObj)) {
                return "[object Function]";
            } else if (interop.isInstant(thisObj)) {
                return "[object Date]";
            } else {
                return "[object Object]";
            }
        }

        @Specialization
        protected String doSymbol(Symbol thisObj) {
            assert thisObj != null;
            return JSObject.defaultToString(toJSObject(thisObj));
        }

        @Specialization
        protected String doLazyString(JSLazyString thisObj) {
            return JSObject.defaultToString(toJSObject(thisObj));
        }

        @Specialization
        protected String doSafeInteger(SafeInteger thisObj) {
            return JSObject.defaultToString(toJSObject(thisObj));
        }

        @Specialization
        protected String doBigInt(BigInt thisObj) {
            return JSObject.defaultToString(toJSObject(thisObj));
        }

        @Specialization
        protected String doRecord(Record thisObj) {
            return JSObject.defaultToString(toJSObject(thisObj));
        }

        @Specialization
        protected String doTuple(Tuple thisObj) {
            return JSObject.defaultToString(toJSObject(thisObj));
        }

        @Specialization(guards = {"!isTruffleObject(thisObj)"})
        protected String doObject(Object thisObj) {
            assert thisObj != null;
            return JSObject.defaultToString(toJSObject(thisObj));
        }
    }

    public abstract static class GetBuiltinToStringTagNode extends JavaScriptBaseNode {

        public abstract String execute(Object object);

        public static GetBuiltinToStringTagNode create() {
            return GetBuiltinToStringTagNodeGen.create();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedClass != null", "cachedClass.isInstance(object)"}, limit = "5")
        protected static String cached(DynamicObject object,
                        @Cached("getJSClassChecked(object)") JSClass cachedClass) {
            return cachedClass.getBuiltinToStringTag(object);
        }

        @TruffleBoundary
        @Specialization(guards = "isJSDynamicObject(object)", replaces = "cached")
        protected static String uncached(DynamicObject object) {
            return JSObject.getJSClass(object).getBuiltinToStringTag(object);
        }

        @Specialization(guards = "!isJSDynamicObject(object)")
        protected static String foreign(@SuppressWarnings("unused") DynamicObject object) {
            return "Foreign";
        }
    }

    public abstract static class FormatCacheNode extends JavaScriptBaseNode {

        public abstract String execute(String name);

        public static FormatCacheNode create() {
            return FormatCacheNodeGen.create();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedName.equals(name)"}, limit = "10")
        protected String executeCached(String name,
                        @Cached("name") String cachedName,
                        @Cached("executeUncached(name)") String cachedResult) {
            return cachedResult;
        }

        @TruffleBoundary
        @Specialization
        protected String executeUncached(String name) {
            return "[object " + name + "]";
        }
    }

    public abstract static class ObjectPrototypeToLocaleStringNode extends ObjectOperation {
        @Child private PropertyGetNode getToString;
        @Child private JSFunctionCallNode callNode;

        public ObjectPrototypeToLocaleStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            getToString = PropertyGetNode.create(JSRuntime.TO_STRING, false, context);
            callNode = JSFunctionCallNode.createCall();
        }

        @Specialization
        protected Object toLocaleString(Object obj) {
            Object objConv = obj;
            if (getContext().getEcmaScriptVersion() < 6 || getContext().isOptionV8CompatibilityMode()) {
                objConv = toJSObject(obj);
            }
            Object toStringFn = getToString.getValue(objConv);
            return callNode.executeCall(JSArguments.createZeroArg(obj, toStringFn));
        }
    }

    public abstract static class ObjectPrototypePropertyIsEnumerableNode extends ObjectOperation {
        public ObjectPrototypePropertyIsEnumerableNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();
        @Child private JSGetOwnPropertyNode getOwnPropertyNode = JSGetOwnPropertyNode.create();
        private final ConditionProfile descNull = ConditionProfile.createBinaryProfile();

        @Specialization
        protected boolean propertyIsEnumerable(Object obj, Object key) {
            Object propertyKey = toPropertyKeyNode.execute(key);
            DynamicObject thisJSObj = toJSObject(obj);
            PropertyDescriptor desc = getOwnPropertyNode.execute(thisJSObj, propertyKey);
            if (descNull.profile(desc == null)) {
                return false;
            } else {
                return desc.getEnumerable();
            }
        }
    }

    public abstract static class ObjectPrototypeHasOwnPropertyNode extends ObjectOperation {

        @Child private JSHasPropertyNode hasOwnPropertyNode;
        @Child private JSToPropertyKeyNode toPropertyKeyNode;

        public ObjectPrototypeHasOwnPropertyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected boolean doJSObjectStringKey(DynamicObject thisObj, String propertyName) {
            return getHasOwnPropertyNode().executeBoolean(thisObj, propertyName);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected boolean doJSObjectIntKey(DynamicObject thisObj, int index) {
            return getHasOwnPropertyNode().executeBoolean(thisObj, index);
        }

        @Specialization(guards = "isJSObject(thisObj)", replaces = {"doJSObjectStringKey", "doJSObjectIntKey"})
        protected boolean doJSObjectAnyKey(DynamicObject thisObj, Object propName) {
            Object key = getToPropertyKeyNode().execute(propName);
            return getHasOwnPropertyNode().executeBoolean(thisObj, key);
        }

        @Specialization(guards = "isNullOrUndefined(thisObj)")
        protected boolean hasOwnPropertyNullOrUndefined(DynamicObject thisObj, Object propName) {
            getToPropertyKeyNode().execute(propName); // may have side effect
            throw Errors.createTypeErrorNotObjectCoercible(thisObj, null, getContext());
        }

        @Specialization
        protected boolean hasOwnPropertyLazyString(JSLazyString thisObj, Object propName) {
            return hasOwnPropertyPrimitive(thisObj, propName);
        }

        @Specialization(guards = "!isTruffleObject(thisObj)")
        protected boolean hasOwnPropertyPrimitive(Object thisObj, Object propName) {
            Object key = getToPropertyKeyNode().execute(propName);
            DynamicObject obj = toJSObject(thisObj);
            return getHasOwnPropertyNode().executeBoolean(obj, key);
        }

        @Specialization
        protected boolean hasOwnPropertySymbol(Symbol thisObj, Object propName) {
            return hasOwnPropertyPrimitive(thisObj, propName);
        }

        @Specialization
        protected boolean hasOwnPropertySafeInteger(SafeInteger thisObj, Object propName) {
            return hasOwnPropertyPrimitive(thisObj, propName);
        }

        @Specialization
        protected boolean hasOwnPropertyBigInt(BigInt thisObj, Object propName) {
            return hasOwnPropertyPrimitive(thisObj, propName);
        }

        @Specialization(guards = "isForeignObject(thisObj)")
        protected boolean hasOwnPropertyForeign(Object thisObj, Object propName) {
            return getHasOwnPropertyNode().executeBoolean(thisObj, propName);
        }

        public JSHasPropertyNode getHasOwnPropertyNode() {
            if (hasOwnPropertyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasOwnPropertyNode = insert(JSHasPropertyNode.create(true));
            }
            return hasOwnPropertyNode;
        }

        protected JSToPropertyKeyNode getToPropertyKeyNode() {
            if (toPropertyKeyNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toPropertyKeyNode = insert(JSToPropertyKeyNode.create());
            }
            return toPropertyKeyNode;
        }
    }

    public abstract static class ObjectPrototypeIsPrototypeOfNode extends ObjectOperation {

        public ObjectPrototypeIsPrototypeOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final ConditionProfile argIsNull = ConditionProfile.createBinaryProfile();
        private final ConditionProfile firstPrototypeFits = ConditionProfile.createBinaryProfile();

        @Specialization(guards = "isJSObject(arg)")
        protected boolean isPrototypeOf(Object thisObj, DynamicObject arg) {
            DynamicObject object = toJSObject(thisObj);
            if (argIsNull.profile(arg == null)) {
                return false;
            }
            // unroll one iteration
            DynamicObject pobj = JSObject.getPrototype(arg);
            if (firstPrototypeFits.profile(pobj == object)) {
                return true;
            }
            int counter = 0;
            do {
                counter++;
                if (counter > getContext().getContextOptions().getMaxPrototypeChainLength()) {
                    throw Errors.createRangeError("prototype chain length exceeded");
                }
                pobj = JSObject.getPrototype(pobj);
                if (pobj == object) {
                    return true;
                }
            } while (pobj != Null.instance);
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(arg)")
        protected boolean isPrototypeOfNoObject(Object thisObj, Object arg) {
            return false;
        }
    }

    public abstract static class ObjectPrototypeDefineGetterOrSetterNode extends ObjectOperation {
        private final boolean getter;
        @Child private IsCallableNode isCallableNode = IsCallableNode.create();
        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();

        public ObjectPrototypeDefineGetterOrSetterNode(JSContext context, JSBuiltin builtin, boolean getter) {
            super(context, builtin);
            this.getter = getter;
        }

        @Specialization
        protected Object define(Object thisObj, Object prop, Object getterOrSetter) {
            DynamicObject object = toJSObject(thisObj);
            if (!isCallableNode.executeBoolean(getterOrSetter)) {
                throw createTypeErrorExpectingFunction();
            }
            Object key = toPropertyKeyNode.execute(prop);
            PropertyDescriptor desc = PropertyDescriptor.createEmpty();
            if (getter) {
                desc.setGet((DynamicObject) getterOrSetter);
            } else {
                desc.setSet((DynamicObject) getterOrSetter);
            }
            desc.setEnumerable(true);
            desc.setConfigurable(true);
            JSRuntime.definePropertyOrThrow(object, key, desc);
            return Undefined.instance;
        }

        @TruffleBoundary
        private JSException createTypeErrorExpectingFunction() {
            return Errors.createTypeErrorFormat("%s: Expecting function", getBuiltin().getFullName());
        }
    }

    public abstract static class ObjectPrototypeLookupGetterOrSetterNode extends ObjectOperation {
        private final boolean getter;
        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();
        @Child private JSGetOwnPropertyNode getOwnPropertyNode = JSGetOwnPropertyNode.create();

        public ObjectPrototypeLookupGetterOrSetterNode(JSContext context, JSBuiltin builtin, boolean getter) {
            super(context, builtin);
            this.getter = getter;
        }

        @Specialization
        protected Object lookup(Object thisObj, Object prop) {
            DynamicObject object = toJSObject(thisObj);
            Object key = toPropertyKeyNode.execute(prop);

            DynamicObject current = object;
            do {
                PropertyDescriptor desc = getOwnPropertyNode.execute(current, key);
                if (desc != null) {
                    if (desc.isAccessorDescriptor()) {
                        return getter ? desc.getGet() : desc.getSet();
                    } else {
                        return Undefined.instance;
                    }
                }
                current = JSObject.getPrototype(current);
            } while (current != Null.instance);
            return Undefined.instance;
        }
    }
}

/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeDefineGetterOrSetterNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeHasOwnPropertyNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeIsPrototypeOfNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeLookupGetterOrSetterNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypePropertyIsEnumerableNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeToStringNodeGen;
import com.oracle.truffle.js.builtins.ObjectPrototypeBuiltinsFactory.ObjectPrototypeValueOfNodeGen;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.JSUnboxOrGetNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * Contains builtins for Object.prototype.
 */
public final class ObjectPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ObjectPrototypeBuiltins.ObjectPrototype> {
    protected ObjectPrototypeBuiltins() {
        super(JSUserObject.PROTOTYPE_NAME, ObjectPrototype.class);
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
        public String getName() {
            return super.name();
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
        protected final DynamicObject toObject(Object target) {
            return JSRuntime.expectJSObject(toTruffleObject(target), notAJSObjectBranch);
        }

        /**
         * Convert to a TruffleObject.
         */
        protected final TruffleObject toTruffleObject(Object target) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObject(getContext()));
            }
            return toObjectNode.executeTruffleObject(target);
        }

        /**
         * Coerce to Object or throw TypeError. Must be the first statement (evaluation order!) and
         * executed only once.
         */
        protected final DynamicObject asObject(Object object) {
            if (isObject.profile(JSRuntime.isObject(object))) {
                return (DynamicObject) object;
            } else {
                throw createTypeErrorCalledOnNonObject(object);
            }
        }

        protected final DynamicObject toOrAsObject(Object thisObj) {
            if (getContext().getEcmaScriptVersion() >= 6) {
                return toObject(thisObj);
            } else {
                return asObject(thisObj); // ES5
            }
        }

        @TruffleBoundary
        protected final JSException createTypeErrorCalledOnNonObject(Object value) {
            assert !JSRuntime.isObject(value);
            return Errors.createTypeErrorFormat("Object.%s called on non-object", getBuiltin().getName());
        }
    }

    public abstract static class ObjectPrototypeValueOfNode extends ObjectOperation {

        public ObjectPrototypeValueOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSType(thisObj)")
        protected DynamicObject valueOf(DynamicObject thisObj) {
            return toObject(thisObj);
        }

        @Specialization
        protected DynamicObject valueOf(Symbol thisObj) {
            return toObject(thisObj);
        }

        @Specialization
        protected DynamicObject valueOf(JSLazyString thisObj) {
            return toObject(thisObj);
        }

        @Specialization
        protected DynamicObject valueOf(LargeInteger thisObj) {
            return toObject(thisObj);
        }

        @Specialization
        protected DynamicObject valueOf(BigInt thisObj) {
            return toObject(thisObj);
        }

        @Specialization(guards = "!isTruffleObject(thisObj)")
        protected DynamicObject valueOf(Object thisObj) {
            return toObject(thisObj);
        }

        @Specialization(guards = "isForeignObject(thisObj)")
        protected Object valueOf(TruffleObject thisObj,
                        @Cached("create()") JSUnboxOrGetNode unboxOrGetNode) {
            return unboxOrGetNode.executeWithTarget(thisObj);
        }
    }

    public abstract static class ObjectPrototypeToStringNode extends ObjectOperation {
        public ObjectPrototypeToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            getStringTagNode = PropertyGetNode.create(Symbol.SYMBOL_TO_STRING_TAG, false, context);
        }

        @Child private PropertyGetNode getStringTagNode;

        @TruffleBoundary
        private static String formatString(String name) {
            return "[object " + name + "]";
        }

        private String getToStringTag(DynamicObject thisObj) {
            if (getContext().getEcmaScriptVersion() >= 6) {
                Object toStringTag = getStringTagNode.getValue(thisObj);
                if (JSRuntime.isString(toStringTag)) {
                    return Boundaries.javaToString(toStringTag);
                }
            }
            return null;
        }

        private String getDefaultToString(DynamicObject thisObj, JSClassProfile jsclassProfile) {
            if (getContext().getEcmaScriptVersion() >= 6) {
                return jsclassProfile.getJSClass(thisObj).getBuiltinToStringTag(thisObj);
            } else {
                return jsclassProfile.getJSClass(thisObj).getClassName(thisObj);
            }
        }

        @Specialization(guards = {"isJSObject(thisObj)", "!isJSProxy(thisObj)"})
        protected String doJSObject(DynamicObject thisObj,
                        @Cached("create()") JSClassProfile jsclassProfile,
                        @Cached("create()") BranchProfile noStringTagProfile) {
            String toString = getToStringTag(thisObj);
            if (toString == null) {
                noStringTagProfile.enter();
                toString = getDefaultToString(thisObj, jsclassProfile);
            }
            return formatString(toString);
        }

        @Specialization(guards = "isJSProxy(thisObj)")
        protected String doJSProxy(DynamicObject thisObj,
                        @Cached("create()") JSClassProfile jsclassProfile,
                        @Cached("create()") BranchProfile noStringTagProfile) {
            JSRuntime.isArray(thisObj); // might throw
            String toString = getToStringTag(thisObj);
            if (toString == null) {
                noStringTagProfile.enter();
                TruffleObject target = JSProxy.getTargetNonProxy(thisObj);
                if (JSObject.isJSObject(target)) {
                    toString = jsclassProfile.getJSClass((DynamicObject) target).getBuiltinToStringTag((DynamicObject) target);
                } else {
                    toString = "Foreign";
                }
            }
            return formatString(toString);
        }

        @Specialization(guards = "isJSNull(thisObj)")
        protected String doNull(@SuppressWarnings("unused") Object thisObj) {
            return "[object Null]";
        }

        @Specialization(guards = "isUndefined(thisObj)")
        protected String doUndefined(@SuppressWarnings("unused") Object thisObj) {
            return "[object Undefined]";
        }

        @Specialization(guards = "isForeignObject(thisObj)")
        @TruffleBoundary
        protected String doForeignObject(TruffleObject thisObj) {
            return "[foreign " + thisObj.getClass().getSimpleName() + "]";
        }

        @Specialization
        protected String doSymbol(Symbol thisObj) {
            assert thisObj != null;
            return JSObject.defaultToString(toObject(thisObj));
        }

        @Specialization
        protected String doLazyString(JSLazyString thisObj) {
            return JSObject.defaultToString(toObject(thisObj));
        }

        @Specialization
        protected String doLargeInteger(LargeInteger thisObj) {
            return JSObject.defaultToString(toObject(thisObj));
        }

        @Specialization
        protected String doBigInt(BigInt thisObj) {
            return JSObject.defaultToString(toObject(thisObj));
        }

        @Specialization(guards = {"!isTruffleObject(thisObj)"})
        protected String doObject(Object thisObj) {
            assert thisObj != null;
            return JSObject.defaultToString(toObject(thisObj));
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
                objConv = toObject(obj);
            }
            Object toStringFn = getToString.getValue(objConv);
            JSFunction.checkIsFunction(toStringFn);
            return callNode.executeCall(JSArguments.createZeroArg(obj, toStringFn));
        }
    }

    public abstract static class ObjectPrototypePropertyIsEnumerableNode extends ObjectOperation {
        public ObjectPrototypePropertyIsEnumerableNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Child private JSToPropertyKeyNode toPropertyKeyNode = JSToPropertyKeyNode.create();
        private final ConditionProfile descNull = ConditionProfile.createBinaryProfile();
        private final JSClassProfile classProfile = JSClassProfile.create();

        @Specialization
        protected boolean propertyIsEnumerable(Object obj, Object key) {
            Object propertyKey = toPropertyKeyNode.execute(key);
            DynamicObject thisJSObj = toObject(obj);
            PropertyDescriptor desc = JSObject.getOwnProperty(thisJSObj, propertyKey, classProfile);
            if (descNull.profile(desc == null)) {
                return false;
            } else {
                return desc.getEnumerable();
            }
        }
    }

    @ImportStatic(value = JSInteropUtil.class)
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
            throw Errors.createTypeErrorNotObjectCoercible(thisObj);
        }

        @Specialization
        protected boolean hasOwnPropertyLazyString(JSLazyString thisObj, Object propName) {
            return hasOwnPropertyPrimitive(thisObj, propName);
        }

        @Specialization(guards = "!isTruffleObject(thisObj)")
        protected boolean hasOwnPropertyPrimitive(Object thisObj, Object propName) {
            Object key = getToPropertyKeyNode().execute(propName);
            DynamicObject obj = toObject(thisObj);
            return getHasOwnPropertyNode().executeBoolean(obj, key);
        }

        @Specialization
        protected boolean hasOwnPropertySymbol(Symbol thisObj, Object propName) {
            return hasOwnPropertyPrimitive(thisObj, propName);
        }

        @Specialization
        protected boolean hasOwnPropertyLargeInteger(LargeInteger thisObj, Object propName) {
            return hasOwnPropertyPrimitive(thisObj, propName);
        }

        @Specialization
        protected boolean hasOwnPropertyBigInt(BigInt thisObj, Object propName) {
            return hasOwnPropertyPrimitive(thisObj, propName);
        }

        @Specialization(guards = "isForeignObject(thisObj)")
        protected boolean hasOwnPropertyForeign(TruffleObject thisObj, Object propName) {
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
            DynamicObject object = toObject(thisObj);
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
                if (counter > JSTruffleOptions.MaxExpectedPrototypeChainLength) {
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
            DynamicObject object = toObject(thisObj);
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
            JSRuntime.definePropertyOrThrow(object, key, desc, getContext());
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

        public ObjectPrototypeLookupGetterOrSetterNode(JSContext context, JSBuiltin builtin, boolean getter) {
            super(context, builtin);
            this.getter = getter;
        }

        @Specialization
        protected Object lookup(Object thisObj, Object prop) {
            DynamicObject object = toObject(thisObj);
            Object key = toPropertyKeyNode.execute(prop);

            DynamicObject current = object;
            do {
                PropertyDescriptor desc = JSObject.getOwnProperty(current, key);
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

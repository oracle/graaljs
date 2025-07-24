/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.decorators;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.ObjectLiteralMemberNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.PrivateClassElementNode;
import com.oracle.truffle.js.nodes.access.PrivateBrandCheckNode;
import com.oracle.truffle.js.nodes.access.PrivateFieldGetNode;
import com.oracle.truffle.js.nodes.access.PrivateFieldSetNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.arguments.AccessFunctionNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.arguments.AccessThisNode;
import com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

@ImportStatic({Strings.class})
public abstract class CreateDecoratorContextObjectNode extends JavaScriptBaseNode {

    public static class DecorationState {
        boolean finished = false;
    }

    private static final TruffleString FIELD_KIND = Strings.constant("field");
    private static final TruffleString GETTER_KIND = Strings.constant("getter");
    private static final TruffleString SETTER_KIND = Strings.constant("setter");
    private static final TruffleString METHOD_KIND = Strings.constant("method");
    private static final TruffleString CLASS_KIND = Strings.constant("class");
    private static final TruffleString AUTO_ACCESSOR_KIND = Strings.ACCESSOR;

    private static final TruffleString ADD_INITIALIZER = Strings.constant("addInitializer");
    private static final TruffleString NAME = Strings.NAME;
    private static final TruffleString KIND = Strings.constant("kind");
    private static final TruffleString ACCESS = Strings.constant("access");

    private static final HiddenKey INIT_KEY = new HiddenKey(":initializers");
    protected static final HiddenKey DECORATION_STATE_KEY = new HiddenKey("DecorationState");
    protected static final HiddenKey ELEMENT_RECORD_KEY = new HiddenKey("ClassElementDefinitionRecord");
    protected static final HiddenKey BACKING_STORAGE_KEY = new HiddenKey("BackingStorageKey");

    static final int LIMIT = 3;

    @Child private ScopeFrameNode privateScopeNode;
    private final int privateMemberSlotIndex;
    private final int privateBrandSlotIndex;

    @Child private CreateObjectNode createObjectNode;
    @Child private PropertySetNode setInitializersKey;
    @Child private PropertySetNode setDecorationState;

    @Child private CreateDataPropertyNode defineKind;
    @Child private CreateDataPropertyNode defineName;
    @Child private CreateDataPropertyNode defineAddInitializer;
    @Child private CreateDataPropertyNode defineAccess;
    @Child private CreateDataPropertyNode defineStatic;
    @Child private CreateDataPropertyNode definePrivate;
    @Child private CreateDataPropertyNode defineGet;
    @Child private CreateDataPropertyNode defineSet;

    protected final boolean isStatic;
    protected final boolean isPrivate;
    protected final JSContext context;

    @NeverDefault
    public static CreateDecoratorContextObjectNode create(JSContext context, ObjectLiteralMemberNode member) {
        ScopeFrameNode privateScopeNode = null;
        int privateMemberSlotIndex = -1;
        int privateBrandSlotIndex = -1;
        if (member.isPrivate()) {
            PrivateClassElementNode privateMember = (PrivateClassElementNode) member;
            privateScopeNode = privateMember.getPrivateScopeNode();
            privateMemberSlotIndex = privateMember.getPrivateMemberSlotIndex();
            privateBrandSlotIndex = privateMember.getPrivateBrandSlotIndex();
        }
        return CreateDecoratorContextObjectNodeGen.create(context, member.isStatic(), member.isPrivate(), privateScopeNode, privateMemberSlotIndex, privateBrandSlotIndex, false);
    }

    public static CreateDecoratorContextObjectNode createForClass(JSContext context) {
        return CreateDecoratorContextObjectNodeGen.create(context, false, false, null, -1, -1, true);
    }

    public abstract JSObject executeContext(VirtualFrame frame, JSRealm realm, ClassElementDefinitionRecord record, Object initializers, DecorationState state);

    CreateDecoratorContextObjectNode(JSContext context, boolean isStatic, boolean isPrivate, ScopeFrameNode privateScopeNode, int privateMemberSlotIndex, int privateBrandSlotIndex, boolean classDef) {
        this.createObjectNode = CreateObjectNode.create(context);
        this.setInitializersKey = PropertySetNode.createSetHidden(INIT_KEY, context);
        this.setDecorationState = PropertySetNode.createSetHidden(DECORATION_STATE_KEY, context);

        this.defineKind = CreateDataPropertyNode.create(context, KIND);
        this.defineName = CreateDataPropertyNode.create(context, NAME);
        this.defineAddInitializer = CreateDataPropertyNode.create(context, ADD_INITIALIZER);
        if (!classDef) {
            this.defineAccess = CreateDataPropertyNode.create(context, ACCESS);
            this.defineStatic = CreateDataPropertyNode.create(context, Strings.STATIC);
            this.definePrivate = CreateDataPropertyNode.create(context, Strings.PRIVATE);
            this.defineGet = CreateDataPropertyNode.create(context, Strings.GET);
            this.defineSet = CreateDataPropertyNode.create(context, Strings.SET);
        }

        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
        this.privateMemberSlotIndex = privateMemberSlotIndex;
        this.privateBrandSlotIndex = privateBrandSlotIndex;
        this.privateScopeNode = privateScopeNode;
        this.context = context;
    }

    public final JSObject evaluateClass(JSRealm realm, Object className, Object initializers, DecorationState state) {
        return createContextObject(realm, CLASS_KIND, className, initializers, state, null, null, true);
    }

    //
    // ##### Method
    //

    @Specialization(guards = {"!isPrivate", "record.isMethod()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doPublicMethodCached(JSRealm realm, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createPropertyGetterCached(cachedName, context)") JSFunctionData valueGetterFunctionData) {
        JSObject getter = JSFunction.create(realm, valueGetterFunctionData);
        return createContextObject(realm, cachedName, initializers, state, getter, null, METHOD_KIND);
    }

    @Specialization(guards = {"!isPrivate", "record.isMethod()"}, replaces = {"doPublicMethodCached"})
    public JSObject doPublicMethodUncached(JSRealm realm, ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("createSetHidden(ELEMENT_RECORD_KEY, context)") @Shared PropertySetNode setElementRecord,
                    @Cached("createGetterFromPropertyUncached(context)") @Shared JSFunctionData valueGetterFunctionData) {
        Object description = record.getKey();
        JSObject getter = createFunctionWithElementRecordField(realm, record, valueGetterFunctionData, setElementRecord);
        return createContextObject(realm, description, initializers, state, getter, null, METHOD_KIND);
    }

    @Specialization(guards = {"isPrivate", "record.isMethod()"})
    public JSObject doPrivateMethod(VirtualFrame frame, JSRealm realm, ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("getName(record.getKey())") Object description,
                    @Cached("createGetterForPrivateMethodOrAccessor()") @Exclusive JSFunctionData valueGetterFunctionData) {
        assert description.equals(getName(record.getKey()));
        JSObject getter = JSFunction.create(realm, valueGetterFunctionData, getScopeFrame(frame));
        return createContextObject(realm, description, initializers, state, getter, null, METHOD_KIND);
    }

    //
    // ##### Field
    //

    @Specialization(guards = {"!isPrivate", "record.isField()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doPublicFieldCached(JSRealm realm, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createPropertyGetterCached(cachedName, context)") JSFunctionData valueGetterFunctionData,
                    @Cached("createPropertySetterCached(cachedName, context)") JSFunctionData valueSetterFunctionData) {
        JSObject getter = JSFunction.create(realm, valueGetterFunctionData);
        JSObject setter = JSFunction.create(realm, valueSetterFunctionData);
        return createContextObject(realm, cachedName, initializers, state, getter, setter, FIELD_KIND);
    }

    @Specialization(guards = {"!isPrivate", "record.isField()"}, replaces = "doPublicFieldCached")
    public JSObject doPublicFieldUncached(JSRealm realm, ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("createSetHidden(ELEMENT_RECORD_KEY, context)") @Shared PropertySetNode setElementRecord,
                    @Cached("createGetterFromPropertyUncached(context)") @Shared JSFunctionData valueGetterFunctionData,
                    @Cached("createSetterFromPropertyUncached(context)") @Shared JSFunctionData valueSetterFunctionData) {
        Object description = record.getKey();
        JSObject getter = createFunctionWithElementRecordField(realm, record, valueGetterFunctionData, setElementRecord);
        JSObject setter = createFunctionWithElementRecordField(realm, record, valueSetterFunctionData, setElementRecord);
        return createContextObject(realm, description, initializers, state, getter, setter, FIELD_KIND);
    }

    @Specialization(guards = {"isPrivate", "record.isField()"})
    public JSObject doPrivateField(JSRealm realm, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("getName(record.getKey())") Object description,
                    @Cached("createSetHidden(BACKING_STORAGE_KEY, context)") PropertySetNode setStorageKeyNode,
                    @Cached("createPrivateFieldGetter(context)") JSFunctionData valueGetterFunctionData,
                    @Cached("createPrivateFieldSetter(context)") JSFunctionData valueSetterFunctionData) {
        assert description.equals(getName(record.getKey()));
        JSObject getter = JSFunction.create(realm, valueGetterFunctionData);
        setStorageKeyNode.setValue(getter, record.getBackingStorageKey());
        JSObject setter = JSFunction.create(realm, valueSetterFunctionData);
        setStorageKeyNode.setValue(setter, record.getBackingStorageKey());
        return createContextObject(realm, description, initializers, state, getter, setter, FIELD_KIND);
    }
    //
    // ##### AutoAccessor
    //

    @Specialization(guards = {"!isPrivate", "record.isAutoAccessor()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doPublicAutoAccessorCached(JSRealm realm, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createPropertyGetterCached(cachedName, context)") JSFunctionData valueGetterFunctionData,
                    @Cached("createPropertySetterCached(cachedName, context)") JSFunctionData valueSetterFunctionData) {
        JSObject getter = JSFunction.create(realm, valueGetterFunctionData);
        JSObject setter = JSFunction.create(realm, valueSetterFunctionData);
        return createContextObject(realm, cachedName, initializers, state, getter, setter, AUTO_ACCESSOR_KIND);
    }

    @Specialization(guards = {"!isPrivate", "record.isAutoAccessor()"}, replaces = "doPublicAutoAccessorCached")
    public JSObject doPublicAutoAccessor(JSRealm realm, ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("createSetHidden(ELEMENT_RECORD_KEY, context)") @Shared PropertySetNode setElementRecord,
                    @Cached("createGetterFromPropertyUncached(context)") @Shared JSFunctionData valueGetterFunctionData,
                    @Cached("createSetterFromPropertyUncached(context)") @Shared JSFunctionData valueSetterFunctionData) {
        Object description = record.getKey();
        JSObject getter = createFunctionWithElementRecordField(realm, record, valueGetterFunctionData, setElementRecord);
        JSObject setter = createFunctionWithElementRecordField(realm, record, valueSetterFunctionData, setElementRecord);
        return createContextObject(realm, description, initializers, state, getter, setter, AUTO_ACCESSOR_KIND);
    }

    @Specialization(guards = {"isPrivate", "record.isAutoAccessor()"})
    public JSObject doPrivateAutoAccessor(VirtualFrame frame, JSRealm realm, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("getName(record.getKey())") Object description,
                    @Cached("createGetterForPrivateMethodOrAccessor()") @Exclusive JSFunctionData valueGetterFunctionData,
                    @Cached("createSetterForPrivateAccessor()") @Exclusive JSFunctionData valueSetterFunctionData) {
        assert description.equals(getName(record.getKey()));
        JSObject getter = JSFunction.create(realm, valueGetterFunctionData, getScopeFrame(frame));
        JSObject setter = JSFunction.create(realm, valueSetterFunctionData, getScopeFrame(frame));
        return createContextObject(realm, description, initializers, state, getter, setter, AUTO_ACCESSOR_KIND);
    }

    //
    // ##### Getter
    //

    @Specialization(guards = {"!isPrivate", "record.isGetter()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doPublicGetterCached(JSRealm realm, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createPropertyGetterCached(cachedName, context)") JSFunctionData valueGetterFunctionData) {
        JSObject getter = JSFunction.create(realm, valueGetterFunctionData);
        return createContextObject(realm, cachedName, initializers, state, getter, null, GETTER_KIND);
    }

    @Specialization(guards = {"!isPrivate", "record.isGetter()"}, replaces = "doPublicGetterCached")
    public JSObject doPublicGetterUncached(JSRealm realm, ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("createSetHidden(ELEMENT_RECORD_KEY, context)") @Shared PropertySetNode setElementRecord,
                    @Cached("createGetterFromPropertyUncached(context)") @Shared JSFunctionData valueGetterFunctionData) {
        Object name = record.getKey();
        JSObject getter = createFunctionWithElementRecordField(realm, record, valueGetterFunctionData, setElementRecord);
        return createContextObject(realm, name, initializers, state, getter, null, GETTER_KIND);
    }

    @Specialization(guards = {"isPrivate", "record.isGetter()"})
    public JSObject doPrivateGetter(VirtualFrame frame, JSRealm realm, ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("getName(record.getKey())") Object description,
                    @Cached("createGetterForPrivateMethodOrAccessor()") @Exclusive JSFunctionData valueGetterFunctionData) {
        assert description.equals(getName(record.getKey()));
        JSObject getter = JSFunction.create(realm, valueGetterFunctionData, getScopeFrame(frame));
        return createContextObject(realm, description, initializers, state, getter, null, GETTER_KIND);
    }

    //
    // ##### Setter
    //

    @Specialization(guards = {"!isPrivate", "record.isSetter()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doPublicSetterCached(JSRealm realm, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached @Shared @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createPropertySetterCached(cachedName, context)") JSFunctionData valueSetterFunctionData) {
        JSObject setter = JSFunction.create(realm, valueSetterFunctionData);
        return createContextObject(realm, cachedName, initializers, state, null, setter, SETTER_KIND);
    }

    @Specialization(guards = {"!isPrivate", "record.isSetter()"}, replaces = "doPublicSetterCached")
    public JSObject doPublicSetterUncached(JSRealm realm, ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("createSetHidden(ELEMENT_RECORD_KEY, context)") @Shared PropertySetNode setElementRecord,
                    @Cached("createSetterFromPropertyUncached(context)") @Shared JSFunctionData valueSetterFunctionData) {
        Object name = record.getKey();
        JSObject setter = createFunctionWithElementRecordField(realm, record, valueSetterFunctionData, setElementRecord);
        return createContextObject(realm, name, initializers, state, null, setter, SETTER_KIND);
    }

    @Specialization(guards = {"isPrivate", "record.isSetter()"})
    public JSObject doPrivateSetter(VirtualFrame frame, JSRealm realm, ClassElementDefinitionRecord record, Object initializers, DecorationState state,
                    @Cached("getName(record.getKey())") Object description,
                    @Cached("createSetterForPrivateAccessor()") @Exclusive JSFunctionData valueSetterFunctionData) {
        assert description.equals(getName(record.getKey()));
        JSObject setter = JSFunction.create(realm, valueSetterFunctionData, getScopeFrame(frame));
        return createContextObject(realm, description, initializers, state, null, setter, SETTER_KIND);
    }

    //
    // ##### Common
    //

    private static JSObject createFunctionWithElementRecordField(JSRealm realm, ClassElementDefinitionRecord record, JSFunctionData functionData, PropertySetNode setElementRecord) {
        assert !record.isPrivate();
        JSObject function = JSFunction.create(realm, functionData);
        setElementRecord.setValue(function, record);
        return function;
    }

    private MaterializedFrame getScopeFrame(VirtualFrame frame) {
        return (MaterializedFrame) privateScopeNode.executeFrame(frame);
    }

    protected static boolean nameEquals(TruffleString.EqualNode strEq, ClassElementDefinitionRecord record, Object expected) {
        if (record.getKey() instanceof TruffleString keyStr && expected instanceof TruffleString expectedStr) {
            return Strings.equals(strEq, expectedStr, keyStr);
        }
        return false;
    }

    @NeverDefault
    @TruffleBoundary
    protected Object getName(Object key) {
        if (key instanceof HiddenKey) {
            String name = ((HiddenKey) key).getName();
            String description = name.charAt(0) == '#' ? name.substring(1) : name;
            return Strings.fromJavaString(description);
        }
        return key;
    }

    private JSFunctionObject createAddInitializerFunction(Object initializers, DecorationState state) {
        JSFunctionObject addInitializerFunction = JSFunction.create(JSRealm.get(this), getAddInitializerFunctionData(context));
        setInitializersKey.setValue(addInitializerFunction, initializers);
        setDecorationState.setValue(addInitializerFunction, state);
        return addInitializerFunction;
    }

    public JSObject createContextObject(JSRealm realm, Object name, Object initializers,
                    DecorationState state,
                    JSObject getter,
                    JSObject setter,
                    TruffleString kindName) {
        return createContextObject(realm, kindName, name, initializers, state, getter, setter, false);
    }

    private JSObject createContextObject(JSRealm realm, TruffleString kindName, Object name, Object initializers, DecorationState state,
                    JSObject getter, JSObject setter, boolean isClass) {
        JSObject contextObj = createObjectNode.execute(realm);
        defineKind.executeVoid(contextObj, KIND, kindName);
        if (!isClass) {
            JSObject accessObject = createObjectNode.execute(realm);
            if (getter != null) {
                defineGet.executeVoid(accessObject, Strings.GET, getter);
            }
            if (setter != null) {
                defineSet.executeVoid(accessObject, Strings.SET, setter);
            }
            defineAccess.executeVoid(contextObj, ACCESS, accessObject);
            defineStatic.executeVoid(contextObj, Strings.STATIC, isStatic);
            definePrivate.executeVoid(contextObj, Strings.PRIVATE, isPrivate);
        }
        defineName.executeVoid(contextObj, NAME, name);
        defineAddInitializer.executeVoid(contextObj, ADD_INITIALIZER, createAddInitializerFunction(initializers, state));
        return contextObj;
    }

    //
    // ##### Functions cached in nodes. One per specialization, they can specialize on
    // key/frame/etc.
    //

    @NeverDefault
    private static JSFunctionData getAddInitializerFunctionData(JSContext ctx) {
        return ctx.getOrCreateBuiltinFunctionData(BuiltinFunctionKey.DecoratorContextAddInitializer, CreateDecoratorContextObjectNode::createAddInitializerFunctionData);
    }

    private static JSFunctionData createAddInitializerFunctionData(JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage()) {
            @Child private PropertyGetNode getDecorationState = PropertyGetNode.createGetHidden(DECORATION_STATE_KEY, context);
            @Child private PropertyGetNode getInitializersKey = PropertyGetNode.createGetHidden(INIT_KEY, context);

            @SuppressWarnings("unchecked")
            @Override
            public Object execute(VirtualFrame frame) {
                JSFunctionObject self = (JSFunctionObject) JSArguments.getFunctionObject(frame.getArguments());
                DecorationState state = (DecorationState) getDecorationState.getValue(self);
                SimpleArrayList<Object> initializers = (SimpleArrayList<Object>) getInitializersKey.getValue(self);
                if (state.finished) {
                    throw Errors.createTypeError("Bad decorator initializer state");
                }
                Object[] args = frame.getArguments();
                Object value = JSArguments.getUserArgumentCount(args) > 0 ? JSArguments.getUserArgument(args, 0) : Undefined.instance;
                initializers.addUncached(value);
                return Undefined.instance;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 1, ADD_INITIALIZER);
    }

    @NeverDefault
    protected static JSFunctionData createPropertyGetterCached(Object name, JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        assert JSRuntime.isPropertyKey(name) : name;
        CallTarget getter = new JavaScriptRootNode(context.getLanguage()) {
            @Child private PropertyGetNode propertyGetNode = PropertyGetNode.create(name, context);

            @Override
            public Object execute(VirtualFrame frame) {
                Object thiz = JSFrameUtil.getThisObj(frame);
                return propertyGetNode.getValue(thiz);
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, getter, 0, Strings.GET);
    }

    @NeverDefault
    protected static JSFunctionData createPropertySetterCached(Object name, JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        assert JSRuntime.isPropertyKey(name) : name;
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage()) {
            @Child private PropertySetNode propertySetNode = PropertySetNode.create(name, false, context, true);

            @Override
            public Object execute(VirtualFrame frame) {
                Object thiz = JSFrameUtil.getThisObj(frame);
                Object[] args = frame.getArguments();
                Object newValue = JSArguments.getUserArgumentCount(args) > 0 ? JSArguments.getUserArgument(args, 0) : Undefined.instance;
                propertySetNode.setValue(thiz, newValue);
                return Undefined.instance;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
    }

    @NeverDefault
    protected static JSFunctionData createPrivateFieldGetter(JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        JavaScriptRootNode getter = new JavaScriptRootNode(context.getLanguage()) {
            @Child private PrivateFieldGetNode privateFieldGet = PrivateFieldGetNode.create(AccessThisNode.create(),
                            PropertyNode.createGetHidden(context, AccessFunctionNode.create(), BACKING_STORAGE_KEY), context);

            @Override
            public Object execute(VirtualFrame frame) {
                return privateFieldGet.execute(frame);
            }
        };
        return JSFunctionData.createCallOnly(context, getter.getCallTarget(), 0, Strings.GET);
    }

    @NeverDefault
    protected static JSFunctionData createPrivateFieldSetter(JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        JavaScriptRootNode setter = new JavaScriptRootNode(context.getLanguage()) {
            @Child private PrivateFieldSetNode privateFieldSet = PrivateFieldSetNode.create(AccessThisNode.create(),
                            PropertyNode.createGetHidden(context, AccessFunctionNode.create(), BACKING_STORAGE_KEY),
                            AccessIndexedArgumentNode.create(0), context);

            @Override
            public Object execute(VirtualFrame frame) {
                privateFieldSet.executeVoid(frame);
                return Undefined.instance;
            }
        };
        return JSFunctionData.createCallOnly(context, setter.getCallTarget(), 1, Strings.SET);
    }

    @NeverDefault
    protected final JSFunctionData createGetterForPrivateMethodOrAccessor() {
        return createPrivateGetter(context, isStatic, privateMemberSlotIndex, privateBrandSlotIndex);
    }

    @NeverDefault
    protected final JSFunctionData createSetterForPrivateAccessor() {
        return createPrivateSetter(context, isStatic, privateMemberSlotIndex, privateBrandSlotIndex);
    }

    private static JSFunctionData createPrivateGetter(JSContext context, boolean isStatic, int memberSlot, int constructorSlot) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage()) {
            @Child private PrivateFieldGetNode privateGetNode;

            @Override
            public Object execute(VirtualFrame frame) {
                if (privateGetNode == null) {
                    initialize(frame);
                }
                return privateGetNode.execute(frame);
            }

            private void initialize(VirtualFrame frame) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                ScopeFrameNode enclosingFrameNode = ScopeFrameNode.create(1);
                FrameDescriptor frameDescriptor = enclosingFrameNode.executeFrame(frame).getFrameDescriptor();
                assert frameDescriptor.getNumberOfSlots() > 0 : frameDescriptor;
                JSReadFrameSlotNode readConstructor = JSReadFrameSlotNode.create(JSFrameSlot.fromIndexedFrameSlot(frameDescriptor, constructorSlot), enclosingFrameNode, false);
                JavaScriptNode readBrand;
                if (isStatic) {
                    readBrand = readConstructor;
                } else {
                    readBrand = PropertyNode.createGetHidden(context, readConstructor, JSFunction.PRIVATE_BRAND_ID);
                }
                JSReadFrameSlotNode readPrivateMemberSlot = JSReadFrameSlotNode.create(JSFrameSlot.fromIndexedFrameSlot(frameDescriptor, memberSlot), enclosingFrameNode, false);
                privateGetNode = insert(PrivateFieldGetNode.create(PrivateBrandCheckNode.create(AccessThisNode.create(), readBrand), readPrivateMemberSlot, context));
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 0, Strings.GET);
    }

    private static JSFunctionData createPrivateSetter(JSContext context, boolean isStatic, int memberSlot, int constructorSlot) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage()) {
            @Child private PrivateFieldSetNode privateSetNode;

            @Override
            public Object execute(VirtualFrame frame) {
                if (privateSetNode == null) {
                    initialize(frame);
                }
                privateSetNode.executeVoid(frame);
                return Undefined.instance;
            }

            private void initialize(VirtualFrame frame) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                ScopeFrameNode enclosingFrameNode = ScopeFrameNode.create(1);
                FrameDescriptor frameDescriptor = enclosingFrameNode.executeFrame(frame).getFrameDescriptor();
                assert frameDescriptor.getNumberOfSlots() > 0 : frameDescriptor;
                JSReadFrameSlotNode readConstructor = JSReadFrameSlotNode.create(JSFrameSlot.fromIndexedFrameSlot(frameDescriptor, constructorSlot), enclosingFrameNode, false);
                JavaScriptNode readBrand;
                if (isStatic) {
                    readBrand = readConstructor;
                } else {
                    readBrand = PropertyNode.createGetHidden(context, readConstructor, JSFunction.PRIVATE_BRAND_ID);
                }
                JSReadFrameSlotNode readPrivateMemberSlot = JSReadFrameSlotNode.create(JSFrameSlot.fromIndexedFrameSlot(frameDescriptor, memberSlot), enclosingFrameNode, false);
                privateSetNode = insert(PrivateFieldSetNode.create(
                                PrivateBrandCheckNode.create(AccessThisNode.create(), readBrand),
                                readPrivateMemberSlot, AccessIndexedArgumentNode.create(0), context));
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
    }

    //
    // ##### Generic Functions that are not cached in nodes and don't specialize on keys/frame.
    //

    @NeverDefault
    protected static JSFunctionData createGetterFromPropertyUncached(JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage()) {
            @Child private PropertyGetNode getElementRecord = PropertyGetNode.createGetHidden(ELEMENT_RECORD_KEY, context);
            @Child private ReadElementNode propertyGetNode = ReadElementNode.create(context);

            @Override
            public Object execute(VirtualFrame frame) {
                Object thiz = JSFrameUtil.getThisObj(frame);
                Object function = JSFrameUtil.getFunctionObject(frame);
                ClassElementDefinitionRecord record = (ClassElementDefinitionRecord) getElementRecord.getValue(function);
                assert !record.isPrivate();
                return propertyGetNode.executeWithTargetAndIndex(thiz, record.getKey());
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 0, Strings.GET);
    }

    @NeverDefault
    protected static JSFunctionData createSetterFromPropertyUncached(JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage()) {
            @Child private PropertyGetNode getElementRecord = PropertyGetNode.createGetHidden(ELEMENT_RECORD_KEY, context);
            @Child private WriteElementNode propertySetNode = WriteElementNode.create(context, false);

            @Override
            public Object execute(VirtualFrame frame) {
                Object thiz = JSFrameUtil.getThisObj(frame);
                Object function = JSFrameUtil.getFunctionObject(frame);
                ClassElementDefinitionRecord record = (ClassElementDefinitionRecord) getElementRecord.getValue(function);
                assert !record.isPrivate();
                Object[] args = frame.getArguments();
                Object newValue = JSArguments.getUserArgumentCount(args) > 0 ? JSArguments.getUserArgument(args, 0) : Undefined.instance;
                propertySetNode.executeWithTargetAndIndexAndValue(thiz, record.getKey(), newValue);
                return Undefined.instance;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
    }
}

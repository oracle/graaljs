/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
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

    public static class Record {
        boolean finished = false;
    }

    private static final TruffleString FIELD_KIND = Strings.constant("field");
    private static final TruffleString GETTER_KIND = Strings.constant("getter");
    private static final TruffleString SETTER_KIND = Strings.constant("setter");
    private static final TruffleString METHOD_KIND = Strings.constant("method");
    private static final TruffleString CLASS_KIND = Strings.constant("class");
    private static final TruffleString AUTO_ACCESSOR_KIND = Strings.constant("accessor");

    private static final TruffleString ADD_INITIALIZER = Strings.constant("addInitializer");
    private static final TruffleString NAME = Strings.constant("name");
    private static final TruffleString KIND = Strings.constant("kind");
    private static final TruffleString ACCESS = Strings.constant("access");

    private static final HiddenKey INIT_KEY = new HiddenKey(":initializers");
    protected static final HiddenKey MAGIC_KEY = new HiddenKey(":magic");
    protected static final HiddenKey BACKING_STORAGE_KEY = new HiddenKey("BackingStorageKey");

    static final int LIMIT = 3;

    @Child private ScopeFrameNode privateScopeNode;
    private final int privateMemberSlotIndex;
    private final int privateBrandSlotIndex;
    @CompilationFinal private JSFunctionData valueGetterPropertyUncached;
    @CompilationFinal private JSFunctionData valueSetterPropertyUncached;
    @CompilationFinal private JSFunctionData methodGetterPropertyUncached;

    @Child private PropertySetNode setRecordKey;
    @Child private CreateObjectNode createObjectNode;
    @Child private PropertySetNode setInitializersKey;
    @Child protected TruffleString.RegionEqualByteIndexNode eqNode;

    protected final boolean isStatic;
    protected final boolean isPrivate;
    private final JSFunctionData initializerData;
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
        return CreateDecoratorContextObjectNodeGen.create(context, member.isStatic(), member.isPrivate(), privateScopeNode, privateMemberSlotIndex, privateBrandSlotIndex);
    }

    public static CreateDecoratorContextObjectNode createForClass(JSContext context) {
        return CreateDecoratorContextObjectNodeGen.create(context, false, false, null, -1, -1);
    }

    public abstract JSObject executeContext(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state);

    CreateDecoratorContextObjectNode(JSContext context, boolean isStatic, boolean isPrivate, ScopeFrameNode privateScopeNode, int privateMemberSlotIndex, int privateBrandSlotIndex) {
        this.initializerData = createInitializerFunctionData(context);
        this.createObjectNode = CreateObjectNode.create(context);
        this.setInitializersKey = PropertySetNode.createSetHidden(INIT_KEY, context);
        this.setRecordKey = PropertySetNode.createSetHidden(MAGIC_KEY, context);
        this.eqNode = TruffleString.RegionEqualByteIndexNode.create();
        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
        this.privateMemberSlotIndex = privateMemberSlotIndex;
        this.privateBrandSlotIndex = privateBrandSlotIndex;
        this.privateScopeNode = privateScopeNode;
        this.context = context;
    }

    public final JSObject evaluateClass(VirtualFrame frame, Object className, Object initializers, Record state) {
        JSObject contextObj = createObjectNode.execute(frame);
        JSRuntime.createDataPropertyOrThrow(contextObj, KIND, CLASS_KIND);
        JSRuntime.createDataPropertyOrThrow(contextObj, NAME, className);
        addInitializerFunction(contextObj, state, initializers);
        return contextObj;
    }

    //
    // ##### Method
    //

    @Specialization(guards = {"!isPrivate", "record.isMethod()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doPublicMethodCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createPropertyGetterCached(cachedName, context)") JSFunctionData valueGetterFunctionData) {
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, null, METHOD_KIND);
    }

    @Specialization(guards = {"!isPrivate", "record.isMethod()"}, replaces = {"doPublicMethodCached"})
    public JSObject doPublicMethodUncached(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY, context)") @Shared("setMagic") PropertySetNode setMagic) {
        Object description = record.getKey();
        JSFunctionData valueGetterFunctionData = getMethodGetterPropertyUncached();
        JSObject getter = initializeMagicField(record, valueGetterFunctionData, setMagic);
        return createContextObject(frame, description, initializers, state, getter, null, METHOD_KIND);
    }

    @Specialization(guards = {"isPrivate", "record.isMethod()"})
    public JSObject doPrivateMethod(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("getName(record.getKey())") Object description,
                    @Cached("createGetterForPrivateMethodOrAccessor()") @Exclusive JSFunctionData valueGetterFunctionData) {
        assert description.equals(getName(record.getKey()));
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData, getScopeFrame(frame));
        return createContextObject(frame, description, initializers, state, getter, null, METHOD_KIND);
    }

    //
    // ##### Field
    //

    @Specialization(guards = {"!isPrivate", "record.isField()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doFieldCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createPropertyGetterCached(cachedName, context)") JSFunctionData valueGetterFunctionData,
                    @Cached("createPropertySetterCached(cachedName, context)") JSFunctionData valueSetterFunctionData) {
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        JSObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, setter, FIELD_KIND);
    }

    @Specialization(guards = {"!isPrivate", "record.isField()"}, replaces = "doFieldCached")
    public JSObject doFieldUncached(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY, context)") @Shared("setMagic") PropertySetNode setMagic) {
        return createContextGetterSetter(frame, record, initializers, state, setMagic, FIELD_KIND);
    }

    @Specialization(guards = {"isPrivate", "record.isField()"})
    public JSObject doPrivateField(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("getName(record.getKey())") Object description,
                    @Cached("createSetHidden(BACKING_STORAGE_KEY, context)") PropertySetNode setStorageKeyNode,
                    @Cached("createPrivateFieldGetter(context)") JSFunctionData valueGetterFunctionData,
                    @Cached("createPrivateFieldSetter(context)") JSFunctionData valueSetterFunctionData) {
        assert description.equals(getName(record.getKey()));
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        setStorageKeyNode.setValue(getter, record.getBackingStorageKey());
        JSObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        setStorageKeyNode.setValue(setter, record.getBackingStorageKey());
        return createContextObject(frame, description, initializers, state, getter, setter, FIELD_KIND);
    }
    //
    // ##### AutoAccessor
    //

    @Specialization(guards = {"!isPrivate", "record.isAutoAccessor()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doAutoAccessorCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createPropertyGetterCached(cachedName, context)") JSFunctionData valueGetterFunctionData,
                    @Cached("createPropertySetterCached(cachedName, context)") JSFunctionData valueSetterFunctionData) {
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        JSObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, setter, AUTO_ACCESSOR_KIND);
    }

    @Specialization(guards = {"!isPrivate", "record.isAutoAccessor()"}, replaces = "doAutoAccessorCached")
    public JSObject doAutoAccessor(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY, context)") @Shared("setMagic") PropertySetNode setMagic) {
        return createContextGetterSetter(frame, record, initializers, state, setMagic, AUTO_ACCESSOR_KIND);
    }

    @Specialization(guards = {"isPrivate", "record.isAutoAccessor()"})
    public JSObject doPrivateAutoAccessor(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("getName(record.getKey())") Object description,
                    @Cached("createGetterForPrivateMethodOrAccessor()") @Exclusive JSFunctionData valueGetterFunctionData,
                    @Cached("createSetterForPrivateAccessor()") @Exclusive JSFunctionData valueSetterFunctionData) {
        assert description.equals(getName(record.getKey()));
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData, getScopeFrame(frame));
        JSObject setter = JSFunction.create(getRealm(), valueSetterFunctionData, getScopeFrame(frame));
        return createContextObject(frame, description, initializers, state, getter, setter, AUTO_ACCESSOR_KIND);
    }

    //
    // ##### Getter
    //

    @Specialization(guards = {"!isPrivate", "record.isGetter()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doGetterCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createPropertyGetterCached(cachedName, context)") JSFunctionData valueGetterFunctionData) {
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        return createContextObject(frame, cachedName, initializers, state, getter, null, GETTER_KIND);
    }

    @Specialization(guards = {"!isPrivate", "record.isGetter()"}, replaces = "doGetterCached")
    public JSObject doGetter(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY, context)") @Shared("setMagic") PropertySetNode setMagic) {
        JSFunctionData valueGetterFunctionData = getValueGetterPropertyUncached();
        JSObject getter = initializeMagicField(record, valueGetterFunctionData, setMagic);
        Object name = record.getKey();
        return createContextObject(frame, name, initializers, state, getter, null, GETTER_KIND);
    }

    @Specialization(guards = {"isPrivate", "record.isGetter()"})
    public JSObject doPrivateGetter(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("getName(record.getKey())") Object description,
                    @Cached("createGetterForPrivateMethodOrAccessor()") @Exclusive JSFunctionData valueGetterFunctionData) {
        assert description.equals(getName(record.getKey()));
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData, getScopeFrame(frame));
        return createContextObject(frame, description, initializers, state, getter, null, GETTER_KIND);
    }

    //
    // ##### Setter
    //

    @Specialization(guards = {"!isPrivate", "record.isSetter()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doSetterCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createPropertySetterCached(cachedName, context)") JSFunctionData valueSetterFunctionData) {
        JSObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        return createContextObject(frame, description, initializers, state, null, setter, SETTER_KIND);
    }

    @Specialization(guards = {"!isPrivate", "record.isSetter()"}, replaces = "doSetterCached")
    public JSObject doSetter(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY, context)") @Shared("setMagic") PropertySetNode setMagic) {
        JSFunctionData valueSetterFunctionData = getValueSetterPropertyUncached();
        JSObject setter = initializeMagicField(record, valueSetterFunctionData, setMagic);
        Object name = record.getKey();
        return createContextObject(frame, name, initializers, state, null, setter, SETTER_KIND);
    }

    @Specialization(guards = {"isPrivate", "record.isSetter()"})
    public JSObject doPrivateSetter(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("getName(record.getKey())") Object description,
                    @Cached("createSetterForPrivateAccessor()") @Exclusive JSFunctionData valueSetterFunctionData) {
        assert description.equals(getName(record.getKey()));
        JSObject setter = JSFunction.create(getRealm(), valueSetterFunctionData, getScopeFrame(frame));
        return createContextObject(frame, description, initializers, state, null, setter, SETTER_KIND);
    }

    //
    // ##### Common
    //

    private JSObject initializeMagicField(ClassElementDefinitionRecord record, JSFunctionData functionData, PropertySetNode setMagic) {
        assert !record.isPrivate();
        JSObject function = JSFunction.create(getRealm(), functionData);
        setMagic.setValue(function, record);
        return function;
    }

    private MaterializedFrame getScopeFrame(VirtualFrame frame) {
        return (MaterializedFrame) privateScopeNode.executeFrame(frame);
    }

    protected JSFunctionData getMethodGetterPropertyUncached() {
        if (methodGetterPropertyUncached == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            methodGetterPropertyUncached = createGetterFromPropertyUncached(context);
        }
        return methodGetterPropertyUncached;
    }

    private JSFunctionData getValueGetterPropertyUncached() {
        if (valueGetterPropertyUncached == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            valueGetterPropertyUncached = createGetterFromPropertyUncached(context);
        }
        return valueGetterPropertyUncached;
    }

    private JSFunctionData getValueSetterPropertyUncached() {
        if (valueSetterPropertyUncached == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            valueSetterPropertyUncached = createSetterFromPropertyUncached(context);
        }
        return valueSetterPropertyUncached;
    }

    protected static boolean nameEquals(TruffleString.EqualNode strEq, ClassElementDefinitionRecord record, Object expected) {
        if (record.getKey() instanceof TruffleString && expected instanceof TruffleString) {
            return Strings.equals(strEq, (TruffleString) expected, (TruffleString) record.getKey());
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

    private void addInitializerFunction(JSObject contextObj, Record state, Object initializers) {
        JSFunctionObject initializerFunction = JSFunction.create(JSRealm.get(this), initializerData);
        setInitializersKey.setValue(initializerFunction, initializers);
        setRecordKey.setValue(initializerFunction, state);
        JSRuntime.createDataPropertyOrThrow(contextObj, ADD_INITIALIZER, initializerFunction);
    }

    public JSObject createContextGetterSetter(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    PropertySetNode setMagic, TruffleString kind) {
        assert !isPrivate;
        Object description = getName(record.getKey());
        JSFunctionData valueGetterFunctionData = getValueGetterPropertyUncached();
        JSFunctionData valueSetterFunctionData = getValueSetterPropertyUncached();
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        JSObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        setMagic.setValue(getter, record);
        setMagic.setValue(setter, record);
        return createContextObject(frame, description, initializers, state, getter, setter, kind);
    }

    public JSObject createContextObject(VirtualFrame frame, Object name, Object initializers,
                    Record state,
                    JSObject getter,
                    JSObject setter,
                    TruffleString kindName) {
        JSObject contextObj = createObjectNode.execute(frame);
        JSRuntime.createDataPropertyOrThrow(contextObj, KIND, kindName);
        JSObject accessObject = createObjectNode.execute(frame);
        if (getter != null) {
            JSRuntime.createDataPropertyOrThrow(accessObject, Strings.GET, getter);
        }
        if (setter != null) {
            JSRuntime.createDataPropertyOrThrow(accessObject, Strings.SET, setter);
        }
        JSRuntime.createDataPropertyOrThrow(contextObj, ACCESS, accessObject);
        JSRuntime.createDataPropertyOrThrow(contextObj, Strings.STATIC, isStatic);
        JSRuntime.createDataPropertyOrThrow(contextObj, Strings.PRIVATE, isPrivate);
        JSRuntime.createDataPropertyOrThrow(contextObj, NAME, name);
        addInitializerFunction(contextObj, state, initializers);
        return contextObj;
    }

    //
    // ##### Functions cached in nodes. One per specialization, they can specialize on
    // key/frame/etc.
    //

    @NeverDefault
    protected static JSFunctionData createInitializerFunctionData(JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private PropertyGetNode getRecordKey = PropertyGetNode.createGetHidden(MAGIC_KEY, context);
            @Child private PropertyGetNode getInitializersKey = PropertyGetNode.createGetHidden(INIT_KEY, context);

            @SuppressWarnings("unchecked")
            @Override
            public Object execute(VirtualFrame frame) {
                JSFunctionObject self = (JSFunctionObject) JSArguments.getFunctionObject(frame.getArguments());
                Record state = (Record) getRecordKey.getValue(self);
                SimpleArrayList<Object> initializers = (SimpleArrayList<Object>) getInitializersKey.getValue(self);
                if (state.finished) {
                    CompilerDirectives.transferToInterpreter();
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
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
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
        JavaScriptRootNode setter = new JavaScriptRootNode(context.getLanguage(), null, null) {
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

    @TruffleBoundary
    protected static JSFunctionData createGetterFromPropertyUncached(JSContext context) {
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private ReadElementNode propertyGetNode = ReadElementNode.create(context);
            @Child private PropertyGetNode getMagic = PropertyGetNode.createGetHidden(MAGIC_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {
                Object thiz = JSFrameUtil.getThisObj(frame);
                Object function = JSFrameUtil.getFunctionObject(frame);
                ClassElementDefinitionRecord record = (ClassElementDefinitionRecord) getMagic.getValue(function);
                assert !record.isPrivate();
                return propertyGetNode.executeWithTargetAndIndex(thiz, record.getKey());
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 0, Strings.GET);
    }

    @TruffleBoundary
    protected static JSFunctionData createSetterFromPropertyUncached(JSContext context) {
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private PropertyGetNode getMagic = PropertyGetNode.createGetHidden(MAGIC_KEY, context);
            @Child private WriteElementNode propertySetNode = WriteElementNode.create(context, false);

            @Override
            public Object execute(VirtualFrame frame) {
                Object thiz = JSFrameUtil.getThisObj(frame);
                Object function = JSFrameUtil.getFunctionObject(frame);
                ClassElementDefinitionRecord record = (ClassElementDefinitionRecord) getMagic.getValue(function);
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

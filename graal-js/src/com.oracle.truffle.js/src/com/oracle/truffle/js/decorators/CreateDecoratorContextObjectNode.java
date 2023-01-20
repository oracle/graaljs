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

import static com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord.Kind.Getter;

import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.HasPropertyCacheNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.ObjectLiteralMemberNode;
import com.oracle.truffle.js.nodes.access.PrivateBrandCheckNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord;
import com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord.PrivateFrameBasedElementDefinitionRecord;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

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

    static final int LIMIT = 3;

    @CompilationFinal private int blockScopeSlot = -1;
    @CompilationFinal private JSFunctionData valueGetterFrameUncached;
    @CompilationFinal private JSFunctionData valueGetterPropertyUncached;
    @CompilationFinal private JSFunctionData valueSetterFrameUncached;
    @CompilationFinal private JSFunctionData valueSetterPropertyUncached;
    @CompilationFinal private JSFunctionData methodGetterFrameUncached;
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
        return CreateDecoratorContextObjectNodeGen.create(context, member.isStatic(), member.isPrivate());
    }

    public static CreateDecoratorContextObjectNode createForClass(JSContext context) {
        return CreateDecoratorContextObjectNodeGen.create(context, false, false);
    }

    public abstract JSObject executeContext(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state);

    CreateDecoratorContextObjectNode(JSContext context, boolean isStatic, boolean isPrivate) {
        this.initializerData = createInitializerFunctionData(context);
        this.createObjectNode = CreateObjectNode.create(context);
        this.setInitializersKey = PropertySetNode.createSetHidden(INIT_KEY, context);
        this.setRecordKey = PropertySetNode.createSetHidden(MAGIC_KEY, context);
        this.eqNode = TruffleString.RegionEqualByteIndexNode.create();
        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
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

    @Specialization(guards = {"record.isMethod()", "nameEquals(strEq, record, cachedName)", "isPrivate"}, limit = "LIMIT")
    public JSObject doPrivateMethodCached(VirtualFrame frame, @SuppressWarnings("unused") PrivateFrameBasedElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") Object cachedName,
                    @Cached("getName(cachedName)") @SuppressWarnings("unused") Object description,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createMethodGetterFromFrameCached(record)") JSFunctionData valueGetterFunctionData) {
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData, getScopeFrame(frame));
        return createContextObject(frame, cachedName, initializers, state, getter, null, METHOD_KIND);
    }

    @Specialization(guards = {"record.isMethod()", "nameEquals(strEq, record, cachedName)", "!isPrivate"}, limit = "LIMIT")
    public JSObject doPublicMethodCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createValueGetterCached(cachedName,false)") JSFunctionData valueGetterFunctionData) {
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, null, METHOD_KIND);
    }

    @Specialization(guards = "record.isMethod()", replaces = {"doPublicMethodCached", "doPrivateMethodCached"})
    public JSObject doMethodGeneric(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY, context)") @Shared("setMagic") PropertySetNode setMagic) {
        Object description = record.isPrivate() ? record.getKey() : getName(record.getKey());
        JSFunctionData valueGetterFunctionData = record.isPrivate() ? getMethodGetterFrameUncached() : getMethodGetterPropertyUncached();
        JSObject getter = initializeMagicField(frame, record, valueGetterFunctionData, setMagic);
        return createContextObject(frame, description, initializers, state, getter, null, METHOD_KIND);
    }

    //
    // ##### Field
    //

    @Specialization(guards = {"record.isField()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doFieldCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createValueGetterCached(cachedName, isPrivate)") JSFunctionData valueGetterFunctionData,
                    @Cached("createValueSetterCached(cachedName, isPrivate)") JSFunctionData valueSetterFunctionData) {
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        JSObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, setter, FIELD_KIND);
    }

    @Specialization(guards = "record.isField()", replaces = "doFieldCached")
    public JSObject doFieldUncached(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY, context)") @Shared("setMagic") PropertySetNode setMagic) {
        return createContextGetterSetter(frame, record, initializers, state, setMagic, FIELD_KIND);
    }

    //
    // ##### AutoAccessor
    //

    @Specialization(guards = {"record.isAutoAccessor()", "nameEquals(strEq, record, cachedName)"}, limit = "LIMIT")
    public JSObject doAutoAccessorCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord.AutoAccessor record, Object initializers, Record state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createValueGetterCached(cachedName, isPrivate)") JSFunctionData valueGetterFunctionData,
                    @Cached("createValueSetterCached(cachedName, isPrivate)") JSFunctionData valueSetterFunctionData) {
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        JSObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, setter, AUTO_ACCESSOR_KIND);
    }

    @Specialization(guards = "record.isAutoAccessor()", replaces = "doAutoAccessorCached")
    public JSObject doAutoAccessor(VirtualFrame frame, ClassElementDefinitionRecord.AutoAccessor record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY, context)") @Shared("setMagic") PropertySetNode setMagic) {
        return createContextGetterSetter(frame, record, initializers, state, setMagic, AUTO_ACCESSOR_KIND);
    }

    //
    // ##### Getter
    //

    @Specialization(guards = {"record.isGetter()", "nameEquals(strEq, record, cachedName)", "!isPrivate"}, limit = "LIMIT")
    public JSObject doGetterCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createValueGetterCached(cachedName, isPrivate)") JSFunctionData valueGetterFunctionData) {
        JSObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, null, GETTER_KIND);
    }

    @Specialization(guards = "record.isGetter()", replaces = "doGetterCached")
    public JSObject doGetter(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY, context)") @Shared("setMagic") PropertySetNode setMagic) {
        JSFunctionData valueGetterFunctionData = record.isPrivate() ? getValueGetterFrameUncached() : getValueGetterPropertyUncached();
        JSObject getter = initializeMagicField(frame, record, valueGetterFunctionData, setMagic);
        Object name = record.isPrivate() ? record.getKey() : getName(record.getKey());
        return createContextObject(frame, name, initializers, state, getter, null, GETTER_KIND);
    }

    //
    // ##### Setter
    //

    @Specialization(guards = {"record.isSetter()", "nameEquals(strEq, record, cachedName)", "!isPrivate"}, limit = "LIMIT")
    public JSObject doSetterCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") @SuppressWarnings("unused") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @Cached @Shared("strEq") @SuppressWarnings("unused") TruffleString.EqualNode strEq,
                    @Cached("createValueSetterCached(cachedName, isPrivate)") JSFunctionData valueSetterFunctionData) {
        JSObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        return createContextObject(frame, description, initializers, state, null, setter, SETTER_KIND);
    }

    @Specialization(guards = "record.isSetter()", replaces = "doSetterCached")
    public JSObject doSetter(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY, context)") @Shared("setMagic") PropertySetNode setMagic) {
        JSFunctionData valueSetterFunctionData = record.isPrivate() ? getValueSetterFrameUncached() : getValueSetterPropertyUncached();
        JSObject setter = initializeMagicField(frame, record, valueSetterFunctionData, setMagic);
        Object name = record.isPrivate() ? record.getKey() : getName(record.getKey());
        return createContextObject(frame, name, initializers, state, null, setter, SETTER_KIND);
    }

    //
    // ##### Common
    //

    private JSObject initializeMagicField(VirtualFrame frame, ClassElementDefinitionRecord record, JSFunctionData functionData, PropertySetNode setMagic) {
        JSObject function;
        if (record.isPrivate()) {
            PrivateFrameBasedElementDefinitionRecord methodRecord = (PrivateFrameBasedElementDefinitionRecord) record;
            function = JSFunction.create(getRealm(), functionData, getScopeFrame(frame));
            int[] slots = {methodRecord.getKeySlot(), methodRecord.getBrandSlot()};
            setMagic.setValue(function, slots);
        } else {
            function = JSFunction.create(getRealm(), functionData);
            setMagic.setValue(function, record);
        }
        return function;
    }

    private MaterializedFrame getScopeFrame(VirtualFrame frame) {
        if (this.blockScopeSlot == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            blockScopeSlot = JSFrameUtil.findRequiredFrameSlotIndex(frame.getFrameDescriptor(), ScopeFrameNode.BLOCK_SCOPE_IDENTIFIER);
        }
        return JSFrameUtil.castMaterializedFrame(frame.getObject(blockScopeSlot));
    }

    protected JSFunctionData getMethodGetterFrameUncached() {
        if (methodGetterFrameUncached == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            methodGetterFrameUncached = createGetterFromFrameUncached(context, ClassElementDefinitionRecord.Kind.Method, isStatic);
        }
        return methodGetterFrameUncached;
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

    private JSFunctionData getValueGetterFrameUncached() {
        if (valueGetterFrameUncached == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            valueGetterFrameUncached = createGetterFromFrameUncached(context, Getter, isStatic);
        }
        return valueGetterFrameUncached;
    }

    private JSFunctionData getValueSetterPropertyUncached() {
        if (valueSetterPropertyUncached == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            valueSetterPropertyUncached = createSetterFromPropertyUncached(context);
        }
        return valueSetterPropertyUncached;
    }

    private JSFunctionData getValueSetterFrameUncached() {
        if (valueSetterFrameUncached == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            valueSetterFrameUncached = createSetterFromFrameUncached(context, isStatic);
        }
        return valueSetterFrameUncached;
    }

    protected static boolean nameEquals(TruffleString.EqualNode strEq, ClassElementDefinitionRecord record, Object expected) {
        if (record.getKey() instanceof TruffleString && expected instanceof TruffleString) {
            return Strings.equals(strEq, (TruffleString) expected, (TruffleString) record.getKey());
        }
        return false;
    }

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
        Object description = getName(record.getKey());
        JSFunctionData valueGetterFunctionData = record.isPrivate() && !record.isField() ? getValueGetterFrameUncached() : getValueGetterPropertyUncached();
        JSFunctionData valueSetterFunctionData = record.isPrivate() && !record.isField() ? getValueSetterFrameUncached() : getValueSetterPropertyUncached();
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

    @TruffleBoundary
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
                List<Object> initializers = (List<Object>) getInitializersKey.getValue(self);
                if (state.finished) {
                    CompilerDirectives.transferToInterpreter();
                    throw JSException.create(JSErrorType.TypeError, "Bad decorator initializer state");
                }
                Object[] args = frame.getArguments();
                Object value = JSArguments.getUserArgumentCount(args) > 0 ? JSArguments.getUserArgument(args, 0) : Undefined.instance;
                Boundaries.listAdd(initializers, value);
                return Undefined.instance;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 1, ADD_INITIALIZER);
    }

    @TruffleBoundary
    protected JSFunctionData createMethodGetterFromFrameCached(PrivateFrameBasedElementDefinitionRecord record) {
        CompilerAsserts.neverPartOfCompilation();
        assert record.getKind() == ClassElementDefinitionRecord.Kind.Method;
        final int keySlot = record.getKeySlot();
        final int constructorSlot = record.getBrandSlot();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private JSReadFrameSlotNode readMethod;
            @Child private PrivateBrandCheckNode brandCheckNode;

            @Override
            public Object execute(VirtualFrame frame) {
                JSFunctionObject functionObject = JSFrameUtil.getFunctionObject(frame);
                Object thiz = JSFrameUtil.getThisObj(frame);
                VirtualFrame blockScopeFrame = JSFunction.getEnclosingFrame(functionObject);
                if (brandCheckNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    JSReadFrameSlotNode readConstructor = JSReadFrameSlotNode.create(blockScopeFrame.getFrameDescriptor(), constructorSlot);
                    JavaScriptNode brandNode;
                    if (isStatic) {
                        brandNode = readConstructor;
                    } else {
                        brandNode = PropertyNode.createGetHidden(context, readConstructor, JSFunction.PRIVATE_BRAND_ID);
                    }
                    this.readMethod = insert(JSReadFrameSlotNode.create(blockScopeFrame.getFrameDescriptor(), keySlot));
                    this.brandCheckNode = insert(PrivateBrandCheckNode.create(readMethod, brandNode));
                }
                if (brandCheckNode.executeWithTarget(blockScopeFrame, thiz) != Undefined.instance) {
                    return readMethod.execute(blockScopeFrame);
                } else {
                    throw Errors.createTypeErrorIllegalAccessorTarget(this);
                }
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 0, Strings.GET);
    }

    @TruffleBoundary
    protected JSFunctionData createValueGetterCached(Object keyName, boolean isPrivateFieldGet) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private PropertyGetNode propertyGetNode = PropertyGetNode.create(keyName, context);
            @Child private HasPropertyCacheNode propertyHasNode = HasPropertyCacheNode.create(keyName, context);

            @Override
            public Object execute(VirtualFrame frame) {
                Object thiz = JSFrameUtil.getThisObj(frame);
                if (isPrivateFieldGet && !propertyHasNode.hasProperty(thiz)) {
                    throw Errors.createTypeErrorIllegalAccessorTarget(this);
                }
                return propertyGetNode.getValue(thiz);
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 0, Strings.GET);
    }

    @TruffleBoundary
    protected JSFunctionData createValueSetterCached(Object name, boolean isPrivateFieldGet) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private HasPropertyCacheNode propertyHasNode = HasPropertyCacheNode.create(name, context);
            @Child private PropertySetNode propertySetNode = PropertySetNode.create(name, false, context, true);

            @Override
            public Object execute(VirtualFrame frame) {
                Object thiz = JSFrameUtil.getThisObj(frame);
                if (isPrivateFieldGet && !propertyHasNode.hasProperty(thiz)) {
                    throw Errors.createTypeErrorIllegalAccessorTarget(this);
                }
                Object[] args = frame.getArguments();
                Object newValue = JSArguments.getUserArgumentCount(args) > 0 ? JSArguments.getUserArgument(args, 0) : Undefined.instance;
                propertySetNode.setValue(thiz, newValue);
                return Undefined.instance;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
    }

    //
    // ##### Generic Functions that are not cached in nodes and don't specialize on keys/frame.
    //

    @TruffleBoundary
    private static JSFunctionData createGetterFromFrameUncached(JSContext context, ClassElementDefinitionRecord.Kind kind, boolean isStatic) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private PropertyGetNode getMagic = PropertyGetNode.createGetHidden(MAGIC_KEY, context);

            @CompilationFinal private int keySlot = -1;
            @CompilationFinal private int classSlot = -1;

            @Override
            public Object execute(VirtualFrame frame) {
                JSFunctionObject functionObject = JSFrameUtil.getFunctionObject(frame);
                Object thiz = JSFrameUtil.getThisObj(frame);
                Frame blockScopeFrame = JSFunction.getEnclosingFrame(functionObject);
                int[] slots = (int[]) getMagic.getValue(functionObject);
                if (keySlot == -1 || classSlot == -1) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    keySlot = slots[0];
                    classSlot = slots[1];
                }
                assert slots[0] == keySlot && slots[1] == classSlot : "slot must not change";
                if (blockScopeFrame.isObject(classSlot) && blockScopeFrame.isObject(keySlot)) {
                    Object method = blockScopeFrame.getObject(keySlot);
                    Object constructor = blockScopeFrame.getObject(classSlot);
                    if (privateBrandCheck(thiz, constructor, isStatic)) {
                        if (kind == Getter) {
                            Accessor accessor = (Accessor) method;
                            assert accessor.hasGetter();
                            return JSRuntime.call(accessor.getGetter(), thiz, JSArguments.EMPTY_ARGUMENTS_ARRAY);
                        }
                        assert kind == ClassElementDefinitionRecord.Kind.Method;
                        return method;
                    }
                }
                throw Errors.createTypeErrorIllegalAccessorTarget(this);
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 0, Strings.GET);
    }

    private static boolean privateBrandCheck(Object target, Object constructor, boolean isStatic) {
        if (isStatic) {
            return target == constructor;
        } else {
            if (target instanceof JSObject && constructor instanceof JSObject) {
                Object brandKey = Properties.getOrDefaultUncached((JSObject) constructor, JSFunction.PRIVATE_BRAND_ID, null);
                if (brandKey instanceof HiddenKey) {
                    return Properties.containsKeyUncached((JSObject) target, brandKey);
                }
            }
            return false;
        }
    }

    @TruffleBoundary
    private static JSFunctionData createSetterFromFrameUncached(JSContext context, boolean isStatic) {
        CompilerAsserts.neverPartOfCompilation();
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private PropertyGetNode getMagic = PropertyGetNode.createGetHidden(MAGIC_KEY, context);

            @CompilationFinal private int keySlot = -1;
            @CompilationFinal private int classSlot = -1;

            @Override
            public Object execute(VirtualFrame frame) {
                JSFunctionObject functionObject = JSFrameUtil.getFunctionObject(frame);
                Object thiz = JSFrameUtil.getThisObj(frame);
                Frame blockScopeFrame = JSFunction.getEnclosingFrame(functionObject);
                int[] slots = (int[]) getMagic.getValue(functionObject);
                if (keySlot == -1 || classSlot == -1) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    keySlot = slots[0];
                    classSlot = slots[1];
                }
                assert slots[0] == keySlot && slots[1] == classSlot : "slot must not change";
                if (blockScopeFrame.isObject(classSlot) && blockScopeFrame.isObject(keySlot)) {
                    Object constructor = blockScopeFrame.getObject(classSlot);
                    if (privateBrandCheck(thiz, constructor, isStatic)) {
                        Object newValue;
                        if (JSArguments.getUserArgumentCount(frame.getArguments()) > 0) {
                            newValue = JSArguments.getUserArgument(frame.getArguments(), 0);
                        } else {
                            newValue = Undefined.instance;
                        }
                        Accessor accessor = (Accessor) blockScopeFrame.getObject(keySlot);
                        assert accessor.hasSetter();
                        JSRuntime.call(accessor.getSetter(), thiz, new Object[]{newValue});
                        return Undefined.instance;
                    }
                }
                throw Errors.createTypeErrorIllegalAccessorTarget(this);
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
    }

    private static Object checkPrivateAccess(VirtualFrame frame, Object thiz, PropertyGetNode getMagic, DynamicObjectLibrary access, Node self) {
        Object function = JSFrameUtil.getFunctionObject(frame);
        ClassElementDefinitionRecord record = (ClassElementDefinitionRecord) getMagic.getValue(function);
        Object key = record.getKey();
        if (record.isPrivate() && !(thiz instanceof JSObject && Properties.containsKey(access, (JSObject) thiz, key))) {
            throw Errors.createTypeErrorIllegalAccessorTarget(self);
        }
        return key;
    }

    @TruffleBoundary
    protected static JSFunctionData createGetterFromPropertyUncached(JSContext context) {
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private DynamicObjectLibrary access = DynamicObjectLibrary.getUncached();
            @Child private ReadElementNode propertyGetNode = ReadElementNode.create(context);
            @Child private PropertyGetNode getMagic = PropertyGetNode.createGetHidden(MAGIC_KEY, context);

            @Override
            public Object execute(VirtualFrame frame) {
                Object thiz = JSFrameUtil.getThisObj(frame);
                Object key = checkPrivateAccess(frame, thiz, getMagic, access, this);
                if (key instanceof HiddenKey) {
                    return Properties.getOrDefault(access, (JSObject) thiz, key, Undefined.instance);
                } else {
                    return propertyGetNode.executeWithTargetAndIndex(thiz, key);
                }
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 0, Strings.GET);
    }

    @TruffleBoundary
    protected static JSFunctionData createSetterFromPropertyUncached(JSContext context) {
        CallTarget callTarget = new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child private DynamicObjectLibrary access = DynamicObjectLibrary.getUncached();
            @Child private PropertyGetNode getMagic = PropertyGetNode.createGetHidden(MAGIC_KEY, context);
            @Child private WriteElementNode propertySetNode = WriteElementNode.create(context, false);

            @Override
            public Object execute(VirtualFrame frame) {
                Object thiz = JSFrameUtil.getThisObj(frame);
                Object key = checkPrivateAccess(frame, thiz, getMagic, access, this);
                Object[] args = frame.getArguments();
                Object newValue = JSArguments.getUserArgumentCount(args) > 0 ? JSArguments.getUserArgument(args, 0) : Undefined.instance;
                if (key instanceof HiddenKey) {
                    Properties.putIfPresent(access, (JSObject) thiz, key, newValue);
                } else {
                    propertySetNode.executeWithTargetAndIndexAndValue(thiz, key, newValue);
                }
                return Undefined.instance;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
    }
}

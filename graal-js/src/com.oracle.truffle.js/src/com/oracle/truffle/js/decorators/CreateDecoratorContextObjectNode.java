/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.HasPropertyCacheNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PrivateBrandCheckNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
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
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.List;

import static com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord.Kind.Field;
import static com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord.Kind.Getter;

@ImportStatic({Strings.class, ClassElementDefinitionRecord.class})
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

    private final boolean isStatic;
    private final JSFunctionData initializerData;
    protected final JSContext context;

    public static CreateDecoratorContextObjectNode create(JSContext context, boolean isStatic) {
        return CreateDecoratorContextObjectNodeGen.create(context, isStatic);
    }

    public abstract JSDynamicObject executeContext(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state);

    CreateDecoratorContextObjectNode(JSContext context, boolean isStatic) {
        this.initializerData = createInitializerFunctionData(context);
        this.createObjectNode = CreateObjectNode.create(context);
        this.setInitializersKey = PropertySetNode.createSetHidden(INIT_KEY, context);
        this.setRecordKey = PropertySetNode.createSetHidden(MAGIC_KEY, context);
        this.eqNode = TruffleString.RegionEqualByteIndexNode.create();
        this.isStatic = isStatic;
        this.context = context;
    }

    public final JSDynamicObject evaluateClass(VirtualFrame frame, Object className, Object initializers, Record state) {
        JSDynamicObject contextObj = createObjectNode.execute(frame);
        JSRuntime.createDataPropertyOrThrow(contextObj, KIND, CLASS_KIND);
        JSRuntime.createDataPropertyOrThrow(contextObj, NAME, className);
        addInitializerFunction(contextObj, state, initializers);
        return contextObj;
    }

    //
    // ##### Method
    //

    @Specialization(guards = {"record.isMethod()", "nameEquals(strEq,record,cachedName)", "privateName"})
    public JSDynamicObject doPrivateMethodCached(VirtualFrame frame, PrivateFrameBasedElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("record.getKey()") Object cachedName,
                    @SuppressWarnings("unused") @Cached("getName(cachedName)") Object description,
                    @SuppressWarnings("unused") @Cached("create()") TruffleString.EqualNode strEq,
                    @Cached("createMethodGetterFromFrameCached(record)") JSFunctionData valueGetterFunctionData,
                    @Cached("record.isPrivate()") boolean privateName) {
        JSDynamicObject getter = JSFunction.create(getRealm(), valueGetterFunctionData, getScopeFrame(frame, record));
        return createContextObject(frame, cachedName, initializers, state, getter, null, privateName, METHOD_KIND);
    }

    @Specialization(guards = {"record.isMethod()", "nameEquals(strEq,record,cachedName)", "!privateName"})
    public JSDynamicObject doPublicMethodCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @SuppressWarnings("unused") @Cached("record.getKey()") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @SuppressWarnings("unused") @Cached("create()") TruffleString.EqualNode strEq,
                    @Cached("record.isPrivate()") boolean privateName,
                    @Cached("createValueGetterCached(cachedName,false)") JSFunctionData valueGetterFunctionData) {
        JSDynamicObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, null, privateName, METHOD_KIND);
    }

    @Specialization(guards = "record.isMethod()", replaces = {"doPublicMethodCached", "doPrivateMethodCached"})
    public JSDynamicObject doMethodGeneric(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY,context)") PropertySetNode setMagic) {
        Object description = record.isPrivate() ? record.getKey() : getName(record.getKey());
        JSFunctionData valueGetterFunctionData = record.isPrivate() ? getMethodGetterFrameUncached() : getMethodGetterPropertyUncached();
        JSDynamicObject getter = initializeMagicField(frame, record, valueGetterFunctionData, setMagic);
        return createContextObject(frame, description, initializers, state, getter, null, record.isPrivate(), METHOD_KIND);
    }

    //
    // ##### Field
    //

    @Specialization(guards = {"record.isField()", "nameEquals(strEq,record,cachedName)"})
    public JSDynamicObject doFieldCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @SuppressWarnings("unused") @Cached("record.getKey()") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @SuppressWarnings("unused") @Cached("create()") TruffleString.EqualNode strEq,
                    @Cached("record.isPrivate()") boolean privateName,
                    @Cached("createValueGetterCached(cachedName,privateName)") JSFunctionData valueGetterFunctionData,
                    @Cached("createValueSetterCached(cachedName,privateName)") JSFunctionData valueSetterFunctionData) {
        JSDynamicObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        JSDynamicObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, setter, privateName, FIELD_KIND);
    }

    @Specialization(guards = "record.isField()", replaces = "doFieldCached")
    public JSDynamicObject doFieldUncached(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY,context)") PropertySetNode setMagic) {
        return createContextGetterSetter(frame, record, initializers, state, setMagic, FIELD_KIND);
    }

    //
    // ##### AutoAccessor
    //

    @Specialization(guards = {"record.isAutoAccessor()", "nameEquals(strEq,record,cachedName)"})
    public JSDynamicObject doAutoAccessorCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord.AutoAccessor record, Object initializers, Record state,
                    @SuppressWarnings("unused") @Cached("record.getKey()") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @SuppressWarnings("unused") @Cached("create()") TruffleString.EqualNode strEq,
                    @Cached("record.isPrivate()") boolean privateName,
                    @Cached("createValueGetterCached(cachedName,privateName)") JSFunctionData valueGetterFunctionData,
                    @Cached("createValueSetterCached(cachedName,privateName)") JSFunctionData valueSetterFunctionData) {
        JSDynamicObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        JSDynamicObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, setter, privateName, AUTO_ACCESSOR_KIND);
    }

    @Specialization(guards = "record.isAutoAccessor()", replaces = "doAutoAccessorCached")
    public JSDynamicObject doAutoAccessor(VirtualFrame frame, ClassElementDefinitionRecord.AutoAccessor record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY,context)") PropertySetNode setMagic) {
        return createContextGetterSetter(frame, record, initializers, state, setMagic, AUTO_ACCESSOR_KIND);
    }

    //
    // ##### Getter
    //

    @Specialization(guards = {"record.isGetter()", "nameEquals(strEq,record,cachedName)", "!privateName"})
    public JSDynamicObject doGetterCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @SuppressWarnings("unused") @Cached("record.getKey()") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @SuppressWarnings("unused") @Cached("create()") TruffleString.EqualNode strEq,
                    @Cached("record.isPrivate()") boolean privateName,
                    @Cached("createValueGetterCached(cachedName,privateName)") JSFunctionData valueGetterFunctionData) {
        JSDynamicObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        return createContextObject(frame, description, initializers, state, getter, null, privateName, GETTER_KIND);
    }

    @Specialization(guards = "record.isGetter()", replaces = "doGetterCached")
    public JSDynamicObject doGetter(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY,context)") PropertySetNode setMagic) {
        JSFunctionData valueGetterFunctionData = record.isPrivate() ? getValueGetterFrameUncached() : getValueGetterPropertyUncached();
        JSDynamicObject getter = initializeMagicField(frame, record, valueGetterFunctionData, setMagic);
        Object name = record.isPrivate() ? record.getKey() : getName(record.getKey());
        return createContextObject(frame, name, initializers, state, getter, null, record.isPrivate(), GETTER_KIND);
    }

    //
    // ##### Setter
    //

    @Specialization(guards = {"record.isSetter()", "nameEquals(strEq,record,cachedName)", "!privateName"})
    public JSDynamicObject doSetterCached(VirtualFrame frame, @SuppressWarnings("unused") ClassElementDefinitionRecord record, Object initializers, Record state,
                    @SuppressWarnings("unused") @Cached("record.getKey()") Object cachedName,
                    @Cached("getName(cachedName)") Object description,
                    @SuppressWarnings("unused") @Cached("create()") TruffleString.EqualNode strEq,
                    @Cached("record.isPrivate()") boolean privateName,
                    @Cached("createValueSetterCached(cachedName,privateName)") JSFunctionData valueSetterFunctionData) {
        JSDynamicObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        return createContextObject(frame, description, initializers, state, null, setter, privateName, SETTER_KIND);
    }

    @Specialization(guards = "record.isSetter()", replaces = "doSetterCached")
    public JSDynamicObject doSetter(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    @Cached("createSetHidden(MAGIC_KEY,context)") PropertySetNode setMagic) {
        JSFunctionData valueSetterFunctionData = record.isPrivate() ? getValueSetterFrameUncached() : getValueSetterPropertyUncached();
        JSDynamicObject setter = initializeMagicField(frame, record, valueSetterFunctionData, setMagic);
        Object name = record.isPrivate() ? record.getKey() : getName(record.getKey());
        return createContextObject(frame, name, initializers, state, null, setter, record.isPrivate(), SETTER_KIND);
    }

    //
    // ##### Common
    //

    private JSDynamicObject initializeMagicField(VirtualFrame frame, ClassElementDefinitionRecord record, JSFunctionData functionData, PropertySetNode setMagic) {
        JSDynamicObject function;
        if (record.isPrivate()) {
            PrivateFrameBasedElementDefinitionRecord methodRecord = (PrivateFrameBasedElementDefinitionRecord) record;
            function = JSFunction.create(getRealm(), functionData, getScopeFrame(frame, methodRecord));
            int[] slots = {methodRecord.getKeySlot(), methodRecord.getBrandSlot()};
            setMagic.setValue(function, slots);
        } else {
            function = JSFunction.create(getRealm(), functionData);
            setMagic.setValue(function, record);
        }
        return function;
    }

    private MaterializedFrame getScopeFrame(VirtualFrame frame, PrivateFrameBasedElementDefinitionRecord record) {
        if (this.blockScopeSlot == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            blockScopeSlot = record.getBlockScopeSlot();
        }
        assert blockScopeSlot == record.getBlockScopeSlot() : "slot must not change";
        return JSFrameUtil.castMaterializedFrame(frame.getObject(blockScopeSlot));
    }

    protected JSFunctionData getMethodGetterFrameUncached() {
        if (methodGetterFrameUncached == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            methodGetterFrameUncached = createGetterFromFrameUncached(context, ClassElementDefinitionRecord.Kind.Method);
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
            valueGetterFrameUncached = createGetterFromFrameUncached(context, Getter);
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
            valueSetterFrameUncached = createSetterFromFrameUncached(context, ClassElementDefinitionRecord.Kind.Setter);
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

    private void addInitializerFunction(JSDynamicObject contextObj, Record state, Object initializers) {
        JSFunctionObject initializerFunction = JSFunction.create(JSRealm.get(this), initializerData);
        setInitializersKey.setValue(initializerFunction, initializers);
        setRecordKey.setValue(initializerFunction, state);
        JSRuntime.createDataPropertyOrThrow(contextObj, ADD_INITIALIZER, initializerFunction);
    }

    public JSDynamicObject createContextGetterSetter(VirtualFrame frame, ClassElementDefinitionRecord record, Object initializers, Record state,
                    PropertySetNode setMagic, TruffleString kind) {
        Object description = getName(record.getKey());
        boolean privateName = record.isPrivate();
        JSFunctionData valueGetterFunctionData = record.isPrivate() && record.getKind() != Field ? getValueGetterFrameUncached() : getValueGetterPropertyUncached();
        JSFunctionData valueSetterFunctionData = record.isPrivate() && record.getKind() != Field ? getValueSetterFrameUncached() : getValueSetterPropertyUncached();
        JSDynamicObject getter = JSFunction.create(getRealm(), valueGetterFunctionData);
        JSDynamicObject setter = JSFunction.create(getRealm(), valueSetterFunctionData);
        setMagic.setValue(getter, record);
        setMagic.setValue(setter, record);
        return createContextObject(frame, description, initializers, state, getter, setter, privateName, kind);
    }

    public JSDynamicObject createContextObject(VirtualFrame frame, Object name, Object initializers,
                    Record state,
                    JSDynamicObject getter,
                    JSDynamicObject setter,
                    boolean privateName, TruffleString kindName) {
        JSDynamicObject contextObj = createObjectNode.execute(frame);
        JSRuntime.createDataPropertyOrThrow(contextObj, KIND, kindName);
        JSDynamicObject accessObject = createObjectNode.execute(frame);
        if (getter != null) {
            JSRuntime.createDataPropertyOrThrow(accessObject, Strings.GET, getter);
        }
        if (setter != null) {
            JSRuntime.createDataPropertyOrThrow(accessObject, Strings.SET, setter);
        }
        JSRuntime.createDataPropertyOrThrow(contextObj, ACCESS, accessObject);
        JSRuntime.createDataPropertyOrThrow(contextObj, Strings.STATIC, isStatic);
        JSRuntime.createDataPropertyOrThrow(contextObj, Strings.PRIVATE, privateName);
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
                    this.readMethod = insert(JSReadFrameSlotNode.create(blockScopeFrame.getFrameDescriptor(), keySlot));
                    this.brandCheckNode = insert(PrivateBrandCheckNode.create(readMethod, readConstructor));
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
                return newValue;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
    }

    //
    // ##### Generic Functions that are not cached in nodes and don't specialize on keys/frame.
    //

    @TruffleBoundary
    private static JSFunctionData createGetterFromFrameUncached(JSContext context, ClassElementDefinitionRecord.Kind kind) {
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
                    if (thiz == constructor) {
                        if (kind == Getter) {
                            Accessor accessor = (Accessor) method;
                            Object getter = accessor.getGetter();
                            if (getter != Undefined.instance) {
                                return JSRuntime.call(getter, thiz, JSArguments.EMPTY_ARGUMENTS_ARRAY);
                            } else {
                                return Undefined.instance;
                            }
                        } else if (kind == ClassElementDefinitionRecord.Kind.Setter) {
                            Accessor accessor = (Accessor) method;
                            return accessor.getSetter();
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

    @TruffleBoundary
    private static JSFunctionData createSetterFromFrameUncached(JSContext context, ClassElementDefinitionRecord.Kind kind) {
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
                    if (thiz == constructor) {
                        JSDynamicObject newValue = Undefined.instance;
                        if (JSArguments.getUserArgumentCount(frame.getArguments()) > 0) {
                            Object maybeNewValue = JSArguments.getUserArgument(frame.getArguments(), 0);
                            if (maybeNewValue instanceof JSDynamicObject) {
                                newValue = (JSDynamicObject) maybeNewValue;
                            } else {
                                throw Errors.createTypeErrorIllegalAccessorTarget(this);
                            }
                        }
                        if (kind == ClassElementDefinitionRecord.Kind.Setter) {
                            Accessor accessor = (Accessor) blockScopeFrame.getObject(keySlot);
                            Accessor updated = new Accessor(accessor.getGetter(), newValue);
                            blockScopeFrame.setObject(keySlot, updated);
                        } else if (kind == Getter) {
                            Accessor accessor = (Accessor) blockScopeFrame.getObject(keySlot);
                            Accessor updated = new Accessor(newValue, accessor.getSetter());
                            blockScopeFrame.setObject(keySlot, updated);
                        } else {
                            assert kind == ClassElementDefinitionRecord.Kind.Method;
                            blockScopeFrame.setObject(keySlot, newValue);
                        }
                    }
                }
                throw Errors.createTypeErrorIllegalAccessorTarget(this);
            }
        }.getCallTarget();
        if (kind == Getter) {
            return JSFunctionData.createCallOnly(context, callTarget, 0, Strings.GET);
        } else {
            return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
        }
    }

    private static Object checkPrivateAccess(VirtualFrame frame, Object thiz, PropertyGetNode getMagic, DynamicObjectLibrary access, Node self) {
        Object function = JSFrameUtil.getFunctionObject(frame);
        ClassElementDefinitionRecord record = (ClassElementDefinitionRecord) getMagic.getValue(function);
        Object key = record.getKey();
        if (record.isPrivate() && !(thiz instanceof JSDynamicObject && Properties.containsKey(access, (JSDynamicObject) thiz, key))) {
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
                return propertyGetNode.executeWithTargetAndIndex(thiz, key);
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
                propertySetNode.executeWithTargetAndIndexAndValue(thiz, key, newValue);
                return newValue;
            }
        }.getCallTarget();
        return JSFunctionData.createCallOnly(context, callTarget, 1, Strings.SET);
    }
}

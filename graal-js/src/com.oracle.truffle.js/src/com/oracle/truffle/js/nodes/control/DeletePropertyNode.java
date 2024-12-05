/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.control;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;

import java.util.Set;

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsArrayNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.array.JSArrayDeleteIndexNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryOperationTag;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSGlobal;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * 11.4.1 The delete Operator ({@code delete object[property]}).
 */
@ImportStatic({JSConfig.class, JSGlobal.class})
@NodeInfo(shortName = "delete")
public abstract class DeletePropertyNode extends JSTargetableNode {
    protected final boolean strict;
    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode propertyNode;

    protected DeletePropertyNode(boolean strict, JavaScriptNode targetNode, JavaScriptNode propertyNode) {
        this.strict = strict;
        this.targetNode = targetNode;
        this.propertyNode = propertyNode;
    }

    @NeverDefault
    public static DeletePropertyNode create(boolean strict) {
        return create(null, null, strict);
    }

    public static DeletePropertyNode create(JavaScriptNode object, JavaScriptNode property, boolean strict) {
        return DeletePropertyNodeGen.create(strict, object, property);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == UnaryOperationTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("operator", getClass().getAnnotation(NodeInfo.class).shortName());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializationNeeded() && materializedTags.contains(UnaryOperationTag.class)) {
            JavaScriptNode key = cloneUninitialized(propertyNode, materializedTags);
            JavaScriptNode target = cloneUninitialized(targetNode, materializedTags);
            transferSourceSectionAddExpressionTag(this, key);
            transferSourceSectionAddExpressionTag(this, target);
            DeletePropertyNode node = DeletePropertyNode.create(target, key, strict);
            transferSourceSectionAndTags(this, node);
            return node;
        } else {
            return this;
        }
    }

    private boolean materializationNeeded() {
        // Both nodes must have a source section in order to be instrumentable.
        return !(propertyNode.hasSourceSection() && targetNode.hasSourceSection());
    }

    @Override
    public final JavaScriptNode getTarget() {
        return targetNode;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        return executeWithTarget(frame, evaluateTarget(frame));
    }

    @Override
    public final Object evaluateTarget(VirtualFrame frame) {
        return getTarget().execute(frame);
    }

    public abstract boolean executeEvaluated(Object objectResult, Object propertyResult);

    @Specialization(guards = {"isJSOrdinaryObject(targetObject)"})
    protected final boolean doJSOrdinaryObject(JSDynamicObject targetObject, Object key,
                    @Shared @Cached JSToPropertyKeyNode toPropertyKeyNode,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") DynamicObjectLibrary dynamicObjectLib) {
        Object propertyKey = toPropertyKeyNode.execute(key);

        Property foundProperty = dynamicObjectLib.getProperty(targetObject, propertyKey);
        if (foundProperty != null) {
            if (!JSProperty.isConfigurable(foundProperty)) {
                if (strict) {
                    throw Errors.createTypeErrorNotConfigurableProperty(propertyKey);
                } else {
                    return false;
                }
            }
            dynamicObjectLib.removeKey(targetObject, propertyKey);
            return true;
        } else {
            /* the prototype might have a property with that name, but we don't care */
            return true;
        }
    }

    @Specialization
    protected final boolean doJSGlobalObject(JSGlobalObject targetObject, Object key,
                    @Shared @Cached JSToPropertyKeyNode toPropertyKeyNode,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") DynamicObjectLibrary dynamicObjectLib) {
        return doJSOrdinaryObject(targetObject, key, toPropertyKeyNode, dynamicObjectLib);
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"!isJSOrdinaryObject(targetObject)", "!isJSGlobalObject(targetObject)"})
    protected final boolean doJSObject(JSDynamicObject targetObject, Object key,
                    @Bind Node node,
                    @Cached("createIsFastArray()") IsArrayNode isArrayNode,
                    @Cached InlinedConditionProfile arrayProfile,
                    @Shared @Cached ToArrayIndexNode toArrayIndexNode,
                    @Cached InlinedConditionProfile arrayIndexProfile,
                    @Cached("create(strict)") JSArrayDeleteIndexNode deleteArrayIndexNode,
                    @Cached JSClassProfile jsclassProfile,
                    @Shared @Cached JSToPropertyKeyNode toPropertyKeyNode) {
        final Object propertyKey;
        if (arrayProfile.profile(node, isArrayNode.execute(targetObject))) {
            Object objIndex = toArrayIndexNode.execute(key);

            if (arrayIndexProfile.profile(node, objIndex instanceof Long)) {
                long longIndex = (long) objIndex;
                return deleteArrayIndexNode.execute(targetObject, arrayGetArrayType(targetObject), longIndex);
            } else {
                propertyKey = objIndex;
            }
        } else {
            propertyKey = toPropertyKeyNode.execute(key);
        }
        return JSObject.delete(targetObject, propertyKey, strict, jsclassProfile);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static boolean doSymbol(Symbol target, Object property,
                    @Shared @Cached JSToPropertyKeyNode toPropertyKeyNode) {
        toPropertyKeyNode.execute(property);
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static boolean doSafeInteger(SafeInteger target, Object property,
                    @Shared @Cached JSToPropertyKeyNode toPropertyKeyNode) {
        toPropertyKeyNode.execute(property);
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static boolean doBigInt(BigInt target, Object property,
                    @Shared @Cached JSToPropertyKeyNode toPropertyKeyNode) {
        toPropertyKeyNode.execute(property);
        return true;
    }

    @Specialization
    protected boolean doString(TruffleString target, Object property,
                    @Shared @Cached ToArrayIndexNode toArrayIndexNode,
                    @Cached TruffleString.EqualNode equalsNode) {
        Object objIndex = toArrayIndexNode.execute(property);
        boolean result;
        if (objIndex instanceof Long) {
            long index = (Long) objIndex;
            result = (index < 0) || (Strings.length(target) <= index);
        } else if (objIndex instanceof TruffleString propertyName) {
            result = !Strings.equals(equalsNode, JSString.LENGTH, propertyName);
        } else {
            assert objIndex instanceof Symbol;
            result = true;
        }
        if (strict && !result) {
            throw Errors.createTypeError("cannot delete index");
        }
        return result;
    }

    @InliningCutoff
    @Specialization(guards = {"isForeignObject(target)", "!interop.hasArrayElements(target)"})
    protected boolean member(Object target, TruffleString name,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        if (getLanguageOptions().hasForeignHashProperties() && interop.hasHashEntries(target)) {
            try {
                return JSInteropUtil.deleteHashEntry(target, name, interop, strict);
            } catch (UnknownKeyException e) {
                // fall through: still need to try members
            }
        }
        if (interop.hasMembers(target)) {
            return JSInteropUtil.deleteMember(target, name, interop, strict);
        }
        return true;
    }

    @InliningCutoff
    @Specialization(guards = {"isForeignObject(target)", "interop.hasArrayElements(target)"})
    protected boolean arrayElementInt(Object target, int index,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
        return JSInteropUtil.deleteArrayElement(target, index, interop, strict);
    }

    @SuppressWarnings("unused")
    @InliningCutoff
    @Specialization(guards = {"isForeignObject(target)"}, replaces = {"member", "arrayElementInt"})
    protected boolean foreignObject(Object target, Object key,
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                    @Shared @Cached ToArrayIndexNode toArrayIndexNode,
                    @Shared @Cached JSToPropertyKeyNode toPropertyKeyNode) {
        Object propertyKey;
        if (interop.hasArrayElements(target)) {
            Object indexOrPropertyKey = toArrayIndexNode.execute(key);
            if (indexOrPropertyKey instanceof Long) {
                return JSInteropUtil.deleteArrayElement(target, (long) indexOrPropertyKey, interop, strict);
            } else {
                propertyKey = indexOrPropertyKey;
                assert JSRuntime.isPropertyKey(propertyKey);
            }
        } else {
            propertyKey = toPropertyKeyNode.execute(key);
        }
        if (getLanguageOptions().hasForeignHashProperties() && interop.hasHashEntries(target)) {
            try {
                return JSInteropUtil.deleteHashEntry(target, propertyKey, interop, strict);
            } catch (UnknownKeyException e) {
                // fall through: still need to try members
            }
        }
        if (interop.hasMembers(target)) {
            if (propertyKey instanceof TruffleString propertyName) {
                return JSInteropUtil.deleteMember(target, propertyName, interop, strict);
            } else {
                assert propertyKey instanceof Symbol;
                return true;
            }
        } else {
            return true;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isTruffleObject(target)", "!isString(target)"})
    public boolean doOther(Object target, Object property,
                    @Shared @Cached JSToPropertyKeyNode toPropertyKeyNode) {
        toPropertyKeyNode.execute(property);
        return true;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(cloneUninitialized(getTarget(), materializedTags), cloneUninitialized(propertyNode, materializedTags), strict);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }
}

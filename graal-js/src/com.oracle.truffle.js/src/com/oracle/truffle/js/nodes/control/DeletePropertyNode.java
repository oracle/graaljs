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
package com.oracle.truffle.js.nodes.control;

import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arrayGetArrayType;
import static com.oracle.truffle.js.runtime.builtins.JSAbstractArray.arraySetArrayType;

import java.util.Set;

import javax.script.Bindings;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsArrayNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * 11.4.1 The delete Operator ({@code delete object[property]}).
 */
@NodeInfo(shortName = "delete")
@ImportStatic(value = JSInteropUtil.class)
public abstract class DeletePropertyNode extends JSTargetableNode {
    private final boolean strict;
    protected final JSContext context;
    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode propertyNode;

    protected DeletePropertyNode(boolean strict, JSContext context, JavaScriptNode targetNode, JavaScriptNode propertyNode) {
        this.strict = strict;
        this.context = context;
        this.targetNode = targetNode;
        this.propertyNode = propertyNode;
    }

    public static DeletePropertyNode create(boolean strict, JSContext context) {
        return create(null, null, strict, context);
    }

    public static DeletePropertyNode createNonStrict(JSContext context) {
        return create(null, null, false, context);
    }

    public static DeletePropertyNode create(JavaScriptNode object, JavaScriptNode property, boolean strict, JSContext context) {
        return DeletePropertyNodeGen.create(strict, context, object, property);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == UnaryExpressionTag.class) {
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
        if (materializationNeeded() && materializedTags.contains(UnaryExpressionTag.class)) {
            JavaScriptNode key = cloneUninitialized(propertyNode);
            JavaScriptNode target = cloneUninitialized(targetNode);
            transferSourceSectionAddExpressionTag(this, key);
            transferSourceSectionAddExpressionTag(this, target);
            DeletePropertyNode node = DeletePropertyNode.create(target, key, strict, context);
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

    public abstract boolean executeEvaluated(TruffleObject objectResult, Object propertyResult);

    @Specialization(guards = "isJSType(targetObject)")
    protected final boolean doJSObject(DynamicObject targetObject, Object key,
                    @Cached("createIsFastArray()") IsArrayNode isArrayNode,
                    @Cached("createBinaryProfile()") ConditionProfile arrayProfile,
                    @Cached("create()") ToArrayIndexNode toArrayIndexNode,
                    @Cached("createBinaryProfile()") ConditionProfile arrayIndexProfile,
                    @Cached("createClassProfile()") ValueProfile arrayTypeProfile,
                    @Cached("create()") JSClassProfile jsclassProfile,
                    @Cached("create()") JSToPropertyKeyNode toPropertyKeyNode) {
        final boolean isArray = isArrayNode.execute(targetObject);
        final Object propertyKey;
        if (arrayProfile.profile(isArray)) {
            Object objIndex = toArrayIndexNode.execute(key);

            if (arrayIndexProfile.profile(objIndex instanceof Long)) {
                long longIndex = (long) objIndex;
                ScriptArray array = arrayTypeProfile.profile(arrayGetArrayType(targetObject, isArray));
                if (array.canDeleteElement(targetObject, longIndex, strict, isArray)) {
                    arraySetArrayType(targetObject, array = array.deleteElement(targetObject, longIndex, strict, isArray));
                    return true;
                } else {
                    return false;
                }
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
    protected static boolean doSymbol(Symbol target, Object property) {
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static boolean doLargeInteger(LargeInteger target, Object property) {
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static boolean doBigInt(BigInt target, Object property) {
        return true;
    }

    @Specialization
    protected static boolean doString(String target, Object property,
                    @Cached("create()") ToArrayIndexNode toArrayIndexNode) {
        Object objIndex = toArrayIndexNode.execute(property);
        if (objIndex instanceof Long) {
            long index = (Long) objIndex;
            return (index < 0) || (target.length() <= index);
        }
        return !JSString.LENGTH.equals(objIndex);
    }

    @TruffleBoundary
    @Specialization(guards = {"isBindings(target)"})
    protected static boolean doBindings(Object target, Object propertyResult) {
        // e.g. TruffleJSBindings, see JDK-8015830.js
        Bindings bindings = (Bindings) target;
        Object result = bindings.remove(propertyResult);
        return result != null;
    }

    @Specialization(guards = {"isForeignObject(target)"})
    protected static boolean doInterop(TruffleObject target, Object property,
                    @Cached("createRemove()") Node removeNode,
                    @Cached("create(context)") ExportValueNode exportNode) {
        try {
            ForeignAccess.sendRemove(removeNode, target, exportNode.executeWithTarget(property, Undefined.instance));
            return true;
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            return false;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isTruffleObject(target)", "!isString(target)", "!isBindings(target)"})
    public boolean doOther(Object target, Object property) {
        return true;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(getTarget()), cloneUninitialized(propertyNode), strict, context);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }
}

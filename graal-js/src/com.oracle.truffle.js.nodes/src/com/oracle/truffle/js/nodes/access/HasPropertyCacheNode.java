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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.interop.JavaAccess;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaImporter;
import com.oracle.truffle.js.runtime.interop.JavaMember;
import com.oracle.truffle.js.runtime.interop.JavaPackage;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * @see PropertyGetNode
 */
public class HasPropertyCacheNode extends PropertyCacheNode<HasPropertyCacheNode.HasCacheNode> {
    private final boolean hasOwnProperty;
    @CompilationFinal private boolean isMethod;
    private boolean propertyAssumptionCheckEnabled = true;

    public static HasPropertyCacheNode create(Object key, JSContext context, boolean hasOwnProperty) {
        return new HasPropertyCacheNode(key, context, hasOwnProperty);
    }

    public static HasPropertyCacheNode create(Object key, JSContext context) {
        return create(key, context, false);
    }

    protected HasPropertyCacheNode(Object key, JSContext context, boolean hasOwnProperty) {
        super(key, context);
        this.hasOwnProperty = hasOwnProperty;
    }

    @ExplodeLoop(kind = LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    public boolean hasProperty(Object thisObj) {
        for (HasCacheNode c = cacheNode; c != null; c = c.next) {
            if (c.isGeneric()) {
                return c.hasProperty(thisObj, this);
            }
            if (!c.isValid()) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                break;
            }
            boolean guard = c.accepts(thisObj);
            if (guard) {
                return c.hasProperty(thisObj, this);
            }
        }
        deoptimize();
        return specialize(thisObj).hasProperty(thisObj, this);
    }

    public abstract static class HasCacheNode extends PropertyCacheNode.CacheNode<HasCacheNode> {
        protected HasCacheNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        protected abstract boolean hasProperty(Object thisObj, HasPropertyCacheNode root);
    }

    public abstract static class LinkedHasPropertyCacheNode extends HasCacheNode {

        protected LinkedHasPropertyCacheNode(ReceiverCheckNode receiverCheckNode) {
            super(receiverCheckNode);
        }
    }

    public static final class PresentHasPropertyCacheNode extends LinkedHasPropertyCacheNode {
        public PresentHasPropertyCacheNode(ReceiverCheckNode shapeCheck) {
            super(shapeCheck);
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            return true;
        }
    }

    /**
     * For use when a property is undefined. Returns undefined.
     */
    public static final class AbsentHasPropertyCacheNode extends LinkedHasPropertyCacheNode {

        public AbsentHasPropertyCacheNode(ReceiverCheckNode shapeCheckNode) {
            super(shapeCheckNode);
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            return false;
        }
    }

    public static final class JSAdapterHasPropertyCacheNode extends LinkedHasPropertyCacheNode {
        private final boolean isMethod;

        public JSAdapterHasPropertyCacheNode(Object key, ReceiverCheckNode receiverCheckNode, boolean isMethod) {
            super(receiverCheckNode);
            assert JSRuntime.isPropertyKey(key);
            this.isMethod = isMethod;
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            if (isMethod) {
                throw new UnsupportedOperationException();
            } else {
                return JSObject.hasOwnProperty((DynamicObject) thisObj, root.getKey());
            }
        }
    }

    public static final class JSProxyDispatcherPropertyHasNode extends LinkedHasPropertyCacheNode {

        private final boolean hasOwnProperty;
        @Child private JSProxyHasPropertyNode proxyGet;

        public JSProxyDispatcherPropertyHasNode(JSContext context, Object key, ReceiverCheckNode receiverCheck, boolean hasOwnProperty) {
            super(receiverCheck);
            this.hasOwnProperty = hasOwnProperty;
            assert JSRuntime.isPropertyKey(key);
            this.proxyGet = hasOwnProperty ? null : JSProxyHasPropertyNodeGen.create(context);
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            Object key = root.getKey();
            if (hasOwnProperty) {
                return JSObject.getOwnProperty(receiverCheck.getStore(thisObj), key) != null;
            } else {
                return proxyGet.executeWithTargetAndKeyBoolean(receiverCheck.getStore(thisObj), key);
            }
        }
    }

    public static final class UnspecializedHasPropertyCacheNode extends LinkedHasPropertyCacheNode {

        public UnspecializedHasPropertyCacheNode(ReceiverCheckNode receiverCheckNode) {
            super(receiverCheckNode);
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            Object key = root.getKey();
            if (root.isOwnProperty()) {
                return JSObject.hasOwnProperty((DynamicObject) thisObj, key);
            } else {
                return JSObject.hasProperty((DynamicObject) thisObj, key);
            }
        }
    }

    @NodeInfo(cost = NodeCost.MEGAMORPHIC)
    public static final class GenericHasPropertyCacheNode extends HasCacheNode {
        private final JSClassProfile jsclassProfile = JSClassProfile.create();

        public GenericHasPropertyCacheNode() {
            super(null);
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            Object key = root.getKey();
            if (root.isOwnProperty()) {
                return JSObject.hasOwnProperty((DynamicObject) thisObj, key, jsclassProfile);
            } else {
                return JSObject.hasProperty((DynamicObject) thisObj, key, jsclassProfile);
            }
        }
    }

    public static final class ForeignHasPropertyCacheNode extends LinkedHasPropertyCacheNode {
        @Child private Node keyInfoNode;

        public ForeignHasPropertyCacheNode() {
            super(new ForeignLanguageCheckNode());
            this.keyInfoNode = Message.KEY_INFO.createNode();
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            return JSInteropNodeUtil.hasProperty((TruffleObject) thisObj, root.getKey(), keyInfoNode);
        }
    }

    public static class JavaClassHasPropertyCacheNode extends LinkedHasPropertyCacheNode {
        protected final boolean isMethod;
        protected final boolean allowReflection;

        public JavaClassHasPropertyCacheNode(ReceiverCheckNode receiverCheckNode, boolean isMethod, boolean allowReflection) {
            super(receiverCheckNode);
            this.isMethod = isMethod;
            this.allowReflection = allowReflection;
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            return hasMember((JavaClass) thisObj, (String) root.getKey());
        }

        protected final boolean hasMember(JavaClass type, String key) {
            JavaMember member = type.getMember(key, JavaClass.STATIC, getJavaMemberTypes(isMethod), allowReflection);
            if (member != null) {
                return true;
            }
            return type.getInnerClass(key) != null;
        }
    }

    /**
     * Make a cache for a JSObject with this property map and requested property.
     *
     * @param property The particular entry of the property being accessed.
     */
    @Override
    protected HasCacheNode createCachedPropertyNode(Property property, Object thisObj, int depth, Object value, HasCacheNode currentHead) {
        assert !isOwnProperty() || depth == 0;
        ReceiverCheckNode check;
        if (JSObject.isDynamicObject(thisObj)) {
            Shape cacheShape = ((DynamicObject) thisObj).getShape();
            check = createShapeCheckNode(cacheShape, (DynamicObject) thisObj, depth, false, false);
        } else {
            check = createPrimitiveReceiverCheck(thisObj, depth);
        }
        return new PresentHasPropertyCacheNode(check);
    }

    @Override
    protected HasCacheNode createUndefinedPropertyNode(Object thisObj, Object store, int depth, Object value) {
        HasCacheNode specialized = createJavaPropertyNodeMaybe(thisObj, depth);
        if (specialized != null) {
            return specialized;
        }
        if (JSObject.isDynamicObject(thisObj)) {
            DynamicObject thisJSObj = (DynamicObject) thisObj;
            Shape cacheShape = thisJSObj.getShape();
            AbstractShapeCheckNode shapeCheck = createShapeCheckNode(cacheShape, thisJSObj, depth, false, false);
            ReceiverCheckNode receiverCheck = (depth == 0) ? new JSClassCheckNode(JSObject.getJSClass(thisJSObj)) : shapeCheck;
            if (JSAdapter.isJSAdapter(store)) {
                return new JSAdapterHasPropertyCacheNode(key, receiverCheck, isMethod());
            } else if (JSProxy.isProxy(store)) {
                return new JSProxyDispatcherPropertyHasNode(context, key, receiverCheck, isOwnProperty());
            } else if (JSModuleNamespace.isJSModuleNamespace(store)) {
                return new UnspecializedHasPropertyCacheNode(receiverCheck);
            } else {
                return new AbsentHasPropertyCacheNode(shapeCheck);
            }
        } else {
            return new AbsentHasPropertyCacheNode(new InstanceofCheckNode(thisObj.getClass(), context));
        }
    }

    @Override
    protected HasCacheNode createJavaPropertyNodeMaybe(Object thisObj, int depth) {
        if (JSTruffleOptions.SubstrateVM) {
            return null;
        }
        if (JavaPackage.isJavaPackage(thisObj)) {
            return new PresentHasPropertyCacheNode(new JSClassCheckNode(JSObject.getJSClass((DynamicObject) thisObj)));
        } else if (JavaImporter.isJavaImporter(thisObj)) {
            return new UnspecializedHasPropertyCacheNode(new JSClassCheckNode(JSObject.getJSClass((DynamicObject) thisObj)));
        }
        if (!JSTruffleOptions.NashornJavaInterop) {
            return null;
        } else if (JSObject.isDynamicObject(thisObj)) {
            assert !JSJavaWrapper.isJSJavaWrapper(thisObj);
            return null;
        } else if (thisObj instanceof JavaClass) {
            return new JavaClassHasPropertyCacheNode(new InstanceofCheckNode(JavaClass.class, context), isMethod(), JavaAccess.isReflectionAllowed(context));
        } else {
            JavaMember member = getInstanceMember(thisObj);
            if (member != null) {
                return new PresentHasPropertyCacheNode(new InstanceofCheckNode(thisObj.getClass(), context));
            }
            return null;
        }
    }

    private JavaMember getInstanceMember(Object thisObj) {
        if (thisObj == null) {
            return null;
        }
        if (!(key instanceof String)) {
            // could be Symbol!
            return null;
        }
        JavaClass javaClass = JavaClass.forClass(thisObj.getClass());
        return javaClass.getMember((String) key, JavaClass.INSTANCE, getJavaMemberTypes(isMethod()), JavaAccess.isReflectionAllowed(context));
    }

    /**
     * Make a generic-case node, for when polymorphism becomes too high.
     */
    @Override
    protected HasCacheNode createGenericPropertyNode() {
        return new GenericHasPropertyCacheNode();
    }

    protected boolean isMethod() {
        return isMethod;
    }

    protected void setMethod() {
        CompilerAsserts.neverPartOfCompilation();
        isMethod = true;
    }

    @Override
    protected boolean isPropertyAssumptionCheckEnabled() {
        return propertyAssumptionCheckEnabled && context.isSingleRealm();
    }

    @Override
    protected void setPropertyAssumptionCheckEnabled(boolean value) {
        this.propertyAssumptionCheckEnabled = value;
    }

    @Override
    protected boolean isGlobal() {
        return false;
    }

    @Override
    protected boolean isOwnProperty() {
        return hasOwnProperty;
    }

    protected static Class<? extends JavaMember>[] getJavaMemberTypes(boolean isMethod) {
        return isMethod ? JavaClass.METHOD_GETTER : JavaClass.GETTER_METHOD;
    }

    @Override
    protected HasCacheNode createTruffleObjectPropertyNode(TruffleObject thisObject) {
        return new ForeignHasPropertyCacheNode();
    }
}

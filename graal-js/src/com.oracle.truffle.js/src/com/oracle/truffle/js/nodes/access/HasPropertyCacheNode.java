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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.module.ReadImportBindingNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespaceObject;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.java.JavaImporter;
import com.oracle.truffle.js.runtime.java.JavaPackage;
import com.oracle.truffle.js.runtime.objects.CyclicModuleRecord;
import com.oracle.truffle.js.runtime.objects.ExportResolution;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * @see PropertyGetNode
 */
public class HasPropertyCacheNode extends PropertyCacheNode<HasPropertyCacheNode.HasCacheNode> {
    private final boolean hasOwnProperty;
    private boolean propertyAssumptionCheckEnabled = true;
    @Child protected HasCacheNode cacheNode;

    @NeverDefault
    public static HasPropertyCacheNode create(Object key, JSContext context, boolean hasOwnProperty) {
        return new HasPropertyCacheNode(key, context, hasOwnProperty);
    }

    @NeverDefault
    public static HasPropertyCacheNode create(Object key, JSContext context) {
        return create(key, context, false);
    }

    protected HasPropertyCacheNode(Object key, JSContext context, boolean hasOwnProperty) {
        super(key, context);
        this.hasOwnProperty = hasOwnProperty;
    }

    @ExplodeLoop
    public boolean hasProperty(Object thisObj) {
        HasCacheNode c = cacheNode;
        for (; c != null; c = c.next) {
            if (c instanceof GenericHasPropertyCacheNode) {
                return ((GenericHasPropertyCacheNode) c).hasProperty(thisObj, this);
            }
            boolean isSimpleShapeCheck = c.isSimpleShapeCheck();
            ReceiverCheckNode receiverCheck = c.receiverCheck;
            boolean guard;
            Object castObj;
            if (isSimpleShapeCheck) {
                Shape shape = receiverCheck.getShape();
                if (isDynamicObject(thisObj, shape)) {
                    JSDynamicObject jsobj = castDynamicObject(thisObj, shape);
                    guard = shape.check(jsobj);
                    castObj = jsobj;
                    if (!shape.getValidAssumption().isValid()) {
                        break;
                    }
                } else {
                    continue;
                }
            } else {
                guard = receiverCheck.accept(thisObj);
                castObj = thisObj;
            }
            if (guard) {
                if (!isSimpleShapeCheck && !receiverCheck.isValid()) {
                    break;
                }
                return c.hasProperty(castObj, this);
            }
        }
        deoptimize(c);
        return hasPropertyAndSpecialize(thisObj);
    }

    @TruffleBoundary
    private boolean hasPropertyAndSpecialize(Object thisObj) {
        HasCacheNode c = specialize(thisObj);
        return c.hasProperty(thisObj, this);
    }

    @Override
    protected HasCacheNode getCacheNode() {
        return this.cacheNode;
    }

    @Override
    protected void setCacheNode(HasCacheNode cache) {
        this.cacheNode = cache;
    }

    public abstract static class HasCacheNode extends PropertyCacheNode.CacheNode<HasCacheNode> {
        @Child protected HasCacheNode next;

        protected HasCacheNode(ReceiverCheckNode receiverCheck) {
            super(receiverCheck);
        }

        @Override
        protected final HasCacheNode getNext() {
            return next;
        }

        @Override
        protected final void setNext(HasCacheNode next) {
            this.next = next;
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

        public JSAdapterHasPropertyCacheNode(Object key, ReceiverCheckNode receiverCheckNode) {
            super(receiverCheckNode);
            assert JSRuntime.isPropertyKey(key);
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            return JSObject.hasOwnProperty((JSDynamicObject) thisObj, root.getKey());
        }
    }

    public static final class JSProxyDispatcherPropertyHasNode extends LinkedHasPropertyCacheNode {

        private final boolean hasOwnProperty;
        @Child private JSProxyHasPropertyNode proxyGet;
        @Child private JSGetOwnPropertyNode getOwnPropertyNode;

        public JSProxyDispatcherPropertyHasNode(JSContext context, Object key, ReceiverCheckNode receiverCheck, boolean hasOwnProperty) {
            super(receiverCheck);
            this.hasOwnProperty = hasOwnProperty;
            assert JSRuntime.isPropertyKey(key);
            this.proxyGet = hasOwnProperty ? null : JSProxyHasPropertyNodeGen.create(context);
            this.getOwnPropertyNode = hasOwnProperty ? JSGetOwnPropertyNode.create() : null;
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            Object key = root.getKey();
            JSDynamicObject store = receiverCheck.getStore(thisObj);
            if (hasOwnProperty) {
                return getOwnPropertyNode.execute(store, key) != null;
            } else {
                return proxyGet.executeWithTargetAndKeyBoolean(store, key);
            }
        }
    }

    static final class ModuleNamespaceHasOwnPropertyNode extends LinkedHasPropertyCacheNode {

        private final Property property;
        @Child private ReadImportBindingNode readBindingNode;

        ModuleNamespaceHasOwnPropertyNode(ReceiverCheckNode receiverCheck, Property property) {
            super(receiverCheck);
            assert JSProperty.isModuleNamespaceExport(property);
            this.property = property;
            this.readBindingNode = ReadImportBindingNode.create();
        }

        @SuppressWarnings("deprecation")
        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            JSModuleNamespaceObject store = (JSModuleNamespaceObject) receiverCheck.getStore(thisObj);
            assert !store.isDeferred(); // guaranteed by shape check
            if (root.isOwnProperty()) {
                // [[GetOwnProperty]] semantics: Performs a [[Get]].
                ExportResolution.Resolved exportResolution = (ExportResolution.Resolved) property.getLocation().get(store, receiverCheck.getShape());
                // Throws ReferenceError in case of an uninitialized binding.
                readBindingNode.execute(exportResolution);
            }
            return true;
        }
    }

    static final class DeferredModuleNamespaceHasOwnPropertyNode extends LinkedHasPropertyCacheNode {

        private final Property property;
        @Child private ReadImportBindingNode readBindingNode;
        private final BranchProfile evaluateDeferredBranch = BranchProfile.create();

        DeferredModuleNamespaceHasOwnPropertyNode(ReceiverCheckNode receiverCheck, Property property) {
            super(receiverCheck);
            this.property = property;
            this.readBindingNode = ReadImportBindingNode.create();
        }

        @SuppressWarnings("deprecation")
        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            JSModuleNamespaceObject store = (JSModuleNamespaceObject) receiverCheck.getStore(thisObj);
            assert store.isDeferred(); // guaranteed by shape check
            // Trigger module evaluation if needed (either not evaluated or errored).
            // Not necessary for synthetic modules since their errors are thrown during load.
            if (store.getModule() instanceof CyclicModuleRecord cyclicModule) {
                if (cyclicModule.getStatus() != CyclicModuleRecord.Status.Evaluated || cyclicModule.getEvaluationError() != null) {
                    evaluateDeferredBranch.enter();
                    store.getModuleExportsList();
                }
            }
            if (property == null) {
                return false;
            }
            if (root.isOwnProperty()) {
                // [[GetOwnProperty]] semantics: Performs a [[Get]].
                ExportResolution.Resolved exportResolution = (ExportResolution.Resolved) property.getLocation().get(store, receiverCheck.getShape());
                // Throws ReferenceError in case of an uninitialized binding.
                readBindingNode.execute(exportResolution);
            }
            return true;
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
                return JSObject.hasOwnProperty((JSDynamicObject) thisObj, key);
            } else {
                return JSObject.hasProperty((JSDynamicObject) thisObj, key);
            }
        }
    }

    public static final class GenericHasPropertyCacheNode extends HasCacheNode {
        @Child private InteropLibrary interop;
        private final JSClassProfile jsclassProfile = JSClassProfile.create();

        public GenericHasPropertyCacheNode() {
            super(null);
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @InliningCutoff
        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            if (JSDynamicObject.isJSDynamicObject(thisObj)) {
                Object key = root.getKey();
                if (root.isOwnProperty()) {
                    return JSObject.hasOwnProperty((JSDynamicObject) thisObj, key, jsclassProfile);
                } else {
                    return JSObject.hasProperty((JSDynamicObject) thisObj, key, jsclassProfile);
                }
            } else {
                assert JSRuntime.isForeignObject(thisObj);
                Object key = root.getKey();
                if (key instanceof TruffleString propertyName) {
                    return interop.isMemberExisting(thisObj, Strings.toJavaString(propertyName));
                } else {
                    return false;
                }
            }
        }
    }

    public static final class ForeignHasPropertyCacheNode extends LinkedHasPropertyCacheNode {
        @Child private InteropLibrary interop;

        public ForeignHasPropertyCacheNode() {
            super(new ForeignLanguageCheckNode());
            this.interop = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        }

        @Override
        protected boolean hasProperty(Object thisObj, HasPropertyCacheNode root) {
            assert JSRuntime.isForeignObject(thisObj);
            Object key = root.getKey();
            if (key instanceof TruffleString propertyName) {
                return interop.isMemberExisting(thisObj, Strings.toJavaString(propertyName));
            } else {
                return false;
            }
        }
    }

    /**
     * Make a cache for a JSObject with this property map and requested property.
     *
     * @param property The particular entry of the property being accessed.
     */
    @Override
    protected HasCacheNode createCachedPropertyNode(Property property, Object thisObj, JSDynamicObject proto, int depth, Object value, HasCacheNode currentHead) {
        assert !isOwnProperty() || depth == 0;
        ReceiverCheckNode check;
        if (JSDynamicObject.isJSDynamicObject(thisObj)) {
            JSDynamicObject thisJSObj = (JSDynamicObject) thisObj;
            Shape cacheShape = thisJSObj.getShape();
            check = createShapeCheckNode(cacheShape, thisJSObj, proto, depth, false, false);
        } else {
            check = createPrimitiveReceiverCheck(thisObj, proto, depth);
        }
        if (JSProperty.isModuleNamespaceExport(property)) {
            if (((JSModuleNamespaceObject) proto).isDeferred()) {
                return new DeferredModuleNamespaceHasOwnPropertyNode(check, property);
            } else {
                return new ModuleNamespaceHasOwnPropertyNode(check, property);
            }
        }
        return new PresentHasPropertyCacheNode(check);
    }

    @Override
    protected HasCacheNode createUndefinedPropertyNode(Object thisObj, Object store, JSDynamicObject proto, int depth, Object value) {
        HasCacheNode specialized = createJavaPropertyNodeMaybe(thisObj, proto, depth);
        if (specialized != null) {
            return specialized;
        }
        if (JSDynamicObject.isJSDynamicObject(thisObj)) {
            JSDynamicObject thisJSObj = (JSDynamicObject) thisObj;
            Shape cacheShape = thisJSObj.getShape();
            if (JSAdapter.isJSAdapter(store)) {
                return new JSAdapterHasPropertyCacheNode(key, createJSClassCheck(thisObj, proto, depth));
            } else if (JSProxy.isJSProxy(store)) {
                return new JSProxyDispatcherPropertyHasNode(context, key, createJSClassCheck(thisObj, proto, depth), isOwnProperty());
            } else {
                var shapeCheckNode = createShapeCheckNode(cacheShape, thisJSObj, proto, depth, false, false);
                if (store instanceof JSModuleNamespaceObject ns && ns.isDeferred() && !ns.isSymbolLikeNamespaceKey(key)) {
                    // Property does not exist in the deferred module namespace object.
                    // We must still intercept the access to ensure the module is evaluated.
                    // Since the prototype of module namespace objects is always null,
                    // we can stop the property lookup here and return false.
                    return new DeferredModuleNamespaceHasOwnPropertyNode(shapeCheckNode, null);
                } else {
                    return new AbsentHasPropertyCacheNode(shapeCheckNode);
                }
            }
        } else {
            return new AbsentHasPropertyCacheNode(new InstanceofCheckNode(thisObj.getClass()));
        }
    }

    @Override
    protected HasCacheNode createJavaPropertyNodeMaybe(Object thisObj, JSDynamicObject proto, int depth) {
        if (JavaPackage.isJavaPackage(thisObj)) {
            return new PresentHasPropertyCacheNode(createJSClassCheck(thisObj, proto, depth));
        } else if (JavaImporter.isJavaImporter(thisObj)) {
            return new UnspecializedHasPropertyCacheNode(createJSClassCheck(thisObj, proto, depth));
        }
        return null;
    }

    /**
     * Make a generic-case node, for when polymorphism becomes too high.
     */
    @Override
    protected HasCacheNode createGenericPropertyNode() {
        return new GenericHasPropertyCacheNode();
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

    @Override
    protected HasCacheNode createTruffleObjectPropertyNode() {
        return new ForeignHasPropertyCacheNode();
    }

    @Override
    protected boolean canCombineShapeCheck(Shape parentShape, Shape cacheShape, Object thisObj, int depth, Object value, Property property) {
        assert shapesHaveCommonLayoutForKey(parentShape, cacheShape);
        if (JSDynamicObject.isJSDynamicObject(thisObj) && JSProperty.isData(property)) {
            assert depth == 0;
            return !isGlobal();
        }
        return false;
    }

    @Override
    protected HasCacheNode createCombinedIcPropertyNode(Shape parentShape, Shape cacheShape, Object thisObj, int depth, Object value, Property property) {
        return new PresentHasPropertyCacheNode(new PropertyGetNode.CombinedShapeCheckNode(parentShape, cacheShape));
    }
}

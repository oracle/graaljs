/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode.WrapperNode;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.binary.DualNode;
import com.oracle.truffle.js.nodes.control.AbstractBlockNode;
import com.oracle.truffle.js.nodes.control.DiscardResultNode;
import com.oracle.truffle.js.nodes.function.BlockScopeNode;
import com.oracle.truffle.js.nodes.function.BlockScopeNode.FrameBlockScopeNode;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.truffleinterop.InteropList;

public abstract class JSScope {

    protected final Node node;
    protected final MaterializedFrame mFrame;

    private static final String THIS_NAME = "this";

    public JSScope(Node node, MaterializedFrame frame) {
        this.node = node;
        this.mFrame = frame;
    }

    public static Iterable<Scope> createLocalScopes(Node node, MaterializedFrame frame) {
        final JSScope scope = createScope(node, frame);
        return new Iterable<Scope>() {
            @Override
            public Iterator<Scope> iterator() {
                return new Iterator<Scope>() {
                    private JSScope previousScope;
                    private JSScope nextScope = scope;

                    @Override
                    public boolean hasNext() {
                        if (nextScope == null) {
                            nextScope = previousScope.findParent();
                        }
                        return nextScope != null;
                    }

                    @Override
                    public Scope next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        Scope vscope = nextScope.toScope(frame);
                        previousScope = nextScope;
                        nextScope = null;
                        return vscope;
                    }
                };
            }
        };
    }

    public static Iterable<Scope> createGlobalScopes(JSRealm realm) {
        Scope globalLexicalScope = Scope.newBuilder("global", new DynamicScopeWrapper(realm.getGlobalScope())).build();
        Scope globalVarScope = Scope.newBuilder("global", realm.getGlobalObject()).build();
        if (realm.getContext().getContextOptions().isScriptEngineGlobalScopeImport()) {
            Scope scriptEngineImportScope = Scope.newBuilder("scriptEngineImport", realm.getScriptEngineImportScope()).build();
            return Arrays.asList(scriptEngineImportScope, globalLexicalScope, globalVarScope);
        }
        return Arrays.asList(globalLexicalScope, globalVarScope);
    }

    protected final Scope toScope(MaterializedFrame frame) {
        return Scope.newBuilder(getName(), getVariables(frame)).node(getNode()).arguments(getArguments(frame)).receiver(THIS_NAME, getThis(frame)).rootInstance(getFunctionObject()).build();
    }

    protected abstract String getName();

    protected abstract Node getNode();

    protected abstract Object getVariables(Frame frame);

    protected abstract Object getArguments(Frame frame);

    protected abstract Object getThis(Frame frame);

    protected final Object getFunctionObject() {
        if (mFrame == null) {
            return null;
        }
        Object[] args = mFrame.getArguments();
        return JSArguments.getFunctionObject(args);
    }

    protected abstract JSScope findParent();

    private static JSScope createScope(Node node, MaterializedFrame frame) {
        if (frame != null) {
            if (ScopeFrameNode.isBlockScopeFrame(frame)) {
                FrameBlockScopeNode blockScopeNode = null;
                for (Node n = node; n != null; n = n.getParent()) {
                    if (n instanceof FrameBlockScopeNode) {
                        if (frame.getFrameDescriptor() == ((FrameBlockScopeNode) n).getFrameDescriptor()) {
                            blockScopeNode = (FrameBlockScopeNode) n;
                            break;
                        }
                    }
                }
                return new JSBlockScope(blockScopeNode, frame);
            }
        } else {
            for (Node n = node; n != null; n = n.getParent()) {
                if (n instanceof FrameBlockScopeNode) {
                    return new JSBlockScope((FrameBlockScopeNode) n, frame);
                }
            }
        }
        return new JSFunctionScope(node, frame);
    }

    protected static Object createVariablesMapObject(FrameDescriptor frameDesc, MaterializedFrame frame, Object[] args) {
        assert frame == null || frame.getFrameDescriptor() == frameDesc;
        Map<String, Variable> slotMap = new LinkedHashMap<>();
        for (FrameSlot slot : frameDesc.getSlots()) {
            if (JSFrameUtil.isThisSlot(slot)) {
                continue;
            }
            if (JSFrameUtil.isInternal(slot)) {
                continue;
            }
            if (isUnsetFrameSlot(frame, slot)) {
                continue;
            }
            String name = slot.getIdentifier().toString();
            slotMap.put(name, new FrameSlotVariable(slot));
        }
        return new VariablesMapObject(slotMap, args, frame);
    }

    static boolean isUnsetFrameSlot(Frame frame, FrameSlot slot) {
        if (frame != null && frame.isObject(slot)) {
            Object value = FrameUtil.getObjectSafe(frame, slot);
            if (value == null || value == Dead.instance() || value instanceof Frame) {
                return true;
            }
        }
        return false;
    }

    protected Node findParentScopeNode() {
        Node parent = node;
        // Find the current scope root:
        while (!(parent == null || parent instanceof BlockScopeNode || parent instanceof RootNode)) {
            parent = parent.getParent();
        }
        if (parent != null) {
            // Find the next scope root:
            parent = parent.getParent();
            while (!(parent == null || parent instanceof BlockScopeNode || parent instanceof RootNode)) {
                parent = parent.getParent();
            }
        }
        return parent;
    }

    abstract static class Variable {
        public abstract Object get(Frame frame, Object[] args);

        public abstract void set(Frame frame, Object[] args, Object value);

        public abstract boolean isWritable();
    }

    static final class FrameSlotVariable extends Variable {
        private final FrameSlot slot;
        private final boolean writable;

        FrameSlotVariable(FrameSlot slot) {
            this.slot = slot;
            this.writable = !JSFrameUtil.isConst(slot) && !JSFrameUtil.isInternal(slot);
        }

        @Override
        public Object get(Frame frame, Object[] args) {
            assert !isUnsetFrameSlot(frame, slot);
            return frame.getValue(slot);
        }

        @Override
        public void set(Frame frame, Object[] args, Object value) {
            if (frame.isInt(slot) && value instanceof Integer) {
                frame.setInt(slot, (int) value);
            } else if (frame.isDouble(slot) && value instanceof Double) {
                frame.setDouble(slot, (double) value);
            } else if (frame.isBoolean(slot) && value instanceof Boolean) {
                frame.setBoolean(slot, (boolean) value);
            } else {
                frame.setObject(slot, value);
            }
        }

        @Override
        public boolean isWritable() {
            return writable;
        }
    }

    static final class ArgumentVariable extends Variable {
        private final int index;

        ArgumentVariable(int index) {
            this.index = index;
        }

        @Override
        public Object get(Frame frame, Object[] args) {
            if (index >= args.length) {
                return Undefined.instance;
            }
            return args[index];
        }

        @Override
        public void set(Frame frame, Object[] args, Object value) {
        }

        @Override
        public boolean isWritable() {
            return false;
        }
    }

    public static class JSBlockScope extends JSScope {

        private final FrameBlockScopeNode blockScopeNode;

        protected JSBlockScope(FrameBlockScopeNode blockScopeNode, MaterializedFrame frame) {
            super(blockScopeNode, frame);
            this.blockScopeNode = blockScopeNode;
            assert frame == null || blockScopeNode == null || frame.getFrameDescriptor() == blockScopeNode.getFrameDescriptor();
        }

        @Override
        protected String getName() {
            String locationStr = "";
            if (node != null && node.getSourceSection() != null) {
                locationStr = " at " + node.getSourceSection().getSource().getName() + ":" + node.getSourceSection().getStartLine();
            }
            return "JS block scope" + locationStr;
        }

        @Override
        protected Node getNode() {
            return blockScopeNode;
        }

        @Override
        protected Object getVariables(Frame frame) {
            if (mFrame == null && frame == null) {
                return new VariablesMapObject(Collections.emptyMap(), null, null);
            }
            MaterializedFrame f = mFrame != null ? mFrame : frame.materialize();
            assert blockScopeNode == null || f.getFrameDescriptor() == blockScopeNode.getFrameDescriptor();
            return createVariablesMapObject(f.getFrameDescriptor(), f, null);
        }

        @Override
        protected Object getArguments(Frame frame) {
            return null; // Block scope does not have a concept of arguments
        }

        @Override
        protected Object getThis(Frame frame) {
            return null; // Block scope does not override function's this
        }

        @Override
        protected JSScope findParent() {
            if (mFrame == null) {
                return null;
            }
            Node parent = findParentScopeNode();
            if (parent == null) {
                return null;
            }
            Frame parentFrame = (Frame) FrameUtil.getObjectSafe(mFrame, mFrame.getFrameDescriptor().findFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER));
            return JSScope.createScope(parent, parentFrame.materialize());
        }
    }

    public static class JSFunctionScope extends JSScope {

        private final RootNode rootNode;

        protected JSFunctionScope(Node node, MaterializedFrame frame) {
            super(node, getFunctionFrame(frame));
            this.rootNode = findRootNode(node);
            assert frame == null || rootNode == null || (rootNode instanceof JavaScriptRootNode && ((JavaScriptRootNode) rootNode).isResumption()) ||
                            frame.getFrameDescriptor() == rootNode.getFrameDescriptor();
        }

        private static MaterializedFrame getFunctionFrame(MaterializedFrame frame) {
            if (frame != null && frame.getArguments().length > 0) {
                Object arg0 = frame.getArguments()[0];
                if (arg0 instanceof MaterializedFrame) {
                    // arg0 is generatorFrame
                    return (MaterializedFrame) arg0;
                }
            }
            return frame;
        }

        private static RootNode findRootNode(Node node) {
            Node n = node;
            while (!(n instanceof RootNode) && (n != null)) {
                n = n.getParent();
            }
            return (RootNode) n;
        }

        @Override
        protected String getName() {
            if (rootNode == null) {
                return "unknown";
            }
            return rootNode.getName();
        }

        @Override
        protected Node getNode() {
            return rootNode;
        }

        @Override
        protected Object getVariables(Frame frame) {
            return createVariablesMapObject(mFrame != null ? mFrame.getFrameDescriptor() : rootNode.getFrameDescriptor(), mFrame, null);
        }

        @Override
        protected Object getArguments(Frame frame) {
            if (rootNode == null || mFrame == null) {
                return null;
            }
            return new VariablesMapObject(collectArgs(rootNode), mFrame.getArguments(), mFrame);
        }

        @Override
        protected Object getThis(Frame frame) {
            if (mFrame == null) {
                return null;
            }
            FrameSlot thisSlot = JSFrameUtil.getThisSlot(mFrame.getFrameDescriptor());
            if (thisSlot == null) {
                return thisFromArguments(mFrame.getArguments());
            } else {
                Object thiz = mFrame.getValue(thisSlot);
                if (thiz == Undefined.instance) {
                    // this can be either undefined or not populated yet
                    // => try to avoid returning undefined in the latter case
                    Object[] args = mFrame.getArguments();
                    Object function = JSArguments.getFunctionObject(args);
                    if (JSFunction.isJSFunction(function)) {
                        DynamicObject jsFunction = (DynamicObject) function;
                        thiz = isArrowFunctionWithThisCaptured(jsFunction) ? JSFunction.getLexicalThis(jsFunction) : thisFromArguments(args);
                    }
                }
                return thiz;
            }
        }

        private static Object thisFromArguments(Object[] args) {
            Object thisObject = JSArguments.getThisObject(args);
            Object function = JSArguments.getFunctionObject(args);
            if (JSFunction.isJSFunction(function) && !JSFunction.isStrict((DynamicObject) function)) {
                JSRealm realm = JavaScriptLanguage.getCurrentJSRealm();
                if (thisObject == Undefined.instance || thisObject == Null.instance) {
                    thisObject = realm.getGlobalObject();
                } else {
                    thisObject = JSRuntime.toObject(realm.getContext(), thisObject);
                }
            }
            return thisObject;
        }

        private static boolean isArrowFunctionWithThisCaptured(DynamicObject function) {
            return !JSFunction.isConstructor(function) && JSFunction.isClassPrototypeInitialized(function);
        }

        private static Map<String, ? extends Variable> collectArgs(Node block) {
            Map<String, Variable> args = new LinkedHashMap<>(4);
            NodeUtil.forEachChild(block, new NodeVisitor() {

                private JSWriteFrameSlotNode wn; // The current write node containing a slot

                @Override
                public boolean visit(Node node) {
                    if (node instanceof JSWriteFrameSlotNode) {
                        wn = (JSWriteFrameSlotNode) node;
                        boolean all = NodeUtil.forEachChild(node, this);
                        wn = null;
                        return all;
                    } else if (wn != null && (node instanceof AccessIndexedArgumentNode)) {
                        FrameSlot slot = wn.getFrameSlot();
                        if (!JSFrameUtil.isInternal(slot)) {
                            String name = Objects.toString(slot.getIdentifier());
                            int argIndex = JSArguments.RUNTIME_ARGUMENT_COUNT + ((AccessIndexedArgumentNode) node).getIndex();
                            assert !args.containsKey(name) : name + " argument exists already.";
                            args.put(name, new ArgumentVariable(argIndex));
                        }
                        return true;
                    } else if (!(node instanceof JavaScriptBaseNode) || (node instanceof WrapperNode) || (node instanceof AbstractBlockNode) || (node instanceof FunctionBodyNode) ||
                                    (node instanceof DualNode) || (node instanceof DiscardResultNode)) {
                        // Visit children of blocks or unknown nodes
                        return NodeUtil.forEachChild(node, this);
                    } else {
                        // Visit a next sibling
                        return true;
                    }
                }
            });
            return args;
        }

        @Override
        protected JSScope findParent() {
            if (mFrame == null) {
                return null;
            }
            MaterializedFrame parentFrame = JSFrameUtil.getParentFrame(mFrame);
            if (parentFrame != null && parentFrame != JSFrameUtil.NULL_MATERIALIZED_FRAME) {
                return createScope(null, JSFrameUtil.getParentFrame(mFrame));
            }
            return null;
        }
    }

    static Object getInteropValue(Object value) {
        if (JSRuntime.isLazyString(value)) {
            return value.toString();
        } else if (value instanceof SafeInteger) {
            return ((SafeInteger) value).doubleValue();
        } else if (value instanceof TruffleObject) {
            return value;
        } else if (JSRuntime.isJSPrimitive(value)) {
            return value;
        } else {
            return JavaScriptLanguage.getCurrentEnv().asGuestValue(value);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class VariablesMapObject implements TruffleObject {

        final Map<String, ? extends Variable> slots;
        final Object[] args;
        final Frame frame;

        private VariablesMapObject(Map<String, ? extends Variable> slots, Object[] args, Frame frame) {
            this.slots = slots;
            this.args = args;
            this.frame = frame;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean hasMembers() {
            return true;
        }

        @ExportMessage
        @TruffleBoundary
        Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            List<String> names = new ArrayList<>(slots.keySet());
            return InteropList.create(names);
        }

        @ExportMessage
        @TruffleBoundary
        boolean isMemberReadable(String name) {
            Variable slot = slots.get(name);
            if (slot == null) {
                return false;
            }
            return true;
        }

        @ExportMessage
        @TruffleBoundary
        boolean isMemberModifiable(String name) {
            Variable slot = slots.get(name);
            if (slot == null) {
                return false;
            } else if (frame == null) {
                return false;
            }
            return slot.isWritable();
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isMemberInsertable(@SuppressWarnings("unused") String name) {
            return false;
        }

        @ExportMessage
        @TruffleBoundary
        Object readMember(String name) throws UnknownIdentifierException {
            Variable slot = slots.get(name);
            if (slot == null) {
                throw UnknownIdentifierException.create(name);
            } else if (frame == null) {
                return Undefined.instance;
            } else {
                Object value = slot.get(frame, args);
                return getInteropValue(value);
            }
        }

        @ExportMessage
        @TruffleBoundary
        void writeMember(String name, Object value) throws UnsupportedMessageException, UnknownIdentifierException {
            if (frame == null) {
                throw UnsupportedMessageException.create();
            }
            Variable slot = slots.get(name);
            if (slot == null) {
                throw UnknownIdentifierException.create(name);
            } else if (slot.isWritable()) {
                slot.set(frame, args, value);
            } else {
                throw UnsupportedMessageException.create();
            }
        }
    }

    /**
     * Wraps a dynamic scope object, filters out dead variables, and prevents const assignment.
     */
    @ExportLibrary(InteropLibrary.class)
    static final class DynamicScopeWrapper implements TruffleObject {
        final DynamicObject scope;

        private DynamicScopeWrapper(DynamicObject scope) {
            this.scope = scope;
        }

        boolean isConst(String name, DynamicObjectLibrary access) {
            return JSProperty.isConst(access.getProperty(scope, name));
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean hasMembers() {
            return true;
        }

        @ExportMessage
        @TruffleBoundary
        Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
                        @CachedLibrary("this.scope") DynamicObjectLibrary access) {
            List<String> keys = new ArrayList<>();
            for (Object key : access.getKeyArray(scope)) {
                if (key instanceof String) {
                    Object value = access.getOrDefault(scope, key, null);
                    if (value != null && value != Dead.instance()) {
                        keys.add((String) key);
                    }
                }
            }
            return InteropList.create(keys);
        }

        @ExportMessage
        @TruffleBoundary
        boolean isMemberReadable(String name,
                        @CachedLibrary("this.scope") DynamicObjectLibrary access) {
            Object value = access.getOrDefault(scope, name, null);
            if (value == null || value == Dead.instance()) {
                return false;
            }
            return true;
        }

        @ExportMessage
        @TruffleBoundary
        boolean isMemberModifiable(String name,
                        @CachedLibrary("this.scope") DynamicObjectLibrary access) {
            return isMemberReadable(name, access) && !isConst(name, access);
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isMemberInsertable(@SuppressWarnings("unused") String name) {
            return false;
        }

        @ExportMessage
        @TruffleBoundary
        Object readMember(String name,
                        @CachedLibrary("this.scope") DynamicObjectLibrary access) throws UnknownIdentifierException {
            Object value = access.getOrDefault(scope, name, null);
            if (value == null || value == Dead.instance()) {
                throw UnknownIdentifierException.create(name);
            } else {
                return getInteropValue(value);
            }
        }

        @ExportMessage
        @TruffleBoundary
        void writeMember(String name, Object value,
                        @CachedLibrary("this.scope") DynamicObjectLibrary access) throws UnsupportedMessageException, UnknownIdentifierException {
            Object curValue = access.getOrDefault(scope, name, null);
            if (curValue == null || curValue == Dead.instance()) {
                throw UnknownIdentifierException.create(name);
            } else if (!isConst(name, access)) {
                access.putIfPresent(scope, name, value);
            } else {
                throw UnsupportedMessageException.create();
            }
        }
    }
}

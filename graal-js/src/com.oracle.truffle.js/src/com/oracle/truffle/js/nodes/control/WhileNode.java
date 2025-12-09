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
package com.oracle.truffle.js.nodes.control;

import java.util.Set;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.js.builtins.ArrayIteratorPrototypeBuiltins.ArrayIteratorGetLengthSafeNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.runtime.builtins.JSArrayIteratorObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * 12.6.2 The while Statement.
 */
@NodeInfo(shortName = "while")
public final class WhileNode extends StatementNode {

    @Child private LoopNode loop;

    private final ControlFlowRootTag.Type loopType;

    private WhileNode(RepeatingNode repeatingNode, ControlFlowRootTag.Type type) {
        this(Truffle.getRuntime().createLoopNode(repeatingNode), type);
    }

    private WhileNode(LoopNode loopNode, ControlFlowRootTag.Type type) {
        this.loop = loopNode;
        this.loopType = type;
    }

    private static JavaScriptNode createWhileDo(LoopNode loopNode, ControlFlowRootTag.Type type) {
        return new WhileNode(loopNode, type);
    }

    public static RepeatingNode createWhileDoRepeatingNode(JavaScriptNode condition, JavaScriptNode body) {
        JavaScriptNode nonVoidBody = body instanceof DiscardResultNode ? ((DiscardResultNode) body).getOperand() : body;
        return new WhileDoRepeatingNode(condition, nonVoidBody);
    }

    public static JavaScriptNode createWhileDo(LoopNode loopNode) {
        return createWhileDo(loopNode, ControlFlowRootTag.Type.WhileIteration);
    }

    public static JavaScriptNode createDesugaredFor(LoopNode loopNode) {
        return createWhileDo(loopNode, ControlFlowRootTag.Type.ForIteration);
    }

    public static JavaScriptNode createDesugaredForOf(LoopNode loopNode) {
        return createWhileDo(loopNode, ControlFlowRootTag.Type.ForOfIteration);
    }

    public static JavaScriptNode createDesugaredForIn(LoopNode loopNode) {
        return createWhileDo(loopNode, ControlFlowRootTag.Type.ForInIteration);
    }

    public static JavaScriptNode createDesugaredForAwaitOf(LoopNode loopNode) {
        return createWhileDo(loopNode, ControlFlowRootTag.Type.ForAwaitOfIteration);
    }

    public static RepeatingNode createDoWhileRepeatingNode(JavaScriptNode condition, JavaScriptNode body) {
        JavaScriptNode nonVoidBody = body instanceof DiscardResultNode ? ((DiscardResultNode) body).getOperand() : body;
        return new DoWhileRepeatingNode(condition, nonVoidBody);
    }

    public static JavaScriptNode createDoWhile(LoopNode loopNode) {
        return new WhileNode(loopNode, ControlFlowRootTag.Type.DoWhileIteration);
    }

    public static RepeatingNode createForOfRepeatingNode(JavaScriptNode iteratorNode, JavaScriptNode nextResultNode, JavaScriptNode body, JSWriteFrameSlotNode writeNextValueNode) {
        JavaScriptNode nonVoidBody = body instanceof DiscardResultNode ? ((DiscardResultNode) body).getOperand() : body;
        return ForOfRepeatingNode.create(iteratorNode, nextResultNode, nonVoidBody, writeNextValueNode);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ControlFlowRootTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("type", loopType.name());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (hasMaterializationTag(materializedTags) &&
                        loop.getRepeatingNode() instanceof AbstractRepeatingNode repeatingNode &&
                        repeatingNode.materializationNeeded()) {
            // The repeating node should not have a wrapper, because it has no source section
            AbstractRepeatingNode materializedLoop = repeatingNode.materializeInstrumentableNodes(materializedTags);
            transferSourceSectionAndTags(this, materializedLoop.bodyNode);
            WhileNode materialized = new WhileNode(materializedLoop, loopType);
            transferSourceSectionAndTags(this, materialized);
            return materialized;
        }
        return this;
    }

    private static boolean hasMaterializationTag(Set<Class<? extends Tag>> materializedTags) {
        return materializedTags.contains(ControlFlowRootTag.class) || materializedTags.contains(ControlFlowBlockTag.class) ||
                        materializedTags.contains(ControlFlowBranchTag.class);
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new WhileNode((RepeatingNode) cloneUninitialized((JavaScriptNode) loop.getRepeatingNode(), materializedTags), loopType);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        loop.execute(frame);
        return EMPTY;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        loop.execute(frame);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }

    public LoopNode getLoopNode() {
        return loop;
    }

    /** do {body} while(condition). */
    private static final class DoWhileRepeatingNode extends AbstractRepeatingNode implements ResumableNode.WithIntState {

        DoWhileRepeatingNode(JavaScriptNode condition, JavaScriptNode body) {
            super(condition, body);
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            executeBody(frame);
            return executeCondition(frame);
        }

        @Override
        public Object resume(VirtualFrame frame, int stateSlot) {
            int index = getStateAsIntAndReset(frame, stateSlot);
            if (index == 0) {
                executeBody(frame);
            }
            try {
                return executeCondition(frame);
            } catch (YieldException e) {
                setStateAsInt(frame, stateSlot, 1);
                throw e;
            }
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new DoWhileRepeatingNode(cloneUninitialized(conditionNode, materializedTags), cloneUninitialized(bodyNode, materializedTags));
        }

        @Override
        public AbstractRepeatingNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            if (!materializationNeeded()) {
                return this;
            }
            return new DoWhileRepeatingNode(materializeCondition(materializedTags), materializeBody(materializedTags));
        }
    }

    /** while(condition) {body}. */
    private static final class WhileDoRepeatingNode extends AbstractRepeatingNode implements ResumableNode.WithIntState {

        WhileDoRepeatingNode(JavaScriptNode condition, JavaScriptNode body) {
            super(condition, body);
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            if (executeCondition(frame)) {
                executeBody(frame);
                return true;
            }
            return false;
        }

        @Override
        public Object resume(VirtualFrame frame, int stateSlot) {
            int index = getStateAsIntAndReset(frame, stateSlot);
            if (index != 0 || executeCondition(frame)) {
                try {
                    executeBody(frame);
                } catch (YieldException e) {
                    setStateAsInt(frame, stateSlot, 1);
                    throw e;
                }
                return true;
            }
            return false;
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new WhileDoRepeatingNode(cloneUninitialized(conditionNode, materializedTags), cloneUninitialized(bodyNode, materializedTags));
        }

        @Override
        public AbstractRepeatingNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            if (!materializationNeeded()) {
                return this;
            }
            return new WhileDoRepeatingNode(materializeCondition(materializedTags), materializeBody(materializedTags));
        }
    }

    /**
     * For-in/for-of/for-await-of loop body.
     */
    abstract static class ForOfRepeatingNode extends AbstractRepeatingNode implements ResumableNode.WithIntState {

        @Child JavaScriptNode iteratorNode;
        @Child JavaScriptNode nextResultNode;
        @Child JSWriteFrameSlotNode writeNextValueNode;
        @Child IteratorCompleteNode iteratorCompleteNode = IteratorCompleteNode.create();
        @Child IteratorValueNode iteratorValueNode = IteratorValueNode.create();

        ForOfRepeatingNode(JavaScriptNode iteratorNode, JavaScriptNode nextResultNode, JavaScriptNode body, JSWriteFrameSlotNode writeNextValueNode) {
            super(null, body);
            this.iteratorNode = iteratorNode;
            this.nextResultNode = nextResultNode;
            this.writeNextValueNode = writeNextValueNode;
        }

        static ForOfRepeatingNode create(JavaScriptNode iteratorNode, JavaScriptNode nextResultNode, JavaScriptNode body, JSWriteFrameSlotNode writeNextValueNode) {
            return WhileNodeFactory.ForOfRepeatingNodeGen.create(iteratorNode, nextResultNode, body, writeNextValueNode);
        }

        /**
         * Specialization for the built-in Array Iterator that supports normal arrays, typed arrays,
         * and foreign arrays and skips creating iterator result objects when the iterator is done,
         * so as to avoid control flow merges between undefined and primitive values that can cause
         * unnecessary boxing allocations in compiled code. For all other objects, or when getting
         * the length would throw an error (in which case length will be -1), we fall back to the
         * generic case so that the Array Iterator next method is visible on the call stack.
         *
         * This specialization will only trigger for {@code for-of} loops, since {@code for-in} uses
         * a different iterator and {@code for-await-of} is invoked via {@link #resume}.
         */
        @Specialization(guards = {"isArrayIterator(iteratorRecord)", "length >= 0"}, limit = "1")
        protected boolean doArrayIterator(VirtualFrame frame,
                        @Bind("getIteratorRecord(frame)") @SuppressWarnings("unused") IteratorRecord iteratorRecord,
                        @Cached @SuppressWarnings("unused") ArrayIteratorGetLengthSafeNode getLengthNode,
                        @Bind("getArrayIterator(iteratorRecord)") JSArrayIteratorObject arrayIterator,
                        @Bind("getLengthNode.execute($node, arrayIterator.getIteratedObject())") long length) {
            long index = arrayIterator.getNextIndex();
            if (index >= length) {
                arrayIterator.setIteratedObject(Undefined.instance);
                return false;
            }

            /*
             * Call the "next" method as usual to advance the iterator and get the next result, but
             * skip getting the length again and checking if (index >= length). Also skip getting
             * the "done" property since it's side-effect-free for built-in iterator result objects.
             */
            arrayIterator.setSkipGetLength(true);
            Object nextResult = nextResultNode.execute(frame);
            Object nextValue = iteratorValueNode.execute(nextResult);
            writeNextValueNode.executeWrite(frame, nextValue);
            try {
                executeBody(frame);
            } finally {
                writeNextValueNode.executeWrite(frame, Undefined.instance);
            }
            return true;
        }

        @Specialization(replaces = "doArrayIterator")
        protected boolean doGeneric(VirtualFrame frame) {
            Object nextResult = nextResultNode.execute(frame);
            boolean done = iteratorCompleteNode.execute(nextResult);
            Object nextValue = iteratorValueNode.execute(nextResult);
            if (done) {
                return false;
            }
            writeNextValueNode.executeWrite(frame, nextValue);
            try {
                executeBody(frame);
            } finally {
                writeNextValueNode.executeWrite(frame, Undefined.instance);
            }
            return true;
        }

        @Override
        public Object resume(VirtualFrame frame, int stateSlot) {
            int state = getStateAsIntAndReset(frame, stateSlot);
            if (state == 0) {
                Object nextResult = nextResultNode.execute(frame);
                boolean done = iteratorCompleteNode.execute(nextResult);
                Object nextValue = iteratorValueNode.execute(nextResult);
                if (done) {
                    return false;
                }
                writeNextValueNode.executeWrite(frame, nextValue);
            }
            try {
                executeBody(frame);
            } catch (YieldException e) {
                setStateAsInt(frame, stateSlot, 1);
                throw e;
            } finally {
                writeNextValueNode.executeWrite(frame, Undefined.instance);
            }
            return true;
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return ForOfRepeatingNode.create(
                            cloneUninitialized(iteratorNode, materializedTags),
                            cloneUninitialized(nextResultNode, materializedTags),
                            cloneUninitialized(bodyNode, materializedTags),
                            cloneUninitialized(writeNextValueNode, materializedTags));
        }

        @Override
        public AbstractRepeatingNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
            if (!materializationNeeded()) {
                return this;
            }
            return ForOfRepeatingNode.create(
                            cloneUninitialized(iteratorNode, materializedTags),
                            cloneUninitialized(nextResultNode, materializedTags),
                            materializeBody(materializedTags),
                            cloneUninitialized(writeNextValueNode, materializedTags));
        }

        protected final IteratorRecord getIteratorRecord(VirtualFrame frame) {
            return (IteratorRecord) iteratorNode.execute(frame);
        }

        protected final boolean isArrayIterator(IteratorRecord iterator) {
            return iterator.getIterator() instanceof JSArrayIteratorObject &&
                            iterator.getNextMethod() == getRealm().getArrayIteratorNextMethod();
        }

        protected static JSArrayIteratorObject getArrayIterator(IteratorRecord iterator) {
            return (JSArrayIteratorObject) iterator.getIterator();
        }
    }
}

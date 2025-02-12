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
package com.oracle.truffle.js.nodes;

import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.function.BlockScopeNode;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.interop.ScopeVariables;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;

@GenerateWrapper
@ExportLibrary(NodeLibrary.class)
public abstract class JavaScriptNode extends JavaScriptBaseNode implements InstrumentableNode {
    /** Source or SourceSection. */
    private Object source;
    private int charIndex;
    private int charLength;

    private static final int STATEMENT_TAG_BIT = 1 << 31;
    private static final int CALL_TAG_BIT = 1 << 30;
    private static final int CHAR_LENGTH_MASK = ~(STATEMENT_TAG_BIT | CALL_TAG_BIT);

    private static final int ROOT_BODY_TAG_BIT = 1 << 31;
    private static final int EXPRESSION_TAG_BIT = 1 << 30;
    private static final int CHAR_INDEX_MASK = ~(ROOT_BODY_TAG_BIT | EXPRESSION_TAG_BIT);

    protected static final String INTERMEDIATE_VALUE = "(intermediate value)";

    protected JavaScriptNode() {
    }

    protected JavaScriptNode(SourceSection sourceSection) {
        setSourceSection(sourceSection);
    }

    @Override
    public boolean isInstrumentable() {
        return hasSourceSection();
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new JavaScriptNodeWrapper(this, probe);
    }

    /**
     * Executes this node using the specified context and frame and returns the result value.
     *
     * @param frame the frame of the currently executing guest language method
     * @return the value of the execution
     */
    public abstract Object execute(VirtualFrame frame);

    /**
     * Like {@link #execute(VirtualFrame)} except that it tries to convert the result value to an
     * int. A node can override this method if it has a better way to producing a value of type int.
     *
     * @param frame the frame of the currently executing guest language method
     * @return the value of the execution as an int
     * @throws UnexpectedResultException if a loss-free conversion of the result to int is not
     *             possible
     */
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        Object o = execute(frame);
        if (o instanceof Integer) {
            return (int) o;
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnexpectedResultException(o);
        }
    }

    /**
     * Like {@link #execute(VirtualFrame)} except that it tries to convert the result value to a
     * double. A node can override this method if it has a better way to producing a value of type
     * double.
     *
     * @param frame the frame of the currently executing guest language method
     * @return the value of the execution as a double
     * @throws UnexpectedResultException if a loss-free conversion of the result to double is not
     *             possible
     */
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        Object o = execute(frame);
        if (o instanceof Double) {
            return (double) o;
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnexpectedResultException(o);
        }
    }

    /**
     * Like {@link #execute(VirtualFrame)} except that it tries to convert the result value to a
     * boolean. A node can override this method if it has a better way to producing a value of type
     * boolean.
     *
     * @param frame the frame of the currently executing guest language method
     * @return the value of the execution as a boolean
     * @throws UnexpectedResultException if a loss-free conversion of the result to double is not
     *             possible
     */
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        Object o = execute(frame);
        if (o instanceof Boolean) {
            return (boolean) o;
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new UnexpectedResultException(o);
        }
    }

    /**
     * Like {@link #execute(VirtualFrame)} except that it throws away the result. A node can
     * override this method if it has a better way to execute without producing a value.
     *
     * @param frame the frame of the currently executing guest language method
     */
    public void executeVoid(VirtualFrame frame) {
        execute(frame);
    }

    @Override
    public JavaScriptNode copy() {
        CompilerAsserts.neverPartOfCompilation("cannot call JavaScriptNode.copy() in compiled code");
        return (JavaScriptNode) super.copy();
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation("cannot call JavaScriptNode.toString() in compiled code");
        String simpleName = getClass().getName().substring(getClass().getName().lastIndexOf('.') + 1);
        StringBuilder sb = new StringBuilder(simpleName);
        sb.append('@').append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" ").append(JSNodeUtil.formatSourceSection(this));
        String tagsString = JSNodeUtil.formatTags(this);
        if (!tagsString.isEmpty()) {
            sb.append("[").append(tagsString).append("]");
        }
        RootNode rootNode = getRootNode();
        if (rootNode != null) {
            sb.append(" '").append(JSNodeUtil.resolveName(rootNode)).append("'");
        }
        String expressionString = expressionToString();
        if (expressionString != null) {
            sb.append(" (").append(expressionString).append(")");
        }
        return sb.toString();
    }

    @Override
    protected void onReplace(Node newNode, CharSequence reason) {
        super.onReplace(newNode, reason);
        transferSourceSectionAndTags(this, (JavaScriptNode) newNode);
    }

    public static void transferSourceSectionAndTags(JavaScriptNode fromNode, JavaScriptNode toNode) {
        if (!toNode.hasSourceSection() && fromNode.hasSourceSection()) {
            // Pass on the source section to the new node.
            toNode.source = fromNode.source;
            toNode.charIndex = fromNode.charIndex | (toNode.charIndex & ~CHAR_INDEX_MASK);
            toNode.charLength = fromNode.charLength | (toNode.charLength & ~CHAR_LENGTH_MASK);
        }
    }

    public static void transferSourceSectionAddExpressionTag(JavaScriptNode fromNode, JavaScriptNode toNode) {
        if (!toNode.hasSourceSection() && fromNode.hasSourceSection()) {
            // Pass on the source section to the new node, but do not propagate tags.
            toNode.source = fromNode.source;
            toNode.charIndex = fromNode.charIndex & CHAR_INDEX_MASK;
            toNode.charLength = fromNode.charLength & CHAR_LENGTH_MASK;
            toNode.addExpressionTag();
        }
    }

    public static void transferSourceSection(JavaScriptNode fromNode, JavaScriptNode toNode) {
        if (!toNode.hasSourceSection() && fromNode.hasSourceSection()) {
            // Pass on the source section to the new node, but do not propagate tags.
            toNode.source = fromNode.source;
            toNode.charIndex = fromNode.charIndex & CHAR_INDEX_MASK;
            toNode.charLength = fromNode.charLength & CHAR_LENGTH_MASK;
        }
    }

    public final boolean hasSourceSection() {
        return source != null;
    }

    @Override
    public final SourceSection getSourceSection() {
        if (hasSourceSection()) {
            Object src = source;
            if (src instanceof SourceSection) {
                return (SourceSection) src;
            } else {
                SourceSection section = ((Source) src).createSection(charIndex & CHAR_INDEX_MASK, charLength & CHAR_LENGTH_MASK);
                source = section;
                return section;
            }
        }
        return null;
    }

    public final void setSourceSection(SourceSection section) {
        CompilerAsserts.neverPartOfCompilation();
        if (hasSourceSection()) {
            checkSameSourceSection(section);
        }
        this.source = section;
    }

    public final void setSourceSection(Source source, int charIndex, int charLength) {
        CompilerAsserts.neverPartOfCompilation();
        checkValidSourceSection(source, charIndex, charLength);
        if (hasSourceSection()) {
            checkSameSourceSection(source.createSection(charIndex, charLength));
        }
        assert charIndex <= CHAR_INDEX_MASK && charLength <= CHAR_LENGTH_MASK;
        this.charIndex = charIndex | (this.charIndex & ~CHAR_INDEX_MASK);
        this.charLength = charLength | (this.charLength & ~CHAR_LENGTH_MASK);
        this.source = source;
    }

    /** @see Source#createSection(int, int) */
    private static void checkValidSourceSection(Source source, int charIndex, int charLength) {
        if (charIndex < 0) {
            throw new IllegalArgumentException("charIndex < 0");
        } else if (charLength < 0) {
            throw new IllegalArgumentException("length < 0");
        }
        assert charIndex + charLength <= source.getCharacters().length();
    }

    private void checkSameSourceSection(SourceSection newSection) {
        SourceSection sourceSection = getSourceSection();
        if (sourceSection != null && !sourceSection.equals(newSection) && !equivalentUnavailableSections(sourceSection, newSection)) {
            throw new IllegalStateException(String.format("Source section is already assigned. Old: %s, new: %s", sourceSection, newSection));
        }
    }

    private static boolean equivalentUnavailableSections(SourceSection section1, SourceSection section2) {
        return !section1.isAvailable() && !section2.isAvailable() && section1.getSource().equals(section2.getSource());
    }

    public boolean isResultAlwaysOfType(@SuppressWarnings("unused") Class<?> clazz) {
        return false;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == StandardTags.StatementTag.class) {
            return (charLength & STATEMENT_TAG_BIT) != 0;
        } else if (tag == StandardTags.CallTag.class) {
            return (charLength & CALL_TAG_BIT) != 0;
        } else if (tag == StandardTags.RootBodyTag.class) {
            return (charIndex & ROOT_BODY_TAG_BIT) != 0;
        } else if (tag == StandardTags.ExpressionTag.class) {
            return (charIndex & EXPRESSION_TAG_BIT) != 0;
        } else {
            return false;
        }
    }

    public final void addStatementTag() {
        charLength |= STATEMENT_TAG_BIT;
    }

    public final void addCallTag() {
        charLength |= CALL_TAG_BIT;
    }

    public final void addRootBodyTag() {
        charIndex |= ROOT_BODY_TAG_BIT;
    }

    public final void addExpressionTag() {
        charIndex |= EXPRESSION_TAG_BIT;
    }

    final boolean hasImportantTag() {
        return ((charIndex & ROOT_BODY_TAG_BIT) != 0 ||
                        (charIndex & EXPRESSION_TAG_BIT) != 0 ||
                        (charLength & STATEMENT_TAG_BIT) != 0 ||
                        (charLength & CALL_TAG_BIT) != 0);
    }

    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        if (this instanceof WrapperNode) {
            WrapperNode wrapperNode = (WrapperNode) this;
            return cloneUninitialized((JavaScriptNode) wrapperNode.getDelegateNode(), materializedTags);
        }

        throw Errors.notImplemented(getClass().getSimpleName() + ".copyUninitialized()");
    }

    @SuppressWarnings("unchecked")
    public static <T extends JavaScriptNode> T cloneUninitialized(T node, Set<Class<? extends Tag>> materializedTags) {
        if (node == null) {
            return null;
        } else {
            T copy = node;
            if (materializedTags != null && node.isInstrumentable()) {
                copy = (T) node.materializeInstrumentableNodes(materializedTags);
            }
            if (node == copy) {
                copy = (T) node.copyUninitialized(materializedTags);
                // Assertion might not always hold and fail spuriously.
                assert copy.getClass() == node.getClass() || node instanceof JSBuiltinNode || node instanceof WrapperNode : node.getClass() + " => " + copy.getClass();
                transferSourceSectionAndTags(node, copy);
            }
            return copy;
        }
    }

    public static <T extends JavaScriptNode> T[] cloneUninitialized(T[] nodeArray, Set<Class<? extends Tag>> materializedTags) {
        if (nodeArray == null) {
            return null;
        } else {
            T[] copy = nodeArray.clone();
            for (int i = 0; i < copy.length; i++) {
                copy[i] = cloneUninitialized(copy[i], materializedTags);
            }
            return copy;
        }
    }

    public String expressionToString() {
        if (this instanceof WrapperNode wrapperNode) {
            return ((JavaScriptNode) wrapperNode.getDelegateNode()).expressionToString();
        } else {
            return null;
        }
    }

    // NodeLibrary

    @ExportMessage
    boolean accepts(@Cached(value = "this", adopt = false) JavaScriptNode cachedNode) {
        return this == cachedNode;
    }

    @ExportMessage
    final boolean hasScope(@SuppressWarnings("unused") Frame frame) {
        return this.getParent() != null;
    }

    @ExportMessage
    final Object getScope(Frame frame, boolean nodeEnter,
                    @Cached(value = "findBlockScopeNode(this)", allowUncached = true, adopt = false, neverDefault = false) Node blockNode,
                    @Cached(value = "findFrameScopeNode(blockNode)", allowUncached = true, adopt = false, neverDefault = false) Node frameBlockNode) throws UnsupportedMessageException {
        if (hasScope(frame)) {
            Frame functionFrame;
            Frame scopeFrame;
            if (frame != null) {
                RootNode rootNode = getRootNode();
                if (rootNode instanceof JavaScriptRootNode && ((JavaScriptRootNode) rootNode).isResumption() && frame.getFrameDescriptor() == rootNode.getFrameDescriptor()) {
                    functionFrame = JSArguments.getResumeExecutionContext(frame.getArguments());
                } else if (rootNode.getFrameDescriptor() == JavaScriptRootNode.MODULE_DUMMY_FRAMEDESCRIPTOR) {
                    functionFrame = ((JSModuleRecord) JSArguments.getUserArgument(frame.getArguments(), 0)).getEnvironment();
                } else {
                    functionFrame = frame.materialize();
                }
                if (frameBlockNode instanceof BlockScopeNode.FrameBlockScopeNode) {
                    Object maybeScopeFrame = ((BlockScopeNode.FrameBlockScopeNode) frameBlockNode).getBlockScope((VirtualFrame) functionFrame);
                    if (maybeScopeFrame instanceof Frame) {
                        scopeFrame = (Frame) maybeScopeFrame;
                    } else {
                        scopeFrame = functionFrame;
                    }
                } else {
                    scopeFrame = functionFrame;
                }
            } else {
                functionFrame = null;
                scopeFrame = null;
            }
            return ScopeVariables.create(scopeFrame, nodeEnter, blockNode, functionFrame);
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    final boolean hasReceiverMember(Frame frame) {
        return frame != null;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    final Object getReceiverMember(Frame frame) throws UnsupportedMessageException {
        if (frame == null) {
            throw UnsupportedMessageException.create();
        }
        return ScopeVariables.RECEIVER_MEMBER;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    boolean hasRootInstance(Frame frame) {
        return frame != null;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    Object getRootInstance(Frame frame) throws UnsupportedMessageException {
        if (frame == null) {
            throw UnsupportedMessageException.create();
        }
        return JSFrameUtil.getFunctionObject(frame);
    }

    @TruffleBoundary
    public static Node findBlockScopeNode(Node node) {
        if (node == null) {
            return null;
        }
        Node parent = node;
        for (Node n = parent; n != null; parent = n, n = n.getParent()) {
            if (n instanceof BlockScopeNode) {
                return n;
            }
        }
        assert parent instanceof RootNode : "Node " + node + " is not adopted.";
        return parent;
    }

    @TruffleBoundary
    static Node findFrameScopeNode(Node node) {
        if (node == null) {
            return null;
        }
        Node parent = node;
        for (Node n = parent; n != null; parent = n, n = n.getParent()) {
            if (n instanceof BlockScopeNode.FrameBlockScopeNode) {
                return n;
            }
        }
        assert parent instanceof RootNode : "Node " + node + " is not adopted.";
        return parent;
    }
}

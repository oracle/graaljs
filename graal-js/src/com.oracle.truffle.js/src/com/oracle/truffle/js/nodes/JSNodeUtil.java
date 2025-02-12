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

import java.util.Locale;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.instrumentation.InstrumentableNode.WrapperNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.StandardTags.RootBodyTag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.access.JSTargetableWrapperNode;
import com.oracle.truffle.js.nodes.access.VarWrapperNode;
import com.oracle.truffle.js.nodes.control.GeneratorWrapperNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.instrumentation.JSInputGeneratingNodeWrapper;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.runtime.util.DebugCounter;

public final class JSNodeUtil {
    static final DebugCounter NODE_CREATE_COUNT = DebugCounter.create("NodeCreateCount");
    static final DebugCounter NODE_REPLACE_COUNT = DebugCounter.create("NodeReplaceCount");

    private static final SlowPathException SLOW_PATH_EXCEPTION = new SlowPathException();

    private JSNodeUtil() {
        // this class should not be instantiated
    }

    public static SlowPathException slowPathException() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return SLOW_PATH_EXCEPTION;
    }

    static String formatTags(JavaScriptNode node) {
        CompilerAsserts.neverPartOfCompilation();
        StringBuilder sb = new StringBuilder(4);
        if (node.hasTag(StandardTags.StatementTag.class)) {
            sb.append('S');
        }
        if (node.hasTag(StandardTags.CallTag.class)) {
            sb.append('C');
        }
        if (node.hasTag(StandardTags.RootTag.class)) {
            sb.append('R');
        }
        if (node.hasTag(StandardTags.RootBodyTag.class)) {
            sb.append('B');
        }
        if (node.hasTag(StandardTags.ExpressionTag.class)) {
            sb.append('E');
        }
        return sb.toString();
    }

    /**
     * Returns true if this sequence node has an important tag that we must preserve, so we must not
     * eliminate the node during block flattening.
     */
    public static boolean hasImportantTag(JavaScriptNode node) {
        return node.hasImportantTag();
    }

    public static String resolveName(RootNode root) {
        if (root instanceof FunctionRootNode) {
            return ((FunctionRootNode) root).getName();
        }
        return "unknown";
    }

    /**
     * Formats a source section of a node in human readable form. If no source section could be
     * found it looks up the parent hierarchy until it finds a source section. Nodes where this was
     * required append a <code>'~'</code> at the end.
     *
     * @param node the node to format.
     * @return a formatted source section string
     */
    public static String formatSourceSection(Node node) {
        CompilerAsserts.neverPartOfCompilation();
        if (node == null) {
            return "<unknown>";
        }
        SourceSection section = node.getSourceSection();
        boolean estimated = false;
        if (section == null) {
            section = node.getEncapsulatingSourceSection();
            estimated = true;
        }

        if (section == null || !section.isAvailable()) {
            return "<unknown source>";
        } else {
            String sourceName = section.getSource().getName();
            int startLine = section.getStartLine();
            return String.format(Locale.ROOT, "%s:%d%s", sourceName, startLine, estimated ? "~" : "");
        }
    }

    public static boolean hasExactlyOneRootBodyTag(JavaScriptNode body) {
        CompilerAsserts.neverPartOfCompilation();
        return NodeUtil.countNodes(body, node -> !(node instanceof GeneratorWrapperNode) && node instanceof JavaScriptNode && ((JavaScriptNode) node).hasTag(RootBodyTag.class)) == 1;
    }

    /**
     * Returns <code>true</code> if <code>node</code> is a JavaScript node that is considered a
     * wrapper.
     */
    public static boolean isWrapperNode(JavaScriptNode node) {
        return (node instanceof WrapperNode ||
                        node instanceof VarWrapperNode ||
                        node instanceof JSInputGeneratingNodeWrapper ||
                        node instanceof JSTaggedExecutionNode ||
                        node instanceof JSTargetableWrapperNode);
    }

    /**
     * Helper to retrieve the node wrapped by a given JavaScript node.
     *
     * @param node a JavaScript node that is possibly a wrapper
     * @return the (delegate) node that is wrapped by the parameter <code>node</code>, or
     *         <code>node</code> itself if it is not a wrapper that can be stripped
     */
    public static JavaScriptNode getWrappedNode(JavaScriptNode node) {
        JavaScriptNode unwrapped = node;
        if (node instanceof WrapperNode) {
            WrapperNode wrapper = (WrapperNode) node;
            // JavaScriptNode wrappers have a JavaScriptNode as delegate
            unwrapped = (JavaScriptNode) wrapper.getDelegateNode();
        }
        if (unwrapped instanceof JSInputGeneratingNodeWrapper) {
            unwrapped = ((JSInputGeneratingNodeWrapper) unwrapped).getDelegateNode();
        }
        if (unwrapped instanceof JSTaggedExecutionNode) {
            unwrapped = ((JSTaggedExecutionNode) unwrapped).getDelegateNode();
        }
        if (unwrapped instanceof VarWrapperNode) {
            unwrapped = ((VarWrapperNode) unwrapped).getDelegateNode();
        }
        if (unwrapped instanceof JSTargetableWrapperNode) {
            unwrapped = ((JSTargetableWrapperNode) unwrapped).getDelegate();
        }
        if (unwrapped instanceof WrapperNode) {
            WrapperNode wrapper = (WrapperNode) unwrapped;
            unwrapped = (JavaScriptNode) wrapper.getDelegateNode();
        }
        assert !isWrapperNode(unwrapped);
        return unwrapped;
    }

    public static boolean isTaggedNode(Node node) {
        return node instanceof JSTaggedExecutionNode || (node instanceof WrapperNode && ((WrapperNode) node).getDelegateNode() instanceof JSTaggedExecutionNode);
    }

    public static boolean isInputGeneratingNode(Node node) {
        return node instanceof JSInputGeneratingNodeWrapper || (node instanceof WrapperNode && ((WrapperNode) node).getDelegateNode() instanceof JSInputGeneratingNodeWrapper);
    }
}

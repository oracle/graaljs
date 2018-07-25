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
package com.oracle.truffle.js.nodes;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.runtime.util.DebugCounter;

public final class JSNodeUtil {
    public static final DebugCounter NODE_CREATE_COUNT = DebugCounter.create("NodeCreateCount");
    public static final DebugCounter NODE_REPLACE_COUNT = DebugCounter.create("NodeReplaceCount");

    private JSNodeUtil() {
        // this class should not be instantiated
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
        if (node.hasTag(StandardTags.ExpressionTag.class)) {
            sb.append('E');
        }
        return sb.toString();
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
            String sourceName = new File(section.getSource().getName()).getName();
            int startLine = section.getStartLine();
            return String.format("%s:%d%s", sourceName, startLine, estimated ? "~" : "");
        }
    }

    @TruffleBoundary
    public static <T> T getCacheTopNode(Node start, Class<T> baseNodeClass) {
        assert baseNodeClass.isInstance(start);
        Node current = start;
        for (; baseNodeClass.isInstance(current.getParent()); current = current.getParent()) {
        }
        return baseNodeClass.cast(current);
    }

    @TruffleBoundary
    public static int getCacheNodeDepth(Node start, Class<?> baseNodeClass) {
        assert baseNodeClass.isInstance(start);
        int depth = 1;
        for (Node current = start; baseNodeClass.isInstance(current.getParent()); current = current.getParent()) {
            depth++;
        }
        return depth;
    }

    public static void forEachDeep(final Node root, Predicate<? super Node> filter, Consumer<? super Node> action) {
        root.accept(node -> {
            if (filter.test(node)) {
                action.accept(node);
            }
            return true;
        });
    }

    public static <T> void forEachDeep(final Node root, Class<T> classFilter, Consumer<T> action) {
        root.accept(node -> {
            if (classFilter.isInstance(node)) {
                action.accept(classFilter.cast(node));
            }
            return true;
        });
    }
}

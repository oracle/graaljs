/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.instrumentation;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.nodes.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class InstrumentedNodesExecutionEvenListener implements ExecutionEventListener {
    List<Node> enteredNodes = new ArrayList<>();
    List<Node> exitedNodes = new ArrayList<>();
    List<Node> exceptionNodes = new ArrayList<>();

    private final Set<Class<?>> classFilter;

    public InstrumentedNodesExecutionEvenListener(Class<?>... classFilter) {
        this.classFilter = new HashSet<>(Arrays.asList(classFilter));
    }

    private boolean filter(Node node) {
        return classFilter.isEmpty() || classFilter.stream().anyMatch(clazz -> clazz.isAssignableFrom(node.getClass()));
    }

    @Override
    public void onEnter(EventContext context, VirtualFrame frame) {
        if (filter(context.getInstrumentedNode())) {
            enteredNodes.add(context.getInstrumentedNode());
        }
    }

    @Override
    public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        if (filter(context.getInstrumentedNode())) {
            exitedNodes.add(context.getInstrumentedNode());
        }
    }

    @Override
    public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        if (filter(context.getInstrumentedNode())) {
            exceptionNodes.add(context.getInstrumentedNode());
        }
    }

    public void checkEnteredNodes(List<? extends Node> expected) {
        checkNodes(expected, enteredNodes);
    }

    public void checkExitedNodes(List<? extends Node> expected) {
        checkNodes(expected, enteredNodes);
    }

    private void checkNodes(List<? extends Node> expected, List<? extends Node> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertSame(expected.get(i), actual.get(i));
        }
    }
}

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

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;

/**
 * Switch.
 *
 * <pre>
 * <b>switch</b> (switchExpression) {
 * <b>case</b> caseExpression: [statements];
 * <b>default</b>: [statements]
 * }
 * </pre>
 */
@NodeInfo(shortName = "switch")
public final class SwitchNode extends StatementNode {

    @Children private final JavaScriptNode[] caseExpressions;
    /**
     * jumptable[i] has the index of the first statement that should be executed if
     * caseExpression[i] equals switchExpression. jumptable[jumptable.length-1] is always the
     * statement index of the default case.
     */
    @CompilationFinal(dimensions = 1) private final int[] jumptable;
    @Children private final JavaScriptNode[] statements;

    private SwitchNode(JavaScriptNode[] caseExpressions, int[] jumptable, JavaScriptNode[] statements) {
        this.caseExpressions = new JavaScriptNode[caseExpressions.length];
        for (int i = 0; i < caseExpressions.length; i++) {
            this.caseExpressions[i] = caseExpressions[i];
        }
        this.jumptable = jumptable;
        assert caseExpressions.length == jumptable.length - 1;
        this.statements = statements;
    }

    public static SwitchNode create(JavaScriptNode[] caseExpressions, int[] jumptable, JavaScriptNode[] statements) {
        return new SwitchNode(caseExpressions, jumptable, statements);
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
        return JSTags.createNodeObjectDescriptor("type", ControlFlowRootTag.Type.Conditional.name());
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ControlFlowRootTag.class)) {
            JavaScriptNode[] newCaseExpressions = new JavaScriptNode[caseExpressions.length];
            for (int i = 0; i < caseExpressions.length; i++) {
                InstrumentableNode materialized = caseExpressions[i].materializeInstrumentableNodes(materializedTags);
                newCaseExpressions[i] = JSTaggedExecutionNode.createFor((JavaScriptNode) materialized, ControlFlowBranchTag.class,
                                JSTags.createNodeObjectDescriptor("type", ControlFlowBranchTag.Type.Condition.name()));
            }
            JavaScriptNode[] newStatements = new JavaScriptNode[statements.length];
            for (int i = 0; i < statements.length; i++) {
                InstrumentableNode materialized = statements[i].materializeInstrumentableNodes(materializedTags);
                newStatements[i] = JSTaggedExecutionNode.createFor((JavaScriptNode) materialized, ControlFlowBlockTag.class);
            }
            SwitchNode materialized = SwitchNode.create(newCaseExpressions, jumptable, newStatements);
            transferSourceSectionAndTags(this, materialized);
            return materialized;
        } else {
            return this;
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int jumptableIdx = identifyTargetCase(frame);
        return executeStatements(frame, jumptable[jumptableIdx]);
    }

    @ExplodeLoop
    private int identifyTargetCase(VirtualFrame frame) {
        int i;
        for (i = 0; i < caseExpressions.length; i++) {
            if (executeConditionAsBoolean(frame, caseExpressions[i])) {
                break;
            }
        }
        return i;
    }

    @ExplodeLoop
    private Object executeStatements(VirtualFrame frame, int statementStartIndex) {
        Object result = EMPTY;
        for (int statementIndex = 0; statementIndex < statements.length; statementIndex++) {
            if (statementIndex >= statementStartIndex) {
                result = statements[statementIndex].execute(frame);
            }
        }
        return result;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(caseExpressions), jumptable, cloneUninitialized(statements));
    }
}

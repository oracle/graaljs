/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBlockStatementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowConditionStatementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowStatementRootTag;

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
        if (tag == ControlFlowStatementRootTag.class) {
            return true;
        } else if (tag == StatementTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(ControlFlowStatementRootTag.class)) {
            JavaScriptNode[] newCaseExpressions = new JavaScriptNode[caseExpressions.length];
            for (int i = 0; i < caseExpressions.length; i++) {
                InstrumentableNode materialized = caseExpressions[i].materializeInstrumentableNodes(materializedTags);
                newCaseExpressions[i] = JSTaggedExecutionNode.createFor((JavaScriptNode) materialized, ControlFlowConditionStatementTag.class);
            }
            JavaScriptNode[] newStatements = new JavaScriptNode[statements.length];
            for (int i = 0; i < statements.length; i++) {
                InstrumentableNode materialized = statements[i].materializeInstrumentableNodes(materializedTags);
                newStatements[i] = JSTaggedExecutionNode.createFor((JavaScriptNode) materialized, ControlFlowBlockStatementTag.class);
            }
            return SwitchNode.create(newCaseExpressions, jumptable, newStatements);
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

/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;

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

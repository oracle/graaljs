package com.oracle.js.parser.ir;

import com.oracle.js.parser.TokenType;

public class PipelineNode extends BinaryNode{

    private int level;

    public PipelineNode(long token, Expression lhs, Expression rhs, int level) {
        super(token, lhs, rhs);
        this.level = level;
    }

    public static boolean isPipeline(final TokenType tokenType) {
        return tokenType == TokenType.PIPELINE;
    }
}

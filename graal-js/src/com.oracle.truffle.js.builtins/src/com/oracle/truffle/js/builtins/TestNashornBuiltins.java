/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.TestNashornBuiltinsFactory.TestNashornParseToJSONNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTestNashorn;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins to support special behavior used by Test262.
 */
public final class TestNashornBuiltins extends JSBuiltinsContainer.SwitchEnum<TestNashornBuiltins.TestNashorn> {

    protected TestNashornBuiltins() {
        super(JSTestNashorn.CLASS_NAME, TestNashorn.class);
    }

    public enum TestNashorn implements BuiltinEnum<TestNashorn> {
        parseToJSON(3);

        private final int length;

        TestNashorn(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TestNashorn builtinEnum) {
        switch (builtinEnum) {
            case parseToJSON:
                return TestNashornParseToJSONNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
        }
        return null;
    }

    /**
     * For load("nashorn:parser.js") compatibility.
     */
    public abstract static class TestNashornParseToJSONNode extends JSBuiltinNode {
        public TestNashornParseToJSONNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected String parseToJSON(Object code0, Object name0, Object location0) {
            String code = JSRuntime.toString(code0);
            String name = name0 == Undefined.instance ? "<unknown>" : JSRuntime.toString(name0);
            boolean location = JSRuntime.toBoolean(location0);
            return getContext().getEvaluator().parseToJSON(getContext(), code, name, location);
        }
    }
}

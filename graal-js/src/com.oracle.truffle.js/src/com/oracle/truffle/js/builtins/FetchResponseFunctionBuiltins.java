package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFetchResponse;
import com.oracle.truffle.js.builtins.FetchResponseFunctionBuiltinsFactory.FetchErrorNodeGen;
import com.oracle.truffle.js.builtins.FetchResponseFunctionBuiltinsFactory.FetchJsonNodeGen;
import com.oracle.truffle.js.builtins.FetchResponseFunctionBuiltinsFactory.FetchRedirectNodeGen;

public class FetchResponseFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<FetchResponseFunctionBuiltins.FetchResponseFunction> {
    public static final JSBuiltinsContainer BUILTINS = new FetchResponseFunctionBuiltins();

    protected FetchResponseFunctionBuiltins() {
        super(JSFetchResponse.CLASS_NAME, FetchResponseFunction.class);
    }

    public enum FetchResponseFunction implements BuiltinEnum<FetchResponseFunction> {
        error(1),
        json(1),
        redirect(1);

        private final int length;

        FetchResponseFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, FetchResponseFunction builtinEnum) {
        switch (builtinEnum) {
            case error:
                return FetchErrorNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case json:
                return FetchJsonNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case redirect:
                return FetchRedirectNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class FetchErrorNode extends JSBuiltinNode {
        public FetchErrorNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int error() {
            return 0;
        }

    }

    public abstract static class FetchJsonNode extends JSBuiltinNode {
        public FetchJsonNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int json(Object a, Object b) {
            return 0;
        }
    }

    public abstract static class FetchRedirectNode extends JSBuiltinNode {
        public FetchRedirectNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected int redirect(Object a, Object b) {
            return 0;
        }
    }
}

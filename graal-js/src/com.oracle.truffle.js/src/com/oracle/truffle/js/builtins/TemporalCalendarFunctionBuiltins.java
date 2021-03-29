package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.builtins.TemporalCalendarFunctionBuiltinsFactory.JSTemporalCalendarFromNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalTimeObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalCalendarFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalCalendarFunctionBuiltins.TemporalCalendarFunction>{

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainTimeFunctionBuiltins();

    protected TemporalCalendarFunctionBuiltins() {
        super(JSTemporalPlainTime.CLASS_NAME, TemporalCalendarFunctionBuiltins.TemporalCalendarFunction.class);
    }

    public enum TemporalCalendarFunction implements BuiltinEnum<TemporalCalendarFunctionBuiltins.TemporalCalendarFunction> {
        from(1);

        private final int length;

        TemporalCalendarFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalCalendarFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return JSTemporalCalendarFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalCalendarFromNode extends JSBuiltinNode {

        public JSTemporalCalendarFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected Object from(DynamicObject item,
                              @Cached("create()") IsObjectNode isObject,
                              @Cached("create()") IsConstructorNode isConstructor,
                              @CachedLibrary("item") DynamicObjectLibrary dol,
                              @Cached("create()") JSToStringNode toString,
                              @Cached("createNew()") JSFunctionCallNode callNode) {
            DynamicObject constructor = getContext().getRealm().getTemporalCalendarConstructor();
            return JSTemporalCalendar.calendarFrom(item, constructor, dol, isObject, toString,
                    isConstructor, callNode);
        }

    }

}

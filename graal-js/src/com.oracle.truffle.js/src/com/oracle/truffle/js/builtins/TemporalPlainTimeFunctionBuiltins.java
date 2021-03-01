package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.builtins.TemporalPlainTimeFunctionBuiltinsFactory.JSTemporalPlainTimeCompareNodeGen;
import com.oracle.truffle.js.builtins.TemporalPlainTimeFunctionBuiltinsFactory.JSTemporalPlainTimeFromNodeGen;
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
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalTimeObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalPlainTimeFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainTimeFunctionBuiltins.TemporalPlainTimeFunction>{

    public static final JSBuiltinsContainer BUILTINS = new TemporalPlainTimeFunctionBuiltins();

    protected TemporalPlainTimeFunctionBuiltins() {
        super(JSTemporalPlainTime.CLASS_NAME, TemporalPlainTimeFunction.class);
    }

    public enum TemporalPlainTimeFunction implements BuiltinEnum<TemporalPlainTimeFunction> {
        from(2),
        compare(2);

        private final int length;

        TemporalPlainTimeFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalPlainTimeFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return JSTemporalPlainTimeFromNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case compare:
                return JSTemporalPlainTimeCompareNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalPlainTimeFromNode extends JSBuiltinNode {

        public JSTemporalPlainTimeFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected Object from(DynamicObject item, DynamicObject options,
                                     @Cached("create()") IsObjectNode isObject,
                                     @Cached("create()") IsConstructorNode isConstructor,
                                     @CachedLibrary("options") DynamicObjectLibrary dol,
                                     @Cached("create()") JSToBooleanNode toBoolean,
                                     @Cached("create()") JSToStringNode toString,
                                     @Cached("create()") JSToIntegerAsIntNode toInt,
                                     @Cached("createNew()") JSFunctionCallNode callNode) {
            DynamicObject constructor = getContext().getRealm().getTemporalPlainTimeConstructor();
            DynamicObject normalizedOptions = TemporalUtil.normalizeOptionsObject(options,
                    getContext().getRealm(), isObject);
            String overflow = TemporalUtil.toTemporalOverflow(normalizedOptions, dol, isObject, toBoolean, toString);
            if(isObject.executeBoolean(item) && JSTemporalPlainTime.isJSTemporalTime(item)) {
                JSTemporalTimeObject timeItem = (JSTemporalTimeObject) item;
                return JSTemporalPlainTime.createTemporalTimeFromStatic(constructor,
                        timeItem.getHours(), timeItem.getMinutes(), timeItem.getSeconds(), timeItem.getMilliseconds(),
                        timeItem.getMicroseconds(), timeItem.getNanoseconds(), isConstructor, callNode);
            }
            return JSTemporalPlainTime.toTemporalTime(item, constructor, overflow, getContext().getRealm(),
                    isObject, dol, toInt, toString, isConstructor, callNode);
        }

    }

    public abstract static class JSTemporalPlainTimeCompareNode extends JSBuiltinNode {

        public JSTemporalPlainTimeCompareNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSTemporalTime(obj1)", "isJSTemporalTime(obj2)"})
        protected int compare(DynamicObject obj1, DynamicObject obj2) {
            JSTemporalTimeObject time1 = (JSTemporalTimeObject) obj1;
            JSTemporalTimeObject time2 = (JSTemporalTimeObject) obj2;
            return JSTemporalPlainTime.compareTemporalTime(
                    time1.getHours(), time1.getMinutes(), time1.getSeconds(),
                    time1.getMilliseconds(), time1.getMicroseconds(), time1.getNanoseconds(),
                    time2.getHours(), time2.getMinutes(), time2.getSeconds(),
                    time2.getMilliseconds(), time2.getMicroseconds(), time2.getNanoseconds()
            );
        }

        @Specialization(guards = "!isJSTemporalTime(obj1) || !isJSTemporalTime(obj2)")
        protected int cantCompare(Object obj1, Object obj2) {
            throw Errors.createTypeErrorTemporalTimeExpected();
        }

    }

}

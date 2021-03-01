package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDurationObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalDurationFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalDurationFunctionBuiltins.TemporalDurationFunction>{

    public static final JSBuiltinsContainer BUILTINS = new TemporalDurationFunctionBuiltins();

    protected TemporalDurationFunctionBuiltins() {
        super(JSTemporalDuration.CLASS_NAME, TemporalDurationFunction.class);
    }

    public enum TemporalDurationFunction implements BuiltinEnum<TemporalDurationFunction> {
        from(1),
        compare(2);

        private final int length;

        TemporalDurationFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalDurationFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return TemporalDurationFunctionBuiltinsFactory.JSTemporalDurationFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case compare:
                return TemporalDurationFunctionBuiltinsFactory.JSTemporalDurationCompareNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalDurationFrom extends JSBuiltinNode {

        protected JSTemporalDurationFrom(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected Object from(DynamicObject item,
                              @Cached("create()") IsObjectNode isObject,
                              @Cached("create()") IsConstructorNode isConstructor,
                              @Cached("create()") JSToIntegerAsLongNode toInt,
                              @Cached("create()") JSToStringNode toString,
                              @Cached("createNew()") JSFunctionCallNode callNode,
                              @CachedLibrary("item") DynamicObjectLibrary dol) {
            DynamicObject constructor = getContext().getRealm().getTemporalDurationConstructor();
            if(isObject.executeBoolean(item) && JSTemporalDuration.isJSTemporalDuration(item)) {
                JSTemporalDurationObject duration = (JSTemporalDurationObject) item;
                return JSTemporalDuration.createTemporalDurationFromStatic(constructor, duration.getYears(),
                        duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                        duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                        duration.getMicroseconds(), duration.getNanoseconds(), isConstructor, callNode);
            }
            return JSTemporalDuration.toTemporalDuration(item, constructor, getContext().getRealm(), isObject,
                    toInt, dol, toString, isConstructor, callNode);
        }
    }

    public abstract static class JSTemporalDurationCompare extends JSBuiltinNode {

        protected JSTemporalDurationCompare(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected int compare(DynamicObject one, DynamicObject two, DynamicObject options,
                              @Cached("create()") IsObjectNode isObject,
                              @Cached("create()") IsConstructorNode isConstructor,
                              @Cached("create()") JSToIntegerAsLongNode toInt,
                              @Cached("create()") JSToStringNode toString,
                              @Cached("createNew()") JSFunctionCallNode callNode,
                              @CachedLibrary("options") DynamicObjectLibrary dol) {
            try {
                one = (DynamicObject) JSTemporalDuration.toTemporalDuration(one, null, getContext().getRealm(), isObject,
                        toInt, dol, toString, isConstructor, callNode);
                two = (DynamicObject) JSTemporalDuration.toTemporalDuration(two, null, getContext().getRealm(), isObject,
                        toInt, dol, toString, isConstructor, callNode);
                options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                DynamicObject relativeTo = TemporalUtil.toRelativeTemporalObject(options, isObject, dol);
                long shift1 = JSTemporalDuration.calculateOffsetShift(relativeTo,
                        dol.getLongOrDefault(one, JSTemporalDuration.YEARS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.MONTHS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.WEEKS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.DAYS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.NANOSECONDS, 0),
                        isObject
                );
                long shift2 = JSTemporalDuration.calculateOffsetShift(relativeTo,
                        dol.getLongOrDefault(two, JSTemporalDuration.YEARS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.MONTHS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.WEEKS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.DAYS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.NANOSECONDS, 0),
                        isObject
                );
                long days1, days2;
                if(dol.getLongOrDefault(one, JSTemporalDuration.YEARS, 0) != 0 || dol.getLongOrDefault(two, JSTemporalDuration.YEARS, 0) != 0 ||
                        dol.getLongOrDefault(one, JSTemporalDuration.MONTHS, 0) != 0 || dol.getLongOrDefault(two, JSTemporalDuration.MONTHS, 0) != 0 ||
                        dol.getLongOrDefault(one, JSTemporalDuration.WEEKS, 0) != 0 || dol.getLongOrDefault(two, JSTemporalDuration.WEEKS, 0) != 0) {
                    DynamicObject balanceResult1 = JSTemporalDuration.unbalanceDurationRelative(
                            dol.getLongOrDefault(one, JSTemporalDuration.YEARS, 0),
                            dol.getLongOrDefault(one, JSTemporalDuration.YEARS, 0),
                            dol.getLongOrDefault(one, JSTemporalDuration.YEARS, 0),
                            dol.getLongOrDefault(one, JSTemporalDuration.YEARS, 0),
                            JSTemporalDuration.DAYS, relativeTo, dol, getContext().getRealm()
                    );
                    DynamicObject balanceResult2 = JSTemporalDuration.unbalanceDurationRelative(
                            dol.getLongOrDefault(two, JSTemporalDuration.YEARS, 0),
                            dol.getLongOrDefault(two, JSTemporalDuration.YEARS, 0),
                            dol.getLongOrDefault(two, JSTemporalDuration.YEARS, 0),
                            dol.getLongOrDefault(two, JSTemporalDuration.YEARS, 0),
                            JSTemporalDuration.DAYS, relativeTo, dol, getContext().getRealm()
                    );
                    days1 = dol.getLongOrDefault(balanceResult1, JSTemporalDuration.DAYS, 0);
                    days2 = dol.getLongOrDefault(balanceResult2, JSTemporalDuration.DAYS, 0);
                } else {
                    days1 = dol.getLongOrDefault(one, JSTemporalDuration.DAYS, 0);
                    days2 = dol.getLongOrDefault(two, JSTemporalDuration.DAYS, 0);
                }
                long ns1 = JSTemporalDuration.totalDurationNanoseconds(days1,
                        dol.getLongOrDefault(one, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(one, JSTemporalDuration.NANOSECONDS, 0),
                        shift1
                );
                long ns2 = JSTemporalDuration.totalDurationNanoseconds(days2,
                        dol.getLongOrDefault(two, JSTemporalDuration.HOURS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.MINUTES, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.SECONDS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.MILLISECONDS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.MICROSECONDS, 0),
                        dol.getLongOrDefault(two, JSTemporalDuration.NANOSECONDS, 0),
                        shift2
                );
                if (ns1 > ns2) {
                    return 1;
                }
                if (ns1 < ns2) {
                    return -1;
                }
                return 0;
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

}

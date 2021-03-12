package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarDateFromFieldsNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarMonthDayFromFieldsNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarToStringNodeGen;
import com.oracle.truffle.js.builtins.TemporalCalendarPrototypeBuiltinsFactory.JSTemporalCalendarYearMonthFromFieldsNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.cast.JSStringToNumberNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendar;
import com.oracle.truffle.js.runtime.builtins.JSTemporalCalendarObject;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainDate;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalCalendarPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalCalendarPrototypeBuiltins.TemporalCalendarPrototype> {

    public static final TemporalCalendarPrototypeBuiltins INSTANCE = new TemporalCalendarPrototypeBuiltins();

    protected TemporalCalendarPrototypeBuiltins() {
        super(JSTemporalCalendar.PROTOTYPE_NAME, TemporalCalendarPrototype.class);
    }

    public enum TemporalCalendarPrototype implements BuiltinEnum<TemporalCalendarPrototype> {
        dateFromFields(3),
        yearMonthFromFields(3),
        monthDayFromFields(3),
        toString(0),
        toJSON(0);

        private final int length;

        TemporalCalendarPrototype(int length) {
            this.length = length;
        }


        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalCalendarPrototype builtinEnum) {
        switch (builtinEnum) {
            case dateFromFields:
                return JSTemporalCalendarDateFromFieldsNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case yearMonthFromFields:
                return JSTemporalCalendarYearMonthFromFieldsNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case monthDayFromFields:
                return JSTemporalCalendarMonthDayFromFieldsNodeGen.create(context, builtin, args().withThis().fixedArgs(3).createArgumentNodes(context));
            case toString:
            case toJSON:
                return JSTemporalCalendarToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    // 12.4.4
    public abstract static class JSTemporalCalendarDateFromFields extends JSBuiltinNode {

        protected JSTemporalCalendarDateFromFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public Object dateFromFields(DynamicObject thisObj, DynamicObject fields, DynamicObject options,
                                            DynamicObject constructor,
                                            @Cached("create()") IsObjectNode isObject,
                                            @Cached("create()") IsConstructorNode isConstructor,
                                            @Cached("createSameValue()") JSIdenticalNode identicalNode,
                                            @Cached("create()") JSToBooleanNode toBoolean,
                                            @Cached("create()") JSToStringNode toString,
                                            @Cached("create()") JSStringToNumberNode stringToNumber,
                                            @Cached("createNew()") JSFunctionCallNode callNode,
                                            @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            try {
                JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
                assert calendar.getId().equals("iso8601");
                if (!isObject.executeBoolean(fields)) {
                    throw Errors.createRangeError("Given fields is not an object.");
                }
                options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
                DynamicObject result = JSTemporalCalendar.isoDateFromFields(fields, options, getContext().getRealm(),
                        isObject, dol, toBoolean, toString, stringToNumber, identicalNode);
                return JSTemporalPlainDate.createTemporalDateFromStatic(constructor,
                        dol.getLongOrDefault(result, JSTemporalPlainDate.YEAR, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainDate.MONTH, 0),
                        dol.getLongOrDefault(result, JSTemporalPlainDate.DAY, 0),
                        calendar, isConstructor, callNode
                );
            } catch (UnexpectedResultException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 12.4.5
    public abstract static class JSTemporalCalendarYearMonthFromFields extends JSBuiltinNode {

        protected JSTemporalCalendarYearMonthFromFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public Object yearMonthFromFields(DynamicObject thisObj, DynamicObject fields, DynamicObject options,
                                          DynamicObject constructor,
                                          @Cached("create()") IsObjectNode isObject,
                                          @Cached("create()") IsConstructorNode isConstructor,
                                          @Cached("createSameValue()") JSIdenticalNode identicalNode,
                                          @Cached("create()") JSToBooleanNode toBoolean,
                                          @Cached("create()") JSToStringNode toString,
                                          @Cached("create()") JSStringToNumberNode stringToNumber,
                                          @Cached("createNew()") JSFunctionCallNode callNode,
                                          @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals("iso8601");
            if (!isObject.executeBoolean(fields)) {
                throw Errors.createTypeError("Given fields is not an object.");
            }
            options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
            DynamicObject result = JSTemporalCalendar.isoYearMonthFromFields(fields, options, getContext().getRealm(),
                    isObject, dol, toBoolean, toString, stringToNumber, identicalNode);
            return null;    // TODO: Call JSTemporalYearMonth.createTemporalYearMonthFromStatic()
        }
    }

    // 12.4.6
    public abstract static class JSTemporalCalendarMonthDayFromFields extends JSBuiltinNode {

        protected JSTemporalCalendarMonthDayFromFields(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        public Object monthDayFromFields(DynamicObject thisObj, DynamicObject fields, DynamicObject options,
                                         DynamicObject constructor,
                                         @Cached("create()") IsObjectNode isObject,
                                         @Cached("create()") IsConstructorNode isConstructor,
                                         @Cached("createSameValue()") JSIdenticalNode identicalNode,
                                         @Cached("create()") JSToBooleanNode toBoolean,
                                         @Cached("create()") JSToStringNode toString,
                                         @Cached("create()") JSStringToNumberNode stringToNumber,
                                         @Cached("createNew()") JSFunctionCallNode callNode,
                                         @CachedLibrary("thisObj") DynamicObjectLibrary dol) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            assert calendar.getId().equals("iso8601");
            if (!isObject.executeBoolean(fields)) {
                throw Errors.createTypeError("Given fields is not an object.");
            }
            options = TemporalUtil.normalizeOptionsObject(options, getContext().getRealm(), isObject);
            DynamicObject result = JSTemporalCalendar.isoMonthDayFromFields(fields, options, getContext().getRealm(),
                    isObject, dol, toBoolean, toString, stringToNumber, identicalNode);
            return null;    // TODO: Call JSTemporalPlainMonthDay.createTemporalMonthDayFromStatic()
        }
    }

    // 12.4.23
    public abstract static class JSTemporalCalendarToString extends JSBuiltinNode {

        protected JSTemporalCalendarToString(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public String toString(DynamicObject thisObj) {
            JSTemporalCalendarObject calendar = (JSTemporalCalendarObject) thisObj;
            return calendar.getId();
        }
    }
}

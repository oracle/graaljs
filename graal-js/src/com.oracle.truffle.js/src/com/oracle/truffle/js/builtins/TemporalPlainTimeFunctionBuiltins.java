/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalPlainTimeObject;
import com.oracle.truffle.js.runtime.util.TemporalUtil;

public class TemporalPlainTimeFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalPlainTimeFunctionBuiltins.TemporalPlainTimeFunction> {

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
                        @Cached("create()") JSToIntegerAsLongNode toInt,
                        @Cached("createNew()") JSFunctionCallNode callNode) {
            DynamicObject constructor = getContext().getRealm().getTemporalPlainTimeConstructor();
            DynamicObject normalizedOptions = TemporalUtil.normalizeOptionsObject(options,
                            getContext().getRealm(), isObject);
            String overflow = TemporalUtil.toTemporalOverflow(normalizedOptions, dol, isObject, toBoolean, toString);
            if (isObject.executeBoolean(item) && JSTemporalPlainTime.isJSTemporalTime(item)) {
                JSTemporalPlainTimeObject timeItem = (JSTemporalPlainTimeObject) item;
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
            JSTemporalPlainTimeObject time1 = (JSTemporalPlainTimeObject) obj1;
            JSTemporalPlainTimeObject time2 = (JSTemporalPlainTimeObject) obj2;
            return JSTemporalPlainTime.compareTemporalTime(
                            time1.getHours(), time1.getMinutes(), time1.getSeconds(),
                            time1.getMilliseconds(), time1.getMicroseconds(), time1.getNanoseconds(),
                            time2.getHours(), time2.getMinutes(), time2.getSeconds(),
                            time2.getMilliseconds(), time2.getMicroseconds(), time2.getNanoseconds());
        }

        @Specialization(guards = "!isJSTemporalTime(obj1) || !isJSTemporalTime(obj2)")
        protected int cantCompare(@SuppressWarnings("unused") Object obj1, @SuppressWarnings("unused") Object obj2) {
            throw Errors.createTypeErrorTemporalTimeExpected();
        }

    }

}

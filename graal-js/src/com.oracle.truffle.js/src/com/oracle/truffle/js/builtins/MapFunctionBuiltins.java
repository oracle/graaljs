/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.MapFunctionBuiltinsFactory.MapGroupByNodeGen;
import com.oracle.truffle.js.nodes.access.GroupByNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSMapObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.JSHashMap;
import java.util.List;
import java.util.Map;

public class MapFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<MapFunctionBuiltins.MapFunction> {
    public static final JSBuiltinsContainer BUILTINS = new MapFunctionBuiltins();

    protected MapFunctionBuiltins() {
        super(JSMap.CLASS_NAME, MapFunction.class);
    }

    public enum MapFunction implements BuiltinEnum<MapFunction> {
        // staging
        groupBy(2);

        private final int length;

        MapFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (this == groupBy) {
                return JSConfig.ECMAScript2024;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, MapFunction builtinEnum) {
        switch (builtinEnum) {
            case groupBy:
                return MapGroupByNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class MapGroupByNode extends JSBuiltinNode {

        public MapGroupByNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSObject groupBy(Object items, Object callbackfn,
                        @Cached("create(getContext(), false)") GroupByNode groupByNode) {
            Map<Object, List<Object>> groups = groupByNode.execute(items, callbackfn);
            JSMapObject map = JSMap.create(getContext(), getRealm());
            setGroups(map, groups);
            return map;
        }

        @TruffleBoundary
        protected void setGroups(JSMapObject map, Map<Object, List<Object>> groups) {
            JSHashMap internalMap = JSMap.getInternalMap(map);
            JSRealm realm = getRealm();
            JSContext context = getContext();
            for (Map.Entry<Object, List<Object>> entry : groups.entrySet()) {
                JSArrayObject elements = JSArray.createConstant(context, realm, entry.getValue().toArray());
                internalMap.put(entry.getKey(), elements);
            }
        }

    }

}

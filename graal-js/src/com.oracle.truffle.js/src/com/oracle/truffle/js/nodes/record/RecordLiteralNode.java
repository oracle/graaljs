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

package com.oracle.truffle.js.nodes.record;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.function.FunctionNameHolder;
import com.oracle.truffle.js.nodes.function.SetFunctionNameNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RecordLiteralNode extends JavaScriptNode {

    protected final JSContext context;

    @Children protected final AbstractRecordLiteralMemberNode[] elements;

    protected RecordLiteralNode(JSContext context, AbstractRecordLiteralMemberNode[] elements) {
        this.context = context;
        this.elements = elements;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Map<String, Object> entries = new TreeMap<>();
        for (AbstractRecordLiteralMemberNode element : elements) {
            element.evaluate(frame, entries, context);
        }
        return Null.instance; // TODO: create Record primitive
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == LiteralTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor(LiteralTag.TYPE, LiteralTag.Type.RecordLiteral.name());
    }

    public static JavaScriptNode create(JSContext context, AbstractRecordLiteralMemberNode[] elements) {
        return new RecordLiteralNode(context, elements);
    }

    public static AbstractRecordLiteralMemberNode createMember(String keyName, JavaScriptNode value) {
        return new RecordLiteralMemberNode(keyName, value);
    }

    public static AbstractRecordLiteralMemberNode createComputedMember(JavaScriptNode key, JavaScriptNode value) {
        return new ComputedRecordLiteralMemberNode(key, value);
    }

    public static AbstractRecordLiteralMemberNode createSpreadMember(JavaScriptNode node) {
        return new SpreadRecordLiteralMemberNode(node);
    }

    public abstract static class AbstractRecordLiteralMemberNode extends JavaScriptBaseNode {

        public abstract void evaluate(VirtualFrame frame, Map<String, Object> entries, JSContext context);

        /**
         * Records & Tuples Proposal: 3.3.1 AddPropertyIntoRecordEntriesList
         *
         * <pre>
         * 1. Assert: Type(entries) is List.
         * 2. Assert: IsProperyKey(propName) is true.
         * 3. If Type(propName) is Symbol, throw a TypeError exception.
         * 4. If Type(value) is Object, throw a TypeError exception.
         * 5. Add { [[Key]]: propType, [[Value]]: value } to entries.
         * 6. Return entries.
         * </pre>
         */
        protected final void addEntry(Map<String, Object> entries, Object key, Object value) {
            assert JSRuntime.isPropertyKey(key);
            if (key instanceof Symbol) {
                throw Errors.createTypeError("Record may only have string as keys");
            }
            assert key instanceof String;
            if (!JSRuntime.isJSPrimitive(value)) {
                throw Errors.createTypeError("Record may only contain primitive values");
            }
            entries.put((String) key, value);
        }

        protected boolean isAnonymousFunctionDefinition(JavaScriptNode expression) {
            return expression instanceof FunctionNameHolder && ((FunctionNameHolder) expression).isAnonymous();
        }

        protected abstract AbstractRecordLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags);

        public static AbstractRecordLiteralMemberNode[] cloneUninitialized(AbstractRecordLiteralMemberNode[] members, Set<Class<? extends Tag>> materializedTags) {
            AbstractRecordLiteralMemberNode[] copy = members.clone();
            for (int i = 0; i < copy.length; i++) {
                copy[i] = copy[i].copyUninitialized(materializedTags);
            }
            return copy;
        }
    }

    private static class RecordLiteralMemberNode extends AbstractRecordLiteralMemberNode {

        protected final Object name;

        @Child protected JavaScriptNode valueNode;

        RecordLiteralMemberNode(Object name, JavaScriptNode valueNode) {
            this.name = name;
            this.valueNode = valueNode;
        }

        /**
         * Records & Tuples Proposal: 7.1.1.4 Runtime Semantics: RecordPropertyDefinitionEvaluation
         */
        @Override
        public void evaluate(VirtualFrame frame, Map<String, Object> entries, JSContext context) {
            Object value = valueNode.execute(frame);
            addEntry(entries, name, value);
        }

        @Override
        protected AbstractRecordLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new RecordLiteralMemberNode(name, JavaScriptNode.cloneUninitialized(valueNode, materializedTags));
        }
    }

    private static class ComputedRecordLiteralMemberNode extends AbstractRecordLiteralMemberNode {

        @Child private JavaScriptNode keyNode;
        @Child private JavaScriptNode valueNode;
        @Child private SetFunctionNameNode setFunctionName;

        ComputedRecordLiteralMemberNode(JavaScriptNode keyNode, JavaScriptNode valueNode) {
            this.keyNode = keyNode;
            this.valueNode = valueNode;
            this.setFunctionName = isAnonymousFunctionDefinition(valueNode) ? SetFunctionNameNode.create() : null;
        }

        /**
         * Records & Tuples Proposal: 7.1.1.4 Runtime Semantics: RecordPropertyDefinitionEvaluation
         */
        @Override
        public void evaluate(VirtualFrame frame, Map<String, Object> entries, JSContext context) {
            Object key = keyNode.execute(frame);
            Object value = valueNode.execute(frame);
            if (setFunctionName != null) {
                setFunctionName.execute(value, key); // NamedEvaluation
            }
            addEntry(entries, key, value);
        }

        @Override
        protected AbstractRecordLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new ComputedRecordLiteralMemberNode(
                    JavaScriptNode.cloneUninitialized(keyNode, materializedTags),
                    JavaScriptNode.cloneUninitialized(valueNode, materializedTags)
            );
        }
    }

    private static class SpreadRecordLiteralMemberNode extends AbstractRecordLiteralMemberNode {

        @Child private JavaScriptNode valueNode;
        @Child private JSToObjectNode toObjectNode;
        @Child private ReadElementNode readElementNode;

        SpreadRecordLiteralMemberNode(JavaScriptNode valueNode) {
            this.valueNode = valueNode;
        }

        /**
         * Records & Tuples Proposal: 7.1.1.4 Runtime Semantics: RecordPropertyDefinitionEvaluation
         *
         * <pre>
         * RecordPropertyDefinition: ...AssignmentExpression
         *
         * 1. Let exprValue be the result of evaluating AssignmentExpression.
         * 2. Let source be ? GetValue(exprValue).
         * 3. If source is undefined or null, return entries.
         * 4. Let from be ! ToObject(source).
         * 5. Let keys be ? from.[[OwnPropertyKeys]]().
         * 6. For each element nextKey of keys in List order, do
         * 7. Let value be from.[[Get]](nextKey).
         * 9. Perform ? AddPropertyIntoRecordEntriesList(entries, nextKey, value).
         * 10. Return entries.
         * </pre>
         */
        @Override
        public void evaluate(VirtualFrame frame, Map<String, Object> entries, JSContext context) {
            Object source = valueNode.execute(frame);
            if (JSGuards.isNullOrUndefined(source)) {
                return;
            }
            Object from = toObject(source, context);

            List<Object> keys = JSObject.ownPropertyKeys((DynamicObject) from);
            // TODO: Why is ListSizeNode used for getting list.size() ?
            // TODO: Also ListGetNode for list.get(...) ?
            // TODO: see com.oracle.truffle.js.nodes.access.CopyDataPropertiesNode.copyDataProperties
            for (Object key : keys) {
                assert JSRuntime.isPropertyKey(key);
                Object value = get(from, key, context);
                addEntry(entries, key, value);

                // TODO: when spreading members in object literals, [[Enumerable]] gets checked before adding it
                // TODO: see com.oracle.truffle.js.nodes.access.CopyDataPropertiesNode.copyDataProperties
                // TODO: see sample implementation below
                // PropertyDescriptor desc = getOwnProperty.execute(source, key);
                // if (desc != null && desc.getEnumerable()) {
                //     Object value = get(from, key, context);
                //     addMember(entries, key, value);
                // }
                // TODO: see test case below
                // let a = [42];
                // let b = {...a}
                // let c = #{...a}
                // console.log(a.length); // "1"
                // console.log(b.length); // "undefined"
                // console.log(c.length); // "1" according to proposal spec BUT "undefined" according to proposal polyfill
            }
        }

        private Object toObject(Object obj, JSContext context) {
            if (toObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectNode = insert(JSToObjectNode.createToObjectNoCheck(context));
            }
            return toObjectNode.execute(obj);
        }

        private Object get(Object obj, Object key, JSContext context) {
            if (readElementNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readElementNode = insert(ReadElementNode.create(context));
            }
            return readElementNode.executeWithTargetAndIndex(obj, key);
        }

        @Override
        protected AbstractRecordLiteralMemberNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return new SpreadRecordLiteralMemberNode(
                    JavaScriptNode.cloneUninitialized(valueNode, materializedTags)
            );
        }
    }
}

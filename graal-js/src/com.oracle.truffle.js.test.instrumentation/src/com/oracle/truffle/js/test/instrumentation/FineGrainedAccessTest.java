/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.instrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.junit.After;
import org.junit.Before;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.ReturnException;
import com.oracle.truffle.js.nodes.control.YieldException;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.DeclareTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag.Type;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class FineGrainedAccessTest {

    private static final boolean DEBUG = false;

    protected static final String KEY = "key";
    protected static final String NAME = "name";
    protected static final String TYPE = "type";
    protected static final String DECL_NAME = DeclareTag.NAME;
    protected static final String DECL_TYPE = DeclareTag.TYPE;
    protected static final String LITERAL_TYPE = LiteralTag.TYPE;
    protected static final String OPERATOR = "operator";

    @SuppressWarnings("unchecked")
    public static final String getTagNames(JavaScriptNode node) {
        StringJoiner tags = new StringJoiner(" ");

        if (node.hasTag(StandardTags.StatementTag.class)) {
            tags.add("STMT");
        }
        if (node.hasTag(StandardTags.RootTag.class)) {
            tags.add("ROOT");
        }
        if (node.hasTag(StandardTags.RootBodyTag.class)) {
            tags.add("BODY");
        }
        if (node.hasTag(StandardTags.ExpressionTag.class)) {
            tags.add("EXPR");
        }
        for (Class<?> c : JSTags.ALL) {
            if (node.hasTag((Class<? extends Tag>) c)) {
                tags.add(c.getSimpleName());
            }
        }
        return tags.toString();
    }

    protected Context context;
    private boolean collecting;
    private List<Event> events;
    private Deque<JavaScriptNode> stack;
    protected Instrumenter instrumenter;
    protected TestingExecutionInstrument instrument;
    private ExecutionEventNodeFactory factory;
    protected EventBinding<ExecutionEventNodeFactory> binding;

    protected static class Event {
        enum Kind {
            INPUT,
            RETURN,
            ENTER,
            RETURN_EXCEPTIONAL,
            UNEXPECTED_STATE,
        }

        protected final Kind kind;
        protected final Object val;
        protected final JavaScriptNode instrumentedNode;
        protected final EventContext context;
        protected final Object[] others;

        public Event(EventContext context, Kind kind, JavaScriptNode instrumentedNode, Object inputValue, Object... others) {
            if (DEBUG) {
                System.out.println("New event: " + kind + " === " + inputValue + "  === " + instrumentedNode.getClass().getSimpleName());
            }
            this.context = context;
            this.kind = kind;
            this.val = inputValue;
            this.instrumentedNode = instrumentedNode;
            this.others = others;
        }

        @Override
        public String toString() {
            return kind.name() + " " + val;
        }
    }

    private Event getNextEvent() {
        assertFalse("empty queue!", events.isEmpty());
        return events.remove(0);
    }

    static class AssertedEvent {

        private final Class<? extends Tag> tag;
        private final FineGrainedAccessTest test;

        AssertedEvent(FineGrainedAccessTest test, Class<? extends Tag> tag) {
            this.tag = tag;
            this.test = test;
        }

        AssertedEvent input(Object value) {
            Event event = test.getNextEvent();
            assertKindTag(Event.Kind.INPUT, tag, event);
            if (value instanceof Number) {
                assertTrue(event.val instanceof Number);
                assertEquals(((Number) value).doubleValue(), ((Number) event.val).doubleValue(), 0);
            } else {
                assertEquals(value, event.val);
            }
            return this;
        }

        AssertedEvent input() {
            Event event = test.getNextEvent();
            assertKindTag(Event.Kind.INPUT, tag, event);
            return this;
        }

        AssertedEvent input(Consumer<Event> verify) {
            Event event = test.getNextEvent();
            assertKindTag(Event.Kind.INPUT, tag, event);
            verify.accept(event);
            return this;
        }

        void exit() {
            Event event = test.getNextEvent();
            assertKindTag(Event.Kind.RETURN, tag, event);
        }

        void exitExceptional() {
            Event event = test.getNextEvent();
            assertKindTag(Event.Kind.RETURN_EXCEPTIONAL, tag, event);
        }

        void exitMaybeControlFlowException() {
            Event event = test.getNextEvent();
            if (event.kind == Event.Kind.RETURN) {
                // OK
                assertKindTag(Event.Kind.RETURN, tag, event);
            } else if (event.kind == Event.Kind.RETURN_EXCEPTIONAL) {
                assertKindTag(Event.Kind.RETURN_EXCEPTIONAL, tag, event);
                assert event.val instanceof YieldException || event.val instanceof ReturnException : event.val;
            } else {
                assert false;
            }
        }

        void exit(Consumer<Event> verify) {
            Event event = test.getNextEvent();
            assertKindTag(Event.Kind.RETURN, tag, event);
            verify.accept(event);
        }

    }

    private static void assertKindTag(Event.Kind kind, Class<? extends Tag> tag, Event event) {
        assertTrue("expected " + kind.name() + " " + tag.getSimpleName() + ", actual [" + getTagNames(event.instrumentedNode) + ", " + event.kind + "]",
                        event.instrumentedNode.hasTag(tag) && kind.equals(event.kind));
    }

    protected void enterDeclareTag(String expectedVarName) {
        enter(DeclareTag.class, (e, c) -> {
            assertAttribute(e, DECL_NAME, expectedVarName);
        }).exit();
    }

    protected AssertedEvent enter(Class<? extends Tag> tag) {
        Event event = getNextEvent();
        assertKindTag(Event.Kind.ENTER, tag, event);
        return new AssertedEvent(this, tag);
    }

    protected AssertedEvent enter(Class<? extends Tag> tag, Consumer<Event> verify) {
        Event event = getNextEvent();
        assertKindTag(Event.Kind.ENTER, tag, event);
        verify.accept(event);
        return new AssertedEvent(this, tag);
    }

    protected AssertedEvent enter(Class<? extends Tag> tag, BiConsumer<Event, AssertedEvent> verify) {
        Event event = getNextEvent();
        assertKindTag(Event.Kind.ENTER, tag, event);
        AssertedEvent chain = new AssertedEvent(this, tag);
        verify.accept(event, chain);
        return chain;
    }

    protected ExecutionEventNodeFactory getTestFactory() {
        return new ExecutionEventNodeFactory() {

            private Deque<Integer> inputEvents = new ArrayDeque<>();

            @Override
            public ExecutionEventNode create(EventContext c) {
                return new ExecutionEventNode() {

                    @Override
                    public void onEnter(VirtualFrame frame) {
                        /*
                         * Internal sources are executed at engine startup time. Such sources
                         * include internal code for the registration of builtins like Promise. We
                         * skip all these internal events to ensure that tests are deterministic.
                         */
                        if (!collecting && c.getInstrumentedSourceSection().getSource().isInternal()) {
                            return;
                        } else if (!collecting && !c.getInstrumentedSourceSection().getSource().isInternal()) {
                            /*
                             * as soon as we see a non-internal source, we start collecting events
                             * for all available sources. This ensures that we can trace all
                             * internal events (like builtin calls) and ensures that we trace
                             * interesting tagged internal events, but we do not trace other events
                             * (e.g. those coming from de-sugared internal nodes).
                             */
                            collecting = true;
                        }
                        inputEvents.push(0);
                        events.add(new Event(c, Event.Kind.ENTER, (JavaScriptNode) c.getInstrumentedNode(), null));
                        stack.push((JavaScriptNode) c.getInstrumentedNode());
                    }

                    @Override
                    protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
                        if (!collecting) {
                            return;
                        }
                        saveInputValue(frame, inputIndex, inputValue);
                        events.add(new Event(c, Event.Kind.INPUT, (JavaScriptNode) c.getInstrumentedNode(), inputValue, inputIndex));
                        inputEvents.push(inputEvents.pop() + 1);

                    }

                    @Override
                    protected void onReturnValue(VirtualFrame frame, Object result) {
                        if (!collecting) {
                            return;
                        }
                        Object[] values = getSavedInputValues(frame);
                        assertTrue(values != null);

                        if (values.length > 0) {
                            Object[] newValues = new Object[values.length + 1];
                            System.arraycopy(values, 0, newValues, 1, values.length);
                            newValues[0] = result;
                            events.add(new Event(c, Event.Kind.RETURN, (JavaScriptNode) c.getInstrumentedNode(), newValues));
                        } else {
                            events.add(new Event(c, Event.Kind.RETURN, (JavaScriptNode) c.getInstrumentedNode(), new Object[]{result}));
                        }
                        stack.pop();
                        int expectedEvents = inputEvents.pop();
                        if (!c.hasTag(ControlFlowRootTag.class) && !c.hasTag(JSTags.ControlFlowBranchTag.class)) {
                            /*
                             * Iterations may detect more input events than expected, other event
                             * types should not.
                             */
                            if (expectedEvents != values.length) {
                                events.add(new Event(c, Event.Kind.UNEXPECTED_STATE, (JavaScriptNode) c.getInstrumentedNode(), expectedEvents + " != " + values.length));
                            }
                        }
                    }

                    @Override
                    protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
                        if (!collecting) {
                            return;
                        }
                        events.add(new Event(c, Event.Kind.RETURN_EXCEPTIONAL, (JavaScriptNode) c.getInstrumentedNode(), exception));
                        stack.pop();
                        inputEvents.pop();
                    }
                };
            }
        };
    }

    protected static void assertAttribute(Event e, String attribute, Object expected) {
        Object val = getAttributeFrom(e.context, attribute);
        assertEquals(expected, val);
    }

    public static Object getAttributeFrom(EventContext cx, String name) {
        try {
            return InteropLibrary.getFactory().getUncached().readMember(((InstrumentableNode) cx.getInstrumentedNode()).getNodeObject(), name);
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            throw new RuntimeException(e);
        }
    }

    protected Source evalWithTag(String src, Class<? extends Tag> tag) {
        return evalWithTags(src, new Class[]{tag});
    }

    protected Source evalAllTags(String src) {
        return evalWithTags(src, JSTags.ALL);
    }

    protected Source evalWithTags(String src, Class<?>[] filterTags) {
        return evalWithTags(src, filterTags, new Class[]{StandardTags.ExpressionTag.class, JSTags.InputNodeTag.class});
    }

    protected Source eval(String src) {
        Source source = Source.create("js", src);
        context.eval(source);
        return source;
    }

    protected Source evalWithTags(String src, Class<?>[] sourceSectionTags, Class<?>[] inputGeneratingTags) {
        binding = initAgent(sourceSectionTags, inputGeneratingTags);
        Source source = Source.create("js", src);
        evalWithCurrentBinding(source);
        return source;
    }

    protected Source evalWithCurrentBinding(Source source) {
        context.eval(source);
        return source;
    }

    protected Source evalWithTags(Source source, Class<?>[] sourceSectionTags, Class<?>[] inputGeneratingTags) {
        binding = initAgent(sourceSectionTags, inputGeneratingTags);
        context.eval(source);
        return source;
    }

    protected Source evalWithNewTags(Source source, Class<?>[] sourceSectionTags, Class<?>[] inputGeneratingTags) {
        binding.dispose();
        return evalWithTags(source, sourceSectionTags, inputGeneratingTags);
    }

    protected void declareInteropSymbol(String name, ForeignTestObject foreignObject) {
        context.getPolyglotBindings().putMember(name, foreignObject);
    }

    @After
    public void disposeAgent() {
        assertTrue(events.toString(), events.isEmpty());
        assertTrue(stack.toString(), stack.isEmpty());
        context.leave();
        events.clear();
        binding.dispose();
    }

    @Before
    public void initTest() {
        collecting = false;
        context = TestUtil.newContextBuilder().allowPolyglotAccess(PolyglotAccess.ALL).option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2021").build();
        instrument = context.getEngine().getInstruments().get(TestingExecutionInstrument.ID).lookup(TestingExecutionInstrument.class);
        instrumenter = instrument.getEnvironment().getInstrumenter();
        events = new ArrayList<>();
        stack = new ArrayDeque<>();
        factory = getTestFactory();
        context.enter();
    }

    private EventBinding<ExecutionEventNodeFactory> initAgent(Class<?>[] sourceSectionTags, Class<?>[] inputGeneratingTags) {
        SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().tagIs(sourceSectionTags).build();
        SourceSectionFilter inputGeneratingFilter = SourceSectionFilter.newBuilder().tagIs(inputGeneratingTags).build();
        return instrumenter.attachExecutionEventFactory(sourceSectionFilter, inputGeneratingFilter, factory);
    }

    // === common asserts

    protected static final Consumer<Event> assertReturnValue(Object expected) {
        return e -> {
            assertTrue(e.val instanceof Object[]);
            Object[] vals = (Object[]) e.val;
            assertEquals(vals[0], expected);
        };
    }

    protected static final Consumer<Event> assertLiteralType(LiteralTag.Type type) {
        return e -> {
            assertAttribute(e, LITERAL_TYPE, type.name());
        };
    }

    protected static final Consumer<Event> assertPropertyReadName(String name) {
        Function<String, Consumer<Event>> fun = (e) -> {
            return (x) -> {
                assertAttribute(x, KEY, e);
            };
        };
        return fun.apply(name);
    }

    protected static final Consumer<Event> assertVarReadName(String name) {
        Function<String, Consumer<Event>> fun = (e) -> {
            return (x) -> {
                assertAttribute(x, NAME, e);
            };
        };
        return fun.apply(name);
    }

    protected static final Consumer<Event> assertJSObjectInput = (e) -> {
        assertTrue(!JSFunction.isJSFunction(e.val));
        assertTrue(!JSArray.isJSArray(e.val));
        assertTrue(JSDynamicObject.isJSDynamicObject(e.val));
    };

    protected static final Consumer<Event> assertJSPromiseInput = (e) -> {
        assertTrue(!JSFunction.isJSFunction(e.val));
        assertTrue(!JSArray.isJSArray(e.val));
        assertTrue(JSDynamicObject.isJSDynamicObject(e.val));
        assertTrue(JSPromise.isJSPromise(e.val));
    };

    protected static final Consumer<Event> assertTruffleObject = (e) -> {
        assertTrue(e.val instanceof TruffleObject);
    };

    protected static final Consumer<Event> assertJSArrayInput = (e) -> {
        assertTrue(JSDynamicObject.isJSDynamicObject(e.val));
        assertTrue(JSArray.isJSArray(e.val));
    };

    protected static final Consumer<Event> assertUndefinedInput = (e) -> {
        assertEquals(e.val, Undefined.instance);
    };

    protected static final Consumer<Event> assertGlobalObjectInput = (e) -> {
        assertTrue(JSDynamicObject.isJSDynamicObject(e.val));
        DynamicObject globalObject = JavaScriptLanguage.getCurrentJSRealm().getGlobalObject();
        assertEquals(globalObject, e.val);
    };

    protected static final Consumer<Event> assertJSFunctionInput = (e) -> {
        assertTrue(JSFunction.isJSFunction(e.val));
    };

    protected static Consumer<Event> assertJSFunctionInputWithName(String expectedFunctionName) {
        return (e) -> {
            assertTrue(JSFunction.isJSFunction(e.val));
            assertTrue(JSFunction.getName((DynamicObject) e.val).equals(expectedFunctionName));
        };
    }

    protected static final Consumer<Event> assertJSFunctionReturn = (e) -> {
        assertTrue(e.val instanceof Object[]);
        Object[] vals = (Object[]) e.val;
        assertTrue(JSFunction.isJSFunction(vals[0]));
    };

    protected static final Consumer<Event> assertJSObjectReturn = (e) -> {
        assertTrue(e.val instanceof Object[]);
        Object[] vals = (Object[]) e.val;
        assertTrue(JSDynamicObject.isJSDynamicObject(vals[0]));
        assertTrue(vals[0] != Undefined.instance);
        assertFalse(JSFunction.isJSFunction(vals[0]));
    };

    protected void assertGlobalVarDeclaration(String name, Object value) {
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, name);
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class, (e2) -> {
                if (value instanceof Integer) {
                    assertAttribute(e2, LITERAL_TYPE, Type.NumericLiteral.name());
                } else if (value instanceof Boolean) {
                    assertAttribute(e2, LITERAL_TYPE, Type.BooleanLiteral.name());
                } else if (value instanceof String) {
                    assertAttribute(e2, LITERAL_TYPE, Type.StringLiteral.name());
                }
            }).exit();
            write.input(value);
        }).exit();
    }

    protected void assertGlobalFunctionExpressionDeclaration(String name) {
        enter(WritePropertyTag.class, (e, write) -> {
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class).exit((e1) -> {
                assertAttribute(e1, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
                Object[] results = (Object[]) e1.val;
                assertTrue(results.length == 1);
                assertTrue(JSFunction.isJSFunction(results[0]));
            });
            assertAttribute(e, KEY, name);
            write.input(assertJSFunctionInput);
        }).exit();
    }

    protected void assertGlobalArrayLiteralDeclaration(String name) {
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, name);
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class).exit();
            write.input(assertJSArrayInput);
        }).exit();
    }

}

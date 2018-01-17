package com.oracle.truffle.js.jalangilike.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.graalvm.polyglot.Context;
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
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.jalangilike.MyBasicExecutionTracer;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.ConditionalExpressionTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.ElementReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.ElementWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.EvalCallTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.ObjectAllocationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag.Type;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class FineGrainedAccessTest {

    private static final boolean DEBUG = false;

    protected static final String KEY = "key";
    protected static final String NAME = "name";
    protected static final String TYPE = "type";

    public static final Class<?>[] allJSSpecificTags = new Class[]{
                    ObjectAllocationTag.class,
                    BinaryOperationTag.class,
                    UnaryOperationTag.class,
                    ConditionalExpressionTag.class,
                    VariableWriteTag.class,
                    ElementReadTag.class,
                    ElementWriteTag.class,
                    PropertyReadTag.class,
                    PropertyWriteTag.class,
                    VariableReadTag.class,
                    LiteralTag.class,
                    FunctionCallTag.class,
                    BuiltinRootTag.class,
                    EvalCallTag.class
    };

    @SuppressWarnings("unchecked")
    public static final String getTagNames(JavaScriptNode node) {
        String tags = "";
        for (Class<?> c : allJSSpecificTags) {
            if (node.hasTag((Class<? extends Tag>) c)) {
                tags += c.getSimpleName() + " ";
            }
        }
        return tags;
    }

    protected Context context;
    protected ArrayList<Event> events;
    protected Stack<JavaScriptNode> stack;

    protected Instrumenter instrumenter;
    protected MyBasicExecutionTracer instrument;
    protected ExecutionEventNodeFactory factory;

    protected static class Event {
        public static enum Kind {
            INPUT,
            RETURN,
            ENTER,
        }

        protected final Object val;
        protected final Object kind;
        protected final JavaScriptNode instrumentedNode;
        protected final EventContext context;

        public Event(EventContext context, Kind kind, JavaScriptNode instrumentedNode, Object inputValue) {
            if (DEBUG) {
                System.out.println("New event: " + kind + " === " + inputValue + "  === " + instrumentedNode.getClass().getSimpleName());
            }
            this.context = context;
            this.kind = kind;
            this.val = inputValue;
            this.instrumentedNode = instrumentedNode;
        }
    }

    protected void assertOn(Event.Kind expectedKind, Class<? extends Tag> expectedTag, Consumer<Event> verify) {
        Event event = getNextEvent();
        assertEquals(expectedKind, event.kind);
        assertTrue(event.instrumentedNode.hasTag(expectedTag));
        verify.accept(event);
    }

    protected void assertOn(Event.Kind expectedKind, Class<? extends Tag> expectedTag, Object expected) {
        Event event = getNextEvent();
        assertEquals(expectedKind, event.kind);
        assertTrue(event.instrumentedNode.hasTag(expectedTag));
        Consumer<Event> verify = (e) -> {
            assertEquals(e.val, expected);
        };
        verify.accept(event);
    }

    protected void assertOn(Event.Kind expectedKind) {
        Event event = getNextEvent();
        assertEquals(expectedKind, event.kind);
    }

    protected void assertOn(Event.Kind expectedKind, Consumer<Event> verify) {
        Event event = getNextEvent();
        assertEquals(expectedKind, event.kind);
        verify.accept(event);
    }

    private Event getNextEvent() {
        assertFalse("empty queue!", events.isEmpty());
        Event event = events.remove(0);
        return event;
    }

    protected void assertEngineInit() {
        // By default, we perform some operations to load Promises.
        enter(PropertyReadTag.class).input().exit();
        enter(PropertyReadTag.class).input().exit();
        for (int i = 0; i < 4; i++) {
            assertOn(Event.Kind.ENTER, BuiltinRootTag.class);
            assertOn(Event.Kind.RETURN, BuiltinRootTag.class, (e) -> {
                assertAttribute(e, NAME, "Object.create");
            });
        }
    }

    static class AssertedEvent {

        private final Class<? extends Tag> tag;
        private final FineGrainedAccessTest test;

        public AssertedEvent(FineGrainedAccessTest test, Class<? extends Tag> tag) {
            this.tag = tag;
            this.test = test;
        }

        AssertedEvent input(Object value) {
            Event event = test.getNextEvent();
            assertTrue(event.instrumentedNode.hasTag(tag));
            assertEquals(event.kind, Event.Kind.INPUT);
            assertEquals(event.val, value);
            return this;
        }

        AssertedEvent input(Class<? extends Tag> expectedTag) {
            Event event = test.getNextEvent();
            assertTrue(event.instrumentedNode.hasTag(expectedTag));
            assertEquals(event.kind, Event.Kind.INPUT);
            return this;
        }

        AssertedEvent input() {
            Event event = test.getNextEvent();
            assertTrue(event.instrumentedNode.hasTag(tag));
            assertEquals(event.kind, Event.Kind.INPUT);
            return this;
        }

        AssertedEvent input(Consumer<Event> verify) {
            Event event = test.getNextEvent();
            assertTrue(event.instrumentedNode.hasTag(tag));
            assertEquals(event.kind, Event.Kind.INPUT);
            verify.accept(event);
            return this;
        }

        AssertedEvent input(Class<? extends Tag> expectedTag, Consumer<Event> verify) {
            Event event = test.getNextEvent();
            assertTrue(event.instrumentedNode.hasTag(expectedTag));
            assertEquals(event.kind, Event.Kind.INPUT);
            verify.accept(event);
            return this;
        }

        void exit() {
            Event event = test.getNextEvent();
            assertTrue(event.instrumentedNode.hasTag(tag));
            assertEquals(event.kind, Event.Kind.RETURN);
        }

        void exit(Consumer<Event> verify) {
            Event event = test.getNextEvent();
            assertTrue(event.instrumentedNode.hasTag(tag));
            assertEquals(event.kind, Event.Kind.RETURN);
            verify.accept(event);
        }
    }

    protected AssertedEvent enter(Class<? extends Tag> tag) {
        Event event = getNextEvent();
        assertTrue(event.instrumentedNode.hasTag(tag));
        assertEquals(event.kind, Event.Kind.ENTER);
        return new AssertedEvent(this, tag);
    }

    protected AssertedEvent enter(Class<? extends Tag> tag, Consumer<Event> verify) {
        Event event = getNextEvent();
        assertTrue(event.instrumentedNode.hasTag(tag));
        assertEquals(event.kind, Event.Kind.ENTER);
        verify.accept(event);
        return new AssertedEvent(this, tag);
    }

    protected AssertedEvent enter(Class<? extends Tag> tag, BiConsumer<Event, AssertedEvent> verify) {
        Event event = getNextEvent();
        assertTrue(event.instrumentedNode.hasTag(tag));
        assertEquals(event.kind, Event.Kind.ENTER);
        AssertedEvent chain = new AssertedEvent(this, tag);
        verify.accept(event, chain);
        return chain;
    }

    protected void assertOn(Event.Kind expectedKind, Class<? extends Tag> expectedTag) {
        Event event = getNextEvent();
        assertTrue(event.instrumentedNode.hasTag(expectedTag));
        assertEquals(expectedKind, event.kind);
    }

    protected ExecutionEventNodeFactory getTestFactory() {
        return new ExecutionEventNodeFactory() {

            @Override
            public ExecutionEventNode create(EventContext c) {
                return new ExecutionEventNode() {

                    @Override
                    public void onEnter(VirtualFrame frame) {
                        events.add(new Event(c, Event.Kind.ENTER, (JavaScriptNode) c.getInstrumentedNode(), null));
                        stack.push((JavaScriptNode) c.getInstrumentedNode());
                    }

                    @Override
                    protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
                        events.add(new Event(c, Event.Kind.INPUT, (JavaScriptNode) c.getInstrumentedNode(), inputValue));
                        saveInputValue(frame, inputIndex, inputValue);
                    }

                    @Override
                    protected void onReturnValue(VirtualFrame frame, Object result) {
                        Object[] values = getSavedInputValues(frame);
                        assert values != null;
                        if (values.length > 0) {
                            Object[] newValues = new Object[values.length + 1];
                            System.arraycopy(values, 0, newValues, 1, values.length);
                            newValues[0] = result;
                            events.add(new Event(c, Event.Kind.RETURN, (JavaScriptNode) c.getInstrumentedNode(), newValues));
                        } else {
                            events.add(new Event(c, Event.Kind.RETURN, (JavaScriptNode) c.getInstrumentedNode(), new Object[]{result}));
                        }
                        assert stack.pop() == c.getInstrumentedNode();
                    }
                };
            }
        };
    }

    protected static void assertAttribute(Event e, String attribute, Object expected) {
        Object val = getAttributeFrom(e.context, attribute);
        assertEquals(expected, val);
    }

    public static String getAttributeFrom(EventContext cx, String name) {
        try {
            return (String) ForeignAccess.sendRead(Message.READ.createNode(), (TruffleObject) ((InstrumentableNode) cx.getInstrumentedNode()).getNodeObject(), name);
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void init() {
        this.context = Context.create("js");
        this.instrument = context.getEngine().getInstruments().get(MyBasicExecutionTracer.ID).lookup(MyBasicExecutionTracer.class);
        this.instrumenter = instrument.environment.getInstrumenter();
        this.events = new ArrayList<>();
        this.stack = new Stack<>();
        this.factory = getTestFactory();
    }

    protected void disposeAgent(EventBinding<ExecutionEventNodeFactory> binding) {
        assertTrue(events.isEmpty());
        assertTrue(stack.isEmpty());
        events.clear();
        binding.dispose();
    }

    protected EventBinding<ExecutionEventNodeFactory> initAgent(Class<?>[] filterTags) {
        SourceSectionFilter expressionFilter = SourceSectionFilter.newBuilder().tagIs(filterTags).build();
        SourceSectionFilter inputFilter = SourceSectionFilter.newBuilder().tagIs(
                        StandardTags.ExpressionTag.class
        //
        ).build();
        EventBinding<ExecutionEventNodeFactory> binding = instrumenter.attachExecutionEventFactory(expressionFilter, inputFilter, factory);
        return binding;
    }

    // === common asserts

    protected static final Consumer<Event> assertReturnValue(Object expected) {
        Consumer<Event> c = (e) -> {
            assertTrue(e.val instanceof Object[]);
            Object[] vals = (Object[]) e.val;
            assertEquals(vals[0], expected);
        };
        return c;
    }

    protected static final Consumer<Event> assertLiteralType(LiteralTag.Type type) {
        Consumer<Event> c = (e) -> {
            assertAttribute(e, TYPE, type.name());
        };
        return c;
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
        assertTrue(JSObject.isJSObject(e.val));
    };

    protected static final Consumer<Event> assertJSArrayInput = (e) -> {
        assertTrue(JSObject.isJSObject(e.val));
        assertTrue(JSArray.isJSArray(e.val));
    };

    protected static final Consumer<Event> assertUndefinedInput = (e) -> {
        assertEquals(e.val, Undefined.instance);
    };

    protected static final Consumer<Event> assertGlobalObjectInput = (e) -> {
        assertTrue(JSObject.isJSObject(e.val));
        DynamicObject globalObject = JSObject.getJSContext((DynamicObject) e.val).getRealm().getGlobalObject();
        assertEquals(globalObject, e.val);
    };

    protected static final Consumer<Event> assertJSFunctionInput = (e) -> {
        assertTrue(JSFunction.isJSFunction(e.val));
    };

    protected void assertGlobalVarDeclaration(String name, Object value) {
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, name);
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class, (e2) -> {
                if (value instanceof Integer) {
                    assertAttribute(e2, TYPE, Type.NumericLiteral.name());
                } else if (value instanceof Boolean) {
                    assertAttribute(e2, TYPE, Type.BooleanLiteral.name());
                } else if (value instanceof String) {
                    assertAttribute(e2, TYPE, Type.StringLiteral.name());
                }
            }).exit();
            write.input(value);
        }).exit();
    }

    protected void assertGlobalFunctionExpressionDeclaration(String name) {
        enter(PropertyWriteTag.class, (e, write) -> {
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class).exit((e1) -> {
                assertAttribute(e1, TYPE, LiteralTag.Type.FunctionLiteral.name());
                Object[] results = (Object[]) e1.val;
                assertTrue(results.length == 1);
                assertTrue(JSFunction.isJSFunction(results[0]));
            });
            assertAttribute(e, KEY, name);
            write.input(assertJSFunctionInput);
        }).exit();
    }

    protected void assertGlobalArrayLiteralDeclaration(String name) {
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, name);
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class).exit();
            write.input(assertJSArrayInput);
        }).exit();
    }

}

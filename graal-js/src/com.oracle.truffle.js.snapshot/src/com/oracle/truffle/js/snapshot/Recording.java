/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.snapshot;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.codec.BinaryEncoder;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.control.BreakTarget;
import com.oracle.truffle.js.nodes.control.ContinueTarget;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.parser.BinarySnapshotProvider;
import com.oracle.truffle.js.parser.SnapshotProvider;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.parser.json.JSONParser;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class Recording {
    private static final boolean VERBOSE = false;
    private static final boolean BATCHES_ENABLED = true;
    private static final boolean LAZY_FUNCTIONS = true;
    private static final boolean SORT_BY_ID = false;
    private static final boolean EARLY_FIXUP = true;
    private static final boolean CONST_IN_VAR = true;
    private static final boolean SOURCE_SECTIONS = true;
    private static final boolean FIXUP_SOURCE_SECTIONS = false;
    private static final boolean FIXUP_TAGS = false;
    private static final boolean TEST_DECODE = true;
    private static final boolean LAMBDA = true;

    private static final String ENTRY_METHOD_NAME = "apply";
    private static final String EXTRACTED_METHOD_NAME_PREFIX = "function";

    private final VarIdTable table = new VarIdTable();
    private final Map<Integer, Inst> defs = new HashMap<>();
    private final ArrayList<Inst> insts = new ArrayList<>();
    private final ArrayDeque<MethodCall> callStack = new ArrayDeque<>();
    private final Set<FrameDescriptor> frameDescriptorSet = new LinkedHashSet<>();
    private final Set<JSFunctionData> functionDataSet = new LinkedHashSet<>();
    private final ArrayDeque<BooleanSupplier> fixups = new ArrayDeque<>();

    private final Map<Inst, Collection<Inst>> usageMap = new HashMap<>();
    private final Map<Inst, Integer> indexMap = new HashMap<>();
    private final List<InstBatch> instBatches = new ArrayList<>();

    private Source source;

    private static final class MethodCall {
        final Method method;
        final Object[] args;

        MethodCall(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }

    private static class InstBatch {
        final List<Inst> insts;
        final String name;
        final List<Inst> inputs;
        final Class<?> outputType;

        InstBatch(List<Inst> insts, String name, List<Inst> inputs, Class<?> outputType) {
            this.insts = insts;
            this.name = name;
            this.inputs = inputs;
            this.outputType = outputType;
        }
    }

    private abstract static class Inst {
        private static final int ROOT_ID = -1;
        private static final int UNASSIGNED_ID = -2;

        interface Visitor {
            default void enterInst(@SuppressWarnings("unused") Inst inst) {
            }

            boolean visitInst(Inst inst);

            default void leaveInst(@SuppressWarnings("unused") Inst inst) {
            }
        }

        private int resultId;
        private final Class<?> declaredType;
        private final Type genericDeclaredType;
        private int varCount;

        protected Inst() {
            this(ROOT_ID, null, null);
        }

        protected Inst(Class<?> declaredType) {
            this(declaredType, null);
        }

        protected Inst(Class<?> declaredType, Type genericDeclaredType) {
            this(UNASSIGNED_ID, declaredType, genericDeclaredType);
        }

        private Inst(int resultId, Class<?> declaredType, Type genericDeclaredType) {
            this.resultId = resultId;
            this.declaredType = declaredType;
            this.genericDeclaredType = genericDeclaredType;
        }

        @Override
        public String toString() {
            return declaredTypeName() + " " + "v" + resultId + " = " + rhs();
        }

        final String declaredTypeName() {
            if (getGenericDeclaredType() != null) {
                return typeName(getGenericDeclaredType());
            }

            return typeName(getDeclaredType());
        }

        public abstract String rhs();

        public int getId() {
            if (resultId == UNASSIGNED_ID) {
                throw new IllegalStateException("result id not assigned");
            }
            return resultId;
        }

        public Class<?> getDeclaredType() {
            return declaredType;
        }

        public Type getGenericDeclaredType() {
            return genericDeclaredType;
        }

        public Inst asVar() {
            varCount++;
            return new VarInst(this);
        }

        public boolean inVar() {
            return true;
        }

        public boolean isRoot() {
            return getId() == ROOT_ID;
        }

        /**
         * @return {@code true} if this value should be duplicated/recreated when building batches
         *         rather than passed along as an argument.
         */
        public boolean isPrimitiveValue() {
            return false;
        }

        public void accept(Visitor v) {
            v.visitInst(this);
        }

        public final void forEachInput(Consumer<Inst> v) {
            accept(new Visitor() {
                private int level;

                @Override
                public void enterInst(Inst inst) {
                    level++;
                }

                @Override
                public boolean visitInst(Inst inst) {
                    v.accept(inst);
                    return level == 0;
                }

                @Override
                public void leaveInst(Inst inst) {
                    level--;
                }
            });
        }

        public void assignId(int id) {
            if (this.resultId != UNASSIGNED_ID) {
                throw new IllegalStateException("result id already assigned");
            }
            this.resultId = id;
        }

        @SuppressWarnings("unused")
        public int getVarCount() {
            return varCount;
        }

        @SuppressWarnings("unused")
        public void encodeTo(JSNodeEncoder encoder) {
            throw Errors.notYetImplemented(getClass().isAnonymousClass() ? getClass().getName() : getClass().getSimpleName());
        }

        public String getName() {
            return "";
        }
    }

    static boolean isAssignable(Class<?> toType, Class<?> fromType) {
        return toType == fromType || (toType == Object.class && fromType.isPrimitive()) || toType.isAssignableFrom(fromType);
    }

    static int[] toIdArray(Inst[] args) {
        return Arrays.stream(args).mapToInt(arg -> arg.getId()).toArray();
    }

    static int[] toIdArray(List<Inst> args) {
        return args.stream().mapToInt(arg -> arg.getId()).toArray();
    }

    private static class NodeInst extends Inst {
        private final Method method;
        private final Inst[] args;
        private final Object result;

        NodeInst(int id, Method method, Inst[] args, Object result) {
            super(id, method.getReturnType(), method.getGenericReturnType());
            this.method = method;
            this.args = args;
            this.result = result;
        }

        @Override
        public String rhs() {
            return "nodeFactory." + method.getName() + IntStream.range(0, args.length).mapToObj(i -> {
                return (isAssignable(method.getParameterTypes()[i], args[i].getDeclaredType()) ? "" : "(" + typeName(method.getGenericParameterTypes()[i]) + ")") + args[i].toString();
            }).collect(Collectors.joining(", ", "(", ")"));
        }

        @Override
        public void accept(Visitor v) {
            if (v.visitInst(this)) {
                v.enterInst(this);
                for (Inst arg : args) {
                    arg.accept(v);
                }
                v.leaveInst(this);
            }
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeNode(method, getId(), toIdArray(args));
        }

        @Override
        public String getName() {
            if (result instanceof RootNode) {
                return ((RootNode) result).getName();
            }
            return super.getName();
        }
    }

    private static class ConstInst extends Inst {
        private final Object constant;

        ConstInst(Object constant, Class<?> declaredType) {
            super(declaredType);
            this.constant = constant;
        }

        @Override
        public String rhs() {
            String stringified;
            if (constant == null || constant instanceof Integer || constant instanceof Double || constant instanceof Boolean) {
                stringified = String.valueOf(constant);
            } else if (constant instanceof String) {
                stringified = JSONParser.quote((String) constant);
            } else if (constant.getClass().isEnum()) {
                stringified = typeName(constant.getClass()) + "." + constant;
            } else if (constant == Dead.instance()) {
                stringified = typeName(Dead.class) + ".instance()";
            } else if (constant == Undefined.instance) {
                stringified = typeName(Undefined.class) + ".instance";
            } else if (constant == Null.instance) {
                stringified = typeName(Null.class) + ".instance";
            } else {
                throw new UnsupportedOperationException("Unsupported constant: " + constant);
            }
            return stringified;
        }

        @Override
        public boolean inVar() {
            return CONST_IN_VAR;
        }

        @Override
        public boolean isPrimitiveValue() {
            return !(constant instanceof String);
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeConstant(getId(), constant);
        }
    }

    static String typeName(Class<?> cls) {
        String fq = cls.getTypeName().replace('$', '.');
        String prefix = "java.lang.";
        if (fq.startsWith(prefix)) {
            fq = fq.substring(prefix.length(), fq.length());
        }
        return fq;
    }

    static String typeName(Type type) {
        if (type instanceof Class<?>) {
            return typeName((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            return typeName(((ParameterizedType) type).getRawType()) +
                            Arrays.stream(((ParameterizedType) type).getActualTypeArguments()).map(t -> typeName(t)).collect(Collectors.joining(",", "<", ">"));
        } else if (type instanceof GenericArrayType) {
            return typeName(((GenericArrayType) type).getGenericComponentType()) + "[]";
        } else {
            return type.getTypeName();
        }
    }

    private static class CollectInst extends Inst {
        private final List<Inst> args;
        private final Type parameterizedType;

        CollectInst(Class<?> type, List<Inst> args, Type genericType) {
            super(type);
            this.args = args;
            this.parameterizedType = genericType;
        }

        @Override
        public String rhs() {
            String stringifiedArgs = args.stream().map(Object::toString).collect(Collectors.joining(", "));
            String stringified;
            Class<?> type = getDeclaredType();
            if (type == ArrayList.class) {
                stringified = "new " + typeName(ArrayList.class) + "<>" + (args.isEmpty() ? "()" : "(" + typeName(Arrays.class) + ".asList(" + stringifiedArgs + "))");
            } else if (type.isArray()) {
                stringified = "new " + typeName(type.getComponentType()) + "[]{" + stringifiedArgs + "}";
            } else {
                throw new UnsupportedOperationException("Unsupported type: " + type);
            }
            return stringified;
        }

        @Override
        public Type getGenericDeclaredType() {
            return parameterizedType;
        }

        @Override
        public void accept(Visitor v) {
            if (v.visitInst(this)) {
                v.enterInst(this);
                for (Inst arg : args) {
                    arg.accept(v);
                }
                v.leaveInst(this);
            }
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeCollect(getId(), getDeclaredType(), toIdArray(args));
        }
    }

    private static class SourceInst extends Inst {
        SourceInst() {
            super(Source.class);
        }

        @Override
        public String rhs() {
            return "source";
        }

        @Override
        public boolean inVar() {
            return CONST_IN_VAR;
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeLoadArg(getId(), -2);
        }
    }

    private static class ContextInst extends Inst {
        ContextInst() {
            super(JSContext.class);
        }

        @Override
        public String rhs() {
            return "context";
        }

        @Override
        public boolean inVar() {
            return CONST_IN_VAR;
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeLoadArg(getId(), -1);
        }
    }

    private static class VarInst extends Inst {
        private final Inst inst;

        VarInst(Inst inst) {
            super(inst.getDeclaredType(), inst.getGenericDeclaredType());
            this.inst = inst;
            assert !(inst instanceof VarInst);
        }

        @Override
        public String toString() {
            return inst.inVar() ? ("v" + getId()) : rhs();
        }

        @Override
        public String rhs() {
            return inst.rhs();
        }

        @Override
        public Inst asVar() {
            return this;
        }

        @Override
        public void accept(Visitor v) {
            inst.accept(v);
        }

        @Override
        public int getId() {
            return inst.getId();
        }

        @Override
        public void assignId(int id) {
            throw new UnsupportedOperationException();
        }
    }

    private static class ParamInst extends Inst {
        private final Inst inst;

        ParamInst(Inst inst) {
            super(inst.getId(), inst.getDeclaredType(), inst.getGenericDeclaredType());
            this.inst = inst;
            assert inst.inVar();
        }

        @Override
        public String toString() {
            return "v" + getId();
        }

        @Override
        public String rhs() {
            return inst.rhs();
        }

        @Override
        public Inst asVar() {
            return this;
        }

        @Override
        public void accept(Visitor v) {
            inst.accept(v);
        }
    }

    private static class PlaceholderInst extends Inst {
        PlaceholderInst(Class<?> type) {
            super(type);
        }

        @Override
        public String rhs() {
            return "null";
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeConstant(getId(), null);
        }
    }

    private static class CallTargetInst extends Inst {
        private final Inst rootNode;

        CallTargetInst(Inst rootNode) {
            super(CallTarget.class);
            this.rootNode = rootNode;
        }

        @Override
        public String rhs() {
            return typeName(Truffle.class) + ".getRuntime().createCallTarget(" + rootNode + ")";
        }

        @Override
        public void accept(Visitor v) {
            if (v.visitInst(this)) {
                v.enterInst(this);
                rootNode.accept(v);
                v.leaveInst(this);
            }
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeCallTarget(getId(), rootNode.getId());
        }
    }

    private static class FrameSlotInst extends Inst implements FixUpInst {
        private final Inst frameDescriptor;
        private final Inst identifier;
        private final int flags;
        private final boolean findOrAdd;

        FrameSlotInst(FrameSlot frameSlot, Inst frameDescriptor, Inst identifier, int flags) {
            super(FrameSlot.class);
            this.frameDescriptor = frameDescriptor;
            this.identifier = identifier;
            this.flags = flags;
            this.findOrAdd = frameSlot.getIdentifier() == ScopeFrameNode.PARENT_SCOPE_IDENTIFIER;
        }

        @Override
        public String rhs() {
            return frameDescriptor + "." + (findOrAdd ? "findOrAddFrameSlot" : "addFrameSlot") +
                            "(" + identifier + ", " + flags + ", " + typeName(FrameSlotKind.class) + ".Illegal)";
        }

        @Override
        public void accept(Visitor v) {
            if (v.visitInst(this)) {
                v.enterInst(this);
                frameDescriptor.accept(v);
                identifier.accept(v);
                v.leaveInst(this);
            }
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeFrameSlot(getId(), frameDescriptor.getId(), identifier.getId(), flags, findOrAdd);
        }

        @Override
        public Inst getFixUpTarget() {
            return frameDescriptor;
        }
    }

    private static class FrameDescriptorInst extends Inst {
        FrameDescriptorInst() {
            super(FrameDescriptor.class);
        }

        @Override
        public String rhs() {
            return "new " + typeName(FrameDescriptor.class) + "(" + typeName(Undefined.class) + ".instance" + ")";
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeFrameDescriptor(getId());
        }
    }

    private static class FunctionDataInst extends Inst {
        private final JSFunctionData functionData;
        private final Inst context;

        FunctionDataInst(JSFunctionData functionData, Inst context) {
            super(JSFunctionData.class);
            this.functionData = functionData;
            this.context = context;
        }

        @Override
        public String rhs() {
            return String.format("%s.create(%s, null, null, null, %d, %s, %d)", typeName(JSFunctionData.class), context, functionData.getLength(),
                            JSONParser.quote(functionData.getName()), functionData.getFlags());
        }

        @Override
        public void accept(Visitor v) {
            if (v.visitInst(this)) {
                v.enterInst(this);
                context.accept(v);
                v.leaveInst(this);
            }
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeFunctionData(getId(), context.getId(), functionData);
        }
    }

    private static class BreakTargetInst extends Inst {
        private final BreakTarget target;

        BreakTargetInst(BreakTarget target) {
            super(BreakTarget.class);
            this.target = target;
        }

        @Override
        public String rhs() {
            if (target instanceof ContinueTarget) {
                return typeName(ContinueTarget.class) + (target.getId() != 0 ? ".forLoop(null, -1)" : ".forUnlabeledLoop()");
            } else {
                return typeName(BreakTarget.class) + (target.getId() != 0 ? ".forLabel(null, -1)" : ".forSwitch()");
            }
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeBreakTarget(getId(), target);
        }
    }

    private static class SourceSectionInst extends Inst {
        private Inst source;
        private SourceSection sourceSection;

        SourceSectionInst(Inst source, SourceSection sourceSection) {
            super(SourceSection.class);
            this.source = source;
            this.sourceSection = sourceSection;
        }

        @Override
        public String rhs() {
            if (SOURCE_SECTIONS) {
                return source + ".createSection(" +
                                sourceSection.getCharIndex() + ", " +
                                sourceSection.getCharLength() +
                                ")";
            } else {
                return "null";
            }
        }

        @Override
        public void accept(Visitor v) {
            if (v.visitInst(this)) {
                v.enterInst(this);
                source.accept(v);
                v.leaveInst(this);
            }
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            if (SOURCE_SECTIONS) {
                encoder.encodeSourceSection(getId(), source.getId(), sourceSection);
            } else {
                encoder.encodeConstant(getId(), null);
            }
        }
    }

    private interface FixUpInst {
        Inst getFixUpTarget();
    }

    private static class FixUpFunctionDataNameInst extends Inst implements FixUpInst {
        private final Inst node;
        private final String name;

        FixUpFunctionDataNameInst(Inst node, String name) {
            this.node = node;
            this.name = name;
        }

        @Override
        public String toString() {
            return node + ".setName(" + JSONParser.quote(name) + ")";
        }

        @Override
        public String rhs() {
            return "null";
        }

        @Override
        public void accept(Visitor v) {
            v.enterInst(this);
            node.accept(v);
            v.leaveInst(this);
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeFunctionDataNameFixup(node.getId(), name);
        }

        @Override
        public Inst getFixUpTarget() {
            return node;
        }

    }

    private static class FixUpNodeSourceSectionInst extends Inst implements FixUpInst {
        private final Inst node;
        private final Inst source;
        private final int charIndex;
        private final int charLength;

        FixUpNodeSourceSectionInst(Inst node, Inst source, SourceSection sourceSection) {
            super();
            this.node = node;
            this.source = source;
            this.charIndex = sourceSection.isAvailable() ? sourceSection.getCharIndex() : -1;
            this.charLength = sourceSection.isAvailable() ? sourceSection.getCharLength() : -1;
        }

        @Override
        public String toString() {
            if (charIndex >= 0 && charLength >= 0) {
                return node + "." + "setSourceSection" + "(" + source + ", " + charIndex + ", " + charLength + ")";
            } else {
                return node + "." + "setSourceSection" + "(" + source + "." + "createUnavailableSection" + "()" + ")";
            }
        }

        @Override
        public String rhs() {
            return "null";
        }

        @Override
        public void accept(Visitor v) {
            v.enterInst(this);
            node.accept(v);
            source.accept(v);
            v.leaveInst(this);
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeNodeSourceSectionFixup(node.getId(), source.getId(), charIndex, charLength);
        }

        @Override
        public Inst getFixUpTarget() {
            return node;
        }
    }

    private static class FixUpNodeTagsInst extends Inst implements FixUpInst {
        private final Inst node;
        private final boolean hasStatementTag;
        private final boolean hasCallTag;
        private final boolean hasExpressionTag;
        private final boolean hasRootTag;

        FixUpNodeTagsInst(Inst node, boolean hasStatementTag, boolean hasCallTag, boolean hasExpressionTag, boolean hasRootTag) {
            super();
            this.node = node;
            this.hasStatementTag = hasStatementTag;
            this.hasCallTag = hasCallTag;
            this.hasExpressionTag = hasExpressionTag;
            this.hasRootTag = hasRootTag;
        }

        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner(";\n");
            if (hasStatementTag) {
                joiner.add(node + "." + "addStatementTag" + "()");
            }
            if (hasCallTag) {
                joiner.add(node + "." + "addCallTag" + "()");
            }
            if (hasExpressionTag) {
                joiner.add(node + "." + "addExpressionTag" + "()");
            }
            if (hasRootTag) {
                joiner.add(node + "." + "addRootTag" + "()");
            }
            return joiner.toString();
        }

        @Override
        public String rhs() {
            return "null";
        }

        @Override
        public void accept(Visitor v) {
            v.enterInst(this);
            node.accept(v);
            v.leaveInst(this);
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeNodeTagsFixup(node.getId(), hasStatementTag, hasCallTag, hasExpressionTag, hasRootTag);
        }

        @Override
        public Inst getFixUpTarget() {
            return node;
        }
    }

    private static class ReturnInst extends Inst {
        private final Inst returnValue;

        ReturnInst(Inst returnValue) {
            super();
            this.returnValue = returnValue;
        }

        @Override
        public String toString() {
            return "return " + returnValue;
        }

        @Override
        public String rhs() {
            return returnValue.toString();
        }

        @Override
        public void accept(Visitor v) {
            v.enterInst(this);
            returnValue.accept(v);
            v.leaveInst(this);
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            encoder.encodeReturn(returnValue.getId());
        }
    }

    private static class CallExtractedInst extends Inst {
        private final String name;
        private final Class<?> methodReturnType;
        private final Inst[] args;

        CallExtractedInst(String name, Inst retval, Class<?> methodReturnType, Inst[] args) {
            super(retval.getId(), retval.getDeclaredType(), retval.getGenericDeclaredType());
            this.name = name;
            this.methodReturnType = methodReturnType;
            this.args = args;
        }

        @Override
        public String rhs() {
            String rhs = (isAssignable(getDeclaredType(), methodReturnType) ? "" : "(" + typeName(getDeclaredType()) + ")") + name + "(nodeFactory, context, source" +
                            (args.length == 0 ? "" : (", " + Arrays.stream(args).map(Object::toString).collect(Collectors.joining(", ")))) + ")";
            return rhs;
        }

        @Override
        public String toString() {
            if (LAZY_FUNCTIONS) {
                Optional<Inst> functionDataOpt = getFunctionDataArg();
                if (functionDataOpt.isPresent()) {
                    Inst functionDataVar = functionDataOpt.get();
                    if (LAMBDA) {
                        return functionDataVar + ".setLazyInit((functionData) -> " + rhs() + ");\n" +
                                        declaredTypeName() + " " + "v" + getId() + " = " + "null";
                    } else {
                        return functionDataVar + ".setLazyInit(" + "new " + typeName(JSFunctionData.Initializer.class) + "() {\n" +
                                        "public void initializeRoot(" + typeName(JSFunctionData.class) + " functionData" + ") {\n" +
                                        rhs() + ";" + "\n" +
                                        "}\n" +
                                        "});\n" +
                                        declaredTypeName() + " " + "v" + getId() + " = " + "null";
                    }
                }
            }
            return super.toString();
        }

        @Override
        public void encodeTo(JSNodeEncoder encoder) {
            if (LAZY_FUNCTIONS) {
                Optional<Inst> functionDataOpt = getFunctionDataArg();
                if (functionDataOpt.isPresent()) {
                    Inst functionDataVar = functionDataOpt.get();
                    encoder.encodeCallExtractedLazy(name, functionDataVar.getId(), toIdArray(args));
                    return;
                }
            }
            encoder.encodeCallExtracted(name, getId(), toIdArray(args));
        }

        private Optional<Inst> getFunctionDataArg() {
            return Arrays.stream(args).filter(arg -> arg.getDeclaredType() == JSFunctionData.class).findFirst();
        }

        @Override
        public void accept(Visitor v) {
            v.visitInst(this);
        }
    }

    private static class VarIdTable {
        private static final int FIRST_ID = 1;
        private int nextId = FIRST_ID;
        private final Map<Object, Integer> varIndexMap = new HashMap<>();

        private static Object getKey(Object node) {
            if (node == null || JSRuntime.isJSPrimitive(node) || node.getClass().isEnum()) {
                return node;
            } else {
                class IdentityKey {
                    private final Object obj;

                    IdentityKey(Object obj) {
                        this.obj = obj;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return other instanceof IdentityKey && this.obj == ((IdentityKey) other).obj;
                    }

                    @Override
                    public int hashCode() {
                        return System.identityHashCode(obj);
                    }

                    @Override
                    public String toString() {
                        return String.valueOf(obj);
                    }
                }
                return new IdentityKey(node);
            }
        }

        public int put(Object node) {
            Object key = getKey(node);
            if (varIndexMap.containsKey(key)) {
                throw new RuntimeException("Duplicate put: " + node);
            }
            int id = nextId++;
            varIndexMap.put(key, id);
            return id;
        }

        public int getId(Object node) {
            Object key = getKey(node);
            if (!varIndexMap.containsKey(key)) {
                throw new RuntimeException("Entry not found: " + node + "(" + node.getClass() + ")");
            }
            return varIndexMap.get(key);
        }

        public boolean contains(Object node) {
            Object key = getKey(node);
            return varIndexMap.containsKey(key);
        }
    }

    public <T> Inst getOrPut(T arg, Function<T, Inst> makeInst) {
        if (table.contains(arg)) {
            return getInst(arg);
        } else {
            Inst inst = makeInst.apply(arg);
            inst.assignId(table.put(arg));
            append(inst);
            return inst;
        }
    }

    public Recording() {
    }

    public void recordCall(Method method, Object[] args) {
        if (EARLY_FIXUP) {
            processFixUps();
        }
        callStack.push(new MethodCall(method, args));
    }

    private Inst[] encodeParameterArray(Object[] args, Class<?>[] paramTypes, Type[] genericTypes) {
        Inst[] encoding = new Inst[args.length];
        for (int i = 0; i < args.length; i++) {
            encoding[i] = encode(args[i], paramTypes[i], genericTypes != null ? genericTypes[i] : null);
        }
        return encoding;
    }

    private Inst[] encodeArray(Object args, Class<?> declaredType, Type genericType) {
        assert declaredType.isArray();
        assert args.getClass().isArray();
        Class<?> elementType = declaredType.getComponentType();
        Type elementGenericType = genericType instanceof GenericArrayType ? ((GenericArrayType) genericType).getGenericComponentType() : null;
        int length = Array.getLength(args);
        Inst[] encoding = new Inst[length];
        for (int i = 0; i < length; i++) {
            encoding[i] = encode(Array.get(args, i), elementType, elementGenericType);
        }
        return encoding;
    }

    private ArrayList<Inst> encodeList(ArrayList<?> args, Type genericType) {
        Type elementGenericType = genericType instanceof ParameterizedType ? ((ParameterizedType) genericType).getActualTypeArguments()[0] : null;
        Class<?> elementType = getRawType(elementGenericType);
        ArrayList<Inst> encoding = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            encoding.add(i, encode(args.get(i), elementType, elementGenericType));
        }
        return encoding;
    }

    private static Class<?> getRawType(Type genericType) {
        if (genericType == null) {
            return null;
        } else if (genericType instanceof Class<?>) {
            return (Class<?>) genericType;
        } else if (genericType instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) genericType).getRawType();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private Inst getInst(Object arg) {
        int id = table.getId(arg);
        return defs.get(id);
    }

    private Inst dumpConst(Object arg, Class<?> declaredType) {
        return getOrPut(arg, (v) -> new ConstInst(v, declaredType));
    }

    private Inst dumpNode(Node arg) {
        return getInst(arg);
    }

    private Inst dumpContext(JSContext arg) {
        return getOrPut(arg, (v) -> new ContextInst());
    }

    private Inst dumpCollectArray(Inst[] arg, Class<?> arrayClass) {
        assert arrayClass.isArray();
        return getOrPut(arg, (v) -> new CollectInst(arrayClass, Arrays.asList(v), null));
    }

    private Inst dumpCollectList(List<Inst> arg, Type genericType) {
        return getOrPut(arg, (v) -> new CollectInst(v.getClass(), v, genericType));
    }

    private Inst dumpCallTarget(CallTarget arg) {
        return getOrPut(arg, (v) -> new CallTargetInst(dumpNode(((RootCallTarget) v).getRootNode()).asVar()));
    }

    private Inst dumpPlaceholder(Object arg) {
        return getOrPut(arg, (v) -> new PlaceholderInst(arg.getClass()));
    }

    private Inst dumpFrameSlot(FrameSlot arg) {
        return getOrPut(arg, (v) -> new FrameSlotInst(v, dumpFrameDescriptor(getFrameDescriptor(v)).asVar(), dumpConst(v.getIdentifier(), Object.class).asVar(), JSFrameUtil.getFlags(v)));
    }

    private FrameDescriptor getFrameDescriptor(FrameSlot slot) {
        return frameDescriptorSet.stream().filter(desc -> desc.findFrameSlot(slot.getIdentifier()) == slot).findFirst().orElseThrow(
                        () -> new NoSuchElementException("FrameDescriptor not found for slot: " + slot));
    }

    private Inst dumpFrameDescriptor(FrameDescriptor arg) {
        frameDescriptorSet.add(arg);
        return getOrPut(arg, (v) -> new FrameDescriptorInst());
    }

    private Inst dumpFunctionData(JSFunctionData arg) {
        functionDataSet.add(arg);
        return getOrPut(arg, (functionData) -> new FunctionDataInst(functionData, dumpContext(functionData.getContext()).asVar()));
    }

    private Inst dumpBreakTarget(BreakTarget arg) {
        return getOrPut(arg, (v) -> new BreakTargetInst(v));
    }

    private Inst dumpSource(Source arg) {
        return getOrPut(arg, (v) -> new SourceInst());
    }

    private Inst dumpSourceSection(SourceSection arg) {
        return getOrPut(arg, (v) -> new SourceSectionInst(dumpSource(arg.getSource()).asVar(), v));
    }

    private Inst encode(Object arg, Class<?> declaredType, Type genericType) {
        Inst enc;
        if (arg == null || JSRuntime.isJSPrimitive(arg) || arg == Dead.instance()) {
            enc = dumpConst(arg, unboxedType(arg, declaredType));
        } else if (arg.getClass().isEnum()) {
            enc = dumpConst(arg, declaredType);
        } else if (arg instanceof Node) {
            enc = dumpNode((Node) arg);
        } else if (arg instanceof JSContext) {
            enc = dumpContext((JSContext) arg);
        } else if (arg.getClass().isArray()) {
            enc = dumpCollectArray(encodeArray(arg, arg.getClass(), genericType), arg.getClass());
        } else if (arg instanceof ArrayList) {
            enc = dumpCollectList(encodeList((ArrayList<?>) arg, genericType), genericType);
        } else if (arg instanceof CallTarget) {
            enc = dumpCallTarget((CallTarget) arg);
        } else if (arg instanceof BreakTarget) {
            enc = dumpBreakTarget((BreakTarget) arg);
        } else if (arg instanceof FrameDescriptor) {
            enc = dumpFrameDescriptor((FrameDescriptor) arg);
        } else if (arg instanceof FrameSlot) {
            enc = dumpFrameSlot((FrameSlot) arg);
        } else if (arg instanceof JSFunctionData) {
            enc = dumpFunctionData((JSFunctionData) arg);
        } else if (arg instanceof SourceSection) {
            enc = dumpSourceSection((SourceSection) arg);
        } else if (arg instanceof Environment) {
            enc = dumpPlaceholder(arg);
        } else {
            throw new RuntimeException("Unrecognized argument: " + arg);
        }
        return enc.asVar();
    }

    private static Class<?> unboxedType(Object arg, Class<?> declaredType) {
        if (arg instanceof Boolean) {
            return boolean.class;
        } else if (arg instanceof Integer) {
            return int.class;
        } else if (arg instanceof Double) {
            return double.class;
        } else if (arg instanceof Long) {
            return long.class;
        }
        return declaredType;
    }

    public <T> T recordReturn(Method method, T result) {
        MethodCall methodCall = callStack.pop();
        assert methodCall.method == method;
        if (!table.contains(result)) {
            Inst[] encoded = encodeParameterArray(methodCall.args, method.getParameterTypes(), method.getGenericParameterTypes());
            NodeInst nodeInst = new NodeInst(table.put(result), method, encoded, result);
            append(nodeInst);
            addNodeFixUps(nodeInst, result);
        } else {
            logv("noop: %s => %s", method.getName(), getInst(result));
        }
        return result;
    }

    private void append(Inst inst) {
        defs.put(inst.getId(), inst);
        if (inst.inVar()) {
            insts.add(inst);
        }
    }

    private void addNodeFixUps(NodeInst nodeInst, Object result) {
        if (result instanceof JSFunctionData) {
            JSFunctionData functionData = (JSFunctionData) result;
            String originalName = functionData.getName();
            addFixUp(() -> {
                String currentName = functionData.getName();
                boolean nameChanged = !originalName.equals(currentName);
                if (nameChanged) {
                    insts.add(new FixUpFunctionDataNameInst(nodeInst.asVar(), currentName));
                }
                return nameChanged;
            });
        } else if (result instanceof FrameDescriptor) {
            frameDescriptorSet.add((FrameDescriptor) result);
        }

        if (!FIXUP_SOURCE_SECTIONS) {
            if (!FIXUP_TAGS) {
                return;
            }
        }

        if (result instanceof JavaScriptNode) {
            JavaScriptNode jsnode = (JavaScriptNode) result;
            addFixUp(() -> {
                if (jsnode.hasSourceSection()) {
                    if (FIXUP_SOURCE_SECTIONS) {
                        SourceSection sourceSection = jsnode.getSourceSection();
                        insts.add(new FixUpNodeSourceSectionInst(nodeInst.asVar(), dumpSource(sourceSection.getSource()).asVar(), sourceSection));
                    }
                    if (FIXUP_TAGS) {
                        boolean hasStatementTag = jsnode.hasTag(StandardTags.StatementTag.class);
                        boolean hasCallTag = jsnode.hasTag(StandardTags.CallTag.class);
                        boolean hasExpressionTag = jsnode.hasTag(StandardTags.ExpressionTag.class);
                        boolean hasRootTag = jsnode.hasTag(StandardTags.RootTag.class);
                        if (hasStatementTag || hasCallTag || hasExpressionTag || hasRootTag) {
                            insts.add(new FixUpNodeTagsInst(nodeInst.asVar(), hasStatementTag, hasCallTag, hasExpressionTag, hasRootTag));
                        }
                    }
                    return true;
                }
                return false;
            });
        }
    }

    private void addFixUp(BooleanSupplier fixup) {
        fixups.add(fixup);
    }

    private void processFixUps() {
        if (!fixups.isEmpty()) {
            fixups.removeIf(fixup -> fixup.getAsBoolean());
        }
    }

    private void dce() {
        BitSet visited = new BitSet();
        for (int i = insts.size() - 1; i >= 0; i--) {
            Inst root = insts.get(i);
            if (root.isRoot()) {
                reachableSet(root, visited);
            }
        }

        if (VERBOSE) {
            insts.forEach(inst -> {
                if (!inst.isRoot() && !visited.get(inst.getId())) {
                    logv("dead: %s", inst);
                }
            });
        }
        insts.removeIf(inst -> !inst.isRoot() && !visited.get(inst.getId()));
    }

    public void finish(RootNode rootNode) {
        processFixUps();
        fixups.clear();
        append(new ReturnInst(getInst(rootNode).asVar()));
        dce();

        if (BATCHES_ENABLED) {
            buildUsageMap();
            buildIndexMap();

            buildBatches();
        }

        source = rootNode.getSourceSection().getSource();
    }

    private void buildUsageMap() {
        insts.forEach(in -> usageMap.put(in, new ArrayList<>()));
        for (Inst inst : insts) {
            inst.forEachInput(in -> {
                if (!in.inVar()) {
                    return;
                }
                usageMap.get(in).add(inst);
            });
        }
    }

    private void buildIndexMap() {
        for (int i = 0; i < insts.size(); i++) {
            indexMap.put(insts.get(i), i);
        }
    }

    private Integer indexOf(Inst inst1) {
        return indexMap.getOrDefault(inst1, -1);
    }

    private void buildBatches() {
        class BatchWorkItem {
            final Inst startInst;
            final BitSet outerExtractedSet;
            final BatchWorkItem caller;
            BitSet extractedSet;

            BatchWorkItem(Inst startInst) {
                this.startInst = startInst;
                this.outerExtractedSet = new BitSet();
                this.caller = null;
            }

            BatchWorkItem(Inst startInst, BitSet outerExtracted, BatchWorkItem caller) {
                this.startInst = startInst;
                this.outerExtractedSet = outerExtracted;
                this.caller = caller;
            }
        }

        class CallInfo {
            final String name;
            final List<Inst> args;
            final Class<?> returnType;

            CallInfo(String name, List<Inst> args, Class<?> returnType) {
                this.name = name;
                this.args = args;
                this.returnType = returnType;
            }
        }

        Map<Inst, BitSet> batches = new LinkedHashMap<>();

        Inst returnInst = insts.get(insts.size() - 1);
        Deque<BatchWorkItem> worklist = new ArrayDeque<>();
        worklist.add(new BatchWorkItem(returnInst));
        Set<Inst> startInstsVisited = new HashSet<>();
        Map<Inst, CallInfo> extractedMethodMap = new HashMap<>();

        int count = 0;
        while (!worklist.isEmpty()) {
            BatchWorkItem batchBoundary = worklist.pop();
            Inst startInst = batchBoundary.startInst;
            BitSet outerExtractedSet = batchBoundary.outerExtractedSet;
            if (!startInstsVisited.add(startInst)) {
                continue;
            }

            logv("starting batch '%s' at: %s", startInst.getName(), startInst);

            List<Inst> boundaryValues = new ArrayList<>();
            BitSet visited = new BitSet();
            startInst.accept(inst1 -> {
                if (!inst1.inVar()) {
                    return true;
                }
                int index = indexOf(inst1);
                if (!visited.get(index)) {
                    visited.set(index);
                    if (startInst != inst1) {
                        if (isBatchBoundary(inst1)) {
                            return false;
                        } else if (outerExtractedSet.get(index) && !inst1.isPrimitiveValue()) {
                            boundaryValues.add(inst1.asVar());
                            return false;
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            });

            BitSet usageSet = new BitSet();
            usageSet.set(indexOf(startInst));
            addInputsToSet(usageSet, startInst);
            addFixUpsToSet(usageSet, startInst, outerExtractedSet);
            fixpoint(() -> {
                int before = usageSet.cardinality();
                addInputsToSet(usageSet);
                addFixUpsToSet(usageSet, startInst, outerExtractedSet);
                int after = usageSet.cardinality();
                return before != after;
            });

            if (VERBOSE) {
                if (!usageSet.isEmpty()) {
                    logv("search for usages " + startInst + " " + usageSet.cardinality());
                    usageSet.stream().mapToObj(insts::get).forEachOrdered(in -> logv("--" + in));
                }

                BitSet added = new BitSet();
                added.or(usageSet);
                added.andNot(visited);
                if (!added.isEmpty()) {
                    logv("added usages " + startInst + " " + added.cardinality());
                    added.stream().mapToObj(insts::get).forEachOrdered(in -> logv("--" + in));
                }
                BitSet removed = new BitSet();
                removed.or(visited);
                removed.andNot(usageSet);
                if (!removed.isEmpty()) {
                    logv("removed usages " + startInst + " " + removed.cardinality());
                    removed.stream().mapToObj(insts::get).forEachOrdered(in -> logv("--" + in));
                }
            }

            usageSet.stream().filter(outerExtractedSet::get).mapToObj(insts::get).filter(in -> !in.isPrimitiveValue()).filter(in -> !isBatchBoundary(in)).forEach(in -> {
                logv("cleared %s", in);
                usageSet.clear(indexOf(in));
            });

            boundaryValues.forEach(var -> {
                Inst in = deref(var);
                int index = indexOf(in);
                usageSet.clear(index);
                for (BatchWorkItem caller = batchBoundary.caller; caller != null; caller = caller.caller) {
                    if (caller.extractedSet.get(index)) {
                        break;
                    }
                    // not found in caller batch, look for it in the arguments
                    List<Inst> callerArgs = extractedMethodMap.get(caller.startInst).args;
                    if (!callerArgs.stream().anyMatch(callerArg -> callerArg.getId() == in.getId())) {
                        // ~= (!callerArgs.contains(in))
                        // value is not directly provided by or used in the caller but needs to
                        // be forwarded via the caller (as argument) from the defining caller
                        logv("forwarding variable through caller: %s", in);
                        callerArgs.add(in.asVar());
                    }
                }
            });

            usageSet.stream().mapToObj(insts::get).filter(in -> isBatchBoundary(in) && !isContained(in, usageSet)).forEach(
                            nextBoundary -> worklist.push(new BatchWorkItem(nextBoundary, mergeBitSets(outerExtractedSet, usageSet), batchBoundary)));
            batches.put(startInst, usageSet);

            String nameSuffix = startInst.getName();
            nameSuffix = nameSuffix.isEmpty() ? "" : "_" + mangleName(nameSuffix);
            extractedMethodMap.put(startInst, new CallInfo(EXTRACTED_METHOD_NAME_PREFIX + count++ + nameSuffix, boundaryValues, FunctionRootNode.class));
            batchBoundary.extractedSet = usageSet;
        }

        logv("XXX found %d batches", batches.size());

        // sort parameters by id
        extractedMethodMap.values().forEach(callInfo -> callInfo.args.sort(Comparator.comparing(Inst::getId)));

        for (Map.Entry<Inst, BitSet> batch : batches.entrySet()) {
            Inst ret = batch.getKey();
            BitSet extractedSet = batch.getValue();
            List<Inst> batchInsts = extractedSet.stream().mapToObj(i -> {
                Inst inst = insts.get(i);
                if (inst != ret) {
                    CallInfo callInfo = extractedMethodMap.get(inst);
                    if (callInfo != null) {
                        assert !(inst instanceof ReturnInst);
                        inst = new CallExtractedInst(callInfo.name, inst, callInfo.returnType, callInfo.args.stream().toArray(Inst[]::new));
                    }
                }
                return inst;
            }).collect(Collectors.toCollection(ArrayList::new));
            if (!(ret instanceof ReturnInst)) {
                batchInsts.add(new ReturnInst(ret.asVar()));
            }

            CallInfo callInfo = extractedMethodMap.get(ret);
            instBatches.add(new InstBatch(batchInsts, ret instanceof ReturnInst ? ENTRY_METHOD_NAME : callInfo.name, callInfo.args.stream().map(ParamInst::new).collect(Collectors.toList()),
                            ret instanceof ReturnInst ? Object.class : callInfo.returnType));
        }

        if (VERBOSE) {
            duplicateCheck(batches);
        }
    }

    private Inst deref(Inst var) {
        return defs.get(var.getId());
    }

    private static String mangleName(String nameSuffix) {
        return nameSuffix.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static BitSet mergeBitSets(BitSet first, BitSet second) {
        BitSet merged = new BitSet();
        merged.or(first);
        merged.or(second);
        return merged;
    }

    private boolean isContained(Inst in, BitSet usageSet) {
        AtomicBoolean result = new AtomicBoolean(true);
        in.forEachInput(input -> {
            if (result.get()) {
                assert !input.isRoot();
                if (!input.inVar()) {
                    return;
                }
                if (!usageSet.get(indexOf(input))) {
                    result.set(false);
                }
            }
        });
        return result.get();
    }

    private void duplicateCheck(Map<Inst, BitSet> batches) {
        for (Map.Entry<Inst, BitSet> batch1 : batches.entrySet()) {
            BitSet extractedSet1 = batch1.getValue();
            BitSet others = new BitSet();
            for (Map.Entry<Inst, BitSet> batch2 : batches.entrySet()) {
                BitSet extractedSet2 = batch2.getValue();
                if (extractedSet1 == extractedSet2) {
                    continue;
                }
                others.or(extractedSet2);
            }
            if (extractedSet1.intersects(others)) {
                BitSet intersection = new BitSet();
                intersection.or(extractedSet1);
                intersection.and(others);
                intersection.stream().mapToObj(insts::get).filter(in -> !batches.containsKey(in)).filter(in -> !in.isPrimitiveValue()).forEachOrdered(in -> logv("dupe: " + in));
            }
        }
    }

    private static void fixpoint(BooleanSupplier run) {
        do {
            continue;
        } while (run.getAsBoolean());
    }

    private void addInputsToSet(BitSet usageSet) {
        usageSet.stream().forEach(j -> {
            Inst inst = insts.get(j);
            if (isBatchBoundary(inst)) {
                return;
            } else if (/* inst.getDeclaredType() == FrameSlot.class || */inst.getDeclaredType() == FrameDescriptor.class) {
                return;
            }
            addInputsToSet(usageSet, inst);
        });
    }

    private void addInputsToSet(BitSet usageSet, Inst inst) {
        inst.forEachInput(input -> {
            if (!input.inVar()) {
                return;
            }
            int index = indexOf(input);
            if (!usageSet.get(index)) {
                usageSet.set(index);
            }
        });
    }

    private static boolean isBatchBoundary(Inst inst) {
        return inst.getDeclaredType() == FunctionRootNode.class;
    }

    private void addFixUpsToSet(BitSet usageSet, Inst startInst, BitSet outerExtractedSet) {
        usageSet.stream().mapToObj(insts::get).forEach(inst -> {
            if (inst instanceof FunctionDataInst && !usageMap.get(inst).stream().filter(usage -> isBatchBoundary(usage)).allMatch(boundary -> boundary == startInst)) {
                // if this is the function data of an extracted function (a boundary), skip it
                return;
            }

            // if inst has a fix-up inst usage, add the usage to the set
            usageMap.get(inst).stream().filter(usage -> usage instanceof FixUpInst && ((FixUpInst) usage).getFixUpTarget().getId() == inst.getId()).forEach(fixup -> {
                int index = indexOf(fixup);
                if (!usageSet.get(index) && !outerExtractedSet.get(index)) {
                    logv("fixup %s -> %s", fixup, inst);
                    usageSet.set(index);
                }
            });
        });
    }

    private static void reachableSet(Inst root, BitSet reachable) {
        root.accept(inst -> {
            if (!reachable.get(inst.getId())) {
                reachable.set(inst.getId());
                return true;
            } else {
                return false;
            }
        });
    }

    public void saveToStream(String fileName, OutputStream outs, boolean binary) {
        logv("dumping %s", fileName);
        if (binary) {
            saveAsBinary(outs);
        } else {
            saveAsJava(fileName, outs);
        }
    }

    private ByteBuffer saveAsBinary(OutputStream outs) {
        BinaryEncoder sink = new BinaryEncoder();
        JSNodeEncoder encoder = new JSNodeEncoder(sink, source.getCharacters());

        if (!instBatches.isEmpty()) {
            for (InstBatch instBatch : instBatches) {
                encodeMethod(encoder, instBatch.name, instBatch.insts, instBatch.inputs);
            }
        } else {
            encodeMethod(encoder, ENTRY_METHOD_NAME, insts, Collections.emptyList());
        }
        try {
            outs.write(byteBufferToByteArray(sink.getBuffer()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (TEST_DECODE) {
            testDecode(sink.getBuffer());
        }

        return sink.getBuffer();
    }

    private static byte[] byteBufferToByteArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private void saveAsJava(String fileName, OutputStream outs) {
        String qualifiedClassName = mangleFileName(fileName);
        String packageName = qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf('.'));
        String unqualifiedClassName = qualifiedClassName.substring(qualifiedClassName.lastIndexOf('.') + 1, qualifiedClassName.length());
        try (PrintStream out = new PrintStream(outs, false, "UTF-8")) {
            saveImpl(fileName, packageName, unqualifiedClassName, out);
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void saveImpl(String fileName, String packageName, String unqualifiedClassName, PrintStream out) {
        out.println("// Checkstyle: stop");
        out.println("// Autogenerated from " + fileName);
        out.println("package " + packageName + ";");
        out.println();
        out.println("@SuppressWarnings(\"all\")");
        out.println("public class " + unqualifiedClassName + " implements " + typeName(SnapshotProvider.class) + " {");
        if (!instBatches.isEmpty()) {
            for (InstBatch instBatch : instBatches) {
                printMethod(out, instBatch.name, instBatch.outputType, instBatch.insts, instBatch.inputs);
            }
        } else {
            printMethod(out, ENTRY_METHOD_NAME, Object.class, insts, Collections.emptyList());
        }
        out.println("}");

        for (FrameDescriptor fd : frameDescriptorSet) {
            out.println("//" + fd);
        }
        for (JSFunctionData fd : functionDataSet) {
            out.println("//" + fd);
        }
    }

    private static void printMethod(PrintStream out, String name, Class<?> returnType, List<Inst> insts, List<Inst> params) {
        out.println();
        out.println("public " + typeName(returnType) + " " + name + "(" +
                        typeName(NodeFactory.class) + " nodeFactory, " +
                        typeName(JSContext.class) + " context, " +
                        typeName(Source.class) + " source" +
                        (params.isEmpty() ? "" : ", ") +
                        params.stream().map(arg -> arg.declaredTypeName() + " " + arg.toString()).collect(Collectors.joining(", ")) + ") {");
        if (SORT_BY_ID) {
            sortInstsById(insts);
        }
        for (Inst inst : insts) {
            out.println(inst + ";");
        }
        out.println("}");
    }

    private static void encodeMethod(JSNodeEncoder encoder, String name, List<Inst> methodInsts, List<Inst> params) {
        encoder.markExtractedPosition(name);
        int regs = countRegs(methodInsts, params);
        encoder.encodeRegisterArraySize(regs);
        for (int i = 0; i < params.size(); i++) {
            Inst param = params.get(i);
            encoder.encodeLoadArg(param.getId(), i);
        }
        try {
            for (Inst inst : methodInsts) {
                inst.encodeTo(encoder);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private static int countRegs(List<Inst> methodInsts, List<Inst> params) {
        int count = (int) methodInsts.stream().filter(in -> !in.isRoot()).count() + params.size();
        logv(() -> String.format("regs: %d => %d", (methodInsts.stream().mapToInt(Inst::getId).max().orElse(-1) + 1), count));
        return count;
    }

    private void testDecode(ByteBuffer buffer) {
        BinarySnapshotProvider snapshot = new BinarySnapshotProvider(buffer);
        JSContext context = SnapshotTool.createDefaultContext();
        snapshot.apply(NodeFactory.getDefaultInstance(), context, source);
    }

    private static void sortInstsById(List<Inst> insts) {
        Collections.sort(insts, (a, b) -> {
            int ai = a.getId();
            int bi = b.getId();
            if (ai != bi) {
                if (ai == -1) {
                    return 1;
                } else if (bi == -1) {
                    return -1;
                }
            }
            return Integer.compare(ai, bi);
        });
    }

    static String packageName(Class<?> clazz) {
        return clazz.getName().substring(0, clazz.getName().lastIndexOf('.'));
    }

    /**
     * Replace non-word characters with {@code '_'}.
     */
    private static String mangleFileName(String fileName) {
        StringBuilder sb = null;
        for (int i = 0; i < fileName.length(); i++) {
            char ch = fileName.charAt(i);
            if (!isAsciiWordChar(ch)) {
                if (sb == null) {
                    sb = new StringBuilder(fileName);
                }
                sb.setCharAt(i, '_');
            }
        }
        return sb == null ? fileName : sb.toString();
    }

    private static boolean isAsciiWordChar(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || (ch == '_');
    }

    static void logv(String line) {
        if (VERBOSE) {
            System.out.println(line);
        }
    }

    static void logv(String format, Object... args) {
        if (VERBOSE) {
            System.out.println(String.format(format, args));
        }
    }

    static void logv(Supplier<String> line) {
        if (VERBOSE) {
            System.out.println(line.get());
        }
    }
}

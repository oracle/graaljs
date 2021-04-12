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
package com.oracle.truffle.js.parser.env;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.nodes.access.EvalVariableNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.access.WriteNode;
import com.oracle.truffle.js.nodes.access.WritePropertyNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;

public abstract class Environment {

    public static final String ARGUMENTS_NAME = "arguments";
    public static final String THIS_NAME = "this";
    public static final String SUPER_NAME = "super";
    public static final String NEW_TARGET_NAME = "new.target";

    private final Environment parent;
    protected final NodeFactory factory;
    protected final JSContext context;

    public Environment(Environment parent, NodeFactory factory, JSContext context) {
        this.parent = parent;
        this.factory = factory;
        this.context = context;
    }

    public FrameSlot declareLocalVar(Object name) {
        return function().declareLocalVar(name);
    }

    public FrameSlot declareVar(Object name) {
        return function().declareVar(name);
    }

    public boolean hasLocalVar(String name) {
        return getFunctionFrameDescriptor().getIdentifiers().contains(name);
    }

    public VarRef findThisVar() {
        return findInternalSlot(FunctionEnvironment.THIS_SLOT_IDENTIFIER, true);
    }

    public VarRef findSuperVar() {
        assert !function().isGlobal();
        return findInternalSlot(FunctionEnvironment.SUPER_SLOT_IDENTIFIER);
    }

    public VarRef findArgumentsVar() {
        assert !function().isGlobal();
        return findInternalSlot(FunctionEnvironment.ARGUMENTS_SLOT_IDENTIFIER);
    }

    public VarRef findNewTargetVar() {
        assert !function().isGlobal();
        return findInternalSlot(FunctionEnvironment.NEW_TARGET_SLOT_IDENTIFIER);
    }

    public VarRef findAsyncContextVar() {
        // top-level-await uses async generators in global scope
        assert !function().isGlobal() || function().isAsyncGeneratorFunction();
        declareLocalVar(FunctionEnvironment.ASYNC_CONTEXT_SLOT_IDENTIFIER);
        return findInternalSlot(FunctionEnvironment.ASYNC_CONTEXT_SLOT_IDENTIFIER);
    }

    public VarRef findAsyncResultVar() {
        // top-level-await uses async generators in global scope
        assert !function().isGlobal() || function().isAsyncGeneratorFunction();
        declareLocalVar(FunctionEnvironment.ASYNC_RESULT_SLOT_IDENTIFIER);
        return findInternalSlot(FunctionEnvironment.ASYNC_RESULT_SLOT_IDENTIFIER);
    }

    public VarRef findYieldValueVar() {
        assert !function().isGlobal();
        declareLocalVar(FunctionEnvironment.YIELD_VALUE_SLOT_IDENTIFIER);
        return findInternalSlot(FunctionEnvironment.YIELD_VALUE_SLOT_IDENTIFIER);
    }

    public VarRef findDynamicScopeVar() {
        assert !function().isGlobal();
        return findInternalSlot(FunctionEnvironment.DYNAMIC_SCOPE_IDENTIFIER);
    }

    public final JavaScriptNode createLocal(FrameSlot frameSlot, int level, int scopeLevel) {
        return factory.createLocal(frameSlot, level, scopeLevel, getParentSlots(level, scopeLevel), false);
    }

    public final JavaScriptNode createLocal(FrameSlot frameSlot, int level, int scopeLevel, boolean checkTDZ) {
        return factory.createLocal(frameSlot, level, scopeLevel, getParentSlots(level, scopeLevel), checkTDZ);
    }

    protected final VarRef findInternalSlot(String name) {
        return findInternalSlot(name, false);
    }

    protected final VarRef findInternalSlot(String name, boolean allowDebug) {
        Environment current = this;
        int frameLevel = 0;
        int scopeLevel = 0;
        do {
            FrameSlot slot = current.findBlockFrameSlot(name);
            if (slot != null) {
                return newFrameSlotVarRef(slot, scopeLevel, frameLevel, name, current);
            }
            if (current instanceof FunctionEnvironment) {
                frameLevel++;
                scopeLevel = 0;
            } else if (current instanceof BlockEnvironment) {
                scopeLevel++;
            } else if (current instanceof DebugEnvironment) {
                if (allowDebug && ((DebugEnvironment) current).hasMember(name)) {
                    ensureFrameLevelAvailable(frameLevel);
                    return new DebugVarRef(name);
                }
            }
            current = current.getParent();
        } while (current != null);

        return null;
    }

    enum WrapAccess {
        Read,
        Write,
        Delete,
    }

    @FunctionalInterface
    interface WrapClosure {
        JavaScriptNode apply(JavaScriptNode node, WrapAccess access);

        default Pair<Supplier<JavaScriptNode>, UnaryOperator<JavaScriptNode>> applyCompound(Pair<Supplier<JavaScriptNode>, UnaryOperator<JavaScriptNode>> suppliers) {
            Supplier<JavaScriptNode> readSupplier = suppliers.getFirst();
            UnaryOperator<JavaScriptNode> writeSupplier = suppliers.getSecond();
            return new Pair<>(() -> apply(readSupplier.get(), WrapAccess.Read),
                            (rhs) -> apply(writeSupplier.apply(rhs), WrapAccess.Write));
        }

        static WrapClosure compose(WrapClosure inner, WrapClosure before) {
            Objects.requireNonNull(before);
            if (inner == null) {
                return before;
            }
            return new WrapClosure() {
                @Override
                public JavaScriptNode apply(JavaScriptNode v, WrapAccess w) {
                    return inner.apply(before.apply(v, w), w);
                }

                @Override
                public Pair<Supplier<JavaScriptNode>, UnaryOperator<JavaScriptNode>> applyCompound(Pair<Supplier<JavaScriptNode>, UnaryOperator<JavaScriptNode>> suppliers) {
                    return inner.applyCompound(before.applyCompound(suppliers));
                }
            };
        }
    }

    public final VarRef findLocalVar(String name) {
        return findVar(name, true, true, false, true);
    }

    public final VarRef findVar(String name, boolean skipWith) {
        return findVar(name, skipWith, skipWith, false, false);
    }

    public final VarRef findVar(String name, boolean skipWith, boolean skipEval, boolean skipBlockScoped, boolean skipGlobal) {
        assert !name.equals(Null.NAME);
        Environment current = this;
        int frameLevel = 0;
        int scopeLevel = 0;
        WrapClosure wrapClosure = null;
        int wrapFrameLevel = 0;
        do {
            if (current instanceof WithEnvironment) {
                if (!skipWith) {
                    wrapClosure = makeWithWrapClosure(wrapClosure, name, ((WithEnvironment) current).getWithVarName());
                    wrapFrameLevel = frameLevel;
                }
                // with environments don't introduce any variables, skip lookup
            } else if (current instanceof GlobalEnvironment) {
                // We can distinguish 3 cases:
                // 1. HasLexicalDeclaration: statically resolved to lexical declaration (with TDZ).
                // 2. HasVarDeclaration: statically resolved to global object property.
                // Note: 1. and 2. mutually exclude each other (early SyntaxError).
                // 3. Undeclared global (external lexical declaration or global object property).
                // .. Dynamically resolved using lexical global scope and global object lookups.
                GlobalEnvironment globalEnv = (GlobalEnvironment) current;
                if (globalEnv.hasLexicalDeclaration(name) && !GlobalEnvironment.isGlobalObjectConstant(name)) {
                    return wrapIn(wrapClosure, wrapFrameLevel, new GlobalLexVarRef(name, globalEnv.hasConstDeclaration(name)));
                } else if (!globalEnv.hasVarDeclaration(name) && !GlobalEnvironment.isGlobalObjectConstant(name)) {
                    wrapClosure = makeGlobalWrapClosure(wrapClosure, name);
                }
            } else {
                FrameSlot slot = current.findBlockFrameSlot(name);
                if (slot != null) {
                    if (!skipBlockScoped || !(JSFrameUtil.isConst(slot) || JSFrameUtil.isLet(slot))) {
                        return wrapIn(wrapClosure, wrapFrameLevel, newFrameSlotVarRef(slot, scopeLevel, frameLevel, name, current));
                    }
                }
                if (current instanceof FunctionEnvironment) {
                    FunctionEnvironment fnEnv = current.function();
                    if (fnEnv.isNamedFunctionExpression() && fnEnv.getFunctionName().equals(name)) {
                        return wrapIn(wrapClosure, wrapFrameLevel, new FunctionCalleeVarRef(scopeLevel, frameLevel, name, current));
                    }
                    if (!skipEval && fnEnv.isDynamicallyScoped()) {
                        wrapClosure = makeEvalWrapClosure(wrapClosure, name, frameLevel, scopeLevel, current);
                        wrapFrameLevel = frameLevel;
                    }
                    if (!fnEnv.isGlobal() && !fnEnv.isEval() && name.equals(ARGUMENTS_NAME)) {
                        if (fnEnv.hasArgumentsSlot()) {
                            return wrapIn(wrapClosure, wrapFrameLevel, new ArgumentsVarRef(scopeLevel, frameLevel, name, current));
                        } else {
                            assert fnEnv.isArrowFunction();
                            // we need to go deeper
                        }
                    }

                    frameLevel++;
                    scopeLevel = 0;
                } else if (current instanceof BlockEnvironment) {
                    scopeLevel++;
                } else if (current instanceof DebugEnvironment) {
                    if (((DebugEnvironment) current).hasMember(name)) {
                        wrapClosure = makeDebugWrapClosure(wrapClosure, name);
                        wrapFrameLevel = frameLevel;
                    }
                }
            }
            current = current.getParent();
        } while (current != null);

        if (skipGlobal) {
            return null;
        }

        return wrapIn(wrapClosure, wrapFrameLevel, new GlobalVarRef(name));
    }

    void ensureFrameLevelAvailable(int frameLevel) {
        int level = 0;
        for (FunctionEnvironment currentFunction = this.function(); currentFunction != null && level < frameLevel; currentFunction = currentFunction.getParentFunction(), level++) {
            currentFunction.setNeedsParentFrame(true);
        }
    }

    private WrapClosure makeEvalWrapClosure(WrapClosure wrapClosure, String name, int frameLevel, int scopeLevel, Environment current) {
        final FrameSlot dynamicScopeSlot = current.findBlockFrameSlot(FunctionEnvironment.DYNAMIC_SCOPE_IDENTIFIER);
        assert dynamicScopeSlot != null;
        return WrapClosure.compose(wrapClosure, new WrapClosure() {
            @Override
            public JavaScriptNode apply(JavaScriptNode delegateNode, WrapAccess access) {
                JavaScriptNode dynamicScopeNode = createLocal(dynamicScopeSlot, frameLevel, scopeLevel);
                JSTargetableNode scopeAccessNode;
                if (access == WrapAccess.Delete) {
                    scopeAccessNode = factory.createDeleteProperty(null, factory.createConstantString(name), isStrictMode(), context);
                } else if (access == WrapAccess.Write) {
                    assert delegateNode instanceof WriteNode : delegateNode;
                    scopeAccessNode = factory.createWriteProperty(null, name, null, context, isStrictMode());
                } else if (access == WrapAccess.Read) {
                    assert delegateNode instanceof ReadNode || delegateNode instanceof RepeatableNode : delegateNode;
                    scopeAccessNode = factory.createReadProperty(context, null, name);
                } else {
                    throw new IllegalArgumentException();
                }
                return new EvalVariableNode(context, name, delegateNode, dynamicScopeNode, scopeAccessNode);
            }
        });
    }

    private WrapClosure makeWithWrapClosure(WrapClosure wrapClosure, String name, String withVarName) {
        return WrapClosure.compose(wrapClosure, new WrapClosure() {
            @Override
            public JavaScriptNode apply(JavaScriptNode delegateNode, WrapAccess access) {
                VarRef withVarNameRef = Objects.requireNonNull(findInternalSlot(withVarName));
                JSTargetableNode withAccessNode;
                if (access == WrapAccess.Delete) {
                    withAccessNode = factory.createDeleteProperty(null, factory.createConstantString(name), isStrictMode(), context);
                } else if (access == WrapAccess.Write) {
                    assert delegateNode instanceof WriteNode : delegateNode;
                    withAccessNode = factory.createWriteProperty(null, name, null, context, isStrictMode(), false, true);
                } else if (access == WrapAccess.Read) {
                    assert delegateNode instanceof ReadNode || delegateNode instanceof RepeatableNode : delegateNode;
                    withAccessNode = factory.createReadProperty(context, null, name);
                } else {
                    throw new IllegalArgumentException();
                }
                JavaScriptNode withTarget = factory.createWithTarget(context, name, withVarNameRef.createReadNode());
                return factory.createWithVarWrapper(name, withTarget, withAccessNode, delegateNode);
            }

            @Override
            public Pair<Supplier<JavaScriptNode>, UnaryOperator<JavaScriptNode>> applyCompound(Pair<Supplier<JavaScriptNode>, UnaryOperator<JavaScriptNode>> suppliers) {
                // Use temp var to ensure unscopables check is evaluated only once.
                VarRef withTargetTempVar = Environment.this.createTempVar();
                VarRef withObjVar = Objects.requireNonNull(findInternalSlot(withVarName));
                Supplier<JavaScriptNode> innerReadSupplier = suppliers.getFirst();
                UnaryOperator<JavaScriptNode> innerWriteSupplier = suppliers.getSecond();
                Supplier<JavaScriptNode> readSupplier = () -> {
                    JSTargetableNode readWithProperty = factory.createReadProperty(context, null, name);
                    return factory.createWithVarWrapper(name, withTargetTempVar.createReadNode(), readWithProperty, innerReadSupplier.get());
                };
                UnaryOperator<JavaScriptNode> writeSupplier = (rhs) -> {
                    JavaScriptNode withTarget = factory.createWithTarget(context, name, withObjVar.createReadNode());
                    WritePropertyNode writeWithProperty = factory.createWriteProperty(null, name, null, context, isStrictMode(), false, true);
                    return factory.createWithVarWrapper(name, withTargetTempVar.createWriteNode(withTarget), writeWithProperty, innerWriteSupplier.apply(rhs));
                };
                return new Pair<>(readSupplier, writeSupplier);
            }
        });
    }

    private WrapClosure makeGlobalWrapClosure(WrapClosure wrapClosure, String name) {
        return WrapClosure.compose(wrapClosure, new WrapClosure() {
            @Override
            public JavaScriptNode apply(JavaScriptNode delegateNode, WrapAccess access) {
                JSTargetableNode scopeAccessNode;
                if (access == WrapAccess.Delete) {
                    scopeAccessNode = factory.createDeleteProperty(null, factory.createConstantString(name), isStrictMode(), context);
                } else if (access == WrapAccess.Write) {
                    assert delegateNode instanceof WriteNode : delegateNode;
                    scopeAccessNode = factory.createWriteProperty(null, name, null, context, true);
                } else if (access == WrapAccess.Read) {
                    assert delegateNode instanceof ReadNode || delegateNode instanceof RepeatableNode : delegateNode;
                    scopeAccessNode = factory.createReadProperty(context, null, name);
                } else {
                    throw new IllegalArgumentException();
                }
                JavaScriptNode globalScope = factory.createGlobalScope(context);
                return factory.createGlobalVarWrapper(name, delegateNode, globalScope, scopeAccessNode);
            }
        });
    }

    private WrapClosure makeDebugWrapClosure(WrapClosure wrapClosure, String name) {
        return WrapClosure.compose(wrapClosure, new WrapClosure() {
            @Override
            public JavaScriptNode apply(JavaScriptNode delegateNode, WrapAccess access) {
                JSTargetableNode scopeAccessNode;
                if (access == WrapAccess.Delete) {
                    scopeAccessNode = factory.createDeleteProperty(null, factory.createConstantString(name), isStrictMode(), context);
                } else if (access == WrapAccess.Write) {
                    assert delegateNode instanceof WriteNode : delegateNode;
                    scopeAccessNode = factory.createWriteProperty(null, name, null, context, true);
                } else if (access == WrapAccess.Read) {
                    assert delegateNode instanceof ReadNode || delegateNode instanceof RepeatableNode : delegateNode;
                    scopeAccessNode = factory.createReadProperty(context, null, name);
                } else {
                    throw new IllegalArgumentException();
                }
                JavaScriptNode debugScope = factory.createDebugScope();
                return factory.createDebugVarWrapper(name, delegateNode, debugScope, scopeAccessNode);
            }
        });
    }

    private VarRef wrapIn(WrapClosure wrapClosure, int wrapFrameLevel, VarRef wrappee) {
        if (wrapClosure != null) {
            ensureFrameLevelAvailable(wrapFrameLevel);
            return new WrappedVarRef(wrappee.getName(), wrappee, wrapClosure);
        }
        return wrappee;
    }

    protected abstract FrameSlot findBlockFrameSlot(String name);

    public FrameDescriptor getBlockFrameDescriptor() {
        return getFunctionFrameDescriptor();
    }

    private VarRef newFrameSlotVarRef(FrameSlot slot, int scopeLevel, int frameLevel, String name, Environment current) {
        if (isMappedArgumentsParameter(slot, current)) {
            return new MappedArgumentVarRef(slot, scopeLevel, frameLevel, name, current);
        } else {
            return new FrameSlotVarRef(slot, scopeLevel, frameLevel, name, current);
        }
    }

    private JavaScriptNode findLocalVarNodeForArguments(Environment current, int frameLevel, int scopeLevel) {
        assert current.function().getArgumentsSlot() != null;
        JavaScriptNode argumentsVarNode = createReadArgumentObject(current, frameLevel, scopeLevel);
        if (function().isDirectArgumentsAccess()) {
            FunctionEnvironment currentFunction = current.function();
            JavaScriptNode createArgumentsObjectNode = factory.createArgumentsObjectNode(context, isStrictMode(), currentFunction.getLeadingArgumentCount(),
                            currentFunction.getTrailingArgumentCount());
            JavaScriptNode writeNode = factory.createWriteFrameSlot(currentFunction.getArgumentsSlot(), frameLevel, scopeLevel, currentFunction.getFunctionFrameDescriptor(),
                            getParentSlots(frameLevel, scopeLevel), createArgumentsObjectNode);
            return factory.createAccessArgumentsArrayDirectly(writeNode, argumentsVarNode, currentFunction.getLeadingArgumentCount(), currentFunction.getTrailingArgumentCount());
        } else {
            return argumentsVarNode;
        }
    }

    private static boolean isMappedArgumentsParameter(FrameSlot slot, Environment current) {
        FunctionEnvironment function = current.function();
        return function.getArgumentsSlot() != null && !function.isStrictMode() && function.hasSimpleParameterList() && function.isParam(slot);
    }

    private JavaScriptNode createReadParameterFromMappedArguments(Environment current, int frameLevel, int scopeLevel, FrameSlot slot) {
        assert current.function().hasSimpleParameterList();
        assert !current.function().isDirectArgumentsAccess();
        int parameterIndex = current.function().getParameterIndex(slot);
        JavaScriptNode readArgumentsObject = createReadArgumentObject(current, frameLevel, scopeLevel);
        ReadElementNode readArgumentsObjectElement = factory.createReadElementNode(context, factory.copy(readArgumentsObject), factory.createConstantInteger(parameterIndex));
        return factory.createGuardDisconnectedArgumentRead(parameterIndex, readArgumentsObjectElement, readArgumentsObject, slot);
    }

    private JavaScriptNode createWriteParameterFromMappedArguments(Environment current, int frameLevel, int scopeLevel, FrameSlot slot, JavaScriptNode rhs) {
        assert current.function().hasSimpleParameterList();
        assert !current.function().isDirectArgumentsAccess();
        int parameterIndex = current.function().getParameterIndex(slot);
        JavaScriptNode readArgumentsObject = createReadArgumentObject(current, frameLevel, scopeLevel);
        WriteElementNode writeArgumentsObjectElement = factory.createWriteElementNode(factory.copy(readArgumentsObject), factory.createConstantInteger(parameterIndex), null, context, false);
        return factory.createGuardDisconnectedArgumentWrite(parameterIndex, writeArgumentsObjectElement, readArgumentsObject, rhs, slot);
    }

    private JavaScriptNode createReadArgumentObject(Environment current, int frameLevel, int scopeLevel) {
        return createLocal(current.function().getArgumentsSlot(), frameLevel, scopeLevel);
    }

    public final Environment getParent() {
        return parent;
    }

    public abstract FunctionEnvironment function();

    public final Environment getParentAt(int frameLevel, int scopeLevel) {
        Environment current = this;
        int currentFrameLevel = 0;
        int currentScopeLevel = 0;
        do {
            if (currentFrameLevel == frameLevel && currentScopeLevel == scopeLevel) {
                return current;
            }
            if (current instanceof FunctionEnvironment) {
                currentFrameLevel++;
                currentScopeLevel = 0;
            } else if (current instanceof BlockEnvironment) {
                currentScopeLevel++;
            }
            current = current.getParent();
        } while (current != null);

        return null;
    }

    public VarRef createTempVar() {
        FrameSlot var = declareTempVar("tmp");
        return findTempVar(var);
    }

    public VarRef findTempVar(FrameSlot var) {
        return new VarRef((String) var.getIdentifier()) {
            @Override
            public boolean isGlobal() {
                return false;
            }

            @Override
            public boolean isFunctionLocal() {
                return false;
            }

            @Override
            public FrameSlot getFrameSlot() {
                return var;
            }

            @Override
            public JavaScriptNode createReadNode() {
                return factory.createLocal(var, 0, getScopeLevel(), getParentSlots());
            }

            @Override
            public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
                return factory.createWriteFrameSlot(var, 0, getScopeLevel(), getFunctionFrameDescriptor(), getParentSlots(), rhs);
            }

            @Override
            public JavaScriptNode createDeleteNode() {
                throw Errors.shouldNotReachHere();
            }
        };
    }

    private FrameSlot declareTempVar(String prefix) {
        return declareLocalVar("<" + prefix + getFunctionFrameDescriptor().getSize() + ">");
    }

    public FrameDescriptor getFunctionFrameDescriptor() {
        return function().getFunctionFrameDescriptor();
    }

    public boolean isStrictMode() {
        return function().isStrictMode();
    }

    public int getScopeLevel() {
        return 0;
    }

    public FrameSlot[] getParentSlots() {
        throw new UnsupportedOperationException(getClass().getName());
    }

    public final FrameSlot[] getParentSlots(int frameLevel, int scopeLevel) {
        if (scopeLevel == 0) {
            return ScopeFrameNode.EMPTY_FRAME_SLOT_ARRAY;
        }
        if (frameLevel > 0) {
            return function().getParent().getParentSlots(frameLevel - 1, scopeLevel);
        }
        FrameSlot[] parentSlots = getParentSlots();
        assert parentSlots.length >= scopeLevel;
        if (parentSlots.length == scopeLevel) {
            return parentSlots;
        } else {
            return Arrays.copyOf(parentSlots, scopeLevel);
        }
    }

    public void addFrameSlotsFromSymbols(Iterable<com.oracle.js.parser.ir.Symbol> symbols) {
        addFrameSlotsFromSymbols(symbols, false);
    }

    public void addFrameSlotsFromSymbols(Iterable<com.oracle.js.parser.ir.Symbol> symbols, boolean onlyBlockScoped) {
        for (com.oracle.js.parser.ir.Symbol symbol : symbols) {
            if (symbol.isBlockScoped() || (!onlyBlockScoped && symbol.isVar() && !symbol.isParam() && !symbol.isGlobal())) {
                addFrameSlotFromSymbol(symbol);
            }
        }
    }

    public void addFrameSlotFromSymbol(com.oracle.js.parser.ir.Symbol symbol) {
        // Frame slot may already exist for simple parameters and "arguments".
        assert !getBlockFrameDescriptor().getIdentifiers().contains(symbol.getName()) || this instanceof FunctionEnvironment;
        // other bits not needed
        int flags = symbol.getFlags() & JSFrameUtil.SYMBOL_FLAG_MASK;
        getBlockFrameDescriptor().findOrAddFrameSlot(symbol.getName(), FrameSlotFlags.of(flags), FrameSlotKind.Illegal);
    }

    public boolean isDynamicallyScoped() {
        return false;
    }

    /**
     * Environment chain contains a dynamic scope (eval or with) that may shadow variables.
     */
    public boolean isDynamicScopeContext() {
        return getParent() == null ? false : getParent().isDynamicScopeContext();
    }

    public Environment getVariableEnvironment() {
        return function().getVariableEnvironment();
    }

    public abstract static class VarRef {
        protected final String name;

        protected VarRef(String name) {
            this.name = name;
        }

        public abstract JavaScriptNode createReadNode();

        public abstract JavaScriptNode createWriteNode(JavaScriptNode rhs);

        public abstract boolean isFunctionLocal();

        public boolean isFrameVar() {
            return getFrameSlot() != null;
        }

        public abstract boolean isGlobal();

        public boolean isConst() {
            return false;
        }

        public FrameSlot getFrameSlot() {
            return null;
        }

        public String getName() {
            return name;
        }

        public abstract JavaScriptNode createDeleteNode();

        public Pair<Supplier<JavaScriptNode>, UnaryOperator<JavaScriptNode>> createCompoundAssignNode() {
            return new Pair<>(this::createReadNode, this::createWriteNode);
        }

        public VarRef withTDZCheck() {
            return this;
        }

        public VarRef withRequired(@SuppressWarnings("unused") boolean required) {
            return this;
        }

        public boolean hasTDZCheck() {
            return false;
        }
    }

    public abstract class AbstractFrameVarRef extends VarRef {
        protected final int scopeLevel;
        protected final int frameLevel;
        protected final Environment current;

        public AbstractFrameVarRef(int scopeLevel, int frameLevel, String name, Environment current) {
            super(name);
            this.scopeLevel = scopeLevel;
            this.frameLevel = frameLevel;
            this.current = current;
            ensureFrameLevelAvailable(frameLevel);
        }

        public int getScopeLevel() {
            return scopeLevel;
        }

        public int getFrameLevel() {
            return frameLevel;
        }

        @Override
        public boolean isFunctionLocal() {
            return frameLevel == 0;
        }

        @Override
        public boolean isGlobal() {
            return false;
        }

        @Override
        public JavaScriptNode createDeleteNode() {
            return factory.createConstantBoolean(false);
        }

        public ScopeFrameNode createScopeFrameNode() {
            return factory.createScopeFrame(frameLevel, scopeLevel, getParentSlots(frameLevel, scopeLevel));
        }

        public FrameDescriptor getFrameDescriptor() {
            return current.getBlockFrameDescriptor();
        }
    }

    public class FrameSlotVarRef extends AbstractFrameVarRef {
        protected final FrameSlot frameSlot;
        private final boolean checkTDZ;

        public FrameSlotVarRef(FrameSlot frameSlot, int scopeLevel, int frameLevel, String name, Environment current) {
            this(frameSlot, scopeLevel, frameLevel, name, current, JSFrameUtil.needsTemporalDeadZoneCheck(frameSlot, frameLevel));
        }

        public FrameSlotVarRef(FrameSlot frameSlot, int scopeLevel, int frameLevel, String name, Environment current, boolean checkTDZ) {
            super(scopeLevel, frameLevel, name, current);
            this.frameSlot = frameSlot;
            this.checkTDZ = checkTDZ;
        }

        @Override
        public FrameSlot getFrameSlot() {
            return frameSlot;
        }

        @Override
        public boolean isConst() {
            return JSFrameUtil.isConst(frameSlot);
        }

        @Override
        public JavaScriptNode createReadNode() {
            JavaScriptNode readNode = createLocal(frameSlot, frameLevel, scopeLevel, checkTDZ);
            if (JSFrameUtil.isImportBinding(frameSlot)) {
                return factory.createReadImportBinding(readNode);
            }
            return readNode;
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return factory.createWriteFrameSlot(frameSlot, frameLevel, scopeLevel, current.getBlockFrameDescriptor(), getParentSlots(frameLevel, scopeLevel), rhs, checkTDZ);
        }

        @Override
        public VarRef withTDZCheck() {
            if (this.checkTDZ || !JSFrameUtil.hasTemporalDeadZone(frameSlot)) {
                return this;
            }
            return new FrameSlotVarRef(frameSlot, scopeLevel, frameLevel, name, current, true);
        }

        @Override
        public boolean hasTDZCheck() {
            return checkTDZ;
        }
    }

    private abstract class AbstractArgumentsVarRef extends AbstractFrameVarRef {
        AbstractArgumentsVarRef(int scopeLevel, int frameLevel, String name, Environment current) {
            super(scopeLevel, frameLevel, name, current);
        }

        @Override
        public FrameSlot getFrameSlot() {
            return null;
        }
    }

    private final class FunctionCalleeVarRef extends AbstractArgumentsVarRef {
        FunctionCalleeVarRef(int scopeLevel, int frameLevel, String name, Environment current) {
            super(scopeLevel, frameLevel, name, current);
        }

        @Override
        public JavaScriptNode createReadNode() {
            return factory.createAccessCallee(frameLevel);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return factory.createWriteConstantVariable(rhs, isStrictMode());
        }
    }

    private final class ArgumentsVarRef extends AbstractArgumentsVarRef {
        ArgumentsVarRef(int scopeLevel, int frameLevel, String name, Environment current) {
            super(scopeLevel, frameLevel, name, current);
        }

        @Override
        public JavaScriptNode createReadNode() {
            return findLocalVarNodeForArguments(current, frameLevel, scopeLevel);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            assert !current.function().isDirectArgumentsAccess();
            return factory.createWriteFrameSlot(current.function().getArgumentsSlot(), frameLevel, scopeLevel, current.getFunctionFrameDescriptor(), getParentSlots(frameLevel, scopeLevel), rhs);
        }
    }

    public class MappedArgumentVarRef extends AbstractArgumentsVarRef {
        protected final FrameSlot frameSlot;

        public MappedArgumentVarRef(FrameSlot frameSlot, int scopeLevel, int frameLevel, String name, Environment current) {
            super(scopeLevel, frameLevel, name, current);
            this.frameSlot = frameSlot;
            assert !JSFrameUtil.hasTemporalDeadZone(frameSlot);
        }

        @Override
        public JavaScriptNode createReadNode() {
            return createReadParameterFromMappedArguments(current, frameLevel, scopeLevel, frameSlot);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return createWriteParameterFromMappedArguments(current, frameLevel, scopeLevel, frameSlot, rhs);
        }
    }

    public class GlobalVarRef extends VarRef {
        private final boolean required;

        public GlobalVarRef(String name) {
            this(name, true);
        }

        private GlobalVarRef(String name, boolean required) {
            super(name);
            assert !name.equals(Null.NAME);
            this.required = required;
        }

        @Override
        public JavaScriptNode createReadNode() {
            if (name.equals(Undefined.NAME)) {
                return factory.createConstantUndefined();
            }

            if (!required) {
                return factory.createReadProperty(context, factory.createGlobalObject(context), name);
            }

            return factory.createReadGlobalProperty(context, name);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return factory.createWriteProperty(factory.createGlobalObject(context), name, rhs, context, isStrictMode(), isGlobal(), required);
        }

        @Override
        public boolean isFunctionLocal() {
            return false;
        }

        @Override
        public FrameSlot getFrameSlot() {
            return null;
        }

        @Override
        public boolean isGlobal() {
            return true;
        }

        @Override
        public JavaScriptNode createDeleteNode() {
            JavaScriptNode element = factory.createConstantString(name);
            JavaScriptNode object = factory.createGlobalObject(context);
            return factory.createDeleteProperty(object, element, isStrictMode(), context);
        }

        @Override
        public VarRef withRequired(@SuppressWarnings("hiding") boolean required) {
            if (this.required != required) {
                return new GlobalVarRef(name, required);
            }
            return this;
        }
    }

    public class GlobalLexVarRef extends VarRef {
        private final boolean isConst;
        private final boolean required;
        private final boolean checkTDZ;

        public GlobalLexVarRef(String name, boolean isConst) {
            this(name, isConst, true, false);
        }

        private GlobalLexVarRef(String name, boolean isConst, boolean required, boolean checkTDZ) {
            super(name);
            assert !name.equals(Null.NAME) && !name.equals(Undefined.NAME);
            this.isConst = isConst;
            this.required = required;
            this.checkTDZ = checkTDZ;
        }

        @Override
        public JavaScriptNode createReadNode() {
            if (!required) {
                JavaScriptNode globalScope = factory.createGlobalScopeTDZCheck(context, name, checkTDZ);
                return factory.createReadProperty(context, globalScope, name);
            }
            return factory.createReadLexicalGlobal(name, checkTDZ, context);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            JavaScriptNode globalScope = factory.createGlobalScopeTDZCheck(context, name, checkTDZ);
            return factory.createWriteProperty(globalScope, name, rhs, context, true, required, false);
        }

        @Override
        public boolean isFunctionLocal() {
            return function().isGlobal();
        }

        @Override
        public FrameSlot getFrameSlot() {
            return null;
        }

        @Override
        public boolean isGlobal() {
            return true;
        }

        @Override
        public boolean isConst() {
            return isConst;
        }

        @Override
        public JavaScriptNode createDeleteNode() {
            JavaScriptNode element = factory.createConstantString(name);
            JavaScriptNode object = factory.createGlobalScope(context);
            return factory.createDeleteProperty(object, element, isStrictMode(), context);
        }

        @Override
        public VarRef withRequired(@SuppressWarnings("hiding") boolean required) {
            if (this.required != required) {
                return new GlobalLexVarRef(name, isConst, required, checkTDZ);
            }
            return this;
        }

        @Override
        public VarRef withTDZCheck() {
            if (!this.checkTDZ) {
                return new GlobalLexVarRef(name, isConst, required, true);
            }
            return this;
        }

        @Override
        public boolean hasTDZCheck() {
            return checkTDZ;
        }
    }

    public class WrappedVarRef extends VarRef {
        private final VarRef wrappee;
        private final WrapClosure wrapClosure;

        public WrappedVarRef(String name, VarRef wrappee, WrapClosure wrapClosure) {
            super(name);
            this.wrappee = wrappee;
            this.wrapClosure = wrapClosure;
            assert !(wrappee instanceof WrappedVarRef); // currently unexpected
        }

        @Override
        public JavaScriptNode createReadNode() {
            return wrapClosure.apply(wrappee.createReadNode(), WrapAccess.Read);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            JavaScriptNode writeNode = wrappee.isConst() ? factory.createWriteConstantVariable(rhs, true) : wrappee.createWriteNode(rhs);
            return wrapClosure.apply(writeNode, WrapAccess.Write);
        }

        @Override
        public JavaScriptNode createDeleteNode() {
            return wrapClosure.apply(wrappee.createDeleteNode(), WrapAccess.Delete);
        }

        @Override
        public Pair<Supplier<JavaScriptNode>, UnaryOperator<JavaScriptNode>> createCompoundAssignNode() {
            return wrapClosure.applyCompound(wrappee.createCompoundAssignNode());
        }

        @Override
        public boolean isFunctionLocal() {
            return wrappee.isFunctionLocal();
        }

        @Override
        public FrameSlot getFrameSlot() {
            return null;
        }

        @Override
        public boolean isGlobal() {
            return wrappee.isGlobal();
        }

        public VarRef getWrappee() {
            return wrappee;
        }

        @Override
        public VarRef withTDZCheck() {
            return new WrappedVarRef(name, wrappee.withTDZCheck(), wrapClosure);
        }

        @Override
        public VarRef withRequired(boolean required) {
            return new WrappedVarRef(name, wrappee.withRequired(required), wrapClosure);
        }

        @Override
        public boolean hasTDZCheck() {
            return wrappee.hasTDZCheck();
        }
    }

    class LazyFrameSlotVarRef extends AbstractFrameVarRef {
        protected final FrameSlot frameSlot;

        LazyFrameSlotVarRef(FrameSlot frameSlot, int scopeLevel, int frameLevel, String name, Environment current) {
            super(scopeLevel, frameLevel, name, current);
            this.frameSlot = frameSlot;
        }

        @Override
        public FrameSlot getFrameSlot() {
            return frameSlot;
        }

        @Override
        public JavaScriptNode createReadNode() {
            return factory.createLazyReadFrameSlot(frameSlot.getIdentifier());
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return factory.createLazyWriteFrameSlot(frameSlot.getIdentifier(), rhs);
        }
    }

    class DebugVarRef extends VarRef {
        DebugVarRef(String name) {
            super(name);
        }

        @Override
        public JavaScriptNode createReadNode() {
            return factory.createDebugVarWrapper(name, factory.createConstantUndefined(), factory.createDebugScope(), factory.createReadProperty(context, null, name));
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return factory.createWriteConstantVariable(rhs, isStrictMode());
        }

        @Override
        public JavaScriptNode createDeleteNode() {
            return factory.createConstantBoolean(false);
        }

        @Override
        public boolean isFunctionLocal() {
            return false;
        }

        @Override
        public boolean isGlobal() {
            return false;
        }
    }

    private static final class FrameSlotFlags {
        private static final Map<Integer, Integer> cachedFlags = new ConcurrentHashMap<>();

        static Integer of(int flags) {
            Integer boxed = Integer.valueOf(flags);
            if (flags >= 128) {
                Integer cached = cachedFlags.get(boxed);
                if (cached != null) {
                    return cached;
                }
                cached = cachedFlags.putIfAbsent(boxed, boxed);
                if (cached != null) {
                    return cached;
                }
            }
            return boxed;
        }
    }
}

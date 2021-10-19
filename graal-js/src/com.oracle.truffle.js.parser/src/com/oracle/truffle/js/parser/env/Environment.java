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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.oracle.js.parser.ir.Symbol;
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
import com.oracle.truffle.js.runtime.util.InternalSlotId;
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

    public boolean hasLocalVar(Object name) {
        return getFunctionFrameDescriptor().findFrameSlot(name) != null;
    }

    public VarRef findThisVar() {
        return findInternalSlot(FunctionEnvironment.THIS_SLOT_IDENTIFIER, true);
    }

    public void reserveThisSlot() {
        getBlockFrameDescriptor().findOrAddFrameSlot(FunctionEnvironment.THIS_SLOT_IDENTIFIER);
    }

    public VarRef findSuperVar() {
        assert !function().isGlobal();
        return findInternalSlot(FunctionEnvironment.SUPER_SLOT_IDENTIFIER);
    }

    public void reserveSuperSlot() {
        getBlockFrameDescriptor().findOrAddFrameSlot(FunctionEnvironment.SUPER_SLOT_IDENTIFIER);
    }

    public VarRef findArgumentsVar() {
        assert !function().isGlobal();
        return findInternalSlot(FunctionEnvironment.ARGUMENTS_SLOT_IDENTIFIER);
    }

    public void reserveArgumentsSlot() {
        getBlockFrameDescriptor().findOrAddFrameSlot(FunctionEnvironment.ARGUMENTS_SLOT_IDENTIFIER);
    }

    public VarRef findNewTargetVar() {
        assert !function().isGlobal();
        return findInternalSlot(FunctionEnvironment.NEW_TARGET_SLOT_IDENTIFIER);
    }

    public void reserveNewTargetSlot() {
        getBlockFrameDescriptor().findOrAddFrameSlot(FunctionEnvironment.NEW_TARGET_SLOT_IDENTIFIER);
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

    public void reserveDynamicScopeSlot() {
        assert !function().isGlobal();
        getBlockFrameDescriptor().findOrAddFrameSlot(FunctionEnvironment.DYNAMIC_SCOPE_IDENTIFIER);
    }

    public FrameSlot declareInternalSlot(Object name) {
        assert name instanceof String || name instanceof InternalSlotId : name;
        return getBlockFrameDescriptor().findOrAddFrameSlot(name);
    }

    public final JavaScriptNode createLocal(FrameSlot frameSlot, int frameLevel, int scopeLevel) {
        return factory.createReadFrameSlot(frameSlot, factory.createScopeFrame(frameLevel, scopeLevel, getParentSlots(frameLevel, scopeLevel), getBlockScopeSlot(frameLevel, scopeLevel)), false);
    }

    public final VarRef findInternalSlot(Object name) {
        return findInternalSlot(name, false);
    }

    protected final VarRef findInternalSlot(Object name, boolean allowDebug) {
        Environment current = this;
        int frameLevel = 0;
        int scopeLevel = 0;
        do {
            FrameSlot slot = current.findBlockFrameSlot(name);
            if (slot != null) {
                return new FrameSlotVarRef(slot, scopeLevel, frameLevel, name, current);
            }
            if (current instanceof FunctionEnvironment) {
                frameLevel++;
                scopeLevel = 0;
            } else if (current instanceof BlockEnvironment) {
                scopeLevel++;
            } else if (current instanceof DebugEnvironment && name instanceof String) {
                if (allowDebug && ((DebugEnvironment) current).hasMember((String) name)) {
                    return new DebugVarRef((String) name, frameLevel);
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
        return findVar(name, true, true, false, true, true);
    }

    public final VarRef findVar(String name, boolean skipWith) {
        return findVar(name, skipWith, skipWith, false, false, false);
    }

    public final VarRef findVar(String name, boolean skipWith, boolean skipEval, boolean skipBlockScoped, boolean skipGlobal, boolean skipMapped) {
        assert !name.equals(Null.NAME);
        Environment current = this;
        int frameLevel = 0;
        int scopeLevel = 0;
        WrapClosure wrapClosure = null;
        int wrapFrameLevel = 0;
        do {
            if (current instanceof WithEnvironment) {
                if (!skipWith) {
                    wrapClosure = makeWithWrapClosure(wrapClosure, name, ((WithEnvironment) current).getWithVarIdentifier());
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
            } else if (current instanceof DebugEnvironment) {
                if (((DebugEnvironment) current).hasMember(name)) {
                    wrapClosure = makeDebugWrapClosure(wrapClosure, name, frameLevel);
                    wrapFrameLevel = frameLevel;
                }
            } else {
                FrameSlot slot = current.findBlockFrameSlot(name);
                if (slot != null) {
                    if (!skipBlockScoped || !(JSFrameUtil.isConst(slot) || JSFrameUtil.isLet(slot))) {
                        VarRef varRef;
                        if (!skipMapped && isMappedArgumentsParameter(slot, current)) {
                            varRef = new MappedArgumentVarRef(slot, scopeLevel, frameLevel, name, current);
                        } else if (JSFrameUtil.isArguments(slot)) {
                            assert !current.function().isArrowFunction() && !current.function().isGlobal() && !current.function().isEval();
                            varRef = new ArgumentsVarRef(slot, scopeLevel, frameLevel, name, current);
                        } else {
                            varRef = new FrameSlotVarRef(slot, scopeLevel, frameLevel, name, current);
                        }
                        return wrapIn(wrapClosure, wrapFrameLevel, varRef);
                    }
                }

                if (!skipEval && current.function().isDynamicallyScoped() && current.findBlockFrameSlot(FunctionEnvironment.DYNAMIC_SCOPE_IDENTIFIER) != null) {
                    wrapClosure = makeEvalWrapClosure(wrapClosure, name, frameLevel, scopeLevel, current);
                    wrapFrameLevel = frameLevel;
                }

                if (current instanceof FunctionEnvironment) {
                    FunctionEnvironment fnEnv = current.function();
                    if (fnEnv.isNamedFunctionExpression() && fnEnv.getFunctionName().equals(name)) {
                        return wrapIn(wrapClosure, wrapFrameLevel, new FunctionCalleeVarRef(scopeLevel, frameLevel, name, current));
                    }

                    frameLevel++;
                    scopeLevel = 0;
                } else if (current instanceof BlockEnvironment) {
                    scopeLevel++;
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
                JavaScriptNode dynamicScopeNode = new FrameSlotVarRef(dynamicScopeSlot, scopeLevel, frameLevel, FunctionEnvironment.DYNAMIC_SCOPE_IDENTIFIER, current).createReadNode();
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

    private WrapClosure makeWithWrapClosure(WrapClosure wrapClosure, String name, Object withVarName) {
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

    private WrapClosure makeDebugWrapClosure(WrapClosure wrapClosure, String name, int frameLevel) {
        ensureFrameLevelAvailable(frameLevel);
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
                JavaScriptNode debugScope = factory.createDebugScope(context, factory.createAccessCallee(frameLevel - 1));
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

    protected abstract FrameSlot findBlockFrameSlot(Object name);

    public FrameDescriptor getBlockFrameDescriptor() {
        throw new UnsupportedOperationException();
    }

    private static boolean isMappedArgumentsParameter(FrameSlot slot, Environment current) {
        FunctionEnvironment function = current.function();
        return function.hasMappedParameters() && !function.isStrictMode() && function.hasSimpleParameterList() && JSFrameUtil.isParam(slot);
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
        return new VarRef(var.getIdentifier()) {
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
                return factory.createReadCurrentFrameSlot(var);
            }

            @Override
            public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
                return factory.createWriteCurrentFrameSlot(var, getFunctionFrameDescriptor(), rhs);
            }

            @Override
            public JavaScriptNode createDeleteNode() {
                throw Errors.shouldNotReachHere();
            }
        };
    }

    private FrameSlot declareTempVar(String prefix) {
        return declareLocalVar(factory.createInternalSlotId(prefix, getFunctionFrameDescriptor().getSize()));
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
        Environment current = this;
        for (int currentFrameLevel = frameLevel; currentFrameLevel > 0; currentFrameLevel--) {
            current = current.function().getParent();
        }

        while (current != null) {
            if (current instanceof BlockEnvironment) {
                FrameSlot[] parentSlots = current.getParentSlots();
                assert parentSlots.length >= scopeLevel;
                if (parentSlots.length == scopeLevel) {
                    return parentSlots;
                } else {
                    return Arrays.copyOf(parentSlots, scopeLevel);
                }
            }
            current = current.getParent();
        }
        return ScopeFrameNode.EMPTY_FRAME_SLOT_ARRAY;
    }

    public final FrameSlot getBlockScopeSlot(int frameLevel, int scopeLevel) {
        Environment current = this;
        for (int currentFrameLevel = frameLevel; currentFrameLevel > 0; currentFrameLevel--) {
            current = current.function().getParent();
        }
        int currentScopeLevel = scopeLevel;
        while (current != null) {
            if (current instanceof FunctionEnvironment) {
                assert currentScopeLevel == 0;
                return null;
            } else if (current instanceof BlockEnvironment) {
                if (currentScopeLevel == 0) {
                    return function().getBlockScopeSlot();
                }
                currentScopeLevel--;
            }
            current = current.getParent();
        }
        return null;
    }

    public FrameSlot getCurrentBlockScopeSlot() {
        return null;
    }

    public void addFrameSlotsFromSymbols(Iterable<com.oracle.js.parser.ir.Symbol> symbols) {
        addFrameSlotsFromSymbols(symbols, false, null);
    }

    public void addFrameSlotsFromSymbols(Iterable<com.oracle.js.parser.ir.Symbol> symbols, boolean onlyBlockScoped, Predicate<Symbol> filter) {
        for (com.oracle.js.parser.ir.Symbol symbol : symbols) {
            if (symbol.isBlockScoped() || (!onlyBlockScoped && symbol.isVar() && !symbol.isGlobal())) {
                if (filter == null || filter.test(symbol)) {
                    addFrameSlotFromSymbol(symbol);
                }
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
        protected final Object name;

        protected VarRef(Object name) {
            assert name instanceof String || name instanceof InternalSlotId : name;
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
            assert name instanceof String : name;
            return (String) name;
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

        public AbstractFrameVarRef(int scopeLevel, int frameLevel, Object name, Environment current) {
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
            int effectiveScopeLevel = getEffectiveScopeLevel();
            return factory.createScopeFrame(frameLevel, effectiveScopeLevel, getParentSlots(frameLevel, effectiveScopeLevel), getBlockScopeSlot());
        }

        public FrameDescriptor getFrameDescriptor() {
            return current.getBlockFrameDescriptor();
        }

        public FrameSlot getBlockScopeSlot() {
            return current.getCurrentBlockScopeSlot();
        }

        public int getEffectiveScopeLevel() {
            return (current instanceof FunctionEnvironment && frameLevel == 0) ? 0 : scopeLevel;
        }
    }

    public class FrameSlotVarRef extends AbstractFrameVarRef {
        protected final FrameSlot frameSlot;
        private final boolean checkTDZ;

        public FrameSlotVarRef(FrameSlot frameSlot, int scopeLevel, int frameLevel, Object name, Environment current) {
            this(frameSlot, scopeLevel, frameLevel, name, current, JSFrameUtil.needsTemporalDeadZoneCheck(frameSlot, frameLevel));
        }

        public FrameSlotVarRef(FrameSlot frameSlot, int scopeLevel, int frameLevel, Object name, Environment current, boolean checkTDZ) {
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
            JavaScriptNode readNode = factory.createReadFrameSlot(frameSlot, createScopeFrameNode(), checkTDZ);
            if (JSFrameUtil.isImportBinding(frameSlot)) {
                return factory.createReadImportBinding(readNode);
            }
            return readNode;
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return factory.createWriteFrameSlot(frameSlot, createScopeFrameNode(), current.getBlockFrameDescriptor(), rhs, checkTDZ);
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
        private final FrameSlot frameSlot;

        ArgumentsVarRef(FrameSlot frameSlot, int scopeLevel, int frameLevel, String name, Environment current) {
            super(scopeLevel, frameLevel, name, current);
            this.frameSlot = frameSlot;
        }

        @Override
        public JavaScriptNode createReadNode() {
            JavaScriptNode argumentsVarNode = factory.createReadFrameSlot(frameSlot, createScopeFrameNode());
            if (function().isDirectArgumentsAccess()) {
                FunctionEnvironment currentFunction = current.function();
                JavaScriptNode createArgumentsObjectNode = factory.createArgumentsObjectNode(context, isStrictMode(), currentFunction.getLeadingArgumentCount(),
                                currentFunction.getTrailingArgumentCount());
                JavaScriptNode writeNode = factory.createWriteFrameSlot(frameSlot, createScopeFrameNode(), current.getBlockFrameDescriptor(), createArgumentsObjectNode);
                return factory.createAccessArgumentsArrayDirectly(writeNode, argumentsVarNode, currentFunction.getLeadingArgumentCount(), currentFunction.getTrailingArgumentCount());
            } else {
                return argumentsVarNode;
            }
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            assert !current.function().isDirectArgumentsAccess();
            return factory.createWriteFrameSlot(frameSlot, createScopeFrameNode(), current.getBlockFrameDescriptor(), rhs);
        }

        @Override
        public FrameSlot getFrameSlot() {
            return frameSlot;
        }
    }

    public class MappedArgumentVarRef extends AbstractArgumentsVarRef {
        private final FrameSlot frameSlot;
        private final int parameterIndex;
        private final FrameSlot argumentsSlot;

        public MappedArgumentVarRef(FrameSlot frameSlot, int scopeLevel, int frameLevel, String name, Environment current) {
            super(scopeLevel, frameLevel, name, current);
            assert !JSFrameUtil.hasTemporalDeadZone(frameSlot);
            assert current.function().hasSimpleParameterList();
            assert !current.function().isDirectArgumentsAccess();
            this.argumentsSlot = Objects.requireNonNull(current.getBlockFrameDescriptor().findFrameSlot(FunctionEnvironment.ARGUMENTS_SLOT_IDENTIFIER));
            this.frameSlot = frameSlot;
            this.parameterIndex = current.function().getMappedParameterIndex(frameSlot);
        }

        private JavaScriptNode createReadArgumentsObject() {
            return factory.createReadFrameSlot(argumentsSlot, createScopeFrameNode());
        }

        @Override
        public JavaScriptNode createReadNode() {
            JavaScriptNode readArgumentsObject = createReadArgumentsObject();
            ReadElementNode readArgumentsObjectElement = factory.createReadElementNode(context, factory.copy(readArgumentsObject), factory.createConstantInteger(parameterIndex));
            return factory.createGuardDisconnectedArgumentRead(parameterIndex, readArgumentsObjectElement, readArgumentsObject, frameSlot);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            JavaScriptNode readArgumentsObject = createReadArgumentsObject();
            WriteElementNode writeArgumentsObjectElement = factory.createWriteElementNode(factory.copy(readArgumentsObject), factory.createConstantInteger(parameterIndex), null, context, false);
            return factory.createGuardDisconnectedArgumentWrite(parameterIndex, writeArgumentsObjectElement, readArgumentsObject, rhs, frameSlot);
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
                return factory.createReadProperty(context, factory.createGlobalObject(), getName());
            }

            return factory.createReadGlobalProperty(context, getName());
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return factory.createWriteProperty(factory.createGlobalObject(), getName(), rhs, context, isStrictMode(), isGlobal(), required);
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
            JavaScriptNode element = factory.createConstantString(getName());
            JavaScriptNode object = factory.createGlobalObject();
            return factory.createDeleteProperty(object, element, isStrictMode(), context);
        }

        @Override
        public VarRef withRequired(@SuppressWarnings("hiding") boolean required) {
            if (this.required != required) {
                return new GlobalVarRef(getName(), required);
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

        private GlobalLexVarRef(Object name, boolean isConst, boolean required, boolean checkTDZ) {
            super(name);
            assert !name.equals(Null.NAME) && !name.equals(Undefined.NAME);
            this.isConst = isConst;
            this.required = required;
            this.checkTDZ = checkTDZ;
        }

        @Override
        public JavaScriptNode createReadNode() {
            if (!required) {
                JavaScriptNode globalScope = factory.createGlobalScopeTDZCheck(context, getName(), checkTDZ);
                return factory.createReadProperty(context, globalScope, getName());
            }
            return factory.createReadLexicalGlobal(getName(), checkTDZ, context);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            JavaScriptNode globalScope = factory.createGlobalScopeTDZCheck(context, getName(), checkTDZ);
            return factory.createWriteProperty(globalScope, getName(), rhs, context, true, required, false);
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
            JavaScriptNode element = factory.createConstantString(getName());
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

        public WrappedVarRef(Object name, VarRef wrappee, WrapClosure wrapClosure) {
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

    class DebugVarRef extends VarRef {
        private final int frameLevel;

        DebugVarRef(String name, int frameLevel) {
            super(name);
            this.frameLevel = frameLevel;
            ensureFrameLevelAvailable(frameLevel);
        }

        @Override
        public JavaScriptNode createReadNode() {
            JavaScriptNode debugScope = factory.createDebugScope(context, factory.createAccessCallee(frameLevel - 1));
            return factory.createDebugVarWrapper(getName(), factory.createConstantUndefined(), debugScope, factory.createReadProperty(context, null, getName()));
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Environment current = this;
        int frameLevel = 0;
        int scopeLevel = 0;
        do {
            if (current instanceof FunctionEnvironment) {
                sb.append("Function").append("(").append(frameLevel).append(")");
                sb.append(current.getFunctionFrameDescriptor().getIdentifiers().toString());

                frameLevel++;
                scopeLevel = 0;
            } else if (current instanceof BlockEnvironment) {
                sb.append("Block").append("(").append(frameLevel).append(", ").append(scopeLevel).append(")");
                sb.append(current.getBlockFrameDescriptor().getIdentifiers().toString());

                scopeLevel++;
            } else {
                sb.append(current.getClass().getSimpleName());
            }
            current = current.getParent();
            if (current != null) {
                sb.append('\n');
            }
        } while (current != null);
        return sb.toString();
    }
}

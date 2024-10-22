/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.oracle.js.parser.ir.Scope;
import com.oracle.js.parser.ir.Symbol;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSFrameDescriptor;
import com.oracle.truffle.js.nodes.JSFrameSlot;
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

    private final Environment parent;
    private final FunctionEnvironment functionEnvironment;
    protected final NodeFactory factory;
    protected final JSContext context;

    public Environment(Environment parent, NodeFactory factory, JSContext context) {
        this.parent = parent;
        this.factory = factory;
        this.context = context;
        this.functionEnvironment = this instanceof FunctionEnvironment ? (FunctionEnvironment) this : (parent == null ? null : parent.functionEnvironment);
    }

    public JSFrameSlot declareLocalVar(Object name) {
        return function().declareLocalVar(name);
    }

    public boolean hasLocalVar(Object name) {
        return getFunctionFrameDescriptor().findFrameSlot(name) != null;
    }

    public VarRef findThisVar() {
        return findInternalSlot(FunctionEnvironment.THIS_SLOT_IDENTIFIER, true);
    }

    public void reserveThisSlot() {
        declareInternalSlot(FunctionEnvironment.THIS_SLOT_IDENTIFIER);
    }

    public VarRef findSuperVar() {
        assert !function().isGlobal();
        return findInternalSlot(FunctionEnvironment.SUPER_SLOT_IDENTIFIER);
    }

    public void reserveSuperSlot() {
        declareInternalSlot(FunctionEnvironment.SUPER_SLOT_IDENTIFIER);
    }

    public VarRef findArgumentsVar() {
        assert !function().isGlobal();
        return findInternalSlot(FunctionEnvironment.ARGUMENTS_SLOT_IDENTIFIER);
    }

    public void reserveArgumentsSlot() {
        declareInternalSlot(FunctionEnvironment.ARGUMENTS_SLOT_IDENTIFIER);
    }

    public VarRef findNewTargetVar() {
        assert !function().isGlobal();
        return findInternalSlot(FunctionEnvironment.NEW_TARGET_SLOT_IDENTIFIER);
    }

    public void reserveNewTargetSlot() {
        declareInternalSlot(FunctionEnvironment.NEW_TARGET_SLOT_IDENTIFIER);
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
        declareInternalSlot(FunctionEnvironment.DYNAMIC_SCOPE_IDENTIFIER);
    }

    public JSFrameSlot declareInternalSlot(@SuppressWarnings("unused") Object name) {
        throw unsupported();
    }

    public final JavaScriptNode createLocal(JSFrameSlot frameSlot, int frameLevel, int scopeLevel) {
        return factory.createReadFrameSlot(frameSlot, factory.createScopeFrame(frameLevel, scopeLevel, getBlockScopeSlot(frameLevel, scopeLevel)), false);
    }

    public final VarRef findInternalSlot(Object name) {
        return findInternalSlot(name, false, 0);
    }

    public final VarRef findInternalSlot(Object name, boolean allowDebug) {
        return findInternalSlot(name, allowDebug, 0);
    }

    protected final VarRef findInternalSlot(Object name, boolean allowDebug, int skippedFrames) {
        Environment current = this;
        int frameLevel = 0;
        int scopeLevel = 0;
        do {
            if (frameLevel >= skippedFrames) {
                int effectiveScopeLevel = scopeLevel;
                JSFrameSlot slot = current.findBlockFrameSlot(name);
                if (slot == null) {
                    slot = current.findFunctionFrameSlot(name);
                    effectiveScopeLevel += current.getScopeLevel();
                }
                if (slot != null) {
                    return new FrameSlotVarRef(slot, effectiveScopeLevel, frameLevel, name, current);
                }
            }

            if (current instanceof FunctionEnvironment) {
                frameLevel++;
                scopeLevel = 0;
            } else if (current instanceof BlockEnvironment && current.hasScopeFrame()) {
                scopeLevel++;
            } else if (current instanceof DebugEnvironment && name instanceof TruffleString nameStr) {
                if (allowDebug && ((DebugEnvironment) current).hasMember(nameStr)) {
                    return new DebugVarRef(nameStr, frameLevel);
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

    public final VarRef findLocalVar(TruffleString name) {
        return findVar(name, true, true, false, true, true, false);
    }

    public final VarRef findVar(TruffleString name, boolean skipWith) {
        return findVar(name, skipWith, skipWith, false, false, false, false);
    }

    public final VarRef findBlockScopedVar(TruffleString name) {
        return findVar(name, true, true, false, true, true, true);
    }

    public final VarRef findVar(TruffleString name, boolean skipWith, boolean skipEval, boolean skipBlockScoped, boolean skipGlobal, boolean skipMapped) {
        return findVar(name, skipWith, skipEval, skipBlockScoped, skipGlobal, skipMapped, false);
    }

    public final VarRef findVar(TruffleString name, boolean skipWith, boolean skipEval, boolean skipBlockScoped, boolean skipGlobal, boolean skipMapped, boolean skipVar) {
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
                if (globalEnv.hasLexicalDeclaration(name)) {
                    return wrapIn(wrapClosure, wrapFrameLevel, new GlobalLexVarRef(name, globalEnv.hasConstDeclaration(name), globalEnv));
                } else if (!globalEnv.hasVarDeclaration(name)) {
                    wrapClosure = makeGlobalWrapClosure(wrapClosure, name);
                }
            } else if (current instanceof DebugEnvironment) {
                if (((DebugEnvironment) current).hasMember(name)) {
                    wrapClosure = makeDebugWrapClosure(wrapClosure, name, frameLevel);
                    wrapFrameLevel = frameLevel;
                }
            } else {
                int effectiveScopeLevel = scopeLevel;
                JSFrameSlot slot = current.findBlockFrameSlot(name);
                if (slot == null) {
                    slot = current.findFunctionFrameSlot(name);
                    effectiveScopeLevel += current.getScopeLevel();
                }
                if (slot != null) {
                    if ((!skipBlockScoped || !(JSFrameUtil.isConst(slot) || JSFrameUtil.isLet(slot))) &&
                                    (!skipVar || (JSFrameUtil.isConst(slot) || JSFrameUtil.isLet(slot)))) {
                        VarRef varRef;
                        if (!skipMapped && isMappedArgumentsParameter(slot, current)) {
                            varRef = new MappedArgumentVarRef(slot, effectiveScopeLevel, frameLevel, name, current);
                        } else if (JSFrameUtil.isArguments(slot)) {
                            assert !current.function().isArrowFunction() && !current.function().isGlobal() && !current.function().isEval();
                            varRef = new ArgumentsVarRef(slot, effectiveScopeLevel, frameLevel, name, current);
                        } else {
                            assert frameLevel == 0 || JSFrameUtil.isClosedOver(slot) || (current.getScope() != null && current.getScope().hasNestedEval()) : slot;
                            varRef = new FrameSlotVarRef(slot, effectiveScopeLevel, frameLevel, name, current);
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
                        return wrapIn(wrapClosure, wrapFrameLevel, new FunctionCalleeVarRef(frameLevel, name, current));
                    }

                    frameLevel++;
                    scopeLevel = 0;
                } else if (current instanceof BlockEnvironment && current.hasScopeFrame()) {
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

    private WrapClosure makeEvalWrapClosure(WrapClosure wrapClosure, TruffleString name, int frameLevel, int scopeLevel, Environment current) {
        final JSFrameSlot dynamicScopeSlot = current.findBlockFrameSlot(FunctionEnvironment.DYNAMIC_SCOPE_IDENTIFIER);
        assert dynamicScopeSlot != null;
        return WrapClosure.compose(wrapClosure, new WrapClosure() {
            @Override
            public JavaScriptNode apply(JavaScriptNode delegateNode, WrapAccess access) {
                JavaScriptNode dynamicScopeNode = new FrameSlotVarRef(dynamicScopeSlot, scopeLevel, frameLevel, FunctionEnvironment.DYNAMIC_SCOPE_IDENTIFIER, current).createReadNode();
                JSTargetableNode scopeAccessNode;
                if (access == WrapAccess.Delete) {
                    scopeAccessNode = factory.createDeleteProperty(null, factory.createConstantString(name), isStrictMode());
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

    private WrapClosure makeWithWrapClosure(WrapClosure wrapClosure, TruffleString name, Object withVarName) {
        return WrapClosure.compose(wrapClosure, new WrapClosure() {
            @Override
            public JavaScriptNode apply(JavaScriptNode delegateNode, WrapAccess access) {
                VarRef withVarNameRef = Objects.requireNonNull(findInternalSlot(withVarName));
                JSTargetableNode withAccessNode;
                if (access == WrapAccess.Delete) {
                    withAccessNode = factory.createDeleteProperty(null, factory.createConstantString(name), isStrictMode());
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
                return factory.createWithVarWrapper(context, name, isStrictMode(), withTarget, withAccessNode, delegateNode);
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
                    return factory.createWithVarWrapper(context, name, isStrictMode(), withTargetTempVar.createReadNode(), readWithProperty, innerReadSupplier.get());
                };
                UnaryOperator<JavaScriptNode> writeSupplier = (rhs) -> {
                    JavaScriptNode withTarget = factory.createWithTarget(context, name, withObjVar.createReadNode());
                    WritePropertyNode writeWithProperty = factory.createWriteProperty(null, name, null, context, isStrictMode(), false, true);
                    return factory.createWithVarWrapper(context, name, isStrictMode(), withTargetTempVar.createWriteNode(withTarget), writeWithProperty, innerWriteSupplier.apply(rhs));
                };
                return new Pair<>(readSupplier, writeSupplier);
            }
        });
    }

    private WrapClosure makeGlobalWrapClosure(WrapClosure wrapClosure, TruffleString name) {
        return WrapClosure.compose(wrapClosure, new WrapClosure() {
            @Override
            public JavaScriptNode apply(JavaScriptNode delegateNode, WrapAccess access) {
                JSTargetableNode scopeAccessNode;
                if (access == WrapAccess.Delete) {
                    scopeAccessNode = factory.createDeleteProperty(null, factory.createConstantString(name), isStrictMode());
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

    private WrapClosure makeDebugWrapClosure(WrapClosure wrapClosure, TruffleString name, int frameLevel) {
        ensureFrameLevelAvailable(frameLevel);
        return WrapClosure.compose(wrapClosure, new WrapClosure() {
            @Override
            public JavaScriptNode apply(JavaScriptNode delegateNode, WrapAccess access) {
                JSTargetableNode scopeAccessNode;
                if (access == WrapAccess.Delete) {
                    scopeAccessNode = factory.createDeleteProperty(null, factory.createConstantString(name), isStrictMode());
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

    public JSFrameSlot findBlockFrameSlot(@SuppressWarnings("unused") Object name) {
        return null;
    }

    public JSFrameSlot findFunctionFrameSlot(@SuppressWarnings("unused") Object name) {
        return null;
    }

    public JSFrameDescriptor getBlockFrameDescriptor() {
        throw unsupported();
    }

    private static boolean isMappedArgumentsParameter(JSFrameSlot slot, Environment current) {
        FunctionEnvironment function = current.function();
        return function.hasMappedParameters() && !function.isStrictMode() && function.hasSimpleParameterList() && JSFrameUtil.isParam(slot);
    }

    public final Environment getParent() {
        return parent;
    }

    public final FunctionEnvironment function() {
        return functionEnvironment;
    }

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
            } else if (current instanceof BlockEnvironment && current.hasScopeFrame()) {
                currentScopeLevel++;
            }
            current = current.getParent();
        } while (current != null);

        return null;
    }

    public VarRef createTempVar() {
        JSFrameSlot var = declareTempVar("tmp");
        return findTempVar(var);
    }

    public VarRef findTempVar(JSFrameSlot var) {
        return new VarRef(var.getIdentifier()) {

            @Override
            public JSFrameSlot getFrameSlot() {
                return var;
            }

            @Override
            public JavaScriptNode createReadNode() {
                return factory.createReadCurrentFrameSlot(var);
            }

            @Override
            public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
                return factory.createWriteCurrentFrameSlot(var, rhs);
            }

            @Override
            public JavaScriptNode createDeleteNode() {
                throw Errors.shouldNotReachHere();
            }
        };
    }

    private JSFrameSlot declareTempVar(String prefix) {
        return declareLocalVar(factory.createInternalSlotId(prefix, getFunctionFrameDescriptor().getSize()));
    }

    public JSFrameDescriptor getFunctionFrameDescriptor() {
        return function().getFunctionFrameDescriptor();
    }

    public boolean isStrictMode() {
        return function().isStrictMode();
    }

    public int getScopeLevel() {
        throw unsupported();
    }

    public boolean hasScopeFrame() {
        return false;
    }

    public Scope getScope() {
        return null;
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(getClass().getName());
    }

    public final JSFrameSlot getBlockScopeSlot(int frameLevel, int scopeLevel) {
        Environment current = this;
        for (int currentFrameLevel = frameLevel; currentFrameLevel > 0; currentFrameLevel--) {
            current = current.function().getParent();
        }
        int currentScopeLevel = scopeLevel;
        while (current != null) {
            if (current instanceof FunctionEnvironment) {
                assert currentScopeLevel == 0;
                return null;
            } else if (current instanceof BlockEnvironment && current.hasScopeFrame()) {
                if (currentScopeLevel == 0) {
                    return function().getBlockScopeSlot();
                }
                currentScopeLevel--;
            }
            current = current.getParent();
        }
        return null;
    }

    public JSFrameSlot getCurrentBlockScopeSlot() {
        return null;
    }

    public void addFrameSlotsFromSymbols(Iterable<com.oracle.js.parser.ir.Symbol> symbols) {
        addFrameSlotsFromSymbols(symbols, false, null);
    }

    public void addFrameSlotsFromSymbols(Iterable<com.oracle.js.parser.ir.Symbol> symbols, boolean onlyBlockScoped, Predicate<Symbol> filter) {
        for (com.oracle.js.parser.ir.Symbol symbol : symbols) {
            if (symbol.isBlockScoped() || (!onlyBlockScoped && symbol.isVar() && !symbol.isGlobal() && !symbol.isThis() && !symbol.isSuper() && !symbol.isNewTarget())) {
                if (symbol.isFunctionSelf()) {
                    // Function self reference is retrieved from arguments, no frame slot needed.
                    continue;
                }
                if (filter == null || filter.test(symbol)) {
                    addFrameSlotFromSymbol(symbol);
                }
            }
        }
    }

    public void addFrameSlotFromSymbol(Symbol symbol) {
        // Frame slot may already exist for simple parameters and "arguments".
        assert !getBlockFrameDescriptor().contains(symbol.getNameTS()) || this instanceof FunctionEnvironment : symbol;
        getBlockFrameDescriptor().findOrAddFrameSlot(symbol.getNameTS(), symbol.getFlags(), FrameSlotKind.Illegal);
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
            assert name == null || JSFrameSlot.isAllowedIdentifierType(name) : name;
            this.name = name;
        }

        public abstract JavaScriptNode createReadNode();

        public abstract JavaScriptNode createWriteNode(JavaScriptNode rhs);

        public boolean isFunctionLocal() {
            return false;
        }

        public boolean isFrameVar() {
            return getFrameSlot() != null;
        }

        public boolean isGlobal() {
            return false;
        }

        public boolean isConst() {
            return false;
        }

        public JSFrameSlot getFrameSlot() {
            return null;
        }

        public TruffleString getName() {
            assert name instanceof TruffleString : name;
            return (TruffleString) name;
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

        public boolean hasBeenDeclared() {
            JSFrameSlot frameSlot = getFrameSlot();
            return frameSlot != null && (frameSlot.hasBeenDeclared() || !JSFrameUtil.hasTemporalDeadZone(frameSlot));
        }

        public void setHasBeenDeclared(boolean declared) {
            JSFrameSlot frameSlot = getFrameSlot();
            if (frameSlot != null && frameSlot.hasBeenDeclared() != declared) {
                frameSlot.setHasBeenDeclared(declared);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + getName() + ")";
        }
    }

    public abstract class AbstractFrameVarRef extends VarRef {
        protected final int scopeLevel;
        protected final int frameLevel;
        protected final Environment resolvedEnv;

        public AbstractFrameVarRef(int scopeLevel, int frameLevel, Object name, Environment resolvedEnv) {
            super(name);
            this.scopeLevel = scopeLevel;
            this.frameLevel = frameLevel;
            this.resolvedEnv = resolvedEnv;
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
            return factory.createScopeFrame(frameLevel, getEffectiveScopeLevel(), getBlockScopeSlot());
        }

        private JSFrameSlot getBlockScopeSlot() {
            if (frameLevel != 0) {
                // block scope slot is only needed for function-local accesses
                return null;
            } else if (isInCurrentFunctionFrame()) {
                return null;
            }
            return resolvedEnv.getCurrentBlockScopeSlot();
        }

        private int getEffectiveScopeLevel() {
            if (isInCurrentFunctionFrame()) {
                return 0;
            }
            return scopeLevel;
        }

        protected boolean isInCurrentFunctionFrame() {
            return frameLevel == 0 && (scopeLevel == Environment.this.getScopeLevel());
        }
    }

    public class FrameSlotVarRef extends AbstractFrameVarRef {
        protected final JSFrameSlot frameSlot;
        private final boolean checkTDZ;

        public FrameSlotVarRef(JSFrameSlot frameSlot, int scopeLevel, int frameLevel, Object name, Environment current) {
            this(frameSlot, scopeLevel, frameLevel, name, current, JSFrameUtil.needsTemporalDeadZoneCheck(frameSlot, frameLevel));
        }

        public FrameSlotVarRef(JSFrameSlot frameSlot, int scopeLevel, int frameLevel, Object name, Environment current, boolean checkTDZ) {
            super(scopeLevel, frameLevel, name, current);
            this.frameSlot = frameSlot;
            this.checkTDZ = checkTDZ;
        }

        @Override
        public JSFrameSlot getFrameSlot() {
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
            return factory.createWriteFrameSlot(frameSlot, createScopeFrameNode(), rhs, checkTDZ);
        }

        @Override
        public VarRef withTDZCheck() {
            if (this.checkTDZ || !JSFrameUtil.hasTemporalDeadZone(frameSlot)) {
                return this;
            }
            return new FrameSlotVarRef(frameSlot, scopeLevel, frameLevel, name, resolvedEnv, true);
        }

        @Override
        public boolean hasTDZCheck() {
            return checkTDZ;
        }
    }

    private abstract class AbstractArgumentsVarRef extends AbstractFrameVarRef {
        AbstractArgumentsVarRef(int scopeLevel, int frameLevel, TruffleString name, Environment current) {
            super(scopeLevel, frameLevel, name, current);
        }

        @Override
        public JSFrameSlot getFrameSlot() {
            return null;
        }
    }

    private final class FunctionCalleeVarRef extends AbstractArgumentsVarRef {
        FunctionCalleeVarRef(int frameLevel, TruffleString name, Environment current) {
            super(0, frameLevel, name, current);
        }

        @Override
        public JavaScriptNode createReadNode() {
            return factory.createAccessCallee(frameLevel);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return factory.createWriteConstantVariable(rhs, isStrictMode(), getName());
        }
    }

    private final class ArgumentsVarRef extends AbstractArgumentsVarRef {
        private final JSFrameSlot frameSlot;

        ArgumentsVarRef(JSFrameSlot frameSlot, int scopeLevel, int frameLevel, TruffleString name, Environment current) {
            super(scopeLevel, frameLevel, name, current);
            this.frameSlot = frameSlot;
        }

        @Override
        public JavaScriptNode createReadNode() {
            JavaScriptNode argumentsVarNode = factory.createReadFrameSlot(frameSlot, createScopeFrameNode());
            if (function().isDirectArgumentsAccess()) {
                FunctionEnvironment currentFunction = resolvedEnv.function();
                JavaScriptNode createArgumentsObjectNode = factory.createArgumentsObjectNode(context, isStrictMode(), currentFunction.getLeadingArgumentCount());
                JavaScriptNode writeNode = factory.createWriteFrameSlot(frameSlot, createScopeFrameNode(), createArgumentsObjectNode);
                return factory.createAccessArgumentsArrayDirectly(writeNode, argumentsVarNode, currentFunction.getLeadingArgumentCount());
            } else {
                return argumentsVarNode;
            }
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            assert !resolvedEnv.function().isDirectArgumentsAccess();
            return factory.createWriteFrameSlot(frameSlot, createScopeFrameNode(), rhs);
        }

        @Override
        public JSFrameSlot getFrameSlot() {
            return frameSlot;
        }
    }

    public class MappedArgumentVarRef extends AbstractArgumentsVarRef {
        private final JSFrameSlot frameSlot;
        private final int parameterIndex;

        public MappedArgumentVarRef(JSFrameSlot frameSlot, int scopeLevel, int frameLevel, TruffleString name, Environment current) {
            super(scopeLevel, frameLevel, name, current);
            assert !JSFrameUtil.hasTemporalDeadZone(frameSlot);
            assert current.function().hasSimpleParameterList();
            assert !current.function().isDirectArgumentsAccess();
            assert frameSlot.getMappedParameterIndex() != -1;
            this.frameSlot = frameSlot;
            this.parameterIndex = frameSlot.getMappedParameterIndex();
        }

        private VarRef findArgumentsObject() {
            return findInternalSlot(FunctionEnvironment.ARGUMENTS_SLOT_IDENTIFIER, false, getFrameLevel());
        }

        @Override
        public JavaScriptNode createReadNode() {
            VarRef argumentsObject = findArgumentsObject();
            ReadElementNode readArgumentsObjectElement = factory.createReadElementNode(context, argumentsObject.createReadNode(), factory.createConstantInteger(parameterIndex));
            return factory.createGuardDisconnectedArgumentRead(parameterIndex, readArgumentsObjectElement, argumentsObject.createReadNode(), frameSlot);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            VarRef argumentsObject = findArgumentsObject();
            WriteElementNode writeArgumentsObjectElement = factory.createWriteElementNode(argumentsObject.createReadNode(), factory.createConstantInteger(parameterIndex), null, context, false);
            return factory.createGuardDisconnectedArgumentWrite(parameterIndex, writeArgumentsObjectElement, argumentsObject.createReadNode(), rhs, frameSlot);
        }
    }

    public class GlobalVarRef extends VarRef {
        private final boolean required;

        public GlobalVarRef(TruffleString name) {
            this(name, true);
        }

        private GlobalVarRef(TruffleString name, boolean required) {
            super(name);
            assert !Null.NAME.equals(name);
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
        public JSFrameSlot getFrameSlot() {
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
            return factory.createDeleteProperty(object, element, isStrictMode());
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
        private final GlobalEnvironment globalEnv;

        public GlobalLexVarRef(TruffleString name, boolean isConst, GlobalEnvironment globalEnv) {
            this(name, isConst, globalEnv, true, false);
        }

        private GlobalLexVarRef(Object name, boolean isConst, GlobalEnvironment globalEnv, boolean required, boolean checkTDZ) {
            super(name);
            assert name instanceof TruffleString nameStr && !nameStr.equals(Null.NAME) && !GlobalEnvironment.isGlobalObjectConstant(nameStr) : name;
            this.isConst = isConst;
            this.required = required;
            this.checkTDZ = checkTDZ;
            this.globalEnv = globalEnv;
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
        public JSFrameSlot getFrameSlot() {
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
            return factory.createDeleteProperty(object, element, isStrictMode());
        }

        @Override
        public VarRef withRequired(@SuppressWarnings("hiding") boolean required) {
            if (this.required != required) {
                return new GlobalLexVarRef(name, isConst, globalEnv, required, checkTDZ);
            }
            return this;
        }

        @Override
        public VarRef withTDZCheck() {
            if (!this.checkTDZ) {
                return new GlobalLexVarRef(name, isConst, globalEnv, required, true);
            }
            return this;
        }

        @Override
        public boolean hasTDZCheck() {
            return checkTDZ;
        }

        @Override
        public boolean hasBeenDeclared() {
            return globalEnv.hasBeenDeclared((TruffleString) name);
        }

        @Override
        public void setHasBeenDeclared(boolean declared) {
            globalEnv.setHasBeenDeclared((TruffleString) name, declared);
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
            JavaScriptNode writeNode = wrappee.isConst() ? factory.createWriteConstantVariable(rhs, true, getName()) : wrappee.createWriteNode(rhs);
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
        public JSFrameSlot getFrameSlot() {
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

        DebugVarRef(TruffleString name, int frameLevel) {
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
            return factory.createWriteConstantVariable(rhs, isStrictMode(), getName());
        }

        @Override
        public JavaScriptNode createDeleteNode() {
            return factory.createConstantBoolean(false);
        }

    }

    final class ActiveModuleRef extends AbstractArgumentsVarRef {
        ActiveModuleRef(int scopeLevel, int frameLevel, Environment current) {
            super(scopeLevel, frameLevel, null, current);
        }

        @Override
        public JavaScriptNode createReadNode() {
            return factory.createAccessFrameArgument(createScopeFrameNode(), 0);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            throw Errors.shouldNotReachHere();
        }
    }

    public VarRef findActiveModule() {
        Environment current = this;
        int frameLevel = 0;
        int scopeLevel = 0;
        while (current.getParent() != null) {
            if (current instanceof FunctionEnvironment) {
                if (((FunctionEnvironment) current).isScriptOrModule()) {
                    break;
                }
                ((FunctionEnvironment) current).setNeedsParentFrame(true);
                frameLevel++;
                scopeLevel = 0;
            } else if (current instanceof BlockEnvironment && current.hasScopeFrame()) {
                scopeLevel++;
            }
            current = current.getParent();
        }
        FunctionEnvironment scriptOrModuleEnv = (FunctionEnvironment) current;
        assert scriptOrModuleEnv.isModule();
        return new ActiveModuleRef(scopeLevel, frameLevel, scriptOrModuleEnv);
    }

    protected String toStringImpl(@SuppressWarnings("unused") Map<String, Integer> state) {
        return this.getClass().getSimpleName();
    }

    protected static String joinElements(Iterable<? extends Object> keySet) {
        StringJoiner sj = new StringJoiner(", ", "{", "}");
        for (Object key : keySet) {
            sj.add(String.valueOf(key));
        }
        return sj.toString();
    }

    @Override
    public String toString() {
        StringJoiner output = new StringJoiner("\n");
        Map<String, Integer> state = new HashMap<>();
        Environment current = this;
        do {
            output.add(current.toStringImpl(state));
            current = current.getParent();
        } while (current != null);
        return output.toString();
    }
}

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
package com.oracle.truffle.js.parser.env;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.nodes.access.DoWithNode;
import com.oracle.truffle.js.nodes.access.EvalVariableNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.access.WriteNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

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
        return findInternalSlot(FunctionEnvironment.THIS_SLOT_IDENTIFIER);
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
        assert !function().isGlobal();
        declareLocalVar(FunctionEnvironment.ASYNC_CONTEXT_SLOT_IDENTIFIER);
        return findInternalSlot(FunctionEnvironment.ASYNC_CONTEXT_SLOT_IDENTIFIER);
    }

    public VarRef findAsyncResultVar() {
        assert !function().isGlobal();
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

    protected final JavaScriptNode createLocal(FrameSlot frameSlot, int level, int scopeLevel) {
        JavaScriptNode local = factory.createLocal(frameSlot, level, scopeLevel, false);
        return local;
    }

    protected final JavaScriptNode createLocal(FrameSlot frameSlot, int level, int scopeLevel, boolean checkTDZ) {
        return factory.createLocal(frameSlot, level, scopeLevel, checkTDZ);
    }

    protected final VarRef findInternalSlot(String name) {
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
            }
            current = current.getParent();
        } while (current != null);

        throw new NoSuchElementException(name);
    }

    enum WrapAccess {
        Read,
        Write,
        Delete,
    }

    @FunctionalInterface
    interface WrapClosure extends BiFunction<JavaScriptNode, WrapAccess, JavaScriptNode> {
        default WrapClosure compose(WrapClosure before) {
            Objects.requireNonNull(before);
            return (JavaScriptNode v, WrapAccess w) -> apply(before.apply(v, w), w);
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
            } else {
                FrameSlot slot = current.findBlockFrameSlot(name);
                if (slot != null) {
                    if (!skipBlockScoped || !(JSFrameUtil.isConst(slot) || JSFrameUtil.isLet(slot))) {
                        return wrapIn(wrapClosure, wrapFrameLevel, current instanceof DebugEnvironment
                                        ? new LazyFrameSlotVarRef(slot, scopeLevel, frameLevel, name, current)
                                        : new FrameSlotVarRef(slot, scopeLevel, frameLevel, name, current));
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

                    VarRef importBinding = fnEnv.getImportBinding(name);
                    if (importBinding != null) {
                        // NB: no outer frame access or wrapping required
                        return importBinding;
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
        WrapClosure inner = maybeMakeDefaultWrapClosure(wrapClosure);
        final FrameSlot dynamicScopeSlot = current.findBlockFrameSlot(FunctionEnvironment.DYNAMIC_SCOPE_IDENTIFIER);
        assert dynamicScopeSlot != null;
        return inner.compose(new WrapClosure() {
            @Override
            public JavaScriptNode apply(JavaScriptNode delegateNode, WrapAccess access) {
                JavaScriptNode dynamicScopeNode = createLocal(dynamicScopeSlot, frameLevel, scopeLevel);
                JSTargetableNode scopeAccessNode;
                if (access == WrapAccess.Delete) {
                    scopeAccessNode = factory.createDeleteProperty(null, factory.createConstantString(name), isStrictMode());
                } else if (access == WrapAccess.Write) {
                    assert delegateNode instanceof WriteNode : delegateNode;
                    scopeAccessNode = factory.createWriteProperty(null, name, null, context, isStrictMode());
                } else {
                    assert access == WrapAccess.Read;
                    assert delegateNode instanceof ReadNode || delegateNode instanceof RepeatableNode : delegateNode;
                    scopeAccessNode = factory.createProperty(context, null, name);
                }
                return new EvalVariableNode(context, name, delegateNode, dynamicScopeNode, scopeAccessNode);
            }
        });
    }

    private WrapClosure makeWithWrapClosure(WrapClosure wrapClosure, String name, String withVarName) {
        WrapClosure inner = maybeMakeDefaultWrapClosure(wrapClosure);
        return inner.compose(new WrapClosure() {
            @Override
            public JavaScriptNode apply(JavaScriptNode delegateNode, WrapAccess access) {
                JSTargetableNode withAccessNode;
                if (access == WrapAccess.Delete) {
                    withAccessNode = factory.createDeleteProperty(null, factory.createConstantString(name), isStrictMode());
                } else if (access == WrapAccess.Write) {
                    assert delegateNode instanceof WriteNode : delegateNode;
                    withAccessNode = factory.createWriteProperty(null, name, null, context, isStrictMode());
                } else {
                    assert access == WrapAccess.Read;
                    assert delegateNode instanceof ReadNode || delegateNode instanceof RepeatableNode : delegateNode;
                    withAccessNode = factory.createProperty(context, null, name);
                }
                return new DoWithNode(context, name, findLocalVar(withVarName).createReadNode(), withAccessNode, delegateNode);
            }
        });
    }

    private static WrapClosure maybeMakeDefaultWrapClosure(WrapClosure wrapClosure) {
        WrapClosure inner = wrapClosure;
        if (inner == null) {
            inner = new WrapClosure() {
                @Override
                public JavaScriptNode apply(JavaScriptNode accessNode, WrapAccess write) {
                    return accessNode;
                }
            };
        }
        return inner;
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

    private JavaScriptNode findLocalVarNodeForArguments(Environment current, int frameLevel, int scopeLevel) {
        assert current.function().getArgumentsSlot() != null;
        JavaScriptNode argumentsVarNode = createReadArgumentObject(current, frameLevel, scopeLevel);
        if (function().isDirectArgumentsAccess()) {
            FunctionEnvironment currentFunction = current.function();
            JavaScriptNode createArgumentsObjectNode = factory.createArgumentsObjectNode(context, isStrictMode(), currentFunction.getLeadingArgumentCount(),
                            currentFunction.getTrailingArgumentCount());
            JavaScriptNode writeNode = factory.createWriteFrameSlot(currentFunction.getArgumentsSlot(), frameLevel, scopeLevel, createArgumentsObjectNode);
            return factory.createAccessArgumentsArrayDirectly(writeNode, argumentsVarNode, currentFunction.getLeadingArgumentCount(), currentFunction.getTrailingArgumentCount());
        } else {
            return argumentsVarNode;
        }
    }

    private JavaScriptNode createReadLocalVarNodeFromSlot(Environment current, int frameLevel, FrameSlot slot, int scopeLevel, boolean checkTDZ) {
        if (current instanceof GlobalEnvironment) {
            return factory.createReadGlobal(slot, checkTDZ, context);
        }
        FunctionEnvironment currentFunction = current.function();
        if (currentFunction.getArgumentsSlot() != null && !currentFunction.isStrictMode() && currentFunction.hasSimpleParameterList() && currentFunction.isParam(slot)) {
            return createReadParameterFromMappedArguments(current, frameLevel, scopeLevel, slot);
        }
        return createLocal(slot, frameLevel, scopeLevel, checkTDZ);
    }

    private JavaScriptNode createWriteLocalVarNodeFromSlot(Environment current, int frameLevel, FrameSlot slot, int scopeLevel, boolean checkTDZ, JavaScriptNode rhs) {
        if (current instanceof GlobalEnvironment) {
            return factory.createWriteGlobal(slot, rhs, checkTDZ, context);
        }
        FunctionEnvironment currentFunction = current.function();
        if (currentFunction.getArgumentsSlot() != null && !currentFunction.isStrictMode() && currentFunction.hasSimpleParameterList() && currentFunction.isParam(slot)) {
            return createWriteParameterFromMappedArguments(current, frameLevel, scopeLevel, slot, rhs);
        }
        return factory.createWriteFrameSlot(slot, frameLevel, scopeLevel, rhs, checkTDZ);
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

    public Environment getParent() {
        return parent;
    }

    public abstract FunctionEnvironment function();

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
                return factory.createLocal(var, 0, getScopeLevel());
            }

            @Override
            public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
                return factory.createWriteFrameSlot(var, 0, getScopeLevel(), rhs);
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

    public void addFrameSlotsFromSymbols(Iterable<com.oracle.js.parser.ir.Symbol> symbols) {
        addFrameSlotsFromSymbols(symbols, false);
    }

    public void addFrameSlotsFromSymbols(Iterable<com.oracle.js.parser.ir.Symbol> symbols, boolean onlyBlockScoped) {
        for (com.oracle.js.parser.ir.Symbol symbol : symbols) {
            if (symbol.isImportBinding()) {
                continue; // no frame slot required
            }
            if (symbol.isBlockScoped() || (!onlyBlockScoped && symbol.isVar() && symbol.isVarDeclaredHere())) {
                addFrameSlotFromSymbol(symbol);
            }
        }
    }

    public void addFrameSlotFromSymbol(com.oracle.js.parser.ir.Symbol symbol) {
        assert !getBlockFrameDescriptor().getIdentifiers().contains(symbol.getName()) || this instanceof FunctionEnvironment &&
                        (function().isParameter(symbol.getName()) || symbol.getName().equals(ARGUMENTS_NAME));
        int flags = symbol.getFlags();
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

    public abstract class VarRef {
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

        public abstract FrameSlot getFrameSlot();

        public String getName() {
            return name;
        }

        public JavaScriptNode createDeleteNode() {
            return factory.createConstantBoolean(false);
        }

        public VarRef withTDZCheck() {
            return this;
        }

        public VarRef withRequired(@SuppressWarnings("unused") boolean required) {
            return this;
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
        public JavaScriptNode createReadNode() {
            return createReadLocalVarNodeFromSlot(current, frameLevel, frameSlot, scopeLevel, checkTDZ);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return createWriteLocalVarNodeFromSlot(current, frameLevel, frameSlot, scopeLevel, checkTDZ, rhs);
        }

        @Override
        public VarRef withTDZCheck() {
            if (this.checkTDZ || !JSFrameUtil.hasTemporalDeadZone(frameSlot)) {
                return this;
            }
            return new FrameSlotVarRef(frameSlot, scopeLevel, frameLevel, name, current, true);
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
            if (isStrictMode()) {
                return factory.createThrowError(JSErrorType.TypeError, "Assignment to immutable binding");
            } else {
                return rhs;
            }
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
            return factory.createWriteFrameSlot(current.function().getArgumentsSlot(), frameLevel, scopeLevel, rhs);
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
            return factory.createWriteProperty(factory.createGlobalObject(context), name, rhs, required, context, isStrictMode());
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
            return factory.createDeleteProperty(object, element, isStrictMode());
        }

        @Override
        public VarRef withRequired(@SuppressWarnings("hiding") boolean required) {
            if (this.required != required) {
                return new GlobalVarRef(name, required);
            }
            return this;
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
            return wrapClosure.apply(wrappee.createWriteNode(rhs), WrapAccess.Write);
        }

        @Override
        public JavaScriptNode createDeleteNode() {
            return wrapClosure.apply(wrappee.createDeleteNode(), WrapAccess.Delete);
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
    }

    public class ImportBindingRef extends VarRef {
        private final JSModuleRecord module;
        private final String bindingName;

        public ImportBindingRef(String localName, JSModuleRecord module, String bindingName) {
            super(localName);
            this.module = module;
            this.bindingName = bindingName;
        }

        @Override
        public JavaScriptNode createReadNode() {
            return factory.createReadModuleImportBinding(module, bindingName);
        }

        @Override
        public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
            return factory.createThrowError(JSErrorType.TypeError, "Assignment to immutable binding");
        }

        @Override
        public boolean isFunctionLocal() {
            return false;
        }

        @Override
        public boolean isGlobal() {
            return false;
        }

        @Override
        public FrameSlot getFrameSlot() {
            return null;
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

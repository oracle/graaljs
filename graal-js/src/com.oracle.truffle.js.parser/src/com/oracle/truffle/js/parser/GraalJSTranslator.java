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
package com.oracle.truffle.js.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.oracle.js.parser.Lexer;
import com.oracle.js.parser.Token;
import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.AccessNode;
import com.oracle.js.parser.ir.BaseNode;
import com.oracle.js.parser.ir.BinaryNode;
import com.oracle.js.parser.ir.Block;
import com.oracle.js.parser.ir.BlockExpression;
import com.oracle.js.parser.ir.BlockStatement;
import com.oracle.js.parser.ir.CallNode;
import com.oracle.js.parser.ir.CaseNode;
import com.oracle.js.parser.ir.CatchNode;
import com.oracle.js.parser.ir.ClassNode;
import com.oracle.js.parser.ir.DebuggerNode;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.ExpressionStatement;
import com.oracle.js.parser.ir.ForNode;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.IfNode;
import com.oracle.js.parser.ir.IndexNode;
import com.oracle.js.parser.ir.JoinPredecessorExpression;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.js.parser.ir.LexicalContextNode;
import com.oracle.js.parser.ir.LexicalContextScope;
import com.oracle.js.parser.ir.LiteralNode;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.js.parser.ir.Module.ImportEntry;
import com.oracle.js.parser.ir.ObjectNode;
import com.oracle.js.parser.ir.ParameterNode;
import com.oracle.js.parser.ir.PropertyNode;
import com.oracle.js.parser.ir.Scope;
import com.oracle.js.parser.ir.Statement;
import com.oracle.js.parser.ir.Symbol;
import com.oracle.js.parser.ir.TemplateLiteralNode;
import com.oracle.js.parser.ir.TernaryNode;
import com.oracle.js.parser.ir.TryNode;
import com.oracle.js.parser.ir.UnaryNode;
import com.oracle.js.parser.ir.VarNode;
import com.oracle.js.parser.ir.WithNode;
import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.NodeFactory.BinaryOperation;
import com.oracle.truffle.js.nodes.NodeFactory.UnaryOperation;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.DeclareEvalVariableNode;
import com.oracle.truffle.js.nodes.access.DeclareGlobalNode;
import com.oracle.truffle.js.nodes.access.GlobalPropertyNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.ObjectLiteralMemberNode;
import com.oracle.truffle.js.nodes.access.OptionalChainNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.access.VarWrapperNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.access.WriteNode;
import com.oracle.truffle.js.nodes.access.WritePropertyNode;
import com.oracle.truffle.js.nodes.binary.DualNode;
import com.oracle.truffle.js.nodes.binary.JSBinaryNode;
import com.oracle.truffle.js.nodes.binary.JSTypeofIdenticalNode;
import com.oracle.truffle.js.nodes.control.AbstractBlockNode;
import com.oracle.truffle.js.nodes.control.BreakNode;
import com.oracle.truffle.js.nodes.control.BreakTarget;
import com.oracle.truffle.js.nodes.control.ContinueTarget;
import com.oracle.truffle.js.nodes.control.DiscardResultNode;
import com.oracle.truffle.js.nodes.control.EmptyNode;
import com.oracle.truffle.js.nodes.control.GeneratorWrapperNode;
import com.oracle.truffle.js.nodes.control.ResumableNode;
import com.oracle.truffle.js.nodes.control.ReturnNode;
import com.oracle.truffle.js.nodes.control.ReturnTargetNode;
import com.oracle.truffle.js.nodes.control.SequenceNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.nodes.control.SuspendNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.BlockScopeNode;
import com.oracle.truffle.js.nodes.function.EvalNode;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.function.JSFunctionExpressionNode;
import com.oracle.truffle.js.nodes.function.JSNewNode;
import com.oracle.truffle.js.nodes.function.SpreadArgumentNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.nodes.unary.VoidNode;
import com.oracle.truffle.js.parser.env.BlockEnvironment;
import com.oracle.truffle.js.parser.env.DebugEnvironment;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.parser.env.Environment.AbstractFrameVarRef;
import com.oracle.truffle.js.parser.env.Environment.FrameSlotVarRef;
import com.oracle.truffle.js.parser.env.Environment.VarRef;
import com.oracle.truffle.js.parser.env.EvalEnvironment;
import com.oracle.truffle.js.parser.env.FunctionEnvironment;
import com.oracle.truffle.js.parser.env.FunctionEnvironment.JumpTargetCloseable;
import com.oracle.truffle.js.parser.env.GlobalEnvironment;
import com.oracle.truffle.js.parser.env.WithEnvironment;
import com.oracle.truffle.js.parser.internal.ir.debug.PrintVisitor;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;

abstract class GraalJSTranslator extends com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor<LexicalContext, JavaScriptNode> {
    public static final JavaScriptNode[] EMPTY_NODE_ARRAY = new JavaScriptNode[0];
    private static final JavaScriptNode ANY_JAVA_SCRIPT_NODE = new JavaScriptNode() {
        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }
    };

    private static final SourceSection unavailableInternalSection = Source.newBuilder(JavaScriptLanguage.ID, "<internal>", "<internal>").mimeType(
                    JavaScriptLanguage.APPLICATION_MIME_TYPE).internal(true).build().createUnavailableSection();

    private Environment environment;
    protected final JSContext context;
    protected final NodeFactory factory;
    protected final Source source;
    protected final String[] argumentNames;
    protected final int sourceLength;
    protected final int prologLength;
    private final boolean isParentStrict;

    protected GraalJSTranslator(LexicalContext lc, NodeFactory factory, JSContext context, Source source, String[] argumentNames, int prologLength, Environment environment, boolean isParentStrict) {
        super(lc);
        this.context = context;
        this.environment = environment;
        this.factory = factory;
        this.source = source;
        this.argumentNames = argumentNames;
        this.isParentStrict = isParentStrict;
        this.sourceLength = source.getCharacters().length();
        this.prologLength = prologLength;
    }

    protected final JavaScriptNode transform(com.oracle.js.parser.ir.Node node) {
        if (node != null) {
            return node.accept(this);
        }
        return null;
    }

    private JavaScriptNode tagStatement(JavaScriptNode resultNode, com.oracle.js.parser.ir.Node parseNode) {
        if (!resultNode.hasSourceSection()) {
            assignSourceSection(resultNode, parseNode);
        }
        assert resultNode.getSourceSection() != null;
        if (resultNode instanceof VarWrapperNode) {
            tagStatement(((VarWrapperNode) resultNode).getDelegateNode(), parseNode);
        } else {
            resultNode.addStatementTag();
        }
        return resultNode;
    }

    private JavaScriptNode tagExpression(JavaScriptNode resultNode, com.oracle.js.parser.ir.Node parseNode) {
        if (!resultNode.hasSourceSection()) {
            assignSourceSection(resultNode, parseNode);
        }
        assert resultNode.getSourceSection() != null;
        if (resultNode instanceof VarWrapperNode) {
            tagExpression(((VarWrapperNode) resultNode).getDelegateNode(), parseNode);
        } else {
            resultNode.addExpressionTag();
        }
        return resultNode;
    }

    private static JavaScriptNode tagCall(JavaScriptNode resultNode) {
        resultNode.addCallTag();
        return resultNode;
    }

    private JavaScriptNode tagBody(JavaScriptNode resultNode, com.oracle.js.parser.ir.Node parseNode) {
        if (!resultNode.hasSourceSection()) {
            assignSourceSection(resultNode, parseNode);
        }
        assert resultNode.getSourceSection() != null;
        if (resultNode instanceof VarWrapperNode) {
            tagBody(((VarWrapperNode) resultNode).getDelegateNode(), parseNode);
        } else {
            resultNode.addRootBodyTag();
        }
        return resultNode;
    }

    private FunctionEnvironment currentFunction() {
        return environment.function();
    }

    private JavaScriptNode createBlock(JavaScriptNode... statements) {
        return createBlock(statements, false, false);
    }

    private JavaScriptNode createBlock(JavaScriptNode[] statements, boolean terminal, boolean expressionBlock) {
        if ((JSConfig.ReturnOptimizer && terminal) || expressionBlock || currentFunction().returnsLastStatementResult()) {
            return factory.createExprBlock(statements);
        } else {
            return factory.createVoidBlock(statements);
        }
    }

    protected final ScriptNode translateScript(FunctionNode functionNode) {
        if (!functionNode.isScript()) {
            throw new IllegalArgumentException("root function node is not a script");
        }
        JSFunctionExpressionNode functionExpression = (JSFunctionExpressionNode) transformFunction(functionNode);
        return ScriptNode.fromFunctionRoot(context, functionExpression.getFunctionNode());
    }

    protected final JavaScriptNode transformFunction(FunctionNode functionNode) {
        return transform(functionNode);
    }

    protected abstract GraalJSTranslator newTranslator(Environment env, LexicalContext savedLC);

    // ---

    @Override
    public JavaScriptNode enterFunctionNode(FunctionNode functionNode) {
        if (JSConfig.PrintParse) {
            printParse(functionNode);
        }

        boolean isStrict = functionNode.isStrict() || isParentStrict || (environment != null && environment.function() != null && environment.isStrictMode());
        boolean isArrowFunction = functionNode.isArrow();
        boolean isGeneratorFunction = functionNode.isGenerator();
        boolean isAsyncFunction = functionNode.isAsync();
        boolean isDerivedConstructor = functionNode.isDerivedConstructor();

        boolean isMethod = functionNode.isMethod();
        boolean needsNewTarget = functionNode.needsNewTarget();
        boolean isClassConstructor = functionNode.isClassConstructor();
        boolean isConstructor = !isArrowFunction && !isGeneratorFunction && !isAsyncFunction && ((!isMethod || context.getEcmaScriptVersion() == 5) || isClassConstructor);
        assert !isDerivedConstructor || isConstructor;
        boolean strictFunctionProperties = isStrict || isArrowFunction || isMethod || isGeneratorFunction;
        boolean isBuiltin = false;
        boolean hasSyntheticArguments = functionNode.isScript() && this.argumentNames != null;

        boolean isGlobal;
        boolean isEval = false;
        boolean isIndirectEval = false;
        boolean inDirectEval = false;
        if (environment instanceof EvalEnvironment) {
            isEval = true;
            boolean isDirectEval = ((EvalEnvironment) environment).isDirectEval();
            isIndirectEval = !isDirectEval;
            Environment evalParent = environment.getParent();
            isGlobal = evalParent == null || (isDirectEval && (!isStrict && evalParent.function().isGlobal()));
            inDirectEval = isDirectEval || (evalParent != null && evalParent.function().inDirectEval());
        } else if (environment instanceof DebugEnvironment) {
            isGlobal = environment.getParent() == null;
            isEval = true;
            inDirectEval = true;
        } else {
            isGlobal = environment == null && argumentNames == null;
            inDirectEval = environment != null && currentFunction().inDirectEval();
        }
        boolean functionMode = !isGlobal || (isStrict && isIndirectEval);

        boolean lazyTranslation = context.getContextOptions().isLazyTranslation() && functionMode && !functionNode.isProgram() && !inDirectEval;

        String functionName = getFunctionName(functionNode);
        JSFunctionData functionData;
        FunctionRootNode functionRoot;
        FrameSlot blockScopeSlot;
        if (lazyTranslation) {
            assert functionMode && !functionNode.isProgram();

            // function needs parent frame analysis has already been done
            boolean needsParentFrame = functionNode.usesAncestorScope();
            blockScopeSlot = needsParentFrame && currentFunction() != null ? currentFunction().getBlockScopeSlot() : null;

            functionData = factory.createFunctionData(context, functionNode.getLength(), functionName, isConstructor, isDerivedConstructor, isStrict, isBuiltin,
                            needsParentFrame, isGeneratorFunction, isAsyncFunction, isClassConstructor, strictFunctionProperties, needsNewTarget);

            LexicalContext savedLC = lc.copy();
            Environment parentEnv = environment;
            functionData.setLazyInit(fd -> {
                GraalJSTranslator translator = newTranslator(parentEnv, savedLC);
                translator.translateFunctionOnDemand(functionNode, fd, isStrict, isArrowFunction, isGeneratorFunction, isAsyncFunction, isDerivedConstructor, isGlobal,
                                needsParentFrame, functionName, hasSyntheticArguments);
            });
            functionRoot = null;
        } else {
            try (EnvironmentCloseable functionEnv = enterFunctionEnvironment(isStrict, isArrowFunction, isGeneratorFunction, isDerivedConstructor, isAsyncFunction, isGlobal, hasSyntheticArguments)) {
                FunctionEnvironment currentFunction = currentFunction();
                currentFunction.setFunctionName(functionName);
                currentFunction.setInternalFunctionName(functionNode.getInternalName());
                currentFunction.setNamedFunctionExpression(functionNode.isNamedFunctionExpression());

                declareParameters(functionNode);
                List<JavaScriptNode> declarations;
                if (functionMode) {
                    declarations = functionEnvInit(functionNode);
                } else if (functionNode.isModule()) {
                    assert currentFunction.isGlobal();
                    declarations = Collections.emptyList();
                } else {
                    assert currentFunction.isGlobal();
                    declarations = collectGlobalVars(functionNode, isEval);
                }

                if (functionNode.isProgram()) {
                    functionNeedsParentFramePass(functionNode, context);
                }

                boolean needsParentFrame = functionNode.usesAncestorScope();
                currentFunction.setNeedsParentFrame(needsParentFrame);

                JavaScriptNode body = translateFunctionBody(functionNode, isGeneratorFunction, isAsyncFunction, declarations);

                needsParentFrame = currentFunction.needsParentFrame();
                blockScopeSlot = needsParentFrame && currentFunction.getParentFunction() != null ? currentFunction.getParentFunction().getBlockScopeSlot() : null;
                currentFunction.freeze();

                functionData = factory.createFunctionData(context, functionNode.getLength(), functionName, isConstructor, isDerivedConstructor, isStrict, isBuiltin,
                                needsParentFrame, isGeneratorFunction, isAsyncFunction, isClassConstructor, strictFunctionProperties, needsNewTarget);

                functionRoot = createFunctionRoot(functionNode, functionData, currentFunction, body);

                if (isEval) {
                    // force eager call target init for Function() code to avoid deopt at call site
                    functionData.getCallTarget();
                }
            }
        }

        JavaScriptNode functionExpression;
        if (isArrowFunction && functionNode.needsThis() && !currentFunction().getNonArrowParentFunction().isDerivedConstructor()) {
            JavaScriptNode thisNode = createThisNode();
            functionExpression = factory.createFunctionExpressionLexicalThis(functionData, functionRoot, blockScopeSlot, thisNode);
        } else {
            functionExpression = factory.createFunctionExpression(functionData, functionRoot, blockScopeSlot);
        }

        if (functionNode.isDeclared()) {
            ensureHasSourceSection(functionExpression, functionNode);
        } else {
            functionExpression = tagExpression(functionExpression, functionNode);
        }
        return functionExpression;
    }

    JavaScriptNode translateFunctionBody(FunctionNode functionNode, boolean isGeneratorFunction, boolean isAsyncFunction, List<JavaScriptNode> declarations) {
        JavaScriptNode body = transform(functionNode.getBody());

        if (isAsyncFunction && !isGeneratorFunction) {
            ensureHasSourceSection(body, functionNode);
            body = handleAsyncFunctionBody(body);
        }

        if (!declarations.isEmpty()) {
            body = prepareDeclarations(declarations, body);
        }

        return body;
    }

    private FunctionRootNode translateFunctionOnDemand(FunctionNode functionNode, JSFunctionData functionData, boolean isStrict, boolean isArrowFunction, boolean isGeneratorFunction,
                    boolean isAsyncFunction, boolean isDerivedConstructor, boolean isGlobal, boolean needsParentFrame, String functionName, boolean hasSyntheticArguments) {
        try (EnvironmentCloseable functionEnv = enterFunctionEnvironment(isStrict, isArrowFunction, isGeneratorFunction, isDerivedConstructor, isAsyncFunction, isGlobal, hasSyntheticArguments)) {
            FunctionEnvironment currentFunction = currentFunction();
            currentFunction.setFunctionName(functionName);
            currentFunction.setInternalFunctionName(functionNode.getInternalName());
            currentFunction.setNamedFunctionExpression(functionNode.isNamedFunctionExpression());

            currentFunction.setNeedsParentFrame(needsParentFrame);

            declareParameters(functionNode);
            functionEnvInit(functionNode);

            JavaScriptNode body = translateFunctionBody(functionNode, isGeneratorFunction, isAsyncFunction, Collections.emptyList());

            currentFunction.freeze();
            assert currentFunction.isDeepFrozen();

            return createFunctionRoot(functionNode, functionData, currentFunction, body);
        }
    }

    private FunctionRootNode createFunctionRoot(FunctionNode functionNode, JSFunctionData functionData, FunctionEnvironment currentFunction, JavaScriptNode body) {
        SourceSection functionSourceSection = createSourceSection(functionNode);
        FunctionBodyNode functionBody = factory.createFunctionBody(body);
        FunctionRootNode functionRoot = factory.createFunctionRootNode(functionBody, environment.getFunctionFrameDescriptor(), functionData, functionSourceSection,
                        currentFunction.getInternalFunctionName());

        if (JSConfig.PrintAst) {
            printAST(functionRoot);
        }
        return functionRoot;
    }

    private static void printAST(FunctionRootNode functionRoot) {
        NodeUtil.printCompactTree(System.out, functionRoot);
    }

    private static void printParse(FunctionNode functionNode) {
        System.out.printf(new PrintVisitor(functionNode).toString());
    }

    private JavaScriptNode finishDerivedConstructorBody(FunctionNode function, JavaScriptNode body) {
        JavaScriptNode getThisBinding = (function.hasDirectSuper() || function.hasEval() || function.hasArrowEval()) ? environment.findThisVar().createReadNode() : factory.createConstantUndefined();
        getThisBinding = checkThisBindingInitialized(getThisBinding);
        return factory.createDerivedConstructorResult(body, getThisBinding);
    }

    /**
     * Async function parse-time AST modifications.
     *
     * @return instrumented function body
     */
    private JavaScriptNode handleAsyncFunctionBody(JavaScriptNode body) {
        assert currentFunction().isAsyncFunction() && !currentFunction().isGeneratorFunction();
        VarRef asyncContextVar = environment.findAsyncContextVar();
        VarRef asyncResultVar = environment.findAsyncResultVar();
        JSWriteFrameSlotNode writeResultNode = (JSWriteFrameSlotNode) asyncResultVar.createWriteNode(null);
        JSWriteFrameSlotNode writeContextNode = (JSWriteFrameSlotNode) asyncContextVar.createWriteNode(null);
        JSReadFrameSlotNode readContextNode = (JSReadFrameSlotNode) asyncContextVar.createReadNode();
        JavaScriptNode instrumentedBody = instrumentSuspendNodes(body);
        return factory.createAsyncFunctionBody(context, instrumentedBody, writeContextNode, readContextNode, writeResultNode);
    }

    /**
     * Generator function parse-time AST modifications.
     *
     * @return instrumented function body
     */
    private JavaScriptNode finishGeneratorBody(JavaScriptNode body) {
        // Note: parameter initialization must precede (i.e. wrap) the (async) generator body
        assert lc.getCurrentBlock().isFunctionBody();
        if (currentFunction().isAsyncGeneratorFunction()) {
            return handleAsyncGeneratorBody(body);
        } else {
            return handleGeneratorBody(body);
        }
    }

    private JavaScriptNode handleGeneratorBody(JavaScriptNode body) {
        assert currentFunction().isGeneratorFunction() && !currentFunction().isAsyncGeneratorFunction();
        JavaScriptNode instrumentedBody = instrumentSuspendNodes(body);
        if (lc.getCurrentFunction().isModule()) {
            return factory.createModuleBody(instrumentedBody);
        }
        VarRef yieldVar = environment.findYieldValueVar();
        JSWriteFrameSlotNode writeYieldValueNode = (JSWriteFrameSlotNode) yieldVar.createWriteNode(null);
        JSReadFrameSlotNode readYieldResultNode = JSConfig.YieldResultInFrame ? (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getYieldResultSlot()).createReadNode() : null;
        return factory.createGeneratorBody(context, instrumentedBody, writeYieldValueNode, readYieldResultNode);
    }

    private JavaScriptNode handleAsyncGeneratorBody(JavaScriptNode body) {
        assert currentFunction().isAsyncGeneratorFunction();
        VarRef asyncContextVar = environment.findAsyncContextVar();
        JavaScriptNode instrumentedBody = instrumentSuspendNodes(body);
        VarRef yieldVar = environment.findAsyncResultVar();
        JSWriteFrameSlotNode writeAsyncContextNode = (JSWriteFrameSlotNode) asyncContextVar.createWriteNode(null);
        JSReadFrameSlotNode readAsyncContextNode = (JSReadFrameSlotNode) asyncContextVar.createReadNode();
        JSWriteFrameSlotNode writeYieldValueNode = (JSWriteFrameSlotNode) yieldVar.createWriteNode(null);
        if (lc.getCurrentFunction().isModule()) {
            return factory.createTopLevelAsyncModuleBody(context, instrumentedBody, writeYieldValueNode, writeAsyncContextNode);
        }
        JSReadFrameSlotNode readYieldResultNode = JSConfig.YieldResultInFrame ? (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getYieldResultSlot()).createReadNode() : null;
        return factory.createAsyncGeneratorBody(context, instrumentedBody, writeYieldValueNode, readYieldResultNode, writeAsyncContextNode, readAsyncContextNode);
    }

    /**
     * Instrument code paths leading to yield and await expressions.
     */
    private JavaScriptNode instrumentSuspendNodes(JavaScriptNode body) {
        if (!currentFunction().hasYield() && !currentFunction().hasAwait()) {
            return body;
        }
        JavaScriptNode newBody = (JavaScriptNode) instrumentSuspendHelper(body, null);
        Objects.requireNonNull(newBody);
        return newBody;
    }

    private Node instrumentSuspendHelper(Node parent, Node grandparent) {
        boolean hasSuspendChild = false;
        BitSet suspendableIndices = null;
        if (parent instanceof AbstractBlockNode) {
            Node[] statements = ((AbstractBlockNode) parent).getStatements();
            for (int i = 0; i < statements.length; i++) {
                Node newChild = instrumentSuspendHelper(statements[i], parent);
                if (newChild != null) {
                    hasSuspendChild = true;
                    statements[i] = newChild;
                    if (suspendableIndices == null) {
                        suspendableIndices = new BitSet();
                    }
                    suspendableIndices.set(i);
                }
            }
        } else {
            for (Node child : getChildrenInExecutionOrder(parent)) {
                Node newChild = instrumentSuspendHelper(child, parent);
                if (newChild != null) {
                    hasSuspendChild = true;
                    NodeUtil.replaceChild(parent, child, newChild);
                    assert !(child instanceof ResumableNode) || newChild instanceof GeneratorWrapperNode : "resumable node not wrapped: " + child;
                }
            }
        }
        if (parent instanceof SuspendNode) {
            return wrapResumableNode(parent);
        } else if (!hasSuspendChild) {
            return null;
        }

        if (parent instanceof AbstractBlockNode) {
            assert suspendableIndices != null && !suspendableIndices.isEmpty();
            return toGeneratorBlockNode((AbstractBlockNode) parent, suspendableIndices);
        } else if (parent instanceof ResumableNode) {
            return wrapResumableNode(parent);
        } else if (parent instanceof ReturnNode || parent instanceof ReturnTargetNode || isSideEffectFreeUnaryOpNode(parent)) {
            // these are side-effect-free, skip
            return parent;
        } else if (isSupportedDispersibleExpression(parent)) {
            // need to rescue side-effecting/non-repeatable expressions into temporaries
            // note that the expressions have to be extracted in evaluation order
            List<JavaScriptNode> extracted = new ArrayList<>();
            // we can only replace child fields assignable from JavaScriptNode
            if (grandparent == null || NodeUtil.isReplacementSafe(grandparent, parent, ANY_JAVA_SCRIPT_NODE)) {
                // extraction is a destructive step; only attempt it if replace can succeed
                extractChildrenTo(parent, extracted);
            } else {
                // not assignable to field type (e.g. JSTargetableNode), ignore for now
            }
            if (!extracted.isEmpty()) { // only if there's actually something to rescue
                extracted.add((JavaScriptNode) parent);
                // insert block node wrapper
                JavaScriptNode exprBlock = wrapResumableNode(factory.createExprBlock(extracted.toArray(EMPTY_NODE_ARRAY)));
                tagHiddenExpression(exprBlock);
                return exprBlock;
            } else {
                // nothing to do
                return parent;
            }
        } else {
            // if (parent instanceof JavaScriptNode):
            // unknown expression node type, either safe or unexpected (not handled)
            // else:
            // unsupported node type, skip over
            return parent;
        }
    }

    private JavaScriptNode wrapResumableNode(Node resumableNode) {
        if (resumableNode instanceof AbstractBlockNode) {
            BitSet all = new BitSet();
            all.set(0, ((AbstractBlockNode) resumableNode).getStatements().length);
            return toGeneratorBlockNode((AbstractBlockNode) resumableNode, all);
        }
        FrameDescriptor functionFrameDescriptor = environment.getFunctionFrameDescriptor();
        String identifier = ":generatorstate:" + functionFrameDescriptor.getSize();
        FrameSlot frameSlot = functionFrameDescriptor.addFrameSlot(identifier);
        JavaScriptNode readState = factory.createReadCurrentFrameSlot(frameSlot);
        WriteNode writeState = factory.createWriteCurrentFrameSlot(frameSlot, functionFrameDescriptor, null);
        return factory.createGeneratorWrapper((JavaScriptNode) resumableNode, readState, writeState);
    }

    private JavaScriptNode toGeneratorBlockNode(AbstractBlockNode blockNode, BitSet suspendableIndices) {
        FrameDescriptor functionFrameDescriptor = environment.getFunctionFrameDescriptor();
        String identifier = ":generatorstate:" + functionFrameDescriptor.getSize();
        FrameSlot frameSlot = functionFrameDescriptor.addFrameSlot(identifier);
        JavaScriptNode readState = factory.createReadCurrentFrameSlot(frameSlot);
        WriteNode writeState = factory.createWriteCurrentFrameSlot(frameSlot, functionFrameDescriptor, null);

        JavaScriptNode[] statements = blockNode.getStatements();
        boolean returnsResult = !blockNode.isResultAlwaysOfType(Undefined.class);
        JavaScriptNode genBlock;
        // we can resume at index 0 (start state) and every statement that contains a yield
        int resumePoints = suspendableIndices.cardinality() + (suspendableIndices.get(0) ? 0 : 1);
        if (resumePoints == statements.length) {
            // all statements are resume points
            genBlock = returnsResult ? factory.createGeneratorExprBlock(statements, readState, writeState) : factory.createGeneratorVoidBlock(statements, readState, writeState);
        } else {
            // split block into resumable chunks of at least 1 statement.
            JavaScriptNode[] chunks = new JavaScriptNode[resumePoints];
            int fromIndex = 0;
            int toIndex;
            for (int chunkI = 0; chunkI < resumePoints; chunkI++) {
                toIndex = suspendableIndices.nextSetBit(fromIndex + 1);
                if (toIndex < 0) {
                    assert chunkI == resumePoints - 1;
                    toIndex = statements.length;
                }
                returnsResult = chunkI == resumePoints - 1 && !blockNode.isResultAlwaysOfType(Undefined.class);
                JavaScriptNode chunk;
                if (fromIndex + 1 == toIndex) {
                    chunk = statements[fromIndex];
                } else {
                    JavaScriptNode[] chunkStatements = Arrays.copyOfRange(statements, fromIndex, toIndex);
                    chunk = (returnsResult && chunkI == resumePoints - 1) ? factory.createExprBlock(chunkStatements) : factory.createVoidBlock(chunkStatements);
                }
                chunks[chunkI] = chunk;
                fromIndex = toIndex;
            }
            genBlock = returnsResult ? factory.createGeneratorExprBlock(chunks, readState, writeState) : factory.createGeneratorVoidBlock(chunks, readState, writeState);
        }
        JavaScriptNode.transferSourceSectionAndTags(blockNode, genBlock);
        return genBlock;
    }

    private static boolean isSideEffectFreeUnaryOpNode(Node node) {
        // (conservative) non-exhaustive list
        return node instanceof DiscardResultNode || node instanceof VoidNode || node instanceof TypeOfNode || node instanceof JSTypeofIdenticalNode;
    }

    private static boolean isSupportedDispersibleExpression(Node node) {
        return node instanceof JSBinaryNode || node instanceof JSUnaryNode ||
                        node instanceof ArrayLiteralNode || node instanceof ObjectLiteralNode ||
                        node instanceof com.oracle.truffle.js.nodes.access.PropertyNode || node instanceof GlobalPropertyNode || node instanceof ReadElementNode ||
                        node instanceof WritePropertyNode || node instanceof WriteElementNode ||
                        node instanceof JSFunctionCallNode || node instanceof JSNewNode;
    }

    private static boolean isStatelessExpression(Node child) {
        return child instanceof JSConstantNode || child instanceof CreateObjectNode || (child instanceof RepeatableNode && !(child instanceof ReadNode));
    }

    private static boolean skipOverToChildren(Node node) {
        return node instanceof ObjectLiteralMemberNode || node instanceof AbstractFunctionArgumentsNode || node instanceof ArrayLiteralNode.SpreadArrayNode || node instanceof SpreadArgumentNode;
    }

    private void extractChildTo(Node child, Node parent, List<JavaScriptNode> extracted) {
        if (isStatelessExpression(child)) {
            return;
        }
        if (skipOverToChildren(child)) {
            extractChildrenTo(child, extracted);
        } else if (child instanceof JavaScriptNode) {
            JavaScriptNode jschild = (JavaScriptNode) child;
            if (NodeUtil.isReplacementSafe(parent, child, ANY_JAVA_SCRIPT_NODE)) {
                FrameDescriptor functionFrameDescriptor = environment.getFunctionFrameDescriptor();
                String identifier = ":generatorexpr:" + functionFrameDescriptor.getSize();
                FrameSlot frameSlot = functionFrameDescriptor.addFrameSlot(identifier);
                JavaScriptNode readState = factory.createReadCurrentFrameSlot(frameSlot);
                if (jschild.hasTag(StandardTags.ExpressionTag.class) ||
                                (jschild instanceof GeneratorWrapperNode && ((GeneratorWrapperNode) jschild).getResumableNode().hasTag(StandardTags.ExpressionTag.class))) {
                    tagHiddenExpression(readState);
                }
                JavaScriptNode writeState = factory.createWriteCurrentFrameSlot(frameSlot, functionFrameDescriptor, jschild);
                extracted.add(writeState);
                // replace child with saved expression result
                boolean ok = NodeUtil.replaceChild(parent, child, readState);
                assert ok;
            } else {
                // not assignable to field type (e.g. JSTargetableNode), cannot extract
                // but try to extract grandchildren instead, e.g.:
                // (yield)[yield](yield) => a = yield, b = yield, c = yield, a[b](c)
                extractChildrenTo(child, extracted);
            }
        }
    }

    private static Iterable<Node> getChildrenInExecutionOrder(Node parent) {
        // Note: Child and Children fields must be declared in execution order.
        return parent.getChildren();
    }

    private void extractChildrenTo(Node parent, List<JavaScriptNode> extracted) {
        for (Node child : getChildrenInExecutionOrder(parent)) {
            extractChildTo(child, parent, extracted);
        }
    }

    private JavaScriptNode handleFunctionReturn(FunctionNode functionNode, JavaScriptNode body) {
        assert (currentFunction().isGlobal() || currentFunction().isEval() || currentFunction().hasSyntheticArguments()) == (functionNode.isScript() || functionNode.isModule());
        if (currentFunction().returnsLastStatementResult()) {
            assert !currentFunction().hasReturn();
            return wrapGetCompletionValue(body);
        }
        if (currentFunction().hasReturn()) {
            if (JSConfig.ReturnValueInFrame) {
                return factory.createFrameReturnTarget(body, factory.createReadCurrentFrameSlot(currentFunction().getReturnSlot()));
            } else {
                return factory.createReturnTarget(body);
            }
        }
        return body;
    }

    private EnvironmentCloseable enterFunctionEnvironment(boolean isStrict, boolean isArrowFunction, boolean isGeneratorFunction, boolean isDerivedConstructor, boolean isAsyncFunction,
                    boolean isGlobal, boolean hasSyntheticArguments) {
        Environment functionEnv;
        if (environment instanceof EvalEnvironment) {
            assert !isArrowFunction && !isGeneratorFunction && !isDerivedConstructor && !isAsyncFunction;
            functionEnv = new FunctionEnvironment(environment.getParent(), factory, context, isStrict, true, ((EvalEnvironment) environment).isDirectEval(), false, false, false, false, isGlobal,
                            hasSyntheticArguments);
        } else if (environment instanceof DebugEnvironment) {
            assert !isArrowFunction && !isGeneratorFunction && !isDerivedConstructor && !isAsyncFunction;
            functionEnv = new FunctionEnvironment(environment, factory, context, isStrict, true, true, false, false, false, false, isGlobal, hasSyntheticArguments);
        } else {
            functionEnv = new FunctionEnvironment(environment, factory, context, isStrict, false, false, isArrowFunction, isGeneratorFunction, isDerivedConstructor, isAsyncFunction, isGlobal,
                            hasSyntheticArguments);
        }
        return new EnvironmentCloseable(functionEnv);
    }

    private void declareParameters(FunctionNode functionNode) {
        FunctionEnvironment currentFunction = currentFunction();
        currentFunction.setSimpleParameterList(functionNode.hasSimpleParameterList());
        List<IdentNode> parameters = functionNode.getParameters();
        if (parameters.size() > 0 && parameters.get(parameters.size() - 1).isRestParameter()) {
            currentFunction.setRestParameter(true);
        }
        if (functionNode.getNumOfParams() > context.getFunctionArgumentsLimit()) {
            throw Errors.createSyntaxError("function has too many arguments");
        }
    }

    private JavaScriptNode prepareDeclarations(List<JavaScriptNode> declarations, JavaScriptNode body) {
        declarations.add(body);
        return factory.createExprBlock(declarations.toArray(EMPTY_NODE_ARRAY));
    }

    // footprint: avoid creating identical 0-sized arrays
    private static JavaScriptNode[] javaScriptNodeArray(int size) {
        return size == 0 ? EMPTY_NODE_ARRAY : new JavaScriptNode[size];
    }

    private String getFunctionName(FunctionNode functionNode) {
        if (context.getEcmaScriptVersion() < 6 && (functionNode.isGetter() || functionNode.isSetter())) {
            // strip getter/setter name prefix in ES5 mode
            assert !functionNode.isAnonymous();
            String name = functionNode.getName();
            if ((functionNode.isGetter() && name.startsWith("get ")) || (functionNode.isSetter() && name.startsWith("set "))) {
                name = name.substring(4);
            }
            return name;
        }
        return functionNode.getName();
    }

    private void prepareParameters(List<JavaScriptNode> init) {
        FunctionNode function = lc.getCurrentFunction();
        FunctionEnvironment currentFunction = currentFunction();

        if (needsThisSlot(function, currentFunction) && !function.isDerivedConstructor()) {
            // Derived constructor: `this` binding is initialized after super() call.
            assert environment.findThisVar() != null;
            init.add(prepareThis(function));
        }
        if (function.needsSuper()) {
            assert function.isMethod();
            init.add(prepareSuper());
        }
        if (function.needsNewTarget()) {
            init.add(prepareNewTarget());
        }

        if (function.needsArguments() && !currentFunction.isDirectArgumentsAccess() && !currentFunction.isDirectEval()) {
            assert !function.isArrow() && !function.isClassFieldInitializer();
            init.add(prepareArguments());
        }

        int parameterCount = function.getParameters().size();
        if (parameterCount == 0) {
            return;
        }
        int i = 0;
        boolean hasRestParameter = currentFunction.hasRestParameter();
        boolean hasMappedArguments = function.needsArguments() && !function.isStrict() && function.hasSimpleParameterList();
        for (int argIndex = currentFunction.getLeadingArgumentCount(); i < parameterCount; i++, argIndex++) {
            final JavaScriptNode valueNode;
            if (hasRestParameter && i == parameterCount - 1) {
                valueNode = tagHiddenExpression(factory.createAccessRestArgument(context, argIndex, currentFunction.getTrailingArgumentCount()));
            } else {
                valueNode = tagHiddenExpression(factory.createAccessArgument(argIndex));
            }
            String paramName = function.getParameters().get(i).getName();
            VarRef paramRef = environment.findLocalVar(paramName);
            if (paramRef != null) {
                init.add(tagHiddenExpression(paramRef.createWriteNode(valueNode)));
                if (hasMappedArguments) {
                    currentFunction.addMappedParameter(paramRef.getFrameSlot(), i);
                }
            } else {
                // Duplicate parameter names are allowed in non-strict mode but have no binding.
                assert !currentFunction.isStrictMode();
            }
        }
    }

    private static JavaScriptNode tagHiddenExpression(JavaScriptNode node) {
        node.setSourceSection(unavailableInternalSection);
        if (node instanceof VarWrapperNode) {
            tagHiddenExpression(((VarWrapperNode) node).getDelegateNode());
        } else {
            node.addExpressionTag();
        }
        return node;
    }

    static int getBlockScopedSymbolFlags(VarNode varNode) {
        if (varNode.isConst()) {
            return Symbol.IS_CONST;
        } else {
            assert varNode.isLet();
            return Symbol.IS_LET | (varNode.getName().isCatchParameter() ? Symbol.IS_CATCH_PARAMETER | Symbol.HAS_BEEN_DECLARED : 0);
        }
    }

    private List<JavaScriptNode> functionEnvInit(FunctionNode functionNode) {
        FunctionEnvironment currentFunction = currentFunction();
        assert !currentFunction.isGlobal() || currentFunction.isIndirectEval();

        if (JSConfig.ReturnOptimizer) {
            markTerminalReturnNodes(functionNode.getBody());
        }

        if (JSConfig.OptimizeApplyArguments && functionNode.needsArguments() && functionNode.hasApplyArgumentsCall() &&
                        !functionNode.isArrow() && !functionNode.hasEval() && !functionNode.hasArrowEval() && !currentFunction.isDirectEval() &&
                        functionNode.getNumOfParams() == 0 &&
                        checkDirectArgumentsAccess(functionNode)) {
            currentFunction.setDirectArgumentsAccess(true);
        }

        if (functionNode.needsDynamicScope() && !currentFunction.isDirectEval()) {
            currentFunction.setIsDynamicallyScoped(true);
        }
        if (functionNode.needsNewTarget()) {
            currentFunction.setNeedsNewTarget(true);
        }

        return Collections.emptyList();
    }

    private static void functionNeedsParentFramePass(FunctionNode rootFunctionNode, JSContext context) {
        if (!context.getContextOptions().isLazyTranslation()) {
            return; // nothing to do
        }

        com.oracle.js.parser.ir.visitor.NodeVisitor<LexicalContext> visitor = new com.oracle.js.parser.ir.visitor.NodeVisitor<LexicalContext>(new LexicalContext()) {
            @Override
            public boolean enterIdentNode(IdentNode identNode) {
                if (!identNode.isPropertyName()) {
                    String varName = identNode.getName();
                    findSymbol(varName);
                }
                return true;
            }

            @Override
            public boolean enterAccessNode(AccessNode accessNode) {
                if (accessNode.isPrivate()) {
                    findSymbol(accessNode.getPrivateName());
                }
                return true;
            }

            private void findSymbol(String varName) {
                boolean local = true;
                FunctionNode lastFunction = null;
                for (Iterator<LexicalContextNode> iterator = lc.getAllNodes(); iterator.hasNext();) {
                    LexicalContextNode node = iterator.next();
                    if (node instanceof LexicalContextScope) {
                        Symbol foundSymbol = ((LexicalContextScope) node).getScope().getExistingSymbol(varName);
                        if (foundSymbol != null && !foundSymbol.isGlobal()) {
                            if (!local) {
                                markUsesAncestorScopeUntil(lastFunction, true);
                            }
                            break;
                        }
                    } else if (node instanceof FunctionNode) {
                        FunctionNode function = (FunctionNode) node;
                        if (function.isNamedFunctionExpression() && varName.equals(function.getIdent().getName())) {
                            if (!local) {
                                markUsesAncestorScopeUntil(lastFunction, true);
                            }
                            break;
                        } else if (!function.isProgram() && varName.equals(Environment.ARGUMENTS_NAME)) {
                            // arguments must be local if we are not in an arrow function
                            assert local || (lastFunction != null && lastFunction.isArrow());
                            // Arrow functions inherit 'arguments', but may also declare it.
                            // Therefore, continue until a non-arrow function or found symbol.
                            // Note that (direct) eval may also declare arguments dynamically.
                            if (!function.isArrow()) {
                                if (!local) {
                                    markUsesAncestorScopeUntil(lastFunction, true);
                                }
                                break;
                            }
                        } else if (function.isArrow() && isVarLexicallyScopedInArrowFunction(varName)) {
                            assert !varName.equals(Environment.ARGUMENTS_NAME);
                            FunctionNode nonArrowFunction = lc.getCurrentNonArrowFunction();
                            // `this` is read from the arrow function object,
                            // unless `this` is supplied by a subclass constructor
                            if (!varName.equals(Environment.THIS_NAME) || nonArrowFunction.isDerivedConstructor()) {
                                if (!nonArrowFunction.isProgram()) {
                                    markUsesAncestorScopeUntil(nonArrowFunction, false);
                                }
                            }
                            break;
                        }
                        if (function.hasEval() && !function.isProgram()) {
                            if (!local) {
                                markUsesAncestorScopeUntil(lastFunction, true);
                            }
                        } else if (function.isModule() && isImport(varName)) {
                            // needed for GetActiveScriptOrModule()
                            if (!local) {
                                markUsesAncestorScopeUntil(lastFunction, true);
                            }
                        }
                        lastFunction = function;
                        local = false;
                    } else if (node instanceof WithNode) {
                        if (!local) {
                            markUsesAncestorScopeUntil(lastFunction, true);
                        }
                    }
                }
            }

            private boolean isVarLexicallyScopedInArrowFunction(String varName) {
                switch (varName) {
                    case Environment.ARGUMENTS_NAME:
                    case Environment.NEW_TARGET_NAME:
                    case Environment.SUPER_NAME:
                    case Environment.THIS_NAME:
                        return true;
                    default:
                        return false;
                }
            }

            private boolean isImport(String varName) {
                switch (varName) {
                    case "import":
                    case "import.meta":
                        return true;
                    default:
                        return false;
                }
            }

            private void markUsesAncestorScopeUntil(FunctionNode untilFunction, boolean inclusive) {
                for (final Iterator<FunctionNode> functions = lc.getFunctions(); functions.hasNext();) {
                    FunctionNode function = functions.next();
                    if (!inclusive && function == untilFunction) {
                        break;
                    }
                    if (!function.isProgram()) {
                        function.setUsesAncestorScope(true);
                    }
                    if (inclusive && function == untilFunction) {
                        break;
                    }
                }
            }

            @Override
            public boolean enterFunctionNode(FunctionNode functionNode) {
                if (functionNode.hasEval()) {
                    markUsesAncestorScopeUntil(null, false);
                }
                // TODO if function does not have nested functions we can skip it
                return true;
            }
        };

        rootFunctionNode.accept(visitor);
    }

    private static boolean checkDirectArgumentsAccess(FunctionNode functionNode) {
        assert functionNode.needsArguments() && functionNode.hasApplyArgumentsCall() && !functionNode.isArrow() && !functionNode.hasEval() && !functionNode.hasArrowEval();
        assert functionNode.getNumOfParams() == 0 || functionNode.isStrict() || !functionNode.hasSimpleParameterList() : "must not have mapped parameters";
        class DirectArgumentsAccessVisitor extends com.oracle.js.parser.ir.visitor.NodeVisitor<LexicalContext> {
            boolean directArgumentsAccess = true;

            DirectArgumentsAccessVisitor(LexicalContext lc) {
                super(lc);
            }

            @Override
            public boolean enterIdentNode(IdentNode identNode) {
                if (!identNode.isPropertyName() && !identNode.isApplyArguments() && identNode.getName().equals(Environment.ARGUMENTS_NAME)) {
                    // `arguments` is used outside of `function.apply(_, arguments)`; bail out.
                    directArgumentsAccess = false;
                }
                return false;
            }

            @Override
            public boolean enterFunctionNode(FunctionNode nestedFunctionNode) {
                if (nestedFunctionNode == functionNode) {
                    return true;
                }
                if (nestedFunctionNode.isArrow()) {
                    // 1. arrow functions have lexical `arguments` binding;
                    // direct arguments access to outer frames currently not supported
                    directArgumentsAccess = false;
                }
                // 2. if not in strict mode, nested functions might access mapped parameters;
                // not a problem: we already ensured that this function does not have any.

                // We do not look inside nested functions.
                return false;
            }
        }

        DirectArgumentsAccessVisitor visitor = new DirectArgumentsAccessVisitor(new LexicalContext());
        functionNode.accept(visitor);
        return visitor.directArgumentsAccess;
    }

    private static void markTerminalReturnNodes(com.oracle.js.parser.ir.Node node) {
        if (node instanceof Block && ((Block) node).isTerminal()) {
            Statement lastStatement = ((Block) node).getLastStatement();
            if (lastStatement != null) {
                markTerminalReturnNodes(lastStatement);
            }
        } else if (node instanceof BlockStatement && ((BlockStatement) node).isTerminal()) {
            markTerminalReturnNodes(((BlockStatement) node).getBlock());
        } else if (node instanceof IfNode && ((IfNode) node).isTerminal()) {
            markTerminalReturnNodes(((IfNode) node).getPass());
            markTerminalReturnNodes(((IfNode) node).getFail());
        } else if (node instanceof com.oracle.js.parser.ir.ReturnNode) {
            ((com.oracle.js.parser.ir.ReturnNode) node).setInTerminalPosition(true);
        }
    }

    private List<JavaScriptNode> collectGlobalVars(FunctionNode functionNode, boolean configurable) {
        int symbolCount = functionNode.getBody().getSymbolCount();
        if (symbolCount == 0) {
            return Collections.emptyList();
        }
        final List<DeclareGlobalNode> declarations = new ArrayList<>(symbolCount);
        for (Symbol symbol : functionNode.getBody().getSymbols()) {
            if (symbol.isGlobal() && symbol.isVar()) {
                if (symbol.isHoistableDeclaration()) {
                    declarations.add(factory.createDeclareGlobalFunction(symbol.getName(), configurable, null));
                } else {
                    declarations.add(factory.createDeclareGlobalVariable(symbol.getName(), configurable));
                }
            } else if (!configurable) {
                assert symbol.isBlockScoped();
                declarations.add(factory.createDeclareGlobalLexicalVariable(symbol.getName(), symbol.isConst()));
            }
        }
        final List<JavaScriptNode> nodes = new ArrayList<>(2);
        nodes.add(factory.createGlobalDeclarationInstantiation(context, declarations));
        return nodes;
    }

    private JavaScriptNode prepareArguments() {
        VarRef argumentsVar = environment.findLocalVar(Environment.ARGUMENTS_NAME);
        boolean unmappedArgumentsObject = currentFunction().isStrictMode() || !currentFunction().hasSimpleParameterList();
        JavaScriptNode argumentsObject = factory.createArgumentsObjectNode(context, unmappedArgumentsObject, currentFunction().getLeadingArgumentCount(), currentFunction().getTrailingArgumentCount());
        if (!unmappedArgumentsObject) {
            argumentsObject = environment.findArgumentsVar().createWriteNode(argumentsObject);
        }
        return argumentsVar.createWriteNode(argumentsObject);
    }

    private JavaScriptNode prepareThis(FunctionNode functionNode) {
        // In a derived class constructor, we cannot get the this value from the arguments.
        assert !currentFunction().getNonArrowParentFunction().isDerivedConstructor();
        VarRef thisVar = environment.findThisVar();
        boolean isLexicalThis = functionNode.isArrow();
        JavaScriptNode getThisNode = isLexicalThis ? factory.createAccessLexicalThis() : factory.createAccessThis();
        if (!environment.isStrictMode() && !isLexicalThis) {
            getThisNode = factory.createPrepareThisBinding(context, getThisNode);
        }
        if (functionNode.isClassConstructor()) {
            getThisNode = initializeInstanceElements(getThisNode);
        }
        return thisVar.createWriteNode(getThisNode);
    }

    private JavaScriptNode prepareSuper() {
        JavaScriptNode getHomeObject = factory.createAccessHomeObject(context);
        return environment.findSuperVar().createWriteNode(getHomeObject);
    }

    private JavaScriptNode prepareNewTarget() {
        JavaScriptNode getNewTarget = factory.createAccessNewTarget();
        return environment.findNewTargetVar().createWriteNode(getNewTarget);
    }

    @Override
    public JavaScriptNode enterReturnNode(com.oracle.js.parser.ir.ReturnNode returnNode) {
        JavaScriptNode expression;
        if (returnNode.getExpression() != null) {
            expression = transform(returnNode.getExpression());
            if (currentFunction().isAsyncGeneratorFunction()) {
                expression = createAwaitNode(expression);
            }
        } else {
            expression = factory.createConstantUndefined();
        }

        ReturnNode returnStatement = returnNode.isInTerminalPosition() ? factory.createTerminalPositionReturn(expression) : createReturnNode(expression);
        return tagStatement(returnStatement, returnNode);
    }

    private ReturnNode createReturnNode(JavaScriptNode expression) {
        FunctionEnvironment currentFunction = currentFunction();
        currentFunction.addReturn();
        if (JSConfig.ReturnValueInFrame) {
            JavaScriptNode writeReturnSlotNode = environment.findTempVar(currentFunction.getReturnSlot()).createWriteNode(expression);
            return factory.createFrameReturn(writeReturnSlotNode);
        } else {
            return factory.createReturn(expression);
        }
    }

    @Override
    public JavaScriptNode enterBlock(Block block) {
        FunctionEnvironment currentFunction = currentFunction();
        JavaScriptNode result;
        try (EnvironmentCloseable blockEnv = enterBlockEnvironment(block)) {
            List<Statement> blockStatements = block.getStatements();
            List<JavaScriptNode> scopeInit = new ArrayList<>(block.getSymbolCount());
            if ((block.getScope().hasBlockScopedOrRedeclaredSymbols() || block.isModuleBody()) && !(environment instanceof GlobalEnvironment)) {
                createTemporalDeadZoneInit(block, scopeInit);
            }
            if (block.getScope().isFunctionTopScope() || block.getScope().isEvalScope()) {
                prepareParameters(scopeInit);
            }
            if (block.isParameterBlock() || block.isFunctionBody()) {
                // If there is a parameter scope, we declare a dynamic eval scope in both.
                // This is intentional: eval in parameter expressions share a common var scope that
                // is separate from the function body.
                // Tracking eval separately for parameter and body blocks would allow us to be more
                // precise w.r.t. dynamic eval scope bindings but probably not worth it since eval
                // in parameter expressions is rare and there's little cost in an unused frame slot.
                if (!currentFunction.isGlobal() && currentFunction.isDynamicallyScoped()) {
                    environment.reserveDynamicScopeSlot();
                }
                if (block.isFunctionBody()) {
                    if (currentFunction.isCallerContextEval()) {
                        prependDynamicScopeBindingInit(block, scopeInit);
                    }
                }
            }

            JavaScriptNode blockNode;
            if (block.isFunctionBody()) {
                // Note: Parameters should already be initialized when entering the function body.
                // Therefore, we need to create and tag the body block without the prolog.
                blockNode = transformStatements(blockStatements, block.isTerminal(), block.isExpressionBlock() || block.isParameterBlock());

                FunctionNode function = lc.getCurrentFunction();
                blockNode = handleFunctionReturn(function, blockNode);
                if (currentFunction.isDerivedConstructor()) {
                    blockNode = finishDerivedConstructorBody(function, blockNode);
                }
                tagBody(blockNode, block);

                if (!scopeInit.isEmpty()) {
                    scopeInit.add(blockNode);
                    blockNode = factory.createExprBlock(scopeInit.toArray(EMPTY_NODE_ARRAY));
                }
            } else {
                blockNode = transformStatements(blockStatements, block.isTerminal(), block.isExpressionBlock() || block.isParameterBlock(), scopeInit);
            }

            result = blockEnv.wrapBlockScope(blockNode);
        }
        // Parameter initialization must precede (i.e. wrap) the (async) generator function body
        if (block.isFunctionBody()) {
            if (currentFunction.isGeneratorFunction()) {
                result = finishGeneratorBody(result);
            }
        }
        ensureHasSourceSection(result, block);
        return result;
    }

    /**
     * Initialize block-scoped symbols with a <i>dead</i> marker value.
     */
    private void createTemporalDeadZoneInit(Block block, List<JavaScriptNode> blockWithInit) {
        assert (block.getScope().hasBlockScopedOrRedeclaredSymbols() || block.isModuleBody()) && !(environment instanceof GlobalEnvironment);

        for (Symbol symbol : block.getSymbols()) {
            if (symbol.isImportBinding()) {
                continue;
            }
            if (symbol.isBlockScoped()) {
                if (!symbol.hasBeenDeclared()) {
                    blockWithInit.add(findScopeVar(symbol.getName(), true).createWriteNode(factory.createConstant(Dead.instance())));
                }
            }
            if (symbol.isVarRedeclaredHere()) {
                // redeclaration of parameter binding; initial value is copied from outer scope.
                assert block.isFunctionBody();
                JavaScriptNode outerVar = createReadFromParentEnv(symbol.getName());
                blockWithInit.add(findScopeVar(symbol.getName(), true).createWriteNode(outerVar));
            }
        }
        if (block.isModuleBody()) {
            createResolveImports(lc.getCurrentFunction(), blockWithInit);
        }
    }

    private JavaScriptNode createReadFromParentEnv(String symbolName) {
        assert environment.getScopeLevel() >= 1;
        ScopeFrameNode parentScope = environment.getScopeLevel() == 1
                        ? factory.createScopeFrame(0, 0, ScopeFrameNode.EMPTY_FRAME_SLOT_ARRAY, null)
                        : factory.createScopeFrame(0, 1, environment.getParentSlots(0, 1), environment.function().getBlockScopeSlot());
        return factory.createReadFrameSlot(environment.getParent().findLocalVar(symbolName).getFrameSlot(), parentScope);
    }

    private void createResolveImports(FunctionNode functionNode, List<JavaScriptNode> declarations) {
        assert functionNode.isModule();

        // Assert: all named exports from module are resolvable.
        for (ImportEntry importEntry : functionNode.getModule().getImportEntries()) {
            ModuleRequest moduleRequest = importEntry.getModuleRequest();
            String localName = importEntry.getLocalName();
            JSWriteFrameSlotNode writeLocalNode = (JSWriteFrameSlotNode) environment.findLocalVar(localName).createWriteNode(null);
            JavaScriptNode thisModule = getActiveModule();
            if (importEntry.getImportName().equals(Module.STAR_NAME)) {
                assert functionNode.getBody().getScope().hasSymbol(localName) && functionNode.getBody().getScope().getExistingSymbol(localName).hasBeenDeclared();
                declarations.add(factory.createResolveStarImport(context, thisModule, moduleRequest, writeLocalNode));
            } else {
                assert functionNode.getBody().getScope().hasSymbol(localName) && functionNode.getBody().getScope().getExistingSymbol(localName).isImportBinding();
                declarations.add(factory.createResolveNamedImport(context, thisModule, moduleRequest, importEntry.getImportName(), writeLocalNode));
            }
        }
    }

    /**
     * Create var-declared dynamic scope bindings in the variable environment of the caller.
     */
    private void prependDynamicScopeBindingInit(Block block, List<JavaScriptNode> blockWithInit) {
        assert currentFunction().isCallerContextEval();
        for (Symbol symbol : block.getSymbols()) {
            if (symbol.isVar() && !environment.getVariableEnvironment().hasLocalVar(symbol.getName())) {
                blockWithInit.add(createDynamicScopeBinding(symbol.getName(), true));
            }
        }
    }

    private JavaScriptNode createDynamicScopeBinding(String varName, boolean deleteable) {
        assert deleteable;
        VarRef dynamicScopeVar = environment.findDynamicScopeVar();
        return new DeclareEvalVariableNode(context, varName, dynamicScopeVar.createReadNode(), (WriteNode) dynamicScopeVar.createWriteNode(null));
    }

    private JavaScriptNode transformStatements(List<Statement> blockStatements, boolean terminal, boolean expressionBlock) {
        return transformStatements(blockStatements, terminal, expressionBlock, javaScriptNodeArray(blockStatements.size()), 0);
    }

    private JavaScriptNode transformStatements(List<Statement> blockStatements, boolean terminal, boolean expressionBlock, List<JavaScriptNode> prolog) {
        final int size = prolog.size() + blockStatements.size();
        JavaScriptNode[] statements = javaScriptNodeArray(size);
        int pos = 0;
        if (!prolog.isEmpty()) {
            for (; pos < prolog.size(); pos++) {
                statements[pos] = prolog.get(pos);
            }
        }
        return transformStatements(blockStatements, terminal, expressionBlock, statements, pos);
    }

    private JavaScriptNode transformStatements(List<Statement> blockStatements, boolean terminal, boolean expressionBlock, JavaScriptNode[] statements, int destPos) {
        int pos = destPos;
        int lastNonEmptyIndex = -1;
        for (int i = 0; i < blockStatements.size(); i++) {
            Statement statement = blockStatements.get(i);
            JavaScriptNode statementNode = transformStatementInBlock(statement);
            if (currentFunction().returnsLastStatementResult()) {
                if (!statement.isCompletionValueNeverEmpty()) {
                    if (lastNonEmptyIndex >= 0) {
                        statements[lastNonEmptyIndex] = wrapSetCompletionValue(statements[lastNonEmptyIndex]);
                        lastNonEmptyIndex = -1;
                    }
                } else {
                    lastNonEmptyIndex = pos;
                }
            }
            statements[pos++] = statementNode;
        }
        if (currentFunction().returnsLastStatementResult() && lastNonEmptyIndex >= 0) {
            statements[lastNonEmptyIndex] = wrapSetCompletionValue(statements[lastNonEmptyIndex]);
        }

        assert pos == statements.length;
        return createBlock(statements, terminal, expressionBlock);
    }

    private EnvironmentCloseable enterBlockEnvironment(Block block) {
        // Global lexical environment is shared by scripts (but not eval).
        // Note: indirect eval creates a new environment for lexically-scoped declarations.
        // Note 2: When argument names are present in Source, use standard (local) block environment
        // for function-like semantics, not the global scope.
        if (block.isFunctionBody() && lc.getCurrentFunction().isScript() && this.argumentNames == null) {
            FunctionEnvironment currentFunction = currentFunction();
            if (!currentFunction.isEval()) {
                GlobalEnvironment globalEnv = new GlobalEnvironment(environment, factory, context);
                setupGlobalEnvironment(globalEnv, block);
                return new EnvironmentCloseable(globalEnv);
            } else if (currentFunction.isIndirectEval()) {
                GlobalEnvironment globalEnv = new GlobalEnvironment(environment, factory, context);
                BlockEnvironment blockEnv = new BlockEnvironment(globalEnv, factory, context);
                blockEnv.addFrameSlotsFromSymbols(block.getScope().getSymbols());
                return new EnvironmentCloseable(blockEnv);
            } else {
                assert currentFunction.isDirectEval();
                // We already have a global environment.
            }
        }

        return enterBlockEnvironment(block.getScope());
    }

    private EnvironmentCloseable enterBlockEnvironment(Scope scope) {
        if (scope != null) {
            /*
             * The function environment is filled with top-level vars from the function body, unless
             * the function has parameter expressions, then the function body gets a separate scope
             * and we populate the env with parameter vars (cf. FunctionDeclarationInstantiation).
             */
            if (scope.isFunctionTopScope() || scope.isEvalScope()) {
                assert environment instanceof FunctionEnvironment;
                Environment functionEnv = environment;

                FunctionNode function = lc.getCurrentFunction();
                assert function.hasClosures() || !hasClosures(function.getBody()) : function;
                if (!function.isModule() && !function.isGenerator() && (function.hasClosures() || function.hasEval())) {
                    functionEnv = new BlockEnvironment(environment, factory, context, true);
                }

                boolean onlyBlockScoped = currentFunction().isCallerContextEval();
                functionEnv.addFrameSlotsFromSymbols(scope.getSymbols(), onlyBlockScoped);

                addFunctionFrameSlots(functionEnv, function);

                return new EnvironmentCloseable(functionEnv);
            } else if (scope.hasDeclarations() || JSConfig.ManyBlockScopes) {
                BlockEnvironment blockEnv = new BlockEnvironment(environment, factory, context);
                blockEnv.addFrameSlotsFromSymbols(scope.getSymbols());
                return new EnvironmentCloseable(blockEnv);
            }
        }
        return new EnvironmentCloseable(environment);
    }

    private void addFunctionFrameSlots(Environment env, FunctionNode function) {
        if (function.needsArguments()) {
            assert function.getBody().getScope().hasSymbol(Environment.ARGUMENTS_NAME) : function;
            env.reserveArgumentsSlot();
        }

        FunctionEnvironment currentFunction = env.function();
        if (needsThisSlot(function, currentFunction)) {
            env.reserveThisSlot();
        }
        if (function.needsSuper()) {
            // arrow functions need to access [[HomeObject]] from outer non-arrow scope
            // note: an arrow function using <super> also needs <this> access
            assert function.isMethod();
            env.reserveSuperSlot();
        }
        if (function.needsNewTarget()) {
            env.reserveNewTargetSlot();
        }
    }

    private boolean needsThisSlot(FunctionNode function, FunctionEnvironment currentFunction) {
        if (currentFunction.isGlobal()) {
            return false;
        }
        // Reserve this slot if function uses this or has super(). Arrow functions and direct eval
        // in a derived class constructor must use the constructor's this slot. Otherwise,
        // non-strict direct eval uses the caller's `this` but gets it via the this argument.
        if (function.needsThis() && !((function.isArrow() || currentFunction.isDirectEval()) && currentFunction.getNonArrowParentFunction().isDerivedConstructor())) {
            return true;
        }
        if (function.needsSuper()) {
            assert function.isMethod();
            // A method using `super` also needs `this`.
            return true;
        }
        if (function.isClassConstructor() && (lc.getCurrentClass().hasInstanceFields() || lc.getCurrentClass().hasPrivateInstanceMethods())) {
            // Allocate the this slot to ensure InitializeInstanceElements is performed
            // regardless of whether the class constructor itself uses `this`.
            // Note: the this slot could be elided in this case.
            return true;
        }
        return false;
    }

    /**
     * Set up slots for lexical declarations in the global environment.
     *
     * @see #collectGlobalVars(FunctionNode, boolean)
     */
    private static void setupGlobalEnvironment(GlobalEnvironment globalEnv, Block block) {
        for (com.oracle.js.parser.ir.Symbol symbol : block.getSymbols()) {
            if (symbol.isImportBinding()) {
                continue; // no frame slot required
            }
            if (symbol.isBlockScoped()) {
                globalEnv.addLexicalDeclaration(symbol.getName(), symbol.isConst());
            } else if (symbol.isGlobal() && symbol.isVar()) {
                globalEnv.addVarDeclaration(symbol.getName());
            }
        }
    }

    private JavaScriptNode transformStatementInBlock(Statement statement) {
        return transform(statement);
    }

    @Override
    public JavaScriptNode enterBlockStatement(BlockStatement blockStatement) {
        return transform(blockStatement.getBlock());
    }

    @Override
    public JavaScriptNode enterLiteralNode(LiteralNode<?> literalNode) {
        if (literalNode instanceof LiteralNode.ArrayLiteralNode) {
            return tagExpression(createArrayLiteral(((LiteralNode.ArrayLiteralNode) literalNode).getElementExpressions()), literalNode);
        } else {
            return tagExpression(enterLiteralDefaultNode(literalNode), literalNode);
        }
    }

    private JavaScriptNode enterLiteralDefaultNode(LiteralNode<?> literalNode) {
        Object value = literalNode.getValue();
        if (value == null) {
            return factory.createConstantNull();
        } else if (value instanceof Long) { // we don't support long type
            long longValue = (long) value;
            if (JSRuntime.isSafeInteger(longValue)) {
                return factory.createConstantSafeInteger(longValue);
            }
            return factory.createConstantDouble(longValue);
        } else if (value instanceof Lexer.RegexToken) {
            return factory.createRegExpLiteral(context, ((Lexer.RegexToken) value).getExpression(), ((Lexer.RegexToken) value).getOptions());
        } else if (value instanceof BigInteger) {
            value = BigInt.fromBigInteger((BigInteger) value);
        }
        return factory.createConstant(value);
    }

    private JavaScriptNode createArrayLiteral(List<? extends Expression> elementExpressions) {
        JavaScriptNode[] elements = javaScriptNodeArray(elementExpressions.size());
        boolean hasSpread = false;
        for (int i = 0; i < elementExpressions.size(); i++) {
            Expression elementExpression = elementExpressions.get(i);
            hasSpread = hasSpread || elementExpression != null && elementExpression.isTokenType(TokenType.SPREAD_ARRAY);
            elements[i] = elementExpression != null ? transform(elementExpression) : factory.createEmpty();
        }
        return hasSpread ? factory.createArrayLiteralWithSpread(context, elements) : factory.createArrayLiteral(context, elements);
    }

    @Override
    public JavaScriptNode enterIdentNode(IdentNode identNode) {
        assert !identNode.isPropertyName();
        final JavaScriptNode result;
        if (identNode.isThis()) {
            result = createThisNode();
        } else if (identNode.isSuper()) {
            result = enterIdentNodeSuper(identNode);
        } else if (identNode.isNewTarget()) {
            result = enterNewTarget();
        } else if (identNode.isImportMeta()) {
            result = enterImportMeta();
        } else {
            String varName = identNode.getName();
            VarRef varRef = findScopeVarCheckTDZ(varName, false);
            result = varRef.createReadNode();
        }
        return tagExpression(result, identNode);
    }

    private JavaScriptNode enterNewTarget() {
        return environment.findNewTargetVar().createReadNode();
    }

    private JavaScriptNode enterIdentNodeSuper(IdentNode identNode) {
        if (!identNode.isDirectSuper()) {
            // ES6 12.3.5.3 Runtime Semantics: MakeSuperPropertyReference(propertyKey, strict)
            // ES6 8.1.1.3.5 GetSuperBase()
            JavaScriptNode getSuperBase = factory.createGetPrototype(environment.findSuperVar().createReadNode());
            JavaScriptNode receiver = checkThisBindingInitialized(environment.findThisVar().createReadNode());
            return factory.createSuperPropertyReference(getSuperBase, receiver);
        } else {
            // ES6 12.3.5.2 Runtime Semantics: GetSuperConstructor()
            assert identNode.isDirectSuper(); // super accesses should not reach here
            JavaScriptNode activeFunction = factory.createAccessCallee(currentFunction().getThisFunctionLevel());
            JavaScriptNode superConstructor = factory.createGetPrototype(activeFunction);
            JavaScriptNode receiver = environment.findThisVar().createReadNode();
            return factory.createTargetableWrapper(superConstructor, receiver);
        }
    }

    private JavaScriptNode createThisNode() {
        if (currentFunction().isGlobal()) {
            return factory.createAccessThis();
        } else {
            return checkThisBindingInitialized(environment.findThisVar().createReadNode());
        }
    }

    private JavaScriptNode createThisNodeUnchecked() {
        if (currentFunction().isGlobal()) {
            return factory.createAccessThis();
        } else {
            return environment.findThisVar().createReadNode();
        }
    }

    private JavaScriptNode checkThisBindingInitialized(JavaScriptNode accessThisNode) {
        // TODO in most cases we should be able to prove that `this` is already initialized
        if (currentFunction().getNonArrowParentFunction().isDerivedConstructor()) {
            return factory.createDerivedConstructorThis(accessThisNode);
        }
        return accessThisNode;
    }

    private JavaScriptNode enterImportMeta() {
        return factory.createImportMeta(getActiveModule());
    }

    private JavaScriptNode getActiveModule() {
        assert lc.inModule();
        return factory.createAccessFrameArgument(currentFunction().getOutermostFunctionLevel(), 0);
    }

    private JavaScriptNode getActiveScriptOrModule() {
        if (lc.inModule()) {
            return getActiveModule();
        }
        return null;
    }

    private VarRef findScopeVar(String name, boolean skipWith) {
        return environment.findVar(name, skipWith);
    }

    private VarRef findScopeVarCheckTDZ(String name, boolean initializationAssignment) {
        VarRef varRef = findScopeVar(name, false);
        if (varRef.isFunctionLocal()) {
            Symbol symbol = lc.getCurrentScope().findBlockScopedSymbolInFunction(varRef.getName());
            if (symbol == null) {
                // variable is not block-scoped
                return varRef;
            } else if (symbol.hasBeenDeclared()) {
                // variable has been unconditionally declared already
                return varRef;
            } else if (symbol.isDeclaredInSwitchBlock()) {
                // we cannot statically determine whether a block-scoped variable is in TDZ
                // in an unprotected switch case context, so we always need a dynamic check
                return varRef.withTDZCheck();
            } else {
                assert !symbol.hasBeenDeclared();
                if (initializationAssignment) {
                    symbol.setHasBeenDeclared();
                    return varRef;
                }

                // variable reference is unconditionally in the temporal dead zone, i.e.,
                // var ref is in declaring function and in scope but before the actual declaration
                return new VarRef(name) {
                    @Override
                    public boolean isGlobal() {
                        return varRef.isGlobal();
                    }

                    @Override
                    public boolean isFunctionLocal() {
                        return varRef.isFunctionLocal();
                    }

                    @Override
                    public FrameSlot getFrameSlot() {
                        return null;
                    }

                    @Override
                    public JavaScriptNode createReadNode() {
                        return factory.createThrowError(JSErrorType.ReferenceError, String.format("\"%s\" is not defined", varRef.getName()));
                    }

                    @Override
                    public JavaScriptNode createWriteNode(JavaScriptNode rhs) {
                        JavaScriptNode throwErrorNode = createReadNode();
                        return isPotentiallySideEffecting(rhs) ? DualNode.create(rhs, throwErrorNode) : throwErrorNode;
                    }

                    @Override
                    public JavaScriptNode createDeleteNode() {
                        return createReadNode();
                    }
                };
            }
        }
        return varRef.withTDZCheck();
    }

    @Override
    public JavaScriptNode enterVarNode(VarNode varNode) {
        String varName = varNode.getName().getName();
        assert currentFunction().isGlobal() && (!varNode.isBlockScoped() || lc.getCurrentBlock().isFunctionBody()) || !findScopeVar(varName, true).isGlobal() ||
                        currentFunction().isCallerContextEval() : varNode;

        Symbol symbol = null;
        if (varNode.isBlockScoped()) {
            // below, `symbol!=null` implies `isBlockScoped`
            symbol = lc.getCurrentScope().getExistingSymbol(varName);
            assert symbol != null : varName;
        }

        JavaScriptNode assignment;
        if (varNode.isAssignment()) {
            assignment = createVarAssignNode(varNode, varName);
        } else if (symbol != null && (!varNode.isDestructuring() || symbol.isDeclaredInSwitchBlock()) && !symbol.hasBeenDeclared()) {
            assert varNode.isBlockScoped();
            assignment = findScopeVar(varName, false).createWriteNode(factory.createConstantUndefined());
        } else {
            assignment = factory.createEmpty();
        }
        // mark block-scoped symbols as declared, except:
        // (a) symbols declared in a switch case always need the dynamic TDZ check
        // (b) destructuring: the symbol does not come alive until the destructuring assignment
        if (symbol != null && (!symbol.isDeclaredInSwitchBlock() && !varNode.isDestructuring())) {
            assert varNode.isBlockScoped();
            symbol.setHasBeenDeclared();
        }
        return assignment;
    }

    private JavaScriptNode createVarAssignNode(VarNode varNode, String varName) {
        JavaScriptNode rhs = transform(varNode.getAssignmentSource());
        JavaScriptNode assignment = findScopeVar(varName, false).createWriteNode(rhs);
        if (varNode.isBlockScoped() && varNode.isFunctionDeclaration() && context.isOptionAnnexB()) {
            // B.3.3 Block-Level Function Declarations Web Legacy Compatibility Semantics
            FunctionNode fn = lc.getCurrentFunction();
            if (!fn.isStrict() && !varName.equals(Environment.ARGUMENTS_NAME)) {
                Symbol symbol = lc.getCurrentScope().getExistingSymbol(varName);
                if (symbol.isHoistedBlockFunctionDeclaration()) {
                    assert hasVarSymbol(fn.getVarDeclarationBlock().getScope(), varName) : varName;
                    assignment = environment.findVar(varName, true, false, true, false, false).withRequired(false).createWriteNode(assignment);
                    tagExpression(assignment, varNode);
                }
            }
        }

        // class declarations are not statements nor expressions
        if (varNode.isClassDeclaration()) {
            return discardResult(assignment);
        }
        // do not halt on function declarations
        if (!varNode.isHoistableDeclaration()) {
            tagStatement(assignment, varNode);
        }
        ensureHasSourceSection(assignment, varNode);
        return discardResult(assignment);
    }

    private static boolean hasVarSymbol(Scope scope, String varName) {
        Symbol varSymbol = scope.getExistingSymbol(varName);
        return varSymbol != null && (varSymbol.isVar() && !varSymbol.isParam());
    }

    @Override
    public JavaScriptNode enterWhileNode(com.oracle.js.parser.ir.WhileNode whileNode) {
        JavaScriptNode test = transform(whileNode.getTest());
        tagStatement(test, whileNode.getTest());
        try (JumpTargetCloseable<ContinueTarget> target = currentFunction().pushContinueTarget(null)) {
            JavaScriptNode body = transform(whileNode.getBody());
            JavaScriptNode wrappedBody = wrapClearCompletionValue(target.wrapContinueTargetNode(body));
            JavaScriptNode result;
            if (whileNode.isDoWhile()) {
                result = createDoWhile(test, wrappedBody);
            } else {
                result = createWhileDo(test, wrappedBody);
            }
            return wrapClearAndGetCompletionValue(target.wrapBreakTargetNode(ensureHasSourceSection(result, whileNode)));
        }
    }

    private JavaScriptNode createDoWhile(JavaScriptNode condition, JavaScriptNode body) {
        return factory.createDoWhile(condition, body);
    }

    private JavaScriptNode createWhileDo(JavaScriptNode condition, JavaScriptNode body) {
        return factory.createWhileDo(condition, body);
    }

    private JavaScriptNode wrapGetCompletionValue(JavaScriptNode target) {
        if (currentFunction().returnsLastStatementResult()) {
            VarRef returnVar = environment.findTempVar(currentFunction().getReturnSlot());
            return factory.createExprBlock(target, returnVar.createReadNode());
        }
        return target;
    }

    /**
     * Sets the completion value to the return value of the statement, which must never be empty.
     */
    private JavaScriptNode wrapSetCompletionValue(JavaScriptNode statement) {
        if (currentFunction().returnsLastStatementResult()) {
            VarRef returnVar = environment.findTempVar(currentFunction().getReturnSlot());
            return returnVar.createWriteNode(statement);
        }
        return statement;
    }

    private JavaScriptNode wrapClearCompletionValue(JavaScriptNode statement) {
        if (currentFunction().returnsLastStatementResult()) {
            VarRef returnVar = environment.findTempVar(currentFunction().getReturnSlot());
            return factory.createExprBlock(returnVar.createWriteNode(factory.createConstantUndefined()), statement);
        }
        return statement;
    }

    /**
     * Wraps a statement, completion value of which is never the value empty. Sets the completion
     * value to undefined, executes the statement, and reads and returns the completion value.
     */
    private JavaScriptNode wrapClearAndGetCompletionValue(JavaScriptNode statement) {
        if (currentFunction().returnsLastStatementResult()) {
            VarRef returnVar = environment.findTempVar(currentFunction().getReturnSlot());
            return factory.createExprBlock(returnVar.createWriteNode(factory.createConstantUndefined()), statement, returnVar.createReadNode());
        }
        return statement;
    }

    private JavaScriptNode wrapSaveAndRestoreCompletionValue(JavaScriptNode statement) {
        if (currentFunction().returnsLastStatementResult()) {
            VarRef returnVar = environment.findTempVar(currentFunction().getReturnSlot());
            VarRef tempVar = environment.createTempVar();
            return factory.createExprBlock(tempVar.createWriteNode(returnVar.createReadNode()), statement, returnVar.createWriteNode(tempVar.createReadNode()));
        }
        return statement;
    }

    @Override
    public JavaScriptNode enterForNode(ForNode forNode) {
        // if init is destructuring, wait with transformation
        JavaScriptNode init = forNode.getInit() != null && !forNode.isForInOrOf() ? tagStatement(transform(forNode.getInit()), forNode.getInit()) : factory.createEmpty();
        JavaScriptNode test = forNode.getTest() != null && forNode.getTest().getExpression() != null ? tagStatement(transform(forNode.getTest()), forNode.getTest())
                        : factory.createConstantBoolean(true);
        JavaScriptNode modify = forNode.getModify() != null ? tagStatement(transform(forNode.getModify()), forNode.getModify()) : factory.createEmpty();
        try (JumpTargetCloseable<ContinueTarget> target = currentFunction().pushContinueTarget(null)) {
            JavaScriptNode result;
            if (forNode.isForOf()) {
                result = desugarForOf(forNode, modify, target);
            } else if (forNode.isForIn()) {
                result = desugarForIn(forNode, modify, target);
            } else if (forNode.isForAwaitOf()) {
                result = desugarForAwaitOf(forNode, modify, target);
            } else {
                JavaScriptNode body = transform(forNode.getBody());
                JavaScriptNode wrappedBody = wrapClearCompletionValue(target.wrapContinueTargetNode(body));
                result = target.wrapBreakTargetNode(desugarFor(forNode, init, test, modify, wrappedBody));
            }

            return wrapClearAndGetCompletionValue(result);
        }
    }

    private JavaScriptNode desugarFor(ForNode forNode, JavaScriptNode init, JavaScriptNode test, JavaScriptNode modify, JavaScriptNode wrappedBody) {
        if (needsPerIterationScope(forNode)) {
            VarRef firstTempVar = environment.createTempVar();
            FrameDescriptor iterationBlockFrameDescriptor = environment.getBlockFrameDescriptor();
            StatementNode newFor = factory.createFor(test, wrappedBody, modify, iterationBlockFrameDescriptor, firstTempVar.createReadNode(),
                            firstTempVar.createWriteNode(factory.createConstantBoolean(false)), currentFunction().getBlockScopeSlot());
            ensureHasSourceSection(newFor, forNode);
            return createBlock(init, firstTempVar.createWriteNode(factory.createConstantBoolean(true)), newFor);
        }
        JavaScriptNode whileDo = factory.createDesugaredFor(test, createBlock(wrappedBody, modify));
        if (forNode.getTest() == null) {
            tagStatement(test, forNode);
        } else {
            ensureHasSourceSection(whileDo, forNode);
        }
        return createBlock(init, whileDo);
    }

    private JavaScriptNode desugarForIn(ForNode forNode, JavaScriptNode modify, JumpTargetCloseable<ContinueTarget> jumpTarget) {
        JavaScriptNode createIteratorNode;
        if (forNode.isForEach()) {
            createIteratorNode = factory.createEnumerate(context, modify, true);
        } else {
            assert forNode.isForIn() && !forNode.isForEach() && !forNode.isForOf();
            createIteratorNode = factory.createEnumerate(context, modify, false);
        }
        return desugarForInOrOfBody(forNode, factory.createGetIterator(context, createIteratorNode), jumpTarget);
    }

    private JavaScriptNode desugarForOf(ForNode forNode, JavaScriptNode modify, JumpTargetCloseable<ContinueTarget> jumpTarget) {
        assert forNode.isForOf();
        JavaScriptNode getIterator = factory.createGetIterator(context, modify);
        return desugarForInOrOfBody(forNode, getIterator, jumpTarget);
    }

    private JavaScriptNode desugarForInOrOfBody(ForNode forNode, JavaScriptNode iterator, JumpTargetCloseable<ContinueTarget> jumpTarget) {
        assert forNode.isForInOrOf();
        VarRef iteratorVar = environment.createTempVar();
        JavaScriptNode iteratorInit = iteratorVar.createWriteNode(iterator);
        VarRef nextResultVar = environment.createTempVar();
        JavaScriptNode iteratorNext = factory.createIteratorNext(iteratorVar.createReadNode());
        // nextResult = IteratorNext(iterator)
        // while(!(done = IteratorComplete(nextResult)))
        JavaScriptNode condition = factory.createDual(context,
                        factory.createIteratorSetDone(iteratorVar.createReadNode(), factory.createConstantBoolean(true)),
                        factory.createUnary(UnaryOperation.NOT, factory.createIteratorComplete(context, nextResultVar.createWriteNode(iteratorNext))));
        JavaScriptNode wrappedBody;
        try (EnvironmentCloseable blockEnv = needsPerIterationScope(forNode) ? enterBlockEnvironment(lc.getCurrentBlock()) : new EnvironmentCloseable(environment)) {
            // var nextValue = IteratorValue(nextResult);
            VarRef nextResultVar2 = environment.findTempVar(nextResultVar.getFrameSlot());
            VarRef nextValueVar = environment.createTempVar();
            VarRef iteratorVar2 = environment.findTempVar(iteratorVar.getFrameSlot());
            JavaScriptNode nextResult = nextResultVar2.createReadNode();
            JavaScriptNode nextValue = factory.createIteratorValue(context, nextResult);
            JavaScriptNode writeNextValue = nextValueVar.createWriteNode(nextValue);
            JavaScriptNode writeNext = tagStatement(desugarForHeadAssignment(forNode, nextValueVar.createReadNode()), forNode);
            JavaScriptNode body = transform(forNode.getBody());
            wrappedBody = blockEnv.wrapBlockScope(createBlock(
                            writeNextValue,
                            factory.createIteratorSetDone(iteratorVar2.createReadNode(), factory.createConstantBoolean(false)),
                            writeNext,
                            body));
        }
        wrappedBody = jumpTarget.wrapContinueTargetNode(wrappedBody);
        JavaScriptNode whileNode = forNode.isForOf() ? factory.createDesugaredForOf(condition, wrappedBody) : factory.createDesugaredForIn(condition, wrappedBody);
        JavaScriptNode wrappedWhile = factory.createIteratorCloseIfNotDone(context, jumpTarget.wrapBreakTargetNode(whileNode), iteratorVar.createReadNode());
        JavaScriptNode resetIterator = iteratorVar.createWriteNode(factory.createConstant(JSFrameUtil.DEFAULT_VALUE));
        wrappedWhile = factory.createTryFinally(wrappedWhile, resetIterator);
        ensureHasSourceSection(whileNode, forNode);
        return createBlock(iteratorInit, wrappedWhile);
    }

    private JavaScriptNode desugarForHeadAssignment(ForNode forNode, JavaScriptNode next) {
        boolean lexicalBindingInit = forNode.hasPerIterationScope();
        if (forNode.getInit() instanceof IdentNode && lexicalBindingInit) {
            return tagExpression(findScopeVarCheckTDZ(((IdentNode) forNode.getInit()).getName(), lexicalBindingInit).createWriteNode(next), forNode);
        } else {
            // transform destructuring assignment
            return tagExpression(transformAssignment(forNode.getInit(), forNode.getInit(), next, lexicalBindingInit), forNode);
        }
    }

    private JavaScriptNode desugarForAwaitOf(ForNode forNode, JavaScriptNode modify, JumpTargetCloseable<ContinueTarget> jumpTarget) {
        assert forNode.isForAwaitOf();
        JavaScriptNode getIterator = factory.createGetAsyncIterator(context, modify);
        VarRef iteratorVar = environment.createTempVar();
        JavaScriptNode iteratorInit = iteratorVar.createWriteNode(getIterator);
        VarRef nextResultVar = environment.createTempVar();

        currentFunction().addAwait();
        JSReadFrameSlotNode asyncResultNode = (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getAsyncResultSlot()).createReadNode();
        JSReadFrameSlotNode asyncContextNode = (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getAsyncContextSlot()).createReadNode();
        JavaScriptNode iteratorNext = factory.createAsyncIteratorNext(context, iteratorVar.createReadNode(), asyncContextNode, asyncResultNode);
        // nextResult = Await(IteratorNext(iterator))
        // while(!(done = IteratorComplete(nextResult)))
        JavaScriptNode condition = factory.createDual(context,
                        factory.createIteratorSetDone(iteratorVar.createReadNode(), factory.createConstantBoolean(true)),
                        factory.createUnary(UnaryOperation.NOT, factory.createIteratorComplete(context, nextResultVar.createWriteNode(iteratorNext))));
        JavaScriptNode wrappedBody;
        try (EnvironmentCloseable blockEnv = needsPerIterationScope(forNode) ? enterBlockEnvironment(lc.getCurrentBlock()) : new EnvironmentCloseable(environment)) {
            // var nextValue = IteratorValue(nextResult);
            VarRef nextResultVar2 = environment.findTempVar(nextResultVar.getFrameSlot());
            VarRef nextValueVar = environment.createTempVar();
            VarRef iteratorVar2 = environment.findTempVar(iteratorVar.getFrameSlot());
            JavaScriptNode nextResult = nextResultVar2.createReadNode();
            JavaScriptNode nextValue = factory.createIteratorValue(context, nextResult);
            JavaScriptNode writeNextValue = nextValueVar.createWriteNode(nextValue);
            JavaScriptNode writeNext = tagStatement(desugarForHeadAssignment(forNode, nextValueVar.createReadNode()), forNode);
            JavaScriptNode body = transform(forNode.getBody());
            wrappedBody = blockEnv.wrapBlockScope(createBlock(
                            writeNextValue,
                            factory.createIteratorSetDone(iteratorVar2.createReadNode(), factory.createConstantBoolean(false)),
                            writeNext,
                            body));
        }
        wrappedBody = jumpTarget.wrapContinueTargetNode(wrappedBody);
        JavaScriptNode whileNode = factory.createDesugaredForAwaitOf(condition, wrappedBody);
        currentFunction().addAwait();
        JavaScriptNode wrappedWhile = factory.createAsyncIteratorCloseWrapper(context, jumpTarget.wrapBreakTargetNode(whileNode), iteratorVar.createReadNode(), asyncContextNode, asyncResultNode);
        JavaScriptNode resetIterator = iteratorVar.createWriteNode(factory.createConstant(JSFrameUtil.DEFAULT_VALUE));
        wrappedWhile = factory.createTryFinally(wrappedWhile, resetIterator);
        ensureHasSourceSection(whileNode, forNode);
        return createBlock(iteratorInit, wrappedWhile);
    }

    private boolean needsPerIterationScope(ForNode forNode) {
        // for loop init block may contain closures, too; that's why we check the surrounding block.
        if (forNode.hasPerIterationScope()) {
            FunctionNode function = lc.getCurrentFunction();
            if (function.hasClosures() && hasClosures(lc.getCurrentBlock())) {
                return true;
            } else if (function.hasEval()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClosures(com.oracle.js.parser.ir.Node node) {
        class HasClosuresVisitor extends NodeVisitor<LexicalContext> {
            boolean hasClosures;

            HasClosuresVisitor(LexicalContext lc) {
                super(lc);
            }

            @Override
            public boolean enterFunctionNode(FunctionNode functionNode) {
                hasClosures = true;
                return false; // do not descend into functions
            }
        }
        HasClosuresVisitor visitor = new HasClosuresVisitor(new LexicalContext());
        node.accept(visitor);
        return visitor.hasClosures;
    }

    @Override
    public JavaScriptNode enterLabelNode(com.oracle.js.parser.ir.LabelNode labelNode) {
        try (JumpTargetCloseable<BreakTarget> breakTarget = currentFunction().pushBreakTarget(labelNode.getLabelName())) {
            JavaScriptNode body = transform(labelNode.getBody());
            return breakTarget.wrapLabelBreakTargetNode(body);
        }
    }

    @Override
    public JavaScriptNode enterBreakNode(com.oracle.js.parser.ir.BreakNode breakNode) {
        return tagStatement(factory.createBreak(currentFunction().findBreakTarget(breakNode.getLabelName())), breakNode);
    }

    @Override
    public JavaScriptNode enterContinueNode(com.oracle.js.parser.ir.ContinueNode continueNode) {
        return tagStatement(factory.createContinue(currentFunction().findContinueTarget(continueNode.getLabelName())), continueNode);
    }

    @Override
    public JavaScriptNode enterIfNode(com.oracle.js.parser.ir.IfNode ifNode) {
        JavaScriptNode test = transform(ifNode.getTest());
        JavaScriptNode pass = transform(ifNode.getPass());
        JavaScriptNode fail = transform(ifNode.getFail());
        return tagStatement(factory.createIf(test, pass, fail), ifNode);
    }

    @Override
    public JavaScriptNode enterTernaryNode(TernaryNode ternaryNode) {
        JavaScriptNode test = transform(ternaryNode.getTest());
        JavaScriptNode pass = transform(ternaryNode.getTrueExpression());
        JavaScriptNode fail = transform(ternaryNode.getFalseExpression());
        return tagExpression(factory.createIf(test, pass, fail), ternaryNode);
    }

    @Override
    public JavaScriptNode enterUnaryNode(UnaryNode unaryNode) {
        switch (unaryNode.tokenType()) {
            case ADD:
            case BIT_NOT:
            case NOT:
            case SUB:
            case VOID:
                return enterUnaryDefaultNode(unaryNode);
            case TYPEOF:
                return enterTypeofNode(unaryNode);
            case INCPREFIX:
            case INCPOSTFIX:
            case DECPREFIX:
            case DECPOSTFIX:
                return enterUnaryIncDecNode(unaryNode);
            case NEW:
                return enterNewNode(unaryNode);
            case DELETE:
                return enterDelete(unaryNode);
            case SPREAD_ARGUMENT:
                return tagExpression(factory.createSpreadArgument(context, transform(unaryNode.getExpression())), unaryNode);
            case SPREAD_ARRAY:
                return tagExpression(factory.createSpreadArray(context, transform(unaryNode.getExpression())), unaryNode);
            case YIELD:
            case YIELD_STAR:
                return tagExpression(createYieldNode(unaryNode), unaryNode);
            case AWAIT:
                return tagExpression(translateAwaitNode(unaryNode), unaryNode);
            case NAMEDEVALUATION:
                return enterNamedEvaluation(unaryNode);
            default:
                throw new UnsupportedOperationException(unaryNode.tokenType().toString());
        }
    }

    private JavaScriptNode translateAwaitNode(UnaryNode unaryNode) {
        JavaScriptNode expression = transform(unaryNode.getExpression());
        return createAwaitNode(expression);
    }

    private JavaScriptNode createAwaitNode(JavaScriptNode expression) {
        FunctionEnvironment currentFunction = currentFunction();
        currentFunction.addAwait();
        JSReadFrameSlotNode asyncContextNode = (JSReadFrameSlotNode) environment.findTempVar(currentFunction.getAsyncContextSlot()).createReadNode();
        JSReadFrameSlotNode asyncResultNode = (JSReadFrameSlotNode) environment.findTempVar(currentFunction.getAsyncResultSlot()).createReadNode();
        return factory.createAwait(context, expression, asyncContextNode, asyncResultNode);
    }

    private JavaScriptNode createYieldNode(UnaryNode unaryNode) {
        FunctionEnvironment currentFunction = currentFunction();
        assert currentFunction.isGeneratorFunction();
        if (lc.getCurrentFunction().isModule()) {
            currentFunction.addYield();
            return factory.createModuleYield();
        }

        boolean asyncGeneratorYield = currentFunction.isAsyncFunction();
        boolean yieldStar = unaryNode.tokenType() == TokenType.YIELD_STAR;

        JavaScriptNode expression = transform(unaryNode.getExpression());
        ReturnNode returnNode = createReturnNode(null);
        if (asyncGeneratorYield) {
            currentFunction.addAwait();
            JSReadFrameSlotNode asyncContextNode = (JSReadFrameSlotNode) environment.findTempVar(currentFunction.getAsyncContextSlot()).createReadNode();
            JSReadFrameSlotNode asyncResultNode = (JSReadFrameSlotNode) environment.findTempVar(currentFunction.getAsyncResultSlot()).createReadNode();
            if (yieldStar) {
                VarRef tempVar = environment.createTempVar();
                return factory.createAsyncGeneratorYieldStar(context, expression, asyncContextNode, asyncResultNode, returnNode, tempVar.createReadNode(), (WriteNode) tempVar.createWriteNode(null));
            } else {
                return factory.createAsyncGeneratorYield(context, expression, asyncContextNode, asyncResultNode, returnNode);
            }
        } else {
            currentFunction.addYield();
            JSWriteFrameSlotNode writeYieldResultNode = JSConfig.YieldResultInFrame ? (JSWriteFrameSlotNode) environment.findTempVar(currentFunction.getYieldResultSlot()).createWriteNode(null)
                            : null;
            return factory.createYield(context, expression, environment.findYieldValueVar().createReadNode(), yieldStar, returnNode, writeYieldResultNode);
        }
    }

    private JavaScriptNode enterUnaryDefaultNode(UnaryNode unaryNode) {
        assert unaryNode.tokenType() != TokenType.TYPEOF;
        JavaScriptNode operand = transform(unaryNode.getExpression());
        return tagExpression(factory.createUnary(tokenTypeToUnaryOperation(unaryNode.tokenType()), operand), unaryNode);
    }

    private JavaScriptNode enterTypeofNode(UnaryNode unaryNode) {
        assert unaryNode.tokenType() == TokenType.TYPEOF;
        JavaScriptNode operand = null;
        if (unaryNode.getExpression() instanceof IdentNode) {
            IdentNode identNode = (IdentNode) unaryNode.getExpression();
            String identNodeName = identNode.getName();
            if (context.isOptionNashornCompatibilityMode() && (identNodeName.equals("__LINE__") || identNodeName.equals("__FILE__") || identNodeName.equals("__DIR__"))) {
                operand = GlobalPropertyNode.createPropertyNode(context, identNodeName);
            } else if (!identNode.isThis() && !identNode.isMetaProperty()) {
                // typeof globalVar must not throw ReferenceError if globalVar does not exist
                operand = findScopeVarCheckTDZ(identNodeName, false).withRequired(false).createReadNode();
            }
        }
        if (operand == null) {
            operand = transform(unaryNode.getExpression());
        } else {
            tagExpression(operand, unaryNode.getExpression());
        }
        return tagExpression(factory.createUnary(tokenTypeToUnaryOperation(unaryNode.tokenType()), operand), unaryNode);
    }

    private JavaScriptNode enterUnaryIncDecNode(UnaryNode unaryNode) {
        if (JSConfig.LocalVarIncDecNode && unaryNode.getExpression() instanceof IdentNode) {
            IdentNode identNode = (IdentNode) unaryNode.getExpression();
            assert !identNode.isPropertyName() && !identNode.isThis() && !identNode.isMetaProperty() && !identNode.isSuper();
            VarRef varRef = findScopeVarCheckTDZ(identNode.getName(), false);
            if (varRef instanceof FrameSlotVarRef) {
                FrameSlotVarRef frameVarRef = (FrameSlotVarRef) varRef;
                FrameSlot frameSlot = frameVarRef.getFrameSlot();
                if (JSFrameUtil.isConst(frameSlot)) {
                    // we know this is going to throw. do the read and throw TypeError.
                    return tagExpression(checkMutableBinding(frameVarRef.createReadNode(), frameSlot.getIdentifier()), unaryNode);
                }
                return tagExpression(factory.createLocalVarInc(tokenTypeToUnaryOperation(unaryNode.tokenType()), frameSlot, frameVarRef.hasTDZCheck(),
                                frameVarRef.createScopeFrameNode(), frameVarRef.getFrameDescriptor()), unaryNode);
            }
        }

        BinaryOperation operation = unaryNode.tokenType() == TokenType.INCPREFIX || unaryNode.tokenType() == TokenType.INCPOSTFIX ? BinaryOperation.ADD : BinaryOperation.SUBTRACT;
        boolean isPostfix = unaryNode.tokenType() == TokenType.INCPOSTFIX || unaryNode.tokenType() == TokenType.DECPOSTFIX;
        return tagExpression(transformCompoundAssignment(unaryNode, unaryNode.getExpression(), factory.createConstantNumericUnit(), operation, isPostfix, true), unaryNode);
    }

    private static UnaryOperation tokenTypeToUnaryOperation(TokenType tokenType) {
        switch (tokenType) {
            case ADD:
                return UnaryOperation.PLUS;
            case BIT_NOT:
                return UnaryOperation.BITWISE_COMPLEMENT;
            case NOT:
                return UnaryOperation.NOT;
            case SUB:
                return UnaryOperation.MINUS;
            case TYPEOF:
                return UnaryOperation.TYPE_OF;
            case VOID:
                return UnaryOperation.VOID;
            case DECPREFIX:
                return UnaryOperation.PREFIX_LOCAL_DECREMENT;
            case DECPOSTFIX:
                return UnaryOperation.POSTFIX_LOCAL_DECREMENT;
            case INCPREFIX:
                return UnaryOperation.PREFIX_LOCAL_INCREMENT;
            case INCPOSTFIX:
                return UnaryOperation.POSTFIX_LOCAL_INCREMENT;
            case NEW:
            case DELETE:
            default:
                throw new UnsupportedOperationException(tokenType.toString());
        }
    }

    private JavaScriptNode enterNamedEvaluation(UnaryNode unaryNode) {
        return factory.createNamedEvaluation(transform(unaryNode.getExpression()), factory.createAccessArgument(0));
    }

    private JavaScriptNode enterDelete(UnaryNode unaryNode) {
        Expression rhs = unaryNode.getExpression();
        if (rhs instanceof AccessNode || rhs instanceof IndexNode) {
            return enterDeleteProperty(unaryNode);
        } else {
            return enterDeleteIdent(unaryNode);
        }
    }

    private JavaScriptNode enterDeleteIdent(UnaryNode unaryNode) {
        Expression rhs = unaryNode.getExpression();
        JavaScriptNode result;
        if (rhs instanceof IdentNode) {
            // attempt to delete a binding
            String varName = ((IdentNode) rhs).getName();
            VarRef varRef = findScopeVar(varName, varName.equals(Environment.THIS_NAME));
            result = varRef.createDeleteNode();
        } else {
            // deleting a non-reference, always returns true
            result = factory.createDual(context, transform(rhs), factory.createConstantBoolean(true));
        }
        return tagExpression(result, unaryNode);
    }

    private JavaScriptNode enterDeleteProperty(UnaryNode deleteNode) {
        BaseNode baseNode = (BaseNode) deleteNode.getExpression();
        if (baseNode.isSuper()) {
            return tagExpression(factory.createThrowError(JSErrorType.ReferenceError, "Unsupported reference to 'super'"), deleteNode);
        }

        JavaScriptNode target = transform(baseNode.getBase());
        JavaScriptNode key;
        if (baseNode instanceof AccessNode) {
            AccessNode accessNode = (AccessNode) baseNode;
            assert !accessNode.isPrivate();
            key = factory.createConstantString(accessNode.getProperty());
        } else {
            assert baseNode instanceof IndexNode;
            IndexNode indexNode = (IndexNode) baseNode;
            key = transform(indexNode.getIndex());
        }

        if (baseNode.isOptionalChain()) {
            target = filterOptionalChainTarget(target, baseNode.isOptional());
        }
        JavaScriptNode delete = factory.createDeleteProperty(target, key, environment.isStrictMode(), context);
        tagExpression(delete, deleteNode);
        if (baseNode.isOptionalChain()) {
            delete = factory.createOptionalChain(delete);
        }
        return delete;
    }

    private JavaScriptNode filterOptionalChainTarget(JavaScriptNode target, boolean optional) {
        JavaScriptNode innerAccess;
        if (target instanceof OptionalChainNode) {
            innerAccess = ((OptionalChainNode) target).getAccessNode();
        } else if (target instanceof OptionalChainNode.OptionalTargetableNode) {
            innerAccess = ((OptionalChainNode.OptionalTargetableNode) target).getDelegateNode();
        } else {
            innerAccess = target;
        }
        if (optional) {
            innerAccess = factory.createOptionalChainShortCircuit(innerAccess);
        }
        return innerAccess;
    }

    private JavaScriptNode[] transformArgs(List<Expression> argList) {
        int len = argList.size();
        if (len > context.getFunctionArgumentsLimit()) {
            throw Errors.createSyntaxError("function has too many parameters");
        }
        JavaScriptNode[] args = javaScriptNodeArray(len);
        for (int i = 0; i < len; i++) {
            args[i] = transform(argList.get(i));
        }
        return args;
    }

    private JavaScriptNode enterNewNode(UnaryNode unaryNode) {
        CallNode callNode = (CallNode) unaryNode.getExpression();
        JavaScriptNode function = transform(callNode.getFunction());
        JavaScriptNode[] args = transformArgs(callNode.getArgs());
        JavaScriptNode call = factory.createNew(context, function, args);
        return tagExpression(tagCall(call), unaryNode);
    }

    @Override
    public JavaScriptNode enterCallNode(CallNode callNode) {
        JavaScriptNode function = transform(callNode.getFunction());
        JavaScriptNode[] args = transformArgs(callNode.getArgs());
        if (callNode.isOptionalChain()) {
            function = filterOptionalChainTarget(function, callNode.isOptional());
        }
        JavaScriptNode call;
        if (callNode.isEval() && args.length >= 1) {
            call = createCallEvalNode(function, args);
        } else if (callNode.isApplyArguments() && currentFunction().isDirectArgumentsAccess()) {
            call = createCallApplyArgumentsNode(function, args);
        } else if (callNode.getFunction() instanceof IdentNode && ((IdentNode) callNode.getFunction()).isDirectSuper()) {
            call = createCallDirectSuper(function, args);
        } else if (callNode.isImport()) {
            call = createImportCallNode(args);
        } else {
            call = factory.createFunctionCall(context, function, args);
        }
        tagExpression(tagCall(call), callNode);
        if (callNode.isOptionalChain()) {
            call = factory.createOptionalChain(call);
        }
        return call;
    }

    private JavaScriptNode[] insertNewTargetArg(JavaScriptNode[] args) {
        JavaScriptNode[] result = new JavaScriptNode[args.length + 1];
        result[0] = environment.findNewTargetVar().createReadNode();
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }

    /**
     * Initialize derived constructor this value.
     */
    private JavaScriptNode initializeThis(JavaScriptNode thisValueNode) {
        VarRef thisVar = environment.findThisVar();
        // (GR-2061) we don't have to do this check if super() can be called only once, provably
        // (incl. possible super calls in nested arrow functions)
        // => return factory.createWriteNode(thisVarNode, thisValueNode, context);
        VarRef tempVar = environment.createTempVar();
        JavaScriptNode uninitialized = factory.createBinary(context, BinaryOperation.IDENTICAL, thisVar.createReadNode(), factory.createConstantUndefined());
        return factory.createIf(factory.createDual(context, tempVar.createWriteNode(thisValueNode), uninitialized),
                        initializeInstanceElements(thisVar.createWriteNode(tempVar.createReadNode())),
                        factory.createThrowError(JSErrorType.ReferenceError, "super() called twice"));
    }

    private JavaScriptNode initializeInstanceElements(JavaScriptNode thisValueNode) {
        ClassNode classNode = lc.getCurrentClass();
        if (!classNode.hasInstanceFields() && !classNode.hasPrivateInstanceMethods()) {
            return thisValueNode;
        }

        JavaScriptNode constructor = factory.createAccessCallee(currentFunction().getThisFunctionLevel());
        return factory.createInitializeInstanceElements(context, thisValueNode, constructor);
    }

    private JavaScriptNode createCallEvalNode(JavaScriptNode function, JavaScriptNode[] args) {
        assert (currentFunction().isGlobal() || currentFunction().isStrictMode() || currentFunction().isDirectEval()) || currentFunction().isDynamicallyScoped();
        for (FunctionEnvironment func = currentFunction(); func.getParentFunction() != null; func = func.getParentFunction()) {
            func.setNeedsParentFrame(true);
        }
        return EvalNode.create(context, function, args, createThisNodeUnchecked(), new DirectEvalContext(lc.getCurrentScope(), environment, lc.getCurrentClass()),
                        currentFunction().getBlockScopeSlot());
    }

    private JavaScriptNode createCallApplyArgumentsNode(JavaScriptNode function, JavaScriptNode[] args) {
        return factory.createCallApplyArguments((JSFunctionCallNode) factory.createFunctionCall(context, function, args));
    }

    private JavaScriptNode createCallDirectSuper(JavaScriptNode function, JavaScriptNode[] args) {
        return initializeThis(factory.createFunctionCallWithNewTarget(context, function, insertNewTargetArg(args)));
    }

    private JavaScriptNode createImportCallNode(JavaScriptNode[] args) {
        assert args.length == 1 || (context.getContextOptions().isImportAssertions() && args.length == 2);
        if (context.getContextOptions().isImportAssertions() && args.length == 2) {
            return factory.createImportCall(context, args[0], getActiveScriptOrModule(), args[1]);
        }
        return factory.createImportCall(context, args[0], getActiveScriptOrModule());
    }

    @Override
    public JavaScriptNode enterBinaryNode(BinaryNode binaryNode) {
        switch (binaryNode.tokenType()) {
            case ASSIGN:
            case ASSIGN_INIT:
                return enterBinaryAssignNode(binaryNode);
            case ASSIGN_ADD:
            case ASSIGN_BIT_AND:
            case ASSIGN_BIT_OR:
            case ASSIGN_BIT_XOR:
            case ASSIGN_DIV:
            case ASSIGN_MOD:
            case ASSIGN_MUL:
            case ASSIGN_EXP:
            case ASSIGN_SAR:
            case ASSIGN_SHL:
            case ASSIGN_SHR:
            case ASSIGN_SUB:
            case ASSIGN_AND:
            case ASSIGN_OR:
            case ASSIGN_NULLCOAL:
                return enterBinaryTransformNode(binaryNode);
            case ADD:
            case SUB:
            case MUL:
            case EXP:
            case DIV:
            case MOD:
            case EQ:
            case EQ_STRICT:
            case GE:
            case GT:
            case LE:
            case LT:
            case NE:
            case NE_STRICT:
            case BIT_AND:
            case BIT_OR:
            case BIT_XOR:
            case SAR:
            case SHL:
            case SHR:
            case AND:
            case OR:
            case NULLISHCOALESC:
            case INSTANCEOF:
            case IN:
            case COMMARIGHT:
                return enterBinaryExpressionNode(binaryNode);
            case COMMALEFT:
            case ARROW:
            default:
                throw new UnsupportedOperationException(binaryNode.tokenType().toString());
        }
    }

    private JavaScriptNode enterBinaryExpressionNode(BinaryNode binaryNode) {
        JavaScriptNode lhs = transform(binaryNode.getLhs());
        JavaScriptNode rhs = transform(binaryNode.getRhs());
        return tagExpression(factory.createBinary(context, tokenTypeToBinaryOperation(binaryNode.tokenType()), lhs, rhs), binaryNode);
    }

    private JavaScriptNode enterBinaryTransformNode(BinaryNode binaryNode) {
        JavaScriptNode assignedValue = transform(binaryNode.getAssignmentSource());
        return tagExpression(transformCompoundAssignment(binaryNode, binaryNode.getAssignmentDest(), assignedValue, tokenTypeToBinaryOperation(binaryNode.tokenType()), false, false), binaryNode);
    }

    private JavaScriptNode enterBinaryAssignNode(BinaryNode binaryNode) {
        Expression assignmentDest = binaryNode.getAssignmentDest();
        JavaScriptNode assignedValue = transform(binaryNode.getAssignmentSource());
        JavaScriptNode assignment = transformAssignment(binaryNode, assignmentDest, assignedValue, binaryNode.isTokenType(TokenType.ASSIGN_INIT));
        assert assignedValue != null && (assignedValue.hasTag(StandardTags.ExpressionTag.class) || !assignedValue.isInstrumentable()) : "ExpressionTag expected but not found for: " + assignedValue;
        return tagExpression(assignment, binaryNode);
    }

    private static BinaryOperation tokenTypeToBinaryOperation(TokenType tokenType) {
        switch (tokenType) {
            case ASSIGN_ADD:
            case ADD:
                return BinaryOperation.ADD;
            case ASSIGN_SUB:
            case SUB:
                return BinaryOperation.SUBTRACT;
            case ASSIGN_MUL:
            case MUL:
                return BinaryOperation.MULTIPLY;
            case ASSIGN_EXP:
            case EXP:
                return BinaryOperation.EXPONENTIATE;
            case ASSIGN_DIV:
            case DIV:
                return BinaryOperation.DIVIDE;
            case ASSIGN_MOD:
            case MOD:
                return BinaryOperation.MODULO;
            case ASSIGN_BIT_AND:
            case BIT_AND:
                return BinaryOperation.BITWISE_AND;
            case ASSIGN_BIT_OR:
            case BIT_OR:
                return BinaryOperation.BITWISE_OR;
            case ASSIGN_BIT_XOR:
            case BIT_XOR:
                return BinaryOperation.BITWISE_XOR;
            case ASSIGN_SHL:
            case SHL:
                return BinaryOperation.BITWISE_LEFT_SHIFT;
            case ASSIGN_SAR:
            case SAR:
                return BinaryOperation.BITWISE_RIGHT_SHIFT;
            case ASSIGN_SHR:
            case SHR:
                return BinaryOperation.BITWISE_UNSIGNED_RIGHT_SHIFT;
            case EQ:
                return BinaryOperation.EQUAL;
            case EQ_STRICT:
                return BinaryOperation.IDENTICAL;
            case GE:
                return BinaryOperation.GREATER_OR_EQUAL;
            case GT:
                return BinaryOperation.GREATER;
            case LE:
                return BinaryOperation.LESS_OR_EQUAL;
            case LT:
                return BinaryOperation.LESS;
            case NE:
                return BinaryOperation.NOT_EQUAL;
            case NE_STRICT:
                return BinaryOperation.NOT_IDENTICAL;
            case ASSIGN_AND:
            case AND:
                return BinaryOperation.LOGICAL_AND;
            case ASSIGN_OR:
            case OR:
                return BinaryOperation.LOGICAL_OR;
            case ASSIGN_NULLCOAL:
            case NULLISHCOALESC:
                return BinaryOperation.NULLISH_COALESCING;
            case INSTANCEOF:
                return BinaryOperation.INSTANCEOF;
            case IN:
                return BinaryOperation.IN;
            case COMMARIGHT:
                return BinaryOperation.DUAL;
            default:
                throw new UnsupportedOperationException(tokenType.toString());
        }
    }

    private JavaScriptNode transformAssignment(Expression assignmentExpression, Expression lhsExpression, JavaScriptNode assignedValue, boolean initializationAssignment) {
        return transformAssignmentImpl(assignmentExpression, lhsExpression, assignedValue, initializationAssignment, null, false, false);
    }

    private JavaScriptNode transformCompoundAssignment(Expression assignmentExpression, Expression lhsExpression, JavaScriptNode assignedValue,
                    BinaryOperation binaryOp, boolean returnOldValue, boolean convertLHSToNumeric) {
        return transformAssignmentImpl(assignmentExpression, lhsExpression, assignedValue, false, binaryOp, returnOldValue, convertLHSToNumeric);
    }

    private JavaScriptNode transformAssignmentImpl(Expression assignmentExpression, Expression lhsExpression, JavaScriptNode assignedValue, boolean initializationAssignment,
                    BinaryOperation binaryOp, boolean returnOldValue, boolean convertLHSToNumeric) {
        JavaScriptNode assignedNode;
        switch (lhsExpression.tokenType()) {
            // Checkstyle: stop
            default: // ident with other token type
                // Checkstyle: resume
                if (!(lhsExpression instanceof IdentNode)) {
                    throw Errors.unsupported("unsupported assignment to token type: " + lhsExpression.tokenType().toString() + " " + lhsExpression.toString());
                }
                // fall through
            case IDENT:
                assignedNode = transformAssignmentIdent((IdentNode) lhsExpression, assignedValue, binaryOp, returnOldValue, convertLHSToNumeric, initializationAssignment);
                break;
            case LBRACKET:
                // target[element]
                assignedNode = transformIndexAssignment((IndexNode) lhsExpression, assignedValue, binaryOp, returnOldValue, convertLHSToNumeric);
                break;
            case PERIOD:
                // target.property
                assignedNode = transformPropertyAssignment((AccessNode) lhsExpression, assignedValue, binaryOp, returnOldValue, convertLHSToNumeric);
                break;
            case ARRAY:
                assert binaryOp == null;
                assignedNode = transformDestructuringArrayAssignment(lhsExpression, assignedValue, initializationAssignment);
                break;
            case LBRACE:
                assert binaryOp == null;
                assignedNode = transformDestructuringObjectAssignment(lhsExpression, assignedValue, initializationAssignment);
                break;
        }
        if (returnOldValue && assignedNode instanceof DualNode) {
            ensureHasSourceSection(((DualNode) assignedNode).getLeft(), assignmentExpression);
        }
        return tagExpression(assignedNode, assignmentExpression);
    }

    private static boolean isLogicalOp(BinaryOperation op) {
        return op == BinaryOperation.LOGICAL_AND || op == BinaryOperation.LOGICAL_OR || op == BinaryOperation.NULLISH_COALESCING;
    }

    private JavaScriptNode transformAssignmentIdent(IdentNode identNode, JavaScriptNode assignedValue, BinaryOperation binaryOp, boolean returnOldValue, boolean convertLHSToNumeric,
                    boolean initializationAssignment) {
        JavaScriptNode rhs = assignedValue;
        String ident = identNode.getName();
        VarRef scopeVar = findScopeVarCheckTDZ(ident, initializationAssignment);

        // if scopeVar is const, the assignment will never succeed and is only there to perform
        // the temporal dead zone check and throw a ReferenceError instead of a TypeError
        if (!initializationAssignment && scopeVar.isConst()) {
            if (context.getContextOptions().isV8LegacyConst() && !environment.isStrictMode()) {
                // Note that there is no TDZ check for const in this mode either.
                return rhs;
            }
            rhs = checkMutableBinding(rhs, scopeVar.getName());
        }

        if (binaryOp == null) {
            return scopeVar.createWriteNode(rhs);
        } else {
            if (isLogicalOp(binaryOp)) {
                assert !convertLHSToNumeric && !returnOldValue;
                JavaScriptNode readNode = tagExpression(scopeVar.createReadNode(), identNode);
                JavaScriptNode writeNode = scopeVar.createWriteNode(assignedValue);
                return factory.createBinary(context, binaryOp, readNode, writeNode);
            } else {
                // e.g.: lhs *= rhs => lhs = lhs * rhs
                // If lhs is a side-effecting getter that deletes lhs, we must not throw a
                // ReferenceError at the lhs assignment since the lhs reference is already resolved.
                // We also need to ensure that HasBinding is idempotent or evaluated at most once.
                Pair<Supplier<JavaScriptNode>, UnaryOperator<JavaScriptNode>> pair = scopeVar.createCompoundAssignNode();
                JavaScriptNode readNode = tagExpression(pair.getFirst().get(), identNode);
                if (convertLHSToNumeric) {
                    readNode = factory.createToNumericOperand(readNode);
                }
                VarRef prevValueTemp = null;
                if (returnOldValue) {
                    prevValueTemp = environment.createTempVar();
                    readNode = prevValueTemp.createWriteNode(readNode);
                }
                JavaScriptNode binOpNode = tagExpression(factory.createBinary(context, binaryOp, readNode, rhs), identNode);
                JavaScriptNode writeNode = pair.getSecond().apply(binOpNode);
                if (returnOldValue) {
                    return factory.createDual(context, writeNode, prevValueTemp.createReadNode());
                } else {
                    return writeNode;
                }
            }
        }
    }

    /**
     * If this is an attempt to change the value of an immutable binding, throw a runtime TypeError.
     */
    private JavaScriptNode checkMutableBinding(JavaScriptNode rhsNode, Object identifier) {
        if (context.getContextOptions().isV8LegacyConst() && !environment.isStrictMode()) {
            return rhsNode;
        }
        // evaluate rhs and throw TypeError
        String message = context.isOptionV8CompatibilityMode() ? "Assignment to constant variable." : "Assignment to constant \"" + identifier + "\"";
        JavaScriptNode throwTypeError = factory.createThrowError(JSErrorType.TypeError, message);
        return isPotentiallySideEffecting(rhsNode) ? createBlock(rhsNode, throwTypeError) : throwTypeError;
    }

    private JavaScriptNode transformPropertyAssignment(AccessNode accessNode, JavaScriptNode assignedValue, BinaryOperation binaryOp, boolean returnOldValue, boolean convertToNumeric) {
        JavaScriptNode assignedNode;
        JavaScriptNode target = transform(accessNode.getBase());

        if (binaryOp == null) {
            assignedNode = createWriteProperty(accessNode, target, assignedValue);
        } else {
            JavaScriptNode target1;
            JavaScriptNode target2;
            if (target instanceof RepeatableNode) {
                target1 = target;
                target2 = factory.copy(target);
            } else {
                VarRef targetTemp = environment.createTempVar();
                target1 = targetTemp.createWriteNode(target);
                target2 = targetTemp.createReadNode();
            }
            if (isLogicalOp(binaryOp)) {
                assert !convertToNumeric && !returnOldValue;
                JavaScriptNode readNode = tagExpression(createReadProperty(accessNode, target1), accessNode);
                JavaScriptNode writeNode = createWriteProperty(accessNode, target2, assignedValue);
                assignedNode = factory.createBinary(context, binaryOp, readNode, writeNode);
            } else {
                VarRef prevValueTemp = null;
                JavaScriptNode readNode = tagExpression(createReadProperty(accessNode, target2), accessNode);
                if (convertToNumeric) {
                    readNode = factory.createToNumericOperand(readNode);
                }
                if (returnOldValue) {
                    prevValueTemp = environment.createTempVar();
                    readNode = prevValueTemp.createWriteNode(readNode);
                }
                JavaScriptNode binOpNode = tagExpression(factory.createBinary(context, binaryOp, readNode, assignedValue), accessNode);
                JavaScriptNode writeNode = createWriteProperty(accessNode, target1, binOpNode);
                if (returnOldValue) {
                    assignedNode = factory.createDual(context, writeNode, prevValueTemp.createReadNode());
                } else {
                    assignedNode = writeNode;
                }
            }
        }
        return assignedNode;
    }

    private JavaScriptNode transformIndexAssignment(IndexNode indexNode, JavaScriptNode assignedValue, BinaryOperation binaryOp, boolean returnOldValue, boolean convertToNumeric) {
        JavaScriptNode assignedNode;
        JavaScriptNode target = transform(indexNode.getBase());
        JavaScriptNode elem = transform(indexNode.getIndex());

        if (binaryOp == null) {
            assignedNode = factory.createWriteElementNode(target, elem, assignedValue, context, environment.isStrictMode());
        } else {
            // Evaluation order:
            // 1. target = GetValue(baseReference)
            // 2. key = GetValue(propertyNameReference)
            // 3. RequireObjectCoercible(target); safely repeatable
            // 4. key = ToPropertyKey(key); only once
            // 5. lhs = target[key];
            // 6. result = lhs op rhs;
            // 7. target[key] = result

            // Index must be ToPropertyKey-converted only once, save it in temp var
            VarRef keyTemp = environment.createTempVar();
            JavaScriptNode readIndex = keyTemp.createReadNode();
            JSWriteFrameSlotNode writeIndex = (JSWriteFrameSlotNode) keyTemp.createWriteNode(null);

            JavaScriptNode target1;
            JavaScriptNode target2;
            if (target instanceof RepeatableNode) {
                target1 = target;
                target2 = factory.copy(target);
            } else {
                VarRef targetTemp = environment.createTempVar();
                target1 = targetTemp.createWriteNode(target);
                target2 = targetTemp.createReadNode();
            }

            if (isLogicalOp(binaryOp)) {
                assert !convertToNumeric && !returnOldValue;
                JavaScriptNode readNode = tagExpression(factory.createReadElementNode(context, target1, keyTemp.createWriteNode(elem)), indexNode);
                JavaScriptNode writeNode = factory.createCompoundWriteElementNode(target2, readIndex, assignedValue, null, context, environment.isStrictMode());
                assignedNode = factory.createBinary(context, binaryOp, readNode, writeNode);
            } else {
                JavaScriptNode readNode = tagExpression(factory.createReadElementNode(context, target2, readIndex), indexNode);
                if (convertToNumeric) {
                    readNode = factory.createToNumericOperand(readNode);
                }
                VarRef prevValueTemp = null;
                if (returnOldValue) {
                    prevValueTemp = environment.createTempVar();
                    readNode = prevValueTemp.createWriteNode(readNode);
                }
                JavaScriptNode binOpNode = tagExpression(factory.createBinary(context, binaryOp, readNode, assignedValue), indexNode);
                JavaScriptNode writeNode = factory.createCompoundWriteElementNode(target1, elem, binOpNode, writeIndex, context, environment.isStrictMode());
                if (returnOldValue) {
                    assignedNode = factory.createDual(context, writeNode, prevValueTemp.createReadNode());
                } else {
                    assignedNode = writeNode;
                }
            }
        }
        return assignedNode;
    }

    private JavaScriptNode transformDestructuringArrayAssignment(Expression lhsExpression, JavaScriptNode assignedValue, boolean initializationAssignment) {
        LiteralNode.ArrayLiteralNode arrayLiteralNode = (LiteralNode.ArrayLiteralNode) lhsExpression;
        List<Expression> elementExpressions = arrayLiteralNode.getElementExpressions();
        JavaScriptNode[] initElements = javaScriptNodeArray(elementExpressions.size());
        VarRef iteratorTempVar = environment.createTempVar();
        VarRef valueTempVar = environment.createTempVar();
        JavaScriptNode initValue = valueTempVar.createWriteNode(assignedValue);
        // By default, we use the hint to track the type of iterator.
        JavaScriptNode getIterator = factory.createGetIterator(context, initValue);
        JavaScriptNode initIteratorTempVar = iteratorTempVar.createWriteNode(getIterator);

        for (int i = 0; i < elementExpressions.size(); i++) {
            Expression element = elementExpressions.get(i);
            Expression lhsExpr;
            Expression init = null;
            if (element instanceof IdentNode) {
                lhsExpr = element;
            } else if (element instanceof BinaryNode) {
                assert element.isTokenType(TokenType.ASSIGN) || element.isTokenType(TokenType.ASSIGN_INIT);
                lhsExpr = ((BinaryNode) element).getLhs();
                init = ((BinaryNode) element).getRhs();
            } else {
                lhsExpr = element;
            }
            JavaScriptNode rhsNode = factory.createIteratorGetNextValue(context, iteratorTempVar.createReadNode(), factory.createConstantUndefined(), true);
            if (init != null) {
                rhsNode = factory.createNotUndefinedOr(rhsNode, transform(init));
            }
            if (lhsExpr != null && lhsExpr.isTokenType(TokenType.SPREAD_ARRAY)) {
                rhsNode = factory.createIteratorToArray(context, iteratorTempVar.createReadNode());
                lhsExpr = ((UnaryNode) lhsExpr).getExpression();
            }
            if (lhsExpr != null) {
                initElements[i] = transformAssignment(lhsExpr, lhsExpr, rhsNode, initializationAssignment);
            } else {
                initElements[i] = rhsNode;
            }
        }
        JavaScriptNode closeIfNotDone = factory.createIteratorCloseIfNotDone(context, createBlock(initElements), iteratorTempVar.createReadNode());
        return factory.createExprBlock(initIteratorTempVar, closeIfNotDone, valueTempVar.createReadNode());
    }

    private JavaScriptNode transformDestructuringObjectAssignment(Expression lhsExpression, JavaScriptNode assignedValue, boolean initializationAssignment) {
        ObjectNode objectLiteralNode = (ObjectNode) lhsExpression;
        List<PropertyNode> propertyExpressions = objectLiteralNode.getElements();
        if (propertyExpressions.isEmpty()) {
            return factory.createRequireObjectCoercible(assignedValue);
        }

        int numberOfProperties = propertyExpressions.size();
        boolean hasRest = propertyExpressions.get(numberOfProperties - 1).isRest();
        boolean requireObjectCoercible = hasRest && numberOfProperties == 1;
        JavaScriptNode[] initElements = javaScriptNodeArray(numberOfProperties);
        JavaScriptNode[] excludedKeys = hasRest ? javaScriptNodeArray(numberOfProperties - 1) : null;

        VarRef valueTempVar = environment.createTempVar();
        JavaScriptNode initValueTempVar = valueTempVar.createWriteNode(requireObjectCoercible ? factory.createRequireObjectCoercible(assignedValue) : assignedValue);

        for (int i = 0; i < numberOfProperties; i++) {
            PropertyNode property = propertyExpressions.get(i);
            Expression lhsExpr;
            Expression init = null;
            if (property.getValue() instanceof BinaryNode) {
                assert property.getValue().isTokenType(TokenType.ASSIGN) || property.getValue().isTokenType(TokenType.ASSIGN_INIT);
                lhsExpr = ((BinaryNode) property.getValue()).getLhs();
                init = ((BinaryNode) property.getValue()).getRhs();
            } else if (property.isRest()) {
                assert hasRest;
                lhsExpr = ((UnaryNode) property.getKey()).getExpression();
            } else {
                lhsExpr = property.getValue();
            }
            JavaScriptNode rhsNode;
            JavaScriptNode toPropertyKey = null;
            if (property.isRest()) {
                JavaScriptNode excludedItemsArray = excludedKeys.length == 0 ? null : factory.createArrayLiteral(context, excludedKeys);
                rhsNode = factory.createRestObject(context, valueTempVar.createReadNode(), excludedItemsArray);
            } else if (property.getKey() instanceof IdentNode && !property.isComputed()) {
                String keyName = property.getKeyName();
                if (hasRest) {
                    excludedKeys[i] = factory.createConstantString(keyName);
                }
                rhsNode = factory.createReadProperty(context, valueTempVar.createReadNode(), keyName);
            } else {
                JavaScriptNode key = transform(property.getKey());
                VarRef keyTempVar = environment.createTempVar();
                if (hasRest) {
                    excludedKeys[i] = keyTempVar.createReadNode();
                }
                toPropertyKey = keyTempVar.createWriteNode(factory.createToPropertyKey(key));
                rhsNode = factory.createReadElementNode(context, valueTempVar.createReadNode(), keyTempVar.createReadNode());
            }
            if (init != null) {
                rhsNode = factory.createNotUndefinedOr(rhsNode, transform(init));
            }
            JavaScriptNode initElement = transformAssignment(lhsExpr, lhsExpr, rhsNode, initializationAssignment);
            initElements[i] = (toPropertyKey == null) ? initElement : factory.createDual(context, toPropertyKey, initElement);
        }
        return factory.createExprBlock(initValueTempVar, createBlock(initElements), valueTempVar.createReadNode());
    }

    @Override
    public JavaScriptNode enterAccessNode(AccessNode accessNode) {
        JavaScriptNode base = transform(accessNode.getBase());
        if (accessNode.isOptionalChain()) {
            return createOptionalAccessNode(accessNode, base);
        }
        JavaScriptNode read = createReadProperty(accessNode, base);
        tagExpression(read, accessNode);
        return read;
    }

    private JavaScriptNode createOptionalAccessNode(AccessNode accessNode, JavaScriptNode base) {
        JavaScriptNode innerAccess = filterOptionalChainTarget(base, accessNode.isOptional());
        JavaScriptNode read = createReadProperty(accessNode, innerAccess);
        tagExpression(read, accessNode);
        return factory.createOptionalChain(read);
    }

    private JavaScriptNode createReadProperty(AccessNode accessNode, JavaScriptNode base) {
        if (accessNode.isPrivate()) {
            return createPrivateFieldGet(accessNode, base);
        } else {
            return factory.createReadProperty(context, base, accessNode.getProperty(), accessNode.isFunction());
        }
    }

    private JavaScriptNode createWriteProperty(AccessNode accessNode, JavaScriptNode base, JavaScriptNode rhs) {
        if (accessNode.isPrivate()) {
            return createPrivateFieldSet(accessNode, base, rhs);
        } else {
            return factory.createWriteProperty(base, accessNode.getProperty(), rhs, context, environment.isStrictMode());
        }
    }

    private JavaScriptNode createPrivateFieldGet(AccessNode accessNode, JavaScriptNode base) {
        VarRef privateNameVar = environment.findLocalVar(accessNode.getPrivateName());
        JavaScriptNode privateName = privateNameVar.createReadNode();
        return factory.createPrivateFieldGet(context, insertPrivateBrandCheck(base, privateNameVar), privateName);
    }

    private JavaScriptNode createPrivateFieldSet(AccessNode accessNode, JavaScriptNode base, JavaScriptNode rhs) {
        VarRef privateNameVar = environment.findLocalVar(accessNode.getPrivateName());
        JavaScriptNode privateName = privateNameVar.createReadNode();
        return factory.createPrivateFieldSet(context, insertPrivateBrandCheck(base, privateNameVar), privateName, rhs);
    }

    private JavaScriptNode insertPrivateBrandCheck(JavaScriptNode base, VarRef privateNameVar) {
        FrameSlot frameSlot = privateNameVar.getFrameSlot();
        if (JSFrameUtil.needsPrivateBrandCheck(frameSlot)) {
            int frameLevel = ((AbstractFrameVarRef) privateNameVar).getFrameLevel();
            int scopeLevel = ((AbstractFrameVarRef) privateNameVar).getScopeLevel();
            Environment memberEnv = environment.getParentAt(frameLevel, scopeLevel);
            FrameSlot constructorSlot = memberEnv.getBlockFrameDescriptor().findFrameSlot(ClassNode.PRIVATE_CONSTRUCTOR_BINDING_NAME);
            JavaScriptNode constructor = environment.createLocal(constructorSlot, frameLevel, scopeLevel);
            JavaScriptNode brand;
            if (JSFrameUtil.isPrivateNameStatic(frameSlot)) {
                brand = constructor;
            } else {
                brand = factory.createGetPrivateBrand(context, constructor);
            }
            return factory.createPrivateBrandCheck(base, brand);
        } else {
            return base;
        }
    }

    @Override
    public JavaScriptNode enterIndexNode(IndexNode indexNode) {
        JavaScriptNode base = transform(indexNode.getBase());
        JavaScriptNode index = transform(indexNode.getIndex());
        if (indexNode.isOptionalChain()) {
            return createOptionalIndexNode(indexNode, base, index);
        }
        return tagExpression(factory.createReadElementNode(context, base, index), indexNode);
    }

    private JavaScriptNode createOptionalIndexNode(IndexNode indexNode, JavaScriptNode base, JavaScriptNode index) {
        JavaScriptNode read = factory.createReadElementNode(context, filterOptionalChainTarget(base, indexNode.isOptional()), index);
        tagExpression(read, indexNode);
        return factory.createOptionalChain(read);
    }

    @Override
    public JavaScriptNode enterObjectNode(ObjectNode objectNode) {
        ArrayList<ObjectLiteralMemberNode> members = transformPropertyDefinitionList(objectNode.getElements(), false, null);
        return tagExpression(factory.createObjectLiteral(context, members), objectNode);
    }

    private ArrayList<ObjectLiteralMemberNode> transformPropertyDefinitionList(List<PropertyNode> properties, boolean isClass, Symbol classNameSymbol) {
        ArrayList<ObjectLiteralMemberNode> members = new ArrayList<>(properties.size());
        for (int i = 0; i < properties.size(); i++) {
            PropertyNode property = properties.get(i);

            final ObjectLiteralMemberNode member;
            if (property.getValue() != null || property.isClassField()) {
                member = enterObjectPropertyNode(property, isClass, classNameSymbol);
            } else if (property.isRest()) {
                assert !isClass;
                JavaScriptNode from = transform(((UnaryNode) property.getKey()).getExpression());
                member = factory.createSpreadObjectMember(property.isStatic(), from);
            } else {
                member = enterObjectAccessorNode(property, isClass);
            }
            members.add(member);
        }
        return members;
    }

    private ObjectLiteralMemberNode enterObjectAccessorNode(PropertyNode property, boolean isClass) {
        assert property.getGetter() != null || property.getSetter() != null;
        JavaScriptNode getter = getAccessor(property.getGetter());
        JavaScriptNode setter = getAccessor(property.getSetter());
        boolean enumerable = !isClass;
        if (property.isComputed()) {
            return factory.createComputedAccessorMember(transform(property.getKey()), property.isStatic(), enumerable, getter, setter);
        } else if (property.isPrivate()) {
            VarRef privateVar = environment.findLocalVar(property.getPrivateName());
            JSWriteFrameSlotNode writePrivateNode = (JSWriteFrameSlotNode) privateVar.createWriteNode(null);
            return factory.createPrivateAccessorMember(property.isStatic(), getter, setter, writePrivateNode);
        } else {
            return factory.createAccessorMember(property.getKeyName(), property.isStatic(), enumerable, getter, setter);
        }
    }

    private JavaScriptNode getAccessor(FunctionNode accessorFunction) {
        if (accessorFunction == null) {
            return null;
        }
        JavaScriptNode function = transform(accessorFunction);
        if (accessorFunction.needsSuper()) {
            assert accessorFunction.isMethod();
            function = factory.createMakeMethod(context, function);
        }
        return function;
    }

    private JavaScriptNode transformPropertyValue(Expression propertyValue, Symbol classNameSymbol) {
        if (propertyValue == null) {
            // class field without an initializer
            return factory.createConstantUndefined();
        }

        // TDZ: class name symbol cannot be used as a key but may be used as a value.
        if (classNameSymbol != null) {
            classNameSymbol.setHasBeenDeclared(true);
        }
        JavaScriptNode value = transform(propertyValue);
        if (classNameSymbol != null) {
            classNameSymbol.setHasBeenDeclared(false);
        }

        if (propertyValue instanceof FunctionNode && ((FunctionNode) propertyValue).needsSuper()) {
            assert ((FunctionNode) propertyValue).isMethod();
            value = factory.createMakeMethod(context, value);
        }
        return value;
    }

    private ObjectLiteralMemberNode enterObjectPropertyNode(PropertyNode property, boolean isClass, Symbol classNameSymbol) {
        JavaScriptNode value = transformPropertyValue(property.getValue(), classNameSymbol);

        boolean enumerable = !isClass || property.isClassField();
        if (property.isComputed()) {
            JavaScriptNode computedKey = transform(property.getKey());
            return factory.createComputedDataMember(computedKey, property.isStatic(), enumerable, value, property.isClassField(), property.isAnonymousFunctionDefinition());
        } else if (!isClass && property.isProto()) {
            return factory.createProtoMember(property.getKeyName(), property.isStatic(), value);
        } else if (property.isPrivate()) {
            VarRef privateVar = environment.findLocalVar(property.getPrivateName());
            if (property.isClassField()) {
                JSWriteFrameSlotNode writePrivateNode = (JSWriteFrameSlotNode) privateVar.createWriteNode(factory.createNewPrivateName(property.getPrivateName()));
                return factory.createPrivateFieldMember(privateVar.createReadNode(), property.isStatic(), value, writePrivateNode);
            } else {
                JSWriteFrameSlotNode writePrivateNode = (JSWriteFrameSlotNode) privateVar.createWriteNode(null);
                return factory.createPrivateMethodMember(property.isStatic(), value, writePrivateNode);
            }
        } else {
            return factory.createDataMember(property.getKeyName(), property.isStatic(), enumerable, value, property.isClassField());
        }
    }

    @Override
    public JavaScriptNode enterTryNode(TryNode tryNode) {
        JavaScriptNode tryBlock = transform(tryNode.getBody());
        JavaScriptNode result = tryBlock;
        if (!tryNode.getCatchBlocks().isEmpty()) {
            for (Block catchParamBlock : tryNode.getCatchBlocks()) {
                CatchNode catchClause = (CatchNode) catchParamBlock.getLastStatement();
                Expression catchParameter = catchClause.getException();
                Block catchBody = catchClause.getBody();
                Expression pattern = catchClause.getDestructuringPattern();

                // manually enter the catch block; this hack is only necessary to be able to
                // evaluate the condition in the same block
                try (EnvironmentCloseable catchParamEnv = enterBlockEnvironment(catchParamBlock)) {
                    lc.push(catchParamBlock);
                    try {
                        // mark variables as declared
                        for (Statement statement : catchParamBlock.getStatements().subList(0, catchParamBlock.getStatementCount() - 1)) {
                            assert statement instanceof VarNode;
                            JavaScriptNode empty = transform(statement);
                            assert empty instanceof EmptyNode;
                        }

                        JavaScriptNode writeErrorVar = null;
                        JavaScriptNode destructuring = null;
                        if (catchParameter != null) {
                            String errorVarName = ((IdentNode) catchParameter).getName();
                            VarRef errorVar = environment.findLocalVar(errorVarName);
                            writeErrorVar = errorVar.createWriteNode(null);
                            if (pattern != null) {
                                // exception is being destructured
                                destructuring = transformAssignment(pattern, pattern, errorVar.createReadNode(), true);
                            }
                        }

                        JavaScriptNode catchBlock = transform(catchBody);

                        JavaScriptNode conditionExpression;
                        if (catchClause.getExceptionCondition() != null) {
                            conditionExpression = transform(catchClause.getExceptionCondition());
                        } else {
                            conditionExpression = null; // equivalent to constant true
                        }
                        BlockScopeNode blockScope = (BlockScopeNode) catchParamEnv.wrapBlockScope(null);
                        result = factory.createTryCatch(context, result, catchBlock, writeErrorVar, blockScope, destructuring, conditionExpression);
                        ensureHasSourceSection(result, tryNode);
                    } finally {
                        lc.pop(catchParamBlock);
                    }
                }
            }
        }
        if (tryNode.getFinallyBody() != null) {
            JavaScriptNode finallyBlock = transform(tryNode.getFinallyBody());
            result = factory.createTryFinally(result, wrapSaveAndRestoreCompletionValue(wrapClearCompletionValue(finallyBlock)));
        }
        result = wrapClearAndGetCompletionValue(result);
        return result;
    }

    @Override
    public JavaScriptNode enterThrowNode(com.oracle.js.parser.ir.ThrowNode throwNode) {
        return tagStatement(factory.createThrow(context, transform(throwNode.getExpression())), throwNode);
    }

    @Override
    public JavaScriptNode enterSwitchNode(com.oracle.js.parser.ir.SwitchNode switchNode) {
        Block switchBlock = lc.getCurrentBlock();
        assert switchBlock.isSwitchBlock();

        String switchVarName = makeUniqueTempVarNameForStatement(switchNode);
        environment.declareLocalVar(switchVarName);

        JavaScriptNode switchExpression = transform(switchNode.getExpression());
        boolean isSwitchTypeofString = isSwitchTypeofStringConstant(switchNode, switchExpression);
        if (isSwitchTypeofString) {
            switchExpression = ((TypeOfNode) switchExpression).getOperand();
        }

        VarRef switchVar = environment.findLocalVar(switchVarName);
        JavaScriptNode writeSwitchNode = switchVar.createWriteNode(switchExpression);

        JavaScriptNode switchBody;
        try (JumpTargetCloseable<BreakTarget> target = currentFunction().pushBreakTarget(null)) {
            // when this switch does not use fall-through, rewrite it to an if-else-cascade
            if (JSConfig.OptimizeNoFallthroughSwitch && isNoFallthroughSwitch(switchNode)) {
                switchBody = ifElseFromSwitch(switchNode, switchVar, isSwitchTypeofString);
            } else {
                switchBody = defaultSwitchNode(switchNode, switchVar, isSwitchTypeofString);
            }
            tagStatement(switchBody, switchNode);
            switchBody = wrapClearAndGetCompletionValue(target.wrapBreakTargetNode(switchBody));
        }
        return createBlock(writeSwitchNode, switchBody);
    }

    private JavaScriptNode defaultSwitchNode(com.oracle.js.parser.ir.SwitchNode switchNode, VarRef switchVar, boolean isSwitchTypeofString) {
        List<CaseNode> cases = switchNode.getCases();
        int size = cases.size() + (switchNode.hasDefaultCase() ? 0 : 1);
        int[] jumptable = new int[size];
        int defaultpos = -1;
        List<JavaScriptNode> statementList = new ArrayList<>();
        List<JavaScriptNode> caseExprList = new ArrayList<>();
        int lastNonEmptyIndex = -1;
        for (CaseNode switchCase : cases) {
            if (switchCase.getTest() != null) {
                jumptable[caseExprList.size()] = statementList.size();
                JavaScriptNode readSwitchVarNode = switchVar.createReadNode();
                caseExprList.add(createSwitchCaseExpr(isSwitchTypeofString, switchCase, readSwitchVarNode));
            } else {
                defaultpos = statementList.size();
            }
            if (!switchCase.getStatements().isEmpty()) {
                List<Statement> statements = switchCase.getStatements();
                for (int i = 0; i < statements.size(); i++) {
                    Statement statement = statements.get(i);
                    JavaScriptNode statementNode = transform(statement);
                    if (currentFunction().returnsLastStatementResult()) {
                        if (!statement.isCompletionValueNeverEmpty()) {
                            if (lastNonEmptyIndex >= 0) {
                                statementList.set(lastNonEmptyIndex, wrapSetCompletionValue(statementList.get(lastNonEmptyIndex)));
                                lastNonEmptyIndex = -1;
                            }
                        } else {
                            lastNonEmptyIndex = statementList.size();
                        }
                    }
                    statementList.add(statementNode);
                }
            }
        }
        if (currentFunction().returnsLastStatementResult() && lastNonEmptyIndex >= 0) {
            statementList.set(lastNonEmptyIndex, wrapSetCompletionValue(statementList.get(lastNonEmptyIndex)));
        }
        // set default case position to the end
        jumptable[jumptable.length - 1] = defaultpos != -1 ? defaultpos : statementList.size();
        return factory.createSwitch(caseExprList.toArray(EMPTY_NODE_ARRAY), jumptable, statementList.toArray(EMPTY_NODE_ARRAY));
    }

    private JavaScriptNode createSwitchCaseExpr(boolean isSwitchTypeofString, CaseNode switchCase, JavaScriptNode readSwitchVarNode) {
        tagHiddenExpression(readSwitchVarNode);
        if (isSwitchTypeofString) {
            String typeString = (String) ((LiteralNode<?>) switchCase.getTest()).getValue();
            return tagExpression(factory.createTypeofIdentical(readSwitchVarNode, typeString), switchCase);
        } else {
            return tagExpression(factory.createBinary(context, BinaryOperation.IDENTICAL, readSwitchVarNode, transform(switchCase.getTest())), switchCase);
        }
    }

    /**
     * When a SwitchNode does not have any fall-through behavior, it can be transferred into an
     * if-else-cascade.
     */
    private JavaScriptNode ifElseFromSwitch(com.oracle.js.parser.ir.SwitchNode switchNode, VarRef switchVar, boolean isSwitchTypeofString) {
        assert isNoFallthroughSwitch(switchNode);

        List<CaseNode> cases = switchNode.getCases();
        CaseNode defaultCase = switchNode.getDefaultCase();

        JavaScriptNode curNode = null;
        if (defaultCase != null) {
            curNode = dropTerminalDirectBreakStatement(transformStatements(defaultCase.getStatements(), false, false));
            ensureHasSourceSection(curNode, defaultCase);
        }

        boolean defaultCascade = false;
        boolean lastCase = true;
        for (int i = cases.size() - 1; i >= 0; i--) {
            CaseNode caseNode = cases.get(i);
            if (caseNode.getTest() == null) {
                // start of the cascade with the default case
                // (the default case is the last case in the cascade)
                defaultCascade = true;
            } else {
                JavaScriptNode readSwitchVarNode = switchVar.createReadNode();
                JavaScriptNode test = createSwitchCaseExpr(isSwitchTypeofString, caseNode, readSwitchVarNode);
                if (caseNode.getStatements().isEmpty() && !lastCase) {
                    // fall through to the previous case
                    if (defaultCascade) {
                        // fall through to default case, execute test only for potential side effect
                        if (isPotentiallySideEffecting(test)) {
                            test = factory.createIf(test, null, null);
                            ensureHasSourceSection(test, caseNode);
                            curNode = curNode == null ? discardResult(test) : createBlock(test, curNode);
                        }
                    } else {
                        assert curNode instanceof com.oracle.truffle.js.nodes.control.IfNode;
                        // if (condition) => if (test || condition)
                        com.oracle.truffle.js.nodes.control.IfNode prevIfNode = (com.oracle.truffle.js.nodes.control.IfNode) curNode;
                        curNode = factory.copyIfWithCondition(prevIfNode, factory.createLogicalOr(test, prevIfNode.getCondition()));
                    }
                } else {
                    // start of a cascade (without the default case)
                    JavaScriptNode pass = dropTerminalDirectBreakStatement(transformStatements(caseNode.getStatements(), false, false));
                    ensureHasSourceSection(pass, caseNode);
                    curNode = factory.createIf(test, pass, curNode);
                    defaultCascade = false;
                }
                ensureHasSourceSection(curNode, caseNode.getTest());
            }
            lastCase = false;
        }
        return curNode == null ? factory.createEmpty() : curNode;
    }

    static boolean isPotentiallySideEffecting(JavaScriptNode test) {
        if (test instanceof JSReadFrameSlotNode) {
            return ((JSReadFrameSlotNode) test).hasTemporalDeadZone();
        }
        return !(test instanceof RepeatableNode);
    }

    private JavaScriptNode dropTerminalDirectBreakStatement(JavaScriptNode pass) {
        if (pass instanceof SequenceNode) {
            JavaScriptNode[] statements = ((SequenceNode) pass).getStatements();
            if (statements.length > 0 && isDirectBreakStatement(statements[statements.length - 1])) {
                return createBlock(Arrays.copyOfRange(statements, 0, statements.length - 1));
            }
        }
        return pass;
    }

    private static boolean isDirectBreakStatement(JavaScriptNode statement) {
        return statement instanceof BreakNode && ((BreakNode) statement).isDirectBreak();
    }

    private static boolean isNoFallthroughSwitch(com.oracle.js.parser.ir.SwitchNode switchNode) {
        List<CaseNode> cases = switchNode.getCases();
        for (int i = 0; i < cases.size() - 1; i++) { // all but the last need to be checked
            CaseNode caseNode = cases.get(i);
            List<Statement> statements = caseNode.getStatements();
            if (statements.isEmpty()) {
                // fall-through supported if case body is empty
                if (caseNode.getTest() == null) {
                    // default case fallthrough to other cases is not supported currently;
                    // i.e., default case must either appear last or end in abrupt completion
                    return false;
                }
                continue;
            }
            // Case clause must end in a break, continue, or terminal statement (return, throw).
            Statement lastStatement = statements.get(statements.size() - 1);
            if (!lastStatement.hasTerminalFlags()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Identifies whether a SwitchNode matches the pattern where the expression is a typeof() and
     * the cases are all string constants.
     */
    private static boolean isSwitchTypeofStringConstant(com.oracle.js.parser.ir.SwitchNode switchNode, JavaScriptNode switchExpression) {
        if (!(switchExpression instanceof TypeOfNode)) {
            return false;
        }
        for (CaseNode switchCase : switchNode.getCases()) {
            com.oracle.js.parser.ir.Node test = switchCase.getTest();
            if (!(test == null || (test instanceof LiteralNode && ((LiteralNode<?>) test).getValue() instanceof String))) {
                return false;
            }
        }
        return true;
    }

    private JavaScriptNode discardResult(JavaScriptNode test) {
        if (currentFunction().returnsLastStatementResult()) {
            return factory.createVoidBlock(test);
        }
        return test;
    }

    @Override
    public JavaScriptNode enterEmptyNode(com.oracle.js.parser.ir.EmptyNode emptyNode) {
        return factory.createEmpty();
    }

    @Override
    public JavaScriptNode enterWithNode(com.oracle.js.parser.ir.WithNode withNode) {
        if (context.isOptionDisableWith()) {
            throw Errors.createSyntaxError("with statement is disabled.");
        }
        JavaScriptNode withExpression = transform(withNode.getExpression());
        JavaScriptNode toObject = factory.createToObjectFromWith(context, withExpression, true);
        String withVarName = makeUniqueTempVarNameForStatement(withNode);
        environment.declareInternalSlot(withVarName);
        JavaScriptNode writeWith = environment.findLocalVar(withVarName).createWriteNode(toObject);
        try (EnvironmentCloseable withEnv = enterWithEnvironment(withVarName)) {
            JavaScriptNode withBody = transform(withNode.getBody());
            return tagStatement(factory.createWith(writeWith, wrapClearAndGetCompletionValue(withBody)), withNode);
        }
    }

    private EnvironmentCloseable enterWithEnvironment(String withVarName) {
        return new EnvironmentCloseable(new WithEnvironment(environment, factory, context, withVarName));
    }

    @Override
    public JavaScriptNode enterTemplateLiteralNode(TemplateLiteralNode templateLiteralNode) {
        JavaScriptNode result = null;
        if (templateLiteralNode instanceof TemplateLiteralNode.TaggedTemplateLiteralNode) {
            TemplateLiteralNode.TaggedTemplateLiteralNode tagged = (TemplateLiteralNode.TaggedTemplateLiteralNode) templateLiteralNode;
            result = factory.createTemplateObject(context, createArrayLiteral(tagged.getRawStrings()), createArrayLiteral(tagged.getCookedStrings()));
        } else {
            List<Expression> expressions = ((TemplateLiteralNode.UntaggedTemplateLiteralNode) templateLiteralNode).getExpressions();
            for (int i = 0; i < expressions.size(); i++) {
                JavaScriptNode expr = transform(expressions.get(i));
                assert i % 2 != 0 || expr instanceof JSConstantNode : expr;
                if (i % 2 != 0) {
                    expr = factory.createToString(expr);
                }
                result = result == null ? expr : factory.createBinary(context, BinaryOperation.ADD, result, expr);
            }
        }
        return tagExpression(result, templateLiteralNode);
    }

    @Override
    public JavaScriptNode enterDebuggerNode(DebuggerNode debuggerNode) {
        return tagStatement(factory.createDebugger(), debuggerNode);
    }

    protected static String error(final String message, final long errorToken, final LexicalContext lc) {
        final int position = Token.descPosition(errorToken);
        com.oracle.js.parser.Source internalSource = lc.getCurrentFunction().getSource();
        final int lineNum = internalSource.getLine(position);
        final int columnNum = internalSource.getColumn(position);
        final String formatted = com.oracle.js.parser.ErrorManager.format(message, internalSource, lineNum, columnNum, errorToken);
        return formatted.replace("\r\n", "\n");
    }

    @Override
    public JavaScriptNode enterExpressionStatement(ExpressionStatement expressionStatement) {
        JavaScriptNode expression = transform(expressionStatement.getExpression());
        return tagStatement(expression, expressionStatement);
    }

    @Override
    public JavaScriptNode enterJoinPredecessorExpression(JoinPredecessorExpression expr) {
        return tagExpression(transform(expr.getExpression()), expr);
    }

    @Override
    public JavaScriptNode enterClassNode(ClassNode classNode) {
        Scope classScope = classNode.getScope();
        try (EnvironmentCloseable blockEnv = enterBlockEnvironment(classScope)) {
            String className = null;
            Symbol classNameSymbol = null;
            if (classNode.getIdent() != null) {
                className = classNode.getIdent().getName();
                classNameSymbol = classScope.getExistingSymbol(className);
            }

            JavaScriptNode classHeritage = transform(classNode.getClassHeritage());
            JavaScriptNode classFunction = transform(classNode.getConstructor().getValue());

            ArrayList<ObjectLiteralMemberNode> members = transformPropertyDefinitionList(classNode.getClassElements(), true, classNameSymbol);

            JSWriteFrameSlotNode writeClassBinding = className == null ? null : (JSWriteFrameSlotNode) findScopeVar(className, true).createWriteNode(null);

            JavaScriptNode classDefinition = factory.createClassDefinition(context, (JSFunctionExpressionNode) classFunction, classHeritage,
                            members.toArray(ObjectLiteralMemberNode.EMPTY), writeClassBinding, className,
                            classNode.getInstanceFieldCount(), classNode.getStaticFieldCount(), classNode.hasPrivateInstanceMethods(), currentFunction().getBlockScopeSlot());

            if (classNode.hasPrivateMethods()) {
                // internal constructor binding used for private brand checks.
                classDefinition = environment.findLocalVar(ClassNode.PRIVATE_CONSTRUCTOR_BINDING_NAME).createWriteNode(classDefinition);
            }

            return tagExpression(blockEnv.wrapBlockScope(classDefinition), classNode);
        }
    }

    @Override
    public JavaScriptNode enterBlockExpression(BlockExpression blockExpression) {
        return tagExpression(transform(blockExpression.getBlock()), blockExpression);
    }

    @Override
    public JavaScriptNode enterParameterNode(ParameterNode paramNode) {
        final FunctionEnvironment currentFunction = currentFunction();
        final JavaScriptNode valueNode;
        if (paramNode.isRestParameter()) {
            valueNode = factory.createAccessRestArgument(context, currentFunction.getLeadingArgumentCount() + paramNode.getIndex(), currentFunction.getTrailingArgumentCount());
        } else {
            valueNode = factory.createAccessArgument(currentFunction.getLeadingArgumentCount() + paramNode.getIndex());
        }
        return tagExpression(tagHiddenExpression(valueNode), paramNode);
    }

    // ---

    @Override
    protected JavaScriptNode enterDefault(com.oracle.js.parser.ir.Node node) {
        throw shouldNotReachHere(node);
    }

    private static AssertionError shouldNotReachHere(com.oracle.js.parser.ir.Node node) {
        throw new AssertionError(String.format("should not reach here. %s(%s)", node.getClass().getSimpleName(), node));
    }

    // ---

    private SourceSection createSourceSection(FunctionNode functionNode) {
        int start = functionNode.getStartWithoutParens() - prologLength;
        int finish = functionNode.getFinishWithoutParens() - prologLength;
        int length = sourceLength;
        if (finish <= 0 || length <= start) {
            return source.createUnavailableSection();
        } else {
            start = Math.max(0, start);
            finish = Math.min(length, finish);
            return source.createSection(start, finish - start);
        }
    }

    private JavaScriptNode ensureHasSourceSection(JavaScriptNode resultNode, com.oracle.js.parser.ir.Node parseNode) {
        if (!resultNode.hasSourceSection()) {
            assignSourceSection(resultNode, parseNode);
            if (resultNode instanceof VarWrapperNode) {
                ensureHasSourceSection(((VarWrapperNode) resultNode).getDelegateNode(), parseNode);
            }
        }
        return resultNode;
    }

    private void assignSourceSection(JavaScriptNode resultNode, com.oracle.js.parser.ir.Node parseNode) {
        int start = parseNode.getStart() - prologLength;
        int finish = parseNode.getFinish() - prologLength;
        int length = sourceLength;
        if (finish <= 0 || length <= start) {
            resultNode.setSourceSection(source.createUnavailableSection());
        } else {
            start = Math.max(0, start);
            finish = Math.min(length, finish);
            resultNode.setSourceSection(source, start, finish - start);
        }
    }

    private String makeUniqueTempVarNameForStatement(Statement statement) {
        String name = ':' + statement.getClass().getSimpleName() + ':' + statement.getLineNumber() + ':' + statement.getStart();
        assert !environment.hasLocalVar(name);
        return name;
    }

    private final class EnvironmentCloseable implements AutoCloseable {
        private final Environment prevEnv = environment;
        private final Environment newEnv;
        private int wrappedInBlockScopeNode;

        EnvironmentCloseable(Environment newEnv) {
            this.newEnv = newEnv;
            environment = newEnv;
        }

        public JavaScriptNode wrapBlockScope(JavaScriptNode block) {
            if (prevEnv != newEnv) {
                wrappedInBlockScopeNode++;
                if (newEnv instanceof BlockEnvironment) {
                    BlockEnvironment blockEnv = (BlockEnvironment) newEnv;
                    // Generator functions and modules do not have an extra block environment
                    // since their frames are always materialized anyway; so we need to capture the
                    // function frame in this case. Note that isModule implies isGenerator.
                    boolean captureFunctionFrame = blockEnv.getParent() == blockEnv.function() && blockEnv.function().isGeneratorFunction();
                    return factory.createBlockScope(block, blockEnv.function().getBlockScopeSlot(), blockEnv.getBlockFrameDescriptor(), blockEnv.getParentSlot(),
                                    blockEnv.isFunctionBlock(), captureFunctionFrame);
                }
            }
            return block;
        }

        @Override
        public void close() {
            assert environment == newEnv;
            assert newEnv == prevEnv || !(newEnv instanceof BlockEnvironment) || wrappedInBlockScopeNode == 1;
            environment = prevEnv;
        }
    }
}

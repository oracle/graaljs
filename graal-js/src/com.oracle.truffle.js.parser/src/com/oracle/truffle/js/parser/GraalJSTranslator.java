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
package com.oracle.truffle.js.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.oracle.js.parser.Lexer;
import com.oracle.js.parser.Token;
import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.AccessNode;
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
import com.oracle.js.parser.ir.LiteralNode;
import com.oracle.js.parser.ir.ObjectNode;
import com.oracle.js.parser.ir.ParameterNode;
import com.oracle.js.parser.ir.PropertyNode;
import com.oracle.js.parser.ir.RuntimeNode;
import com.oracle.js.parser.ir.Statement;
import com.oracle.js.parser.ir.Symbol;
import com.oracle.js.parser.ir.TernaryNode;
import com.oracle.js.parser.ir.TryNode;
import com.oracle.js.parser.ir.UnaryNode;
import com.oracle.js.parser.ir.VarNode;
import com.oracle.js.parser.ir.WithNode;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
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
import com.oracle.truffle.js.nodes.access.FrameSlotNode;
import com.oracle.truffle.js.nodes.access.GetTemplateObjectNode;
import com.oracle.truffle.js.nodes.access.GlobalPropertyNode;
import com.oracle.truffle.js.nodes.access.GlobalScopeVarWrapperNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.LazyReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.ObjectLiteralMemberNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.access.WriteNode;
import com.oracle.truffle.js.nodes.access.WritePropertyNode;
import com.oracle.truffle.js.nodes.binary.DualNode;
import com.oracle.truffle.js.nodes.binary.JSBinaryNode;
import com.oracle.truffle.js.nodes.binary.JSOrNode;
import com.oracle.truffle.js.nodes.binary.JSTypeofIdenticalNode;
import com.oracle.truffle.js.nodes.control.BreakNode;
import com.oracle.truffle.js.nodes.control.BreakTarget;
import com.oracle.truffle.js.nodes.control.ContinueTarget;
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
import com.oracle.truffle.js.nodes.function.FunctionNameHolder;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.function.JSFunctionExpressionNode;
import com.oracle.truffle.js.nodes.function.JSNewNode;
import com.oracle.truffle.js.nodes.function.SpreadArgumentNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.nodes.unary.VoidNode;
import com.oracle.truffle.js.parser.env.BlockEnvironment;
import com.oracle.truffle.js.parser.env.Environment;
import com.oracle.truffle.js.parser.env.Environment.VarRef;
import com.oracle.truffle.js.parser.env.EvalEnvironment;
import com.oracle.truffle.js.parser.env.FunctionEnvironment;
import com.oracle.truffle.js.parser.env.FunctionEnvironment.JumpTargetCloseable;
import com.oracle.truffle.js.parser.env.GlobalEnvironment;
import com.oracle.truffle.js.parser.env.WithEnvironment;
import com.oracle.truffle.js.parser.internal.ir.debug.PrintVisitor;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;

abstract class GraalJSTranslator extends com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor<LexicalContext, JavaScriptNode> {
    public static final JavaScriptNode[] EMPTY_NODE_ARRAY = new JavaScriptNode[0];
    private static final JavaScriptNode ANY_JAVA_SCRIPT_NODE = new JavaScriptNode() {
        @Override
        public Object execute(VirtualFrame frame) {
            CompilerAsserts.neverPartOfCompilation();
            throw new UnsupportedOperationException();
        }
    };

    private static final SourceSection unavailableInternalSection = Source.newBuilder("<internal>").name("<internal>").mimeType(
                    AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).internal().build().createUnavailableSection();

    private Environment environment;
    protected final JSContext context;
    protected final NodeFactory factory;
    protected final Source source;
    private final boolean isParentStrict;

    protected GraalJSTranslator(NodeFactory factory, JSContext context, Source source, Environment environment, boolean isParentStrict) {
        super(new LexicalContext());
        this.context = context;
        this.environment = environment;
        this.factory = factory;
        this.source = source;
        this.isParentStrict = isParentStrict;
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
        if (resultNode instanceof GlobalScopeVarWrapperNode) {
            tagStatement(((GlobalScopeVarWrapperNode) resultNode).getDelegateNode(), parseNode);
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
        if (resultNode instanceof GlobalScopeVarWrapperNode) {
            tagExpression(((GlobalScopeVarWrapperNode) resultNode).getDelegateNode(), parseNode);
        } else {
            resultNode.addExpressionTag();
        }
        return resultNode;
    }

    private static JavaScriptNode tagCall(JavaScriptNode resultNode) {
        resultNode.addCallTag();
        return resultNode;
    }

    private FunctionEnvironment currentFunction() {
        return environment.function();
    }

    private JavaScriptNode createBlock(JavaScriptNode... statements) {
        return createBlock(statements, false, false);
    }

    private JavaScriptNode createBlock(JavaScriptNode[] statements, boolean terminal, boolean expressionBlock) {
        if ((JSTruffleOptions.ReturnOptimizer && terminal) || expressionBlock || currentFunction().returnsLastStatementResult()) {
            return factory.createExprBlock(statements);
        } else {
            return factory.createVoidBlock(statements);
        }
    }

    protected final ScriptNode translateScript(FunctionNode functionNode) {
        if (functionNode.getKind() != com.oracle.js.parser.ir.FunctionNode.Kind.SCRIPT) {
            throw new IllegalArgumentException("root function node is not a script");
        }
        JSFunctionExpressionNode functionExpression = (JSFunctionExpressionNode) transformFunction(functionNode);
        return ScriptNode.fromFunctionRoot(context, functionExpression.getFunctionNode());
    }

    protected final JavaScriptNode translateExpression(Expression expression) {
        try (EnvironmentCloseable dummyFunctionEnv = enterFunctionEnvironment(true, false, false, false, false)) {
            currentFunction().setNeedsParentFrame(true);
            currentFunction().freeze(); // cannot add frame slots
            return transform(expression);
        }
    }

    protected final JavaScriptNode transformFunction(FunctionNode functionNode) {
        return transform(functionNode);
    }

    protected abstract GraalJSTranslator newTranslator(Environment env);

    // ---

    @Override
    public JavaScriptNode enterFunctionNode(FunctionNode functionNode) {
        if (JSTruffleOptions.PrintParse) {
            printParse(functionNode);
        }

        boolean isStrict = functionNode.isStrict() || isParentStrict || (environment != null && environment.function() != null && environment.isStrictMode());
        boolean isArrowFunction = functionNode.getKind() == FunctionNode.Kind.ARROW;
        boolean isGeneratorFunction = functionNode.getKind() == FunctionNode.Kind.GENERATOR;
        boolean isAsyncFunction = functionNode.isAsync();
        boolean isDerivedConstructor = functionNode.isSubclassConstructor();

        boolean isMethod = functionNode.isMethod();
        boolean needsNewTarget = functionNode.usesNewTarget() || functionNode.hasDirectSuper();
        boolean isClassConstructor = functionNode.isClassConstructor();
        boolean isConstructor = !isArrowFunction && ((!isMethod || context.getEcmaScriptVersion() == 5) || isClassConstructor || isGeneratorFunction);
        assert !isDerivedConstructor || isConstructor;
        boolean strictFunctionProperties = isStrict || isArrowFunction || isMethod || isGeneratorFunction;
        boolean isBuiltin = false;
        GraalJSParserOptions parserOptions = (GraalJSParserOptions) context.getParserOptions();

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
        } else {
            isGlobal = environment == null;
            inDirectEval = environment != null && currentFunction().inDirectEval();
        }
        boolean functionMode = !isGlobal || (isStrict && isIndirectEval);

        boolean lazyTranslation = JSTruffleOptions.LazyTranslation && functionMode && !functionNode.isProgram() && !inDirectEval;

        String functionName = getFunctionName(functionNode);
        JSFunctionData functionData;
        FunctionRootNode functionRoot;
        if (lazyTranslation) {
            assert functionMode && !functionNode.isProgram();

            // function needs parent frame analysis has already been done
            boolean needsParentFrame = functionNode.usesAncestorScope();

            functionData = factory.createFunctionData(context, functionNode.getLength(), functionName, isConstructor, isDerivedConstructor, isStrict, isBuiltin,
                            needsParentFrame, isGeneratorFunction, isAsyncFunction, isClassConstructor, strictFunctionProperties, needsNewTarget);

            Environment parentEnv = environment;
            functionData.setLazyInit(fd -> {
                GraalJSTranslator translator = newTranslator(parentEnv);
                translator.translateFunctionOnDemand(functionNode, fd, isStrict, isArrowFunction, isGeneratorFunction, isAsyncFunction, isDerivedConstructor, needsNewTarget, needsParentFrame,
                                functionName);
            });
            functionRoot = null;
        } else {
            try (EnvironmentCloseable functionEnv = enterFunctionEnvironment(isStrict, isArrowFunction, isGeneratorFunction, isDerivedConstructor, isAsyncFunction)) {
                FunctionEnvironment currentFunction = currentFunction();
                currentFunction.setFunctionName(functionName);
                currentFunction.setInternalFunctionName(!functionName.isEmpty() ? functionName : functionNode.getIdent().getName());
                currentFunction.setNamedFunctionExpression(functionNode.isNamedFunctionExpression());

                declareParameters(functionNode);
                if (functionNode.getNumOfParams() > JSTruffleOptions.MaxFunctionArgumentsLength) {
                    throw Errors.createSyntaxError("function has too many arguments");
                }

                List<JavaScriptNode> declarations;
                if (functionMode) {
                    declarations = functionEnvInit(functionNode);
                } else if (functionNode.isModule()) {
                    assert currentFunction.isGlobal();
                    declarations = setupModuleEnvironment(functionNode);
                    globalVarPrePass(functionNode, parserOptions);
                    verifyModuleLocalExports(functionNode.getBody());
                } else {
                    assert currentFunction.isGlobal();
                    declarations = collectGlobalVars(functionNode, isEval);
                }
                assert functionNode.isAnalyzed();

                if (functionNode.isProgram()) {
                    functionNeedsParentFramePass(functionNode);
                }

                boolean needsParentFrame = functionNode.usesAncestorScope();
                currentFunction.setNeedsParentFrame(needsParentFrame);

                JavaScriptNode body = translateFunctionBody(functionNode, isArrowFunction, isGeneratorFunction, isAsyncFunction, isDerivedConstructor, needsNewTarget, currentFunction, declarations);

                needsParentFrame = currentFunction.needsParentFrame();
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
        if (isArrowFunction && (functionNode.usesThis() || functionNode.hasEval())) {
            JavaScriptNode thisNode = !currentFunction().isGlobal() ? environment.findThisVar().createReadNode() : factory.createAccessThis();
            functionExpression = factory.createFunctionExpressionLexicalThis(functionData, functionRoot, thisNode);
        } else {
            functionExpression = factory.createFunctionExpression(functionData, functionRoot);
        }
        return tagExpression(functionExpression, functionNode);
    }

    JavaScriptNode translateFunctionBody(FunctionNode functionNode, boolean isArrowFunction, boolean isGeneratorFunction, boolean isAsyncFunction, boolean isDerivedConstructor,
                    boolean needsNewTarget, FunctionEnvironment currentFunction, List<JavaScriptNode> declarations) {
        JavaScriptNode body = transform(functionNode.getBody());

        if (!isGeneratorFunction) {
            // finishGeneratorBody has already taken care of this for (async) generator functions
            body = handleFunctionReturn(functionNode, body);

            if (isAsyncFunction) {
                body = handleAsyncFunctionBody(body);
            }
        }

        if (!declarations.isEmpty()) {
            body = prepareDeclarations(declarations, body);
        }
        if (currentFunction.hasArgumentsSlot() && !currentFunction.isDirectArgumentsAccess() && !currentFunction.isDirectEval()) {
            body = prepareArguments(body);
        }
        if (currentFunction.getParameterCount() > 0) {
            body = prepareParameters(body);
        }
        if (currentFunction.getThisSlot() != null && !isDerivedConstructor) {
            body = prepareThis(body, isArrowFunction);
        }
        if (currentFunction.getSuperSlot() != null) {
            body = prepareSuper(body);
        }
        if (needsNewTarget) {
            body = prepareNewTarget(body);
        }

        if (isDerivedConstructor) {
            JavaScriptNode getThisBinding = checkThisBindingInitialized(functionNode.hasDirectSuper() ? environment.findThisVar().createReadNode() : factory.createConstantUndefined());
            body = factory.createDerivedConstructorResult(body, getThisBinding);
        }

        return body;
    }

    private FunctionRootNode translateFunctionOnDemand(FunctionNode functionNode, JSFunctionData functionData, boolean isStrict, boolean isArrowFunction, boolean isGeneratorFunction,
                    boolean isAsyncFunction, boolean isDerivedConstructor, boolean needsNewTarget, boolean needsParentFrame, String functionName) {
        try (EnvironmentCloseable functionEnv = enterFunctionEnvironment(isStrict, isArrowFunction, isGeneratorFunction, isDerivedConstructor, isAsyncFunction)) {
            FunctionEnvironment currentFunction = currentFunction();
            currentFunction.setFunctionName(functionName);
            currentFunction.setInternalFunctionName(!functionName.isEmpty() ? functionName : functionNode.getIdent().getName());
            currentFunction.setNamedFunctionExpression(functionNode.isNamedFunctionExpression());

            currentFunction.setNeedsParentFrame(needsParentFrame);

            declareParameters(functionNode);
            if (functionNode.getNumOfParams() > JSTruffleOptions.MaxFunctionArgumentsLength) {
                throw Errors.createSyntaxError("function has too many arguments");
            }
            functionEnvInit(functionNode);

            currentFunction.freeze();
            assert currentFunction.isDeepFrozen();

            assert getLexicalContext().isEmpty();
            getLexicalContext().push(functionNode);
            try {
                JavaScriptNode body = translateFunctionBody(functionNode, isArrowFunction, isGeneratorFunction, isAsyncFunction, isDerivedConstructor, needsNewTarget,
                                currentFunction, Collections.emptyList());
                return createFunctionRoot(functionNode, functionData, currentFunction, body);
            } finally {
                getLexicalContext().pop(functionNode);
            }
        }
    }

    private FunctionRootNode createFunctionRoot(FunctionNode functionNode, JSFunctionData functionData, FunctionEnvironment currentFunction, JavaScriptNode body) {
        SourceSection functionSourceSection = createSourceSection(functionNode);
        FunctionBodyNode functionBody = factory.createFunctionBody(body);
        FunctionRootNode functionRoot = factory.createFunctionRootNode(functionBody, environment.getFunctionFrameDescriptor(), functionData, functionSourceSection,
                        currentFunction.getInternalFunctionName());

        if (JSTruffleOptions.PrintAst) {
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
        JavaScriptNode instrumentedBody = instrumentSuspendNodes(body);
        return factory.createAsyncFunctionBody(context, instrumentedBody, writeContextNode, writeResultNode);
    }

    /**
     * Generator function parse-time AST modifications.
     *
     * @return instrumented function body
     */
    private JavaScriptNode finishGeneratorBody(JavaScriptNode bodyBlock) {
        JavaScriptNode body = handleFunctionReturn(lc.getCurrentFunction(), bodyBlock);
        // Note: parameter initialization must precede (i.e. wrap) the (async) generator body
        if (currentFunction().isAsyncGeneratorFunction()) {
            return handleAsyncGeneratorBody(body);
        } else {
            return handleGeneratorBody(body);
        }
    }

    private JavaScriptNode handleGeneratorBody(JavaScriptNode body) {
        assert currentFunction().isGeneratorFunction() && !currentFunction().isAsyncGeneratorFunction();
        VarRef yieldVar = environment.findYieldValueVar();
        JSWriteFrameSlotNode writeYieldValueNode = (JSWriteFrameSlotNode) yieldVar.createWriteNode(null);
        JSReadFrameSlotNode readYieldResultNode = JSTruffleOptions.YieldResultInFrame ? (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getYieldResultSlot()).createReadNode() : null;
        JavaScriptNode instrumentedBody = instrumentSuspendNodes(body);
        return factory.createGeneratorBody(context, instrumentedBody, writeYieldValueNode, readYieldResultNode);
    }

    private JavaScriptNode handleAsyncGeneratorBody(JavaScriptNode body) {
        assert currentFunction().isAsyncGeneratorFunction();
        JSWriteFrameSlotNode writeAsyncContextNode = (JSWriteFrameSlotNode) environment.findAsyncContextVar().createWriteNode(null);
        VarRef yieldVar = environment.findAsyncResultVar();
        JSWriteFrameSlotNode writeYieldValueNode = (JSWriteFrameSlotNode) yieldVar.createWriteNode(null);
        JSReadFrameSlotNode readYieldResultNode = JSTruffleOptions.YieldResultInFrame ? (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getYieldResultSlot()).createReadNode() : null;
        JavaScriptNode instrumentedBody = instrumentSuspendNodes(body);
        return factory.createAsyncGeneratorBody(context, instrumentedBody, writeYieldValueNode, readYieldResultNode, writeAsyncContextNode);
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
        for (Node child : getChildrenInExecutionOrder(parent)) {
            Node newChild = instrumentSuspendHelper(child, parent);
            if (newChild != null) {
                hasSuspendChild = true;
                NodeUtil.replaceChild(parent, child, newChild);
                assert !(child instanceof ResumableNode) || newChild instanceof GeneratorWrapperNode : "resumable node not wrapped: " + child;
            }
        }
        if (parent instanceof SuspendNode) {
            return wrapResumableNode((ResumableNode) parent);
        } else if (!hasSuspendChild) {
            return null;
        }

        if (parent instanceof ResumableNode) {
            return wrapResumableNode((ResumableNode) parent);
        } else if (parent instanceof ReturnNode || parent instanceof ReturnTargetNode || isSideEffectFreeUnaryOpNode(parent)) {
            // these are side-effect-free, skip
            return parent;
        } else if (isSupportedDispersibleExpression(parent)) {
            // need to rescue side-effecting/non-repeatable expressions into temporaries
            // note that the expressions have to be extracted in evaluation order
            List<JavaScriptNode> extracted = new ArrayList<>();
            // we can only replace child fields assignable from JavaScriptNode
            if (NodeUtil.isReplacementSafe(grandparent, parent, ANY_JAVA_SCRIPT_NODE)) {
                // extraction is a destructive step; only attempt it if replace can succeed
                extractChildrenTo(parent, extracted);
            } else {
                // not assignable to field type (e.g. JSTargetableNode), ignore for now
            }
            if (!extracted.isEmpty()) { // only if there's actually something to rescue
                extracted.add((JavaScriptNode) parent);
                // insert block node wrapper
                JavaScriptNode exprBlock = factory.createExprBlock(extracted.toArray(EMPTY_NODE_ARRAY));
                return wrapResumableNode((ResumableNode) exprBlock);
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

    private JavaScriptNode wrapResumableNode(ResumableNode parent) {
        String identifier = ":generatorstate:" + environment.getFunctionFrameDescriptor().getSize();
        environment.getFunctionFrameDescriptor().addFrameSlot(identifier);
        LazyReadFrameSlotNode readState = factory.createLazyReadFrameSlot(identifier);
        WriteNode writeState = factory.createLazyWriteFrameSlot(identifier, null);
        JavaScriptNode wrapper = factory.createGeneratorWrapper((JavaScriptNode) parent, readState, writeState);
        return wrapper;
    }

    private static boolean isSideEffectFreeUnaryOpNode(Node node) {
        // (conservative) non-exhaustive list
        return node instanceof VoidNode || node instanceof TypeOfNode || node instanceof JSTypeofIdenticalNode;
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
            String identifier = ":generatorexpr:" + environment.getFunctionFrameDescriptor().getSize();
            LazyReadFrameSlotNode readState = factory.createLazyReadFrameSlot(identifier);
            JavaScriptNode writeState = factory.createLazyWriteFrameSlot(identifier, (JavaScriptNode) child);
            if (NodeUtil.isReplacementSafe(parent, child, readState)) {
                environment.getFunctionFrameDescriptor().addFrameSlot(identifier);
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
        assert (currentFunction().isGlobal() || currentFunction().isEval()) == (functionNode.getKind() == FunctionNode.Kind.SCRIPT || functionNode.getKind() == FunctionNode.Kind.MODULE);
        if (currentFunction().returnsLastStatementResult()) {
            assert !currentFunction().hasReturn();
            return wrapGetCompletionValue(body);
        }
        if (currentFunction().hasReturn()) {
            if (JSTruffleOptions.ReturnValueInFrame) {
                return factory.createFrameReturnTarget(body, factory.createLocal(currentFunction().getReturnSlot(), 0, 0, ScopeFrameNode.EMPTY_FRAME_SLOT_ARRAY));
            } else {
                return factory.createReturnTarget(body);
            }
        }
        return body;
    }

    private EnvironmentCloseable enterFunctionEnvironment(boolean isStrict, boolean isArrowFunction, boolean isGeneratorFunction, boolean isDerivedConstructor, boolean isAsyncFunction) {
        Environment functionEnv;
        if (environment instanceof EvalEnvironment) {
            functionEnv = new FunctionEnvironment(environment.getParent(), factory, context, isStrict, true, ((EvalEnvironment) environment).isDirectEval(), false, false, false, false);
        } else {
            functionEnv = new FunctionEnvironment(environment, factory, context, isStrict, false, false, isArrowFunction, isGeneratorFunction, isDerivedConstructor, isAsyncFunction);
        }
        return new EnvironmentCloseable(functionEnv);
    }

    private void declareParameters(FunctionNode functionNode) {
        FunctionEnvironment currentFunction = currentFunction();
        currentFunction.setSimpleParameterList(functionNode.hasSimpleParameterList());
        List<IdentNode> parameters = functionNode.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            IdentNode parameter = parameters.get(i);
            // must be simple or rest parameter
            currentFunction.declareParameter(parameter.getName());
            if (parameter.isRestParameter()) {
                assert i == parameters.size() - 1;
                currentFunction.setRestParameter(true);
            }
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
        if (context.getEcmaScriptVersion() < 6 && isGetterOrSetter(functionNode)) {
            // strip getter/setter name prefix in ES5 mode
            assert !functionNode.isAnonymous();
            String name = functionNode.getIdent().getName();
            if ((functionNode.getKind() == com.oracle.js.parser.ir.FunctionNode.Kind.GETTER && name.startsWith("get ")) ||
                            (functionNode.getKind() == com.oracle.js.parser.ir.FunctionNode.Kind.SETTER && name.startsWith("set "))) {
                name = name.substring(4);
            }
            return name;
        }
        return !functionNode.isAnonymous() ? functionNode.getIdent().getName() : "";
    }

    private static boolean isGetterOrSetter(FunctionNode functionNode) {
        return functionNode.getKind() == com.oracle.js.parser.ir.FunctionNode.Kind.GETTER ||
                        functionNode.getKind() == com.oracle.js.parser.ir.FunctionNode.Kind.SETTER;
    }

    private JavaScriptNode prepareParameters(JavaScriptNode body) {
        FrameSlot[] frameSlots = currentFunction().getParameters().toArray(new FrameSlot[currentFunction().getParameterCount()]);
        return createEnterFrameBlock(frameSlots, body);
    }

    private JavaScriptNode createEnterFrameBlock(FrameSlot[] parameterSlots, JavaScriptNode body) {
        if (parameterSlots.length != 0) {
            JavaScriptNode[] parameterAssignment = javaScriptNodeArray(parameterSlots.length + 1);
            int i = 0;
            boolean hasRestParameter = currentFunction().hasRestParameter();
            for (int argIndex = currentFunction().getLeadingArgumentCount(); i < parameterSlots.length; i++, argIndex++) {
                final JavaScriptNode valueNode;
                if (hasRestParameter && i == parameterSlots.length - 1) {
                    valueNode = tagHiddenExpression(factory.createAccessRestArgument(context, argIndex, currentFunction().getTrailingArgumentCount()));
                } else {
                    valueNode = tagHiddenExpression(factory.createAccessArgument(argIndex));
                }
                parameterAssignment[i] = tagHiddenExpression(factory.createWriteFrameSlot(parameterSlots[i], 0, 0, ScopeFrameNode.EMPTY_FRAME_SLOT_ARRAY, valueNode));
            }
            parameterAssignment[i] = body;
            return factory.createExprBlock(parameterAssignment);
        } else {
            return body;
        }
    }

    private static JavaScriptNode tagHiddenExpression(JavaScriptNode node) {
        node.setSourceSection(unavailableInternalSection);
        if (node instanceof GlobalScopeVarWrapperNode) {
            tagHiddenExpression(((GlobalScopeVarWrapperNode) node).getDelegateNode());
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

        if (JSTruffleOptions.ReturnOptimizer) {
            markTerminalReturnNodes(functionNode.getBody());
        }

        if (functionNode.getKind() != FunctionNode.Kind.ARROW && functionNode.needsArguments()) {
            currentFunction.reserveArgumentsSlot();

            if (JSTruffleOptions.OptimizeApplyArguments && functionNode.getNumOfParams() == 0 && !functionNode.hasEval() && checkDirectArgumentsAccess(functionNode, currentFunction)) {
                currentFunction.setDirectArgumentsAccess(true);
            } else {
                currentFunction.declareVar(Environment.ARGUMENTS_NAME);
            }
        }

        // reserve this slot if function uses this or has a direct eval that might use this
        if ((functionNode.usesThis() || functionNode.hasEval()) &&
                        !(functionNode.getKind() == FunctionNode.Kind.ARROW && currentFunction.getNonArrowParentFunction().isDerivedConstructor())) {
            currentFunction.reserveThisSlot();
        }
        if (functionNode.hasDirectSuper()) {
            assert functionNode.getKind() != FunctionNode.Kind.ARROW;
            currentFunction.reserveThisSlot();
        }
        if (functionNode.usesSuper()) {
            // arrow functions need to access [[HomeObject]] from outer non-arrow scope
            // note: an arrow function using <super> also needs <this> access
            assert functionNode.getKind() != FunctionNode.Kind.ARROW;

            currentFunction.reserveThisSlot();
            currentFunction.reserveSuperSlot();
        }
        if (functionNode.usesNewTarget() || functionNode.hasDirectSuper()) {
            currentFunction.reserveNewTargetSlot();
        }

        if (functionNode.needsDynamicScope() && !currentFunction.isDirectEval()) {
            currentFunction.setIsDynamicallyScoped(true);
            currentFunction.reserveDynamicScopeSlot();
        }

        return Collections.emptyList();
    }

    static void functionVarDeclarationPass(FunctionNode rootFunctionNode, GraalJSParserOptions options) {
        com.oracle.js.parser.ir.visitor.NodeVisitor<LexicalContext> visitor = new com.oracle.js.parser.ir.visitor.NodeVisitor<LexicalContext>(new LexicalContext()) {
            @Override
            public boolean enterVarNode(VarNode varNode) {
                String varName = varNode.getName().getName();
                detectVarNameConflict(lc, varNode, options);
                if (varNode.isBlockScoped()) {
                    enterVarNodeBlockScope(varNode, varName);
                } else {
                    enterVarNodeDefault(varName);
                }
                return true;
            }

            private void enterVarNodeBlockScope(VarNode varNode, String varName) {
                Symbol symbol = new Symbol(varName, getBlockScopedSymbolFlags(varNode));
                lc.getCurrentBlock().putSymbol(lc, symbol);

                if (varNode.isFunctionDeclaration() && options.isAnnexB()) {
                    // B.3.3.1 Changes to FunctionDeclarationInstantiation
                    FunctionNode fn = lc.getCurrentFunction();
                    if (!fn.isStrict() && !varName.equals(Environment.ARGUMENTS_NAME) && fn.getBody().getExistingSymbol(varName) == null) {
                        if (!isVarAlreadyDeclaredLexically(lc, varName, options, true)) {
                            assert !lc.getCurrentBlock().isFunctionBody() && !lc.getCurrentBlock().isParameterBlock();
                            fn.getVarDeclarationBlock().putSymbol(lc, new Symbol(varName, Symbol.IS_VAR | Symbol.IS_VAR_DECLARED_HERE));
                        }
                    }
                }
            }

            private void enterVarNodeDefault(String varName) {
                Block currentBlock = lc.getCurrentBlock();
                Block bodyBlock = lc.getCurrentFunction().getVarDeclarationBlock();
                if (currentBlock.isParameterBlock()) {
                    // for duplicate checks record its declaration in the body block, too
                    assert currentBlock != bodyBlock;
                    bodyBlock.putSymbol(lc, new Symbol(varName, Symbol.IS_VAR));
                } else {
                    if (currentBlock != bodyBlock) {
                        // for duplicate checks record its declaration here
                        currentBlock.putSymbol(lc, new Symbol(varName, Symbol.IS_VAR));
                    }

                    Block parameterBlock = lc.getCurrentFunction().getBody();
                    if (!parameterBlock.isParameterBlock() || parameterBlock.getExistingSymbol(varName) == null) {
                        // declare in var declaration scope if not a parameter
                        bodyBlock.putSymbol(lc, new Symbol(varName, Symbol.IS_VAR | Symbol.IS_VAR_DECLARED_HERE));
                    } else {
                        assert parameterBlock != null && parameterBlock.getExistingSymbol(varName) != null;
                        // variable is already declared in parameter block
                        bodyBlock.putSymbol(lc, new Symbol(varName, Symbol.IS_VAR | Symbol.IS_VAR_DECLARED_HERE | Symbol.IS_VAR_REDECLARED_HERE));
                    }
                }
            }

            @Override
            public boolean enterFunctionNode(FunctionNode functionNode) {
                assert !functionNode.isAnalyzed() : functionNode;
                declareParameterSymbols(functionNode);
                functionNode.setAnalyzed(true);
                return true;
            }

            private void declareParameterSymbols(FunctionNode functionNode) {
                List<IdentNode> parameters = functionNode.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    IdentNode parameter = parameters.get(i);
                    Symbol symbol = new Symbol(parameter.getName(), Symbol.IS_PARAM);
                    functionNode.getBody().putSymbol(lc, symbol);
                }
            }
        };

        rootFunctionNode.accept(visitor);
    }

    private static void functionNeedsParentFramePass(FunctionNode rootFunctionNode) {
        if (!JSTruffleOptions.LazyTranslation) {
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

            private void findSymbol(String varName) {
                boolean local = true;
                FunctionNode lastFunction = null;
                for (Iterator<LexicalContextNode> iterator = lc.getAllNodes(); iterator.hasNext();) {
                    LexicalContextNode node = iterator.next();
                    if (node instanceof Block) {
                        Symbol foundSymbol = ((Block) node).getExistingSymbol(varName);
                        if (foundSymbol != null && !(foundSymbol.isGlobal() || foundSymbol.isImportBinding())) {
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
                        } else if (function.getKind() == FunctionNode.Kind.ARROW && isVarLexicallyScopedInArrowFunction(varName)) {
                            FunctionNode nonArrowFunction = lc.getCurrentNonArrowFunction();
                            // `this` is read from the arrow function object,
                            // unless `this` is supplied by a subclass constructor
                            if (!varName.equals(Environment.THIS_NAME) || nonArrowFunction.isSubclassConstructor()) {
                                if (!nonArrowFunction.isProgram()) {
                                    markUsesAncestorScopeUntil(nonArrowFunction, false);
                                }
                            }
                            break;
                        } else if (!function.isProgram() && varName.equals(Environment.ARGUMENTS_NAME)) {
                            assert function.getKind() != FunctionNode.Kind.ARROW;
                            assert local;
                            break;
                        } else if (function.hasEval() && !function.isProgram()) {
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

            private void markUsesAncestorScopeUntil(FunctionNode untilFunction, boolean inclusive) {
                for (final Iterator<FunctionNode> functions = lc.getFunctions(); functions.hasNext();) {
                    FunctionNode function = functions.next();
                    if (!inclusive && function == untilFunction) {
                        break;
                    }
                    function.setUsesAncestorScope(true);
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

    private static boolean checkDirectArgumentsAccess(FunctionNode functionNode, FunctionEnvironment currentFunction) {
        class DirectArgumentsAccessVisitor extends com.oracle.js.parser.ir.visitor.NodeVisitor<LexicalContext> {
            boolean directArgumentsAccess = true;

            DirectArgumentsAccessVisitor(LexicalContext lc) {
                super(lc);
            }

            private boolean isArguments(IdentNode identNode) {
                return !identNode.isPropertyName() && identNode.getName().equals(Environment.ARGUMENTS_NAME);
            }

            @Override
            public boolean enterIdentNode(IdentNode identNode) {
                if (JSTruffleOptions.OptimizeApplyArguments) {
                    if (isArguments(identNode) && functionNode.needsArguments() && !currentFunction.isDirectEval()) {
                        // function.apply(_, arguments);
                        LexicalContextNode callNode = lc.getAllNodes().next();
                        if (!(callNode instanceof CallNode && isApply((CallNode) callNode) && ((CallNode) callNode).getArgs().size() == 2 && ((CallNode) callNode).getArgs().get(1) == identNode)) {
                            directArgumentsAccess = false;
                        }
                    } else {
                        checkParameterUse(identNode);
                    }
                }
                return false;
            }

            private void checkParameterUse(IdentNode identNode) {
                if (directArgumentsAccess && !currentFunction.isStrictMode() && currentFunction.isParameter(identNode.getName())) {
                    directArgumentsAccess = false;
                }
            }

            @Override
            public boolean enterFunctionNode(FunctionNode nestedFunctionNode) {
                if (nestedFunctionNode == functionNode) {
                    return true;
                }
                if (JSTruffleOptions.OptimizeApplyArguments && (nestedFunctionNode.getKind() == FunctionNode.Kind.ARROW || !currentFunction.isStrictMode())) {
                    // 1. arrow functions have lexical `arguments` binding;
                    // direct arguments access to outer frames currently not supported
                    // 2. if not in strict mode, nested functions might access mapped parameters;
                    // since we don't look inside them, bail out
                    directArgumentsAccess = false;
                }
                return false;
            }
        }

        DirectArgumentsAccessVisitor visitor = new DirectArgumentsAccessVisitor(new LexicalContext());
        functionNode.accept(visitor);
        return visitor.directArgumentsAccess;
    }

    static boolean isApply(CallNode callNode) {
        return callNode.getFunction() instanceof AccessNode && ((AccessNode) callNode.getFunction()).getProperty().equals("apply");
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

    static void earlyVariableDeclarationPass(FunctionNode functionNode, GraalJSParserOptions options, boolean eval, boolean evalInGlobalScope) {
        assert !functionNode.isAnalyzed();
        if (functionNode.isModule()) {
            // we must resolve module imports first to know all imported bindings
            return;
        }
        // 1. strict eval code always has its own scope
        // 2. non-strict indirect eval is in global scope
        // 3. non-strict direct eval is in global scope if the caller is
        boolean globalScope = functionNode.isProgram() && (!eval || (evalInGlobalScope && !functionNode.isStrict()));
        if (globalScope) {
            globalVarPrePass(functionNode, options);
        } else {
            functionVarDeclarationPass(functionNode, options);
        }
        assert functionNode.isAnalyzed();
    }

    private static void globalVarPrePass(FunctionNode functionNode, GraalJSParserOptions options) {
        assert !functionNode.isAnalyzed();
        functionNode.accept(new com.oracle.js.parser.ir.visitor.NodeVisitor<LexicalContext>(new LexicalContext()) {
            @Override
            public boolean enterVarNode(VarNode varNode) {
                String varName = varNode.getName().getName();
                detectVarNameConflict(lc, varNode, options);
                Block currentBlock = lc.getCurrentBlock();
                if (varNode.isBlockScoped()) {
                    enterVarNodeBlockScope(varNode, varName, currentBlock);
                } else {
                    enterVarNodeDefault(varNode, varName, currentBlock);
                }
                return true;
            }

            private void enterVarNodeBlockScope(VarNode varNode, String varName, Block currentBlock) {
                Symbol symbol = new Symbol(varName, getBlockScopedSymbolFlags(varNode));
                currentBlock.putSymbol(lc, symbol);

                if (varNode.isFunctionDeclaration() && options.isAnnexB()) {
                    // B.3.3.2 Changes to GlobalDeclarationInstantiation
                    if (!functionNode.isStrict() && functionNode.getBody().getExistingSymbol(varName) == null) {
                        if (!isVarAlreadyDeclaredLexically(lc, varName, options, true)) {
                            functionNode.getBody().putSymbol(lc, new Symbol(varName, Symbol.IS_GLOBAL | Symbol.IS_FUNCTION_DECLARATION));
                        }
                    }
                }
            }

            private void enterVarNodeDefault(VarNode varNode, String varName, Block currentBlock) {
                int symbolKind = functionNode.isModule() ? Symbol.IS_VAR : Symbol.IS_GLOBAL;
                Block bodyBlock = functionNode.getBody();
                if (currentBlock != bodyBlock) {
                    // for duplicate checks record its declaration here
                    currentBlock.putSymbol(lc, new Symbol(varName, symbolKind));
                    // but actually declare it in function body scope
                }
                bodyBlock.putSymbol(lc, new Symbol(varName, symbolKind | Symbol.IS_VAR_DECLARED_HERE | (varNode.isHoistableDeclaration() ? Symbol.IS_FUNCTION_DECLARATION : 0)));
            }

            @Override
            public boolean enterFunctionNode(FunctionNode nestedFunctionNode) {
                if (nestedFunctionNode == functionNode) {
                    return true;
                }
                functionVarDeclarationPass(nestedFunctionNode, options);
                return false;
            }
        });

        assert !functionNode.usesSuper();
        assert !functionNode.hasDirectSuper();

        functionNode.setAnalyzed(true);
    }

    private List<JavaScriptNode> collectGlobalVars(FunctionNode functionNode, boolean configurable) {
        int symbolCount = functionNode.getBody().getSymbolCount();
        if (symbolCount == 0) {
            return Collections.emptyList();
        }
        final List<DeclareGlobalNode> declarations = new ArrayList<>(symbolCount);
        for (Symbol symbol : functionNode.getBody().getSymbols()) {
            if (symbol.isGlobal()) {
                if (symbol.isFunctionDeclaration()) {
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

    static void detectVarNameConflict(LexicalContext lexcon, VarNode varNode, GraalJSParserOptions options) {
        assert lexcon.getCurrentFunction() != null;
        boolean alreadyDeclared = false;
        String varName = varNode.getName().getName();
        if (varNode.isBlockScoped()) {
            Block currentBlock = lexcon.getCurrentBlock();
            if (currentBlock.getExistingSymbol(varName) != null) {
                alreadyDeclared = true;
            } else {
                Block parentBlock = lexcon.getParentBlock();
                if (parentBlock != null && (parentBlock.isCatchBlock() || parentBlock.isParameterBlock())) {
                    if (parentBlock.getExistingSymbol(varName) != null) {
                        alreadyDeclared = true;
                    }
                }
            }
        } else {
            alreadyDeclared = isVarAlreadyDeclaredLexically(lexcon, varName, options, false);
        }
        if (alreadyDeclared) {
            throw Errors.createSyntaxError(error("Variable \"" + varName + "\" has already been declared", varNode.getToken(), lexcon));
        }
    }

    private static boolean isVarAlreadyDeclaredLexically(LexicalContext lexcon, String varName, GraalJSParserOptions options, boolean skipCurrentBlock) {
        Iterator<Block> iterator = lexcon.getBlocks();
        if (skipCurrentBlock) {
            iterator.next();
        }
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Symbol existingSymbol = block.getExistingSymbol(varName);
            if (existingSymbol != null && existingSymbol.isBlockScoped()) {
                if (existingSymbol.isCatchParameter() && !lexcon.getCurrentBlock().isForOfBlock() && options.isAnnexB()) {
                    continue; // B.3.5 VariableStatements in Catch Blocks
                }
                return true;
            }
            if (block.isFunctionBody()) {
                break;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected List<JavaScriptNode> setupModuleEnvironment(FunctionNode functionNode) {
        throw new UnsupportedOperationException();
    }

    protected void createImportBinding(String localName, JSModuleRecord module, String bindingName) {
        currentFunction().addImportBinding(localName, module, bindingName);
    }

    @SuppressWarnings("unused")
    protected void verifyModuleLocalExports(Block moduleBodyBlock) {
        throw new UnsupportedOperationException();
    }

    private JavaScriptNode prepareArguments(JavaScriptNode body) {
        VarRef argumentsVar = environment.findLocalVar(Environment.ARGUMENTS_NAME);
        boolean unmappedArgumentsObject = currentFunction().isStrictMode() || !currentFunction().hasSimpleParameterList();
        JavaScriptNode argumentsObject = factory.createArgumentsObjectNode(context, unmappedArgumentsObject, currentFunction().getLeadingArgumentCount(), currentFunction().getTrailingArgumentCount());
        if (!unmappedArgumentsObject) {
            argumentsObject = environment.findArgumentsVar().createWriteNode(argumentsObject);
        }
        JavaScriptNode setArgumentsNode = argumentsVar.createWriteNode(argumentsObject);
        return factory.createExprBlock(setArgumentsNode, body);
    }

    private JavaScriptNode prepareThis(JavaScriptNode body, boolean isLexicalThis) {
        VarRef thisVar = environment.findThisVar();
        JavaScriptNode getThisNode = isLexicalThis ? factory.createAccessLexicalThis() : factory.createAccessThis();
        if (!environment.isStrictMode() && !isLexicalThis) {
            getThisNode = factory.createPrepareThisBinding(context, getThisNode);
        }
        JavaScriptNode setThisNode = thisVar.createWriteNode(getThisNode);
        return factory.createExprBlock(setThisNode, body);
    }

    private JavaScriptNode prepareSuper(JavaScriptNode body) {
        JavaScriptNode getHomeObject = factory.createProperty(context, factory.createAccessCallee(0), JSFunction.HOME_OBJECT_ID);
        JavaScriptNode setSuperNode = environment.findSuperVar().createWriteNode(getHomeObject);
        return factory.createExprBlock(setSuperNode, body);
    }

    private JavaScriptNode prepareNewTarget(JavaScriptNode body) {
        JavaScriptNode getNewTarget = factory.createAccessNewTarget();
        JavaScriptNode setNewTarget = environment.findNewTargetVar().createWriteNode(getNewTarget);
        return factory.createExprBlock(setNewTarget, body);
    }

    @Override
    public JavaScriptNode enterReturnNode(com.oracle.js.parser.ir.ReturnNode returnNode) {
        JavaScriptNode expression = returnNode.getExpression() != null ? transform(returnNode.getExpression()) : factory.createConstantUndefined();
        if (currentFunction().isAsyncGeneratorFunction()) {
            expression = createAwaitNode(expression);
        }

        if (returnNode.isInTerminalPosition()) {
            return tagStatement(expression, returnNode);
        }

        return tagStatement(createReturnNode(expression), returnNode);
    }

    private ReturnNode createReturnNode(JavaScriptNode expression) {
        FunctionEnvironment currentFunction = currentFunction();
        currentFunction.addReturn();
        if (JSTruffleOptions.ReturnValueInFrame) {
            JavaScriptNode writeReturnSlotNode = environment.findTempVar(currentFunction.getReturnSlot()).createWriteNode(expression);
            return factory.createFrameReturn(writeReturnSlotNode);
        } else {
            return factory.createReturn(expression);
        }
    }

    @Override
    public JavaScriptNode enterBlock(Block block) {
        JavaScriptNode result;
        try (EnvironmentCloseable blockEnv = enterBlockEnvironment(block)) {
            List<Statement> blockStatements = block.getStatements();
            List<JavaScriptNode> scopeInit = createTemporalDeadZoneInit(block);
            JavaScriptNode blockNode = transformStatements(blockStatements, block.isTerminal(), scopeInit, block.isExpressionBlock() || block.isParameterBlock());
            if (block.isFunctionBody() && currentFunction().isCallerContextEval()) {
                blockNode = prependDynamicScopeBindingInit(block, blockNode);
            }
            result = blockEnv.wrapBlockScope(blockNode);
        }
        // Parameter initialization must precede (i.e. wrap) the (async) generator function body
        if (block.isFunctionBody() && currentFunction().isGeneratorFunction()) {
            result = finishGeneratorBody(result);
        }
        ensureHasSourceSection(result, block);
        return result;
    }

    /**
     * Initialize block-scoped symbols with a <i>dead</i> marker value.
     */
    private List<JavaScriptNode> createTemporalDeadZoneInit(Block block) {
        if (!block.hasBlockScopedOrRedeclaredSymbols() || environment instanceof GlobalEnvironment) {
            return Collections.emptyList();
        }

        ArrayList<JavaScriptNode> blockWithInit = new ArrayList<>(block.getSymbolCount() + 1);
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
                assert environment.getScopeLevel() == 1;
                JavaScriptNode outerVar = factory.createLocal(environment.getParent().findLocalVar(symbol.getName()).getFrameSlot(), 0, 1, environment.getParentSlots());
                blockWithInit.add(findScopeVar(symbol.getName(), true).createWriteNode(outerVar));
            }
        }
        return blockWithInit;
    }

    /**
     * Create var-declared dynamic scope bindings in the variable environment of the caller.
     */
    private JavaScriptNode prependDynamicScopeBindingInit(Block block, JavaScriptNode blockNode) {
        assert currentFunction().isCallerContextEval();
        ArrayList<JavaScriptNode> blockWithInit = new ArrayList<>();
        for (Symbol symbol : block.getSymbols()) {
            if (symbol.isVarDeclaredHere() && !environment.getVariableEnvironment().hasLocalVar(symbol.getName())) {
                blockWithInit.add(createDynamicScopeBinding(symbol.getName(), true));
            }
        }
        if (blockWithInit.isEmpty()) {
            return blockNode;
        }
        blockWithInit.add(blockNode);
        return factory.createExprBlock(blockWithInit.toArray(EMPTY_NODE_ARRAY));
    }

    private JavaScriptNode createDynamicScopeBinding(String varName, boolean deleteable) {
        assert deleteable;
        VarRef dynamicScopeVar = environment.findDynamicScopeVar();
        return new DeclareEvalVariableNode(context, varName, dynamicScopeVar.createReadNode(), (WriteNode) dynamicScopeVar.createWriteNode(null));
    }

    private JavaScriptNode transformStatements(List<Statement> blockStatements, boolean terminal) {
        return transformStatements(blockStatements, terminal, Collections.emptyList(), false);
    }

    private JavaScriptNode transformStatements(List<Statement> blockStatements, boolean terminal, List<JavaScriptNode> prolog, boolean expressionBlock) {
        final int size = prolog.size() + blockStatements.size();
        JavaScriptNode[] statements = javaScriptNodeArray(size);
        int pos = 0;
        if (!prolog.isEmpty()) {
            for (; pos < prolog.size(); pos++) {
                statements[pos] = prolog.get(pos);
            }
        }
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

        assert pos == size;
        JavaScriptNode blockNode = createBlock(statements, terminal, expressionBlock);
        return blockNode;
    }

    private EnvironmentCloseable enterBlockEnvironment(Block block) {
        // Global lexical environment is shared by scripts (but not eval).
        if (block.isFunctionBody() && lc.getCurrentFunction().getKind() == FunctionNode.Kind.SCRIPT && !currentFunction().isEval()) {
            GlobalEnvironment globalEnv = new GlobalEnvironment(environment, factory, context);
            setupGlobalEnvironment(globalEnv, block);
            return new EnvironmentCloseable(globalEnv);
        }

        if (block.hasDeclarations() || JSTruffleOptions.ManyBlockScopes) {
            /*
             * The function environment is filled with top-level vars from the function body, unless
             * the function has parameter expressions, then the function body gets a separate scope
             * and we populate the env with parameter vars (cf. FunctionDeclarationInstantiation).
             */
            if (block.isParameterBlock() || (block.isFunctionBody() && block == lc.getCurrentFunction().getBody())) {
                assert environment instanceof FunctionEnvironment;
                boolean onlyBlockScoped = currentFunction().isCallerContextEval();
                environment.addFrameSlotsFromSymbols(block.getSymbols(), onlyBlockScoped);
                return new EnvironmentCloseable(environment);
            } else {
                BlockEnvironment blockEnv = new BlockEnvironment(environment, factory, context);
                blockEnv.addFrameSlotsFromSymbols(block.getSymbols());
                return new EnvironmentCloseable(blockEnv);
            }
        } else {
            return new EnvironmentCloseable(environment);
        }
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
            } else if (symbol.isGlobal() && symbol.isVarDeclaredHere()) {
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
            return tagExpression(enterLiteralArrayNode((LiteralNode.ArrayLiteralNode) literalNode), literalNode);
        } else {
            return tagExpression(enterLiteralDefaultNode(literalNode), literalNode);
        }
    }

    private JavaScriptNode enterLiteralDefaultNode(LiteralNode<?> literalNode) {
        Object value = literalNode.getValue();
        if (value == null) {
            return factory.createConstantNull();
        } else if (value instanceof Long) { // we don't support long type
            return factory.createConstantDouble(((Long) value).doubleValue());
        } else if (value instanceof Lexer.RegexToken) {
            return factory.createRegExpLiteral(context, ((Lexer.RegexToken) value).getExpression(), ((Lexer.RegexToken) value).getOptions());
        }
        return factory.createConstant(value);
    }

    private JavaScriptNode enterLiteralArrayNode(LiteralNode.ArrayLiteralNode arrayLiteralNode) {
        List<Expression> elementExpressions = arrayLiteralNode.getElementExpressions();
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
            result = environment.findNewTargetVar().createReadNode();
        } else {
            String varName = identNode.getName();
            VarRef varRef = findScopeVarCheckTDZ(varName, false);
            result = varRef.createReadNode();
        }
        return tagExpression(result, identNode);
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
            JavaScriptNode activeFunction = factory.createAccessCallee(currentFunction().getArrowFunctionLevel());
            JavaScriptNode superConstructor = factory.createGetPrototype(activeFunction);
            JavaScriptNode receiver = environment.findThisVar().createReadNode();
            return factory.createTargetableWrapper(superConstructor, receiver);
        }
    }

    private JavaScriptNode createThisNode() {
        return !currentFunction().isGlobal() ? checkThisBindingInitialized(environment.findThisVar().createReadNode()) : factory.createAccessThis();
    }

    private JavaScriptNode checkThisBindingInitialized(JavaScriptNode accessThisNode) {
        // TODO in most cases we should be able to prove that `this` is already initialized
        if (currentFunction().getNonArrowParentFunction().isDerivedConstructor()) {
            return factory.createDerivedConstructorThis(accessThisNode);
        }
        return accessThisNode;
    }

    private Symbol findBlockScopedSymbolInFunction(String varName) {
        for (Iterator<LexicalContextNode> iterator = lc.getAllNodes(); iterator.hasNext();) {
            LexicalContextNode node = iterator.next();
            if (node instanceof Block) {
                Symbol existingSymbol = ((Block) node).getExistingSymbol(varName);
                if (existingSymbol != null) {
                    if (existingSymbol.isBlockScoped()) {
                        return existingSymbol;
                    } else {
                        break;
                    }
                }
            } else if (node instanceof FunctionNode) {
                break;
            }
        }
        return null;
    }

    private VarRef findScopeVar(String name, boolean skipWith) {
        return environment.findVar(name, skipWith);
    }

    private VarRef findScopeVarCheckTDZ(String name, boolean initializationAssignment) {
        VarRef varRef = findScopeVar(name, false);
        if (varRef.isFunctionLocal()) {
            Symbol symbol = findBlockScopedSymbolInFunction(varRef.getName());
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
                return environment.new VarRef(name) {
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
            symbol = lc.getCurrentBlock().getExistingSymbol(varName);
            assert symbol != null : varName;
        }

        try {
            if (varNode.isAssignment()) {
                return createVarAssignNode(varNode, varName);
            } else if (varNode.isBlockScoped() && (!varNode.isDestructuring() || lc.inUnprotectedSwitchContext()) && !symbol.hasBeenDeclared()) {
                return findScopeVar(varName, false).createWriteNode(factory.createConstantUndefined());
            }
            return factory.createEmpty();
        } finally {
            if (varNode.isBlockScoped()) {
                if (lc.inUnprotectedSwitchContext()) {
                    // mark as declared in switch block; always needs dynamic TDZ check
                    symbol.setDeclaredInSwitchBlock();
                } else if (!varNode.isDestructuring()) {
                    symbol.setHasBeenDeclared();
                }
            }
        }
    }

    private JavaScriptNode createVarAssignNode(VarNode varNode, String varName) {
        JavaScriptNode rhs = transform(varNode.getAssignmentSource());
        setAnonymousFunctionName(rhs, varName);
        JavaScriptNode assignment = findScopeVar(varName, false).createWriteNode(rhs);
        tagExpression(assignment, varNode);
        if (varNode.isBlockScoped() && varNode.isFunctionDeclaration() && context.isOptionAnnexB()) {
            // B.3.3 Block-Level Function Declarations Web Legacy Compatibility Semantics
            FunctionNode fn = lc.getCurrentFunction();
            if (!fn.isStrict() && !varName.equals(Environment.ARGUMENTS_NAME)) {
                Symbol symbol = fn.getBody().getExistingSymbol(varName);
                if (symbol != null && (symbol.isVar() || symbol.isGlobal())) {
                    if (!isVarAlreadyDeclaredLexically(lc, varName, (GraalJSParserOptions) context.getParserOptions(), true)) {
                        assignment = environment.findVar(varName, true, false, true, false).withRequired(false).createWriteNode(assignment);
                        tagExpression(assignment, varNode);
                    }
                }
            }
        }

        // do not halt on function declarations
        if (!varNode.isHoistableDeclaration()) {
            tagStatement(assignment, varNode);
        }

        return discardResult(assignment);
    }

    /**
     * Set the name of anonymous functions to the supplied name.
     */
    private void setAnonymousFunctionName(JavaScriptNode rhs, String name) {
        if (context.getEcmaScriptVersion() < 6) {
            return;
        }
        if (rhs instanceof FunctionNameHolder) {
            FunctionNameHolder functionNameHolder = (FunctionNameHolder) rhs;
            if (functionNameHolder.isAnonymous()) {
                functionNameHolder.setFunctionName(name);
            }
        } else if (rhs instanceof JSOrNode.NotUndefinedOrNode && ((JSOrNode.NotUndefinedOrNode) rhs).getRight() instanceof FunctionNameHolder) {
            // used in destructuring assignment
            setAnonymousFunctionName(((JSOrNode.NotUndefinedOrNode) rhs).getRight(), name);
        }
    }

    @Override
    public JavaScriptNode enterWhileNode(com.oracle.js.parser.ir.WhileNode whileNode) {
        JavaScriptNode test = transform(whileNode.getTest());
        tagStatement(test, whileNode.getTest());
        try (JumpTargetCloseable<ContinueTarget> target = currentFunction().pushContinueTarget(null)) {
            JavaScriptNode body = transform(whileNode.getBody());
            JavaScriptNode wrappedBody = target.wrapContinueTargetNode(body);
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
                JavaScriptNode wrappedBody = target.wrapContinueTargetNode(body);
                result = target.wrapBreakTargetNode(desugarFor(forNode, init, test, modify, wrappedBody));
            }

            return wrapClearAndGetCompletionValue(result);
        }
    }

    private JavaScriptNode desugarFor(ForNode forNode, JavaScriptNode init, JavaScriptNode test, JavaScriptNode modify, JavaScriptNode wrappedBody) {
        if (forNode.hasPerIterationScope()) {
            VarRef firstTempVar = environment.createTempVar();
            FrameDescriptor iterationBlockFrameDescriptor = environment.getBlockFrameDescriptor();
            StatementNode newFor = factory.createFor(test, wrappedBody, modify, iterationBlockFrameDescriptor, firstTempVar.createReadNode(),
                            firstTempVar.createWriteNode(factory.createConstantBoolean(false)));
            ensureHasSourceSection(newFor, forNode);
            return createBlock(init, firstTempVar.createWriteNode(factory.createConstantBoolean(true)), newFor);
        }
        JavaScriptNode whileDo = createWhileDo(test, createBlock(wrappedBody, modify));
        return createBlock(init, ensureHasSourceSection(whileDo, forNode));
    }

    private JavaScriptNode desugarForIn(ForNode forNode, JavaScriptNode modify, JumpTargetCloseable<ContinueTarget> jumpTarget) {
        JavaScriptNode createIteratorNode;
        if (forNode.isForEach()) {
            createIteratorNode = factory.createEnumerate(context, modify, true);
        } else {
            assert forNode.isForIn() && !forNode.isForEach() && !forNode.isForOf();
            createIteratorNode = factory.createEnumerate(context, modify, false);
        }
        return desugarForInOrOfBody(forNode, createIteratorNode, jumpTarget);
    }

    private JavaScriptNode desugarForOf(ForNode forNode, JavaScriptNode modify, JumpTargetCloseable<ContinueTarget> jumpTarget) {
        assert forNode.isForOf();
        JavaScriptNode getIterator = factory.createGetIterator(context, modify);
        return desugarForInOrOfBody(forNode, getIterator, jumpTarget);
    }

    private JavaScriptNode desugarForInOrOfBody(ForNode forNode, JavaScriptNode iterator, JumpTargetCloseable<ContinueTarget> jumpTarget) {
        VarRef iteratorVar = environment.createTempVar();
        JavaScriptNode iteratorInit = iteratorVar.createWriteNode(iterator);
        VarRef nextResultVar = environment.createTempVar();
        VarRef doneVar = environment.createTempVar();
        JavaScriptNode iteratorNext = factory.createIteratorNext(context, iteratorVar.createReadNode());
        // nextResult = IteratorNext(iterator)
        // while(!(done = IteratorComplete(nextResult)))
        JavaScriptNode condition = factory.createUnary(UnaryOperation.NOT, doneVar.createWriteNode(factory.createIteratorComplete(context, nextResultVar.createWriteNode(iteratorNext))));
        JavaScriptNode wrappedBody;
        try (EnvironmentCloseable blockEnv = forNode.hasPerIterationScope() ? enterBlockEnvironment(lc.getCurrentBlock()) : new EnvironmentCloseable(environment)) {
            // var nextValue = IteratorValue(nextResult);
            VarRef nextResultVar2 = environment.findTempVar(nextResultVar.getFrameSlot());
            JavaScriptNode nextResult = nextResultVar2.createReadNode();
            JavaScriptNode nextValue = factory.createIteratorValue(context, nextResult);
            JavaScriptNode writeNext = tagStatement(desugarForHeadAssignment(forNode, nextValue), forNode);
            JavaScriptNode body = transform(forNode.getBody());
            wrappedBody = blockEnv.wrapBlockScope(createBlock(writeNext, body));
        }
        wrappedBody = jumpTarget.wrapContinueTargetNode(wrappedBody);
        JavaScriptNode whileNode = createWhileDo(condition, wrappedBody);
        JavaScriptNode wrappedWhile = factory.createIteratorCloseIfNotDone(context, jumpTarget.wrapBreakTargetNode(whileNode), iteratorVar.createReadNode(), doneVar.createReadNode());
        JavaScriptNode resetIterator = iteratorVar.createWriteNode(factory.createConstant(JSFrameUtil.DEFAULT_VALUE));
        wrappedWhile = factory.createTryFinally(wrappedWhile, resetIterator);
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
        VarRef doneVar = environment.createTempVar();

        currentFunction().addAwait();
        JSReadFrameSlotNode asyncResultNode = (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getAsyncResultSlot()).createReadNode();
        JSReadFrameSlotNode asyncContextNode = (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getAsyncContextSlot()).createReadNode();
        JavaScriptNode iteratorNext = factory.createAsyncIteratorNext(context, iteratorVar.createReadNode(), asyncContextNode, asyncResultNode);
        // nextResult = Await(IteratorNext(iterator))
        // while(!(done = IteratorComplete(nextResult)))
        JavaScriptNode condition = factory.createUnary(UnaryOperation.NOT, doneVar.createWriteNode(factory.createIteratorComplete(context, nextResultVar.createWriteNode(iteratorNext))));
        JavaScriptNode wrappedBody;
        try (EnvironmentCloseable blockEnv = forNode.hasPerIterationScope() ? enterBlockEnvironment(lc.getCurrentBlock()) : new EnvironmentCloseable(environment)) {
            // var nextValue = IteratorValue(nextResult);
            VarRef nextResultVar2 = environment.findTempVar(nextResultVar.getFrameSlot());
            JavaScriptNode nextResult = nextResultVar2.createReadNode();
            JavaScriptNode nextValue = factory.createIteratorValue(context, nextResult);
            JavaScriptNode writeNext = tagStatement(desugarForHeadAssignment(forNode, nextValue), forNode);
            JavaScriptNode body = transform(forNode.getBody());
            wrappedBody = blockEnv.wrapBlockScope(createBlock(writeNext, body));
        }
        wrappedBody = jumpTarget.wrapContinueTargetNode(wrappedBody);
        JavaScriptNode whileNode = createWhileDo(condition, wrappedBody);
        currentFunction().addAwait();
        JavaScriptNode wrappedWhile = factory.createAsyncIteratorCloseWrapper(context, jumpTarget.wrapBreakTargetNode(whileNode), iteratorVar.createReadNode(), asyncContextNode, asyncResultNode,
                        doneVar.createReadNode());
        JavaScriptNode resetIterator = iteratorVar.createWriteNode(factory.createConstant(JSFrameUtil.DEFAULT_VALUE));
        wrappedWhile = factory.createTryFinally(wrappedWhile, resetIterator);
        return createBlock(iteratorInit, wrappedWhile);
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
            JSWriteFrameSlotNode writeYieldResultNode = JSTruffleOptions.YieldResultInFrame ? (JSWriteFrameSlotNode) environment.findTempVar(currentFunction.getYieldResultSlot()).createWriteNode(null)
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
            } else if (!identNode.isThis() && !identNode.isNewTarget()) {
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
        JavaScriptNode operand = transform(unaryNode.getExpression());

        if (JSTruffleOptions.LocalVarIncDecNode && isLocalVariableOperand(operand)) {
            FrameSlot frameSlot = ((FrameSlotNode) operand).getFrameSlot();
            if (JSFrameUtil.isConst(frameSlot)) {
                // we know this is going to throw. do the read and throw TypeError.
                return checkMutableBinding(operand, frameSlot.getIdentifier());
            }
            return tagExpression(createUnaryIncDecLocalNode(unaryNode, operand), unaryNode);
        } else {
            BinaryOperation operation = unaryNode.tokenType() == TokenType.INCPREFIX || unaryNode.tokenType() == TokenType.INCPOSTFIX ? BinaryOperation.ADD : BinaryOperation.SUBTRACT;
            boolean isPostfix = unaryNode.tokenType() == TokenType.INCPOSTFIX || unaryNode.tokenType() == TokenType.DECPOSTFIX;
            JavaScriptNode constNumericUnit = ensureHasSourceSection(factory.createConstantNumericUnit(), unaryNode);
            return tagExpression(transformAssignment(unaryNode, unaryNode.getExpression(), constNumericUnit, operation, isPostfix, true), unaryNode);
        }
    }

    private static boolean isLocalVariableOperand(JavaScriptNode operand) {
        return operand instanceof JSReadFrameSlotNode;
    }

    private JavaScriptNode createUnaryIncDecLocalNode(UnaryNode unaryNode, JavaScriptNode operand) {
        switch (unaryNode.tokenType()) {
            case INCPREFIX:
                return factory.createUnary(UnaryOperation.PREFIX_LOCAL_INCREMENT, operand);
            case INCPOSTFIX:
                return factory.createUnary(UnaryOperation.POSTFIX_LOCAL_INCREMENT, operand);
            case DECPREFIX:
                return factory.createUnary(UnaryOperation.PREFIX_LOCAL_DECREMENT, operand);
            case DECPOSTFIX:
                return factory.createUnary(UnaryOperation.POSTFIX_LOCAL_DECREMENT, operand);
            default:
                throw Errors.shouldNotReachHere();
        }
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
            case DECPOSTFIX:
            case INCPREFIX:
            case INCPOSTFIX:
            case NEW:
            case DELETE:
            default:
                throw new UnsupportedOperationException(tokenType.toString());
        }
    }

    private JavaScriptNode enterDelete(UnaryNode unaryNode) {
        Expression rhs = unaryNode.getExpression();
        JavaScriptNode result;
        if (rhs instanceof AccessNode) {
            result = enterDeleteAccess(rhs);
        } else if (rhs instanceof IndexNode) {
            result = enterDeleteIndex(rhs);
        } else if (rhs instanceof IdentNode) {
            result = enterDeleteIdent(rhs);
        } else {
            // deleting variables is (thankfully) not supported, so always true
            result = factory.createConstantBoolean(true);
        }
        return tagExpression(result, unaryNode);
    }

    private JavaScriptNode enterDeleteIdent(Expression rhs) {
        String varName = ((IdentNode) rhs).getName();
        VarRef varRef = findScopeVar(varName, varName.equals(Environment.THIS_NAME));
        return varRef.createDeleteNode();
    }

    private JavaScriptNode enterDeleteIndex(Expression rhs) {
        IndexNode indexNode = (IndexNode) rhs;
        JavaScriptNode target = transform(indexNode.getBase());
        JavaScriptNode element = transform(indexNode.getIndex());
        return factory.createDeleteProperty(target, element, environment.isStrictMode(), context);
    }

    private JavaScriptNode enterDeleteAccess(Expression rhs) {
        AccessNode accessNode = (AccessNode) rhs;
        JavaScriptNode target = transform(accessNode.getBase());
        return factory.createDeleteProperty(target, factory.createConstantString(accessNode.getProperty()), environment.isStrictMode(), context);
    }

    private JavaScriptNode[] transformArgs(List<Expression> argList) {
        JavaScriptNode[] args = javaScriptNodeArray(argList.size());
        for (int i = 0; i < argList.size(); i++) {
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
        JavaScriptNode call;
        if (callNode.getFunction() instanceof IdentNode && args.length >= 1 && ((IdentNode) callNode.getFunction()).getName().equals(JSGlobalObject.EVAL_NAME) &&
                        !(args[0] instanceof GetTemplateObjectNode)) {
            call = createCallEvalNode(function, args);
        } else if (currentFunction().isDirectArgumentsAccess() && isCallApplyArguments(callNode)) {
            call = createCallApplyArgumentsNode(function, args);
        } else {
            if (callNode.getFunction() instanceof IdentNode && ((IdentNode) callNode.getFunction()).isDirectSuper()) {
                args = insertNewTargetArg(args);
                call = initializeThis(factory.createFunctionCallWithNewTarget(context, function, args));
            } else {
                call = createCallDefaultNode(function, args);
            }
        }
        return tagExpression(tagCall(call), callNode);
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
        return factory.createIf(factory.createDual(context, tempVar.createWriteNode(thisValueNode), uninitialized), thisVar.createWriteNode(tempVar.createReadNode()),
                        factory.createThrowError(JSErrorType.ReferenceError, "super() called twice"));
    }

    private JavaScriptNode createCallDefaultNode(JavaScriptNode function, JavaScriptNode[] args) {
        return factory.createFunctionCall(context, function, args);
    }

    private JavaScriptNode createCallEvalNode(JavaScriptNode function, JavaScriptNode[] args) {
        if (!currentFunction().isGlobal() && !currentFunction().isStrictMode() && !currentFunction().isDirectEval()) {
            assert environment.function().isDynamicallyScoped();
        }
        for (FunctionEnvironment func = currentFunction(); func.getParentFunction() != null; func = func.getParentFunction()) {
            func.setNeedsParentFrame(true);
        }
        JavaScriptNode[] otherArgs = EMPTY_NODE_ARRAY;
        if (args.length - 1 > 0) {
            otherArgs = Arrays.copyOfRange(args, 1, args.length);
        }
        return EvalNode.create(context, environment, args[0], otherArgs, function, createThisNode());
    }

    private JavaScriptNode createCallApplyArgumentsNode(JavaScriptNode function, JavaScriptNode[] args) {
        return factory.createCallApplyArguments(context, (JSFunctionCallNode) createCallDefaultNode(function, args));
    }

    private static boolean isCallApplyArguments(CallNode callNode) {
        return isApply(callNode) && callNode.getArgs().size() == 2 && callNode.getArgs().get(1) instanceof IdentNode &&
                        ((IdentNode) callNode.getArgs().get(1)).getName().equals(Environment.ARGUMENTS_NAME);
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
        JavaScriptNode lhs = transform(binaryNode.lhs());
        JavaScriptNode rhs = transform(binaryNode.rhs());
        return tagExpression(factory.createBinary(context, tokenTypeToBinaryOperation(binaryNode.tokenType()), lhs, rhs), binaryNode);
    }

    private JavaScriptNode enterBinaryTransformNode(BinaryNode binaryNode) {
        JavaScriptNode assignedValue = transform(binaryNode.getAssignmentSource());
        return tagExpression(transformAssignment(binaryNode, binaryNode.getAssignmentDest(), assignedValue, tokenTypeToBinaryOperation(binaryNode.tokenType()), false, false), binaryNode);
    }

    private JavaScriptNode enterBinaryAssignNode(BinaryNode binaryNode) {
        Expression assignmentDest = binaryNode.getAssignmentDest();
        JavaScriptNode assignedValue = transform(binaryNode.getAssignmentSource());
        JavaScriptNode assignment = transformAssignment(binaryNode, assignmentDest, assignedValue, binaryNode.isTokenType(TokenType.ASSIGN_INIT));
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
            case AND:
                return BinaryOperation.LOGICAL_AND;
            case OR:
                return BinaryOperation.LOGICAL_OR;
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

    private JavaScriptNode transformAssignment(Expression assignmentExpression, Expression lhsExpression, JavaScriptNode assignedValue,
                    BinaryOperation shortcutOperation, boolean returnOldValue, boolean convertLHSToNumeric) {
        return transformAssignmentImpl(assignmentExpression, lhsExpression, assignedValue, false, shortcutOperation, returnOldValue, convertLHSToNumeric);
    }

    private JavaScriptNode transformAssignmentImpl(Expression assignmentExpression, Expression lhsExpression, JavaScriptNode assignedValue, boolean initializationAssignment,
                    BinaryOperation shortcutOperation, boolean returnOldValue, boolean convertLHSToNumeric) {
        assert shortcutOperation != null || (!returnOldValue && !convertLHSToNumeric) : "returnOldValue / convertLHSToNumeric can only be used with shortcut assignments";
        JavaScriptNode assignedNode = null;
        JavaScriptNode prev = null;
        VarRef resultTemp = (shortcutOperation != null && returnOldValue) ? environment.createTempVar() : null;
        TokenType tokenType = lhsExpression.tokenType();
        if (tokenType != TokenType.IDENT && lhsExpression instanceof IdentNode) {
            tokenType = TokenType.IDENT;
        }
        switch (tokenType) {
            case IDENT: {
                setAnonymousFunctionName(assignedValue, ((IdentNode) lhsExpression).getName());
                assignedNode = transformAssignmentIdent((IdentNode) lhsExpression, assignedValue, shortcutOperation, returnOldValue, convertLHSToNumeric, resultTemp, initializationAssignment);
                break;
            }
            case LBRACKET: {
                // target[element]
                IndexNode indexNode = (IndexNode) lhsExpression;
                JavaScriptNode target = transform(indexNode.getBase());
                JavaScriptNode elem = transform(indexNode.getIndex());

                JavaScriptNode rhs;

                if (shortcutOperation != null) {
                    if (!(target instanceof RepeatableNode)) {
                        VarRef newTemp = environment.createTempVar();
                        prev = newTemp.createWriteNode(target);
                        target = newTemp.createReadNode();
                    }
                    if (!(elem instanceof RepeatableNode)) {
                        VarRef newTemp = environment.createTempVar();
                        JavaScriptNode assignment = newTemp.createWriteNode(elem);
                        if (prev == null) {
                            prev = assignment;
                        } else {
                            prev = factory.createDual(context, prev, assignment);
                        }
                        elem = newTemp.createReadNode();
                    }

                    // index must be ToPropertyKey-converted only once, save it in temp var
                    VarRef keyTemp = environment.createTempVar();

                    JavaScriptNode shortcutNode = tagExpression(factory.createReadElementNode(context, target, keyTemp.createReadNode()), lhsExpression);

                    // RequireObjectCoercible(target); safely repeatable, no temp var needed
                    target = factory.createToObject(context, factory.copy(target));
                    // perform the key type conversion in WriteElementNode (evaluated first)
                    elem = keyTemp.createWriteNode(factory.createToArrayIndex(elem));

                    if (convertLHSToNumeric) {
                        shortcutNode = factory.numericConversion(shortcutNode);
                    }

                    if (returnOldValue) {
                        shortcutNode = resultTemp.createWriteNode(shortcutNode);
                    }

                    rhs = tagExpression(factory.createBinary(context, shortcutOperation, shortcutNode, assignedValue), lhsExpression);
                } else {
                    rhs = assignedValue;
                }

                assignedNode = factory.createWriteElementNode(target, elem, rhs, context, environment.isStrictMode());
                break;
            }
            case PERIOD: {
                // target.property
                AccessNode accessNode = (AccessNode) lhsExpression;
                JavaScriptNode target = transform(accessNode.getBase());

                JavaScriptNode rhs;

                if (shortcutOperation != null) {
                    if (!(target instanceof RepeatableNode)) {
                        VarRef newTemp = environment.createTempVar();
                        prev = newTemp.createWriteNode(target);
                        target = newTemp.createReadNode();
                    }

                    JavaScriptNode shortcutNode = tagExpression(factory.createReadProperty(context, target, accessNode.getProperty()), accessNode);

                    target = factory.copy(target);

                    if (convertLHSToNumeric) {
                        shortcutNode = factory.numericConversion(shortcutNode);
                    }

                    if (returnOldValue) {
                        shortcutNode = resultTemp.createWriteNode(shortcutNode);
                    }

                    rhs = tagExpression(factory.createBinary(context, shortcutOperation, shortcutNode, assignedValue), accessNode);
                } else {
                    rhs = assignedValue;
                }

                assignedNode = factory.createWriteProperty(target, accessNode.getProperty(), rhs, context, environment.isStrictMode());
                break;
            }
            case ARRAY: {
                assert shortcutOperation == null;
                LiteralNode.ArrayLiteralNode arrayLiteralNode = (LiteralNode.ArrayLiteralNode) lhsExpression;
                List<Expression> elementExpressions = arrayLiteralNode.getElementExpressions();
                JavaScriptNode[] initElements = javaScriptNodeArray(elementExpressions.size());
                VarRef iteratorTempVar = environment.createTempVar();
                VarRef valueTempVar = environment.createTempVar();
                VarRef doneVar = environment.createTempVar();
                JavaScriptNode initValue = valueTempVar.createWriteNode(assignedValue);
                // By default, we use the hint to track the type of iterator.
                JavaScriptNode getIterator = factory.createGetIterator(context, initValue);
                JavaScriptNode initIteratorTempVar = iteratorTempVar.createWriteNode(getIterator);
                JavaScriptNode writeDone = doneVar.createWriteNode(factory.createConstantBoolean(true));

                for (int i = 0; i < elementExpressions.size(); i++) {
                    Expression element = elementExpressions.get(i);
                    Expression lhsExpr;
                    Expression init = null;
                    if (element instanceof IdentNode) {
                        lhsExpr = element;
                    } else if (element instanceof BinaryNode) {
                        assert element.isTokenType(TokenType.ASSIGN) || element.isTokenType(TokenType.ASSIGN_INIT);
                        lhsExpr = ((BinaryNode) element).lhs();
                        init = ((BinaryNode) element).rhs();
                    } else {
                        lhsExpr = element;
                    }
                    JavaScriptNode rhsNode = factory.createIteratorStepSpecial(context, iteratorTempVar.createReadNode(), factory.createExprBlock(writeDone, factory.createConstantUndefined()), true);
                    if (init != null) {
                        rhsNode = factory.createNotUndefinedOr(rhsNode, transform(init));
                    }
                    if (lhsExpr != null && lhsExpr.isTokenType(TokenType.SPREAD_ARRAY)) {
                        rhsNode = factory.createIteratorToArray(context, iteratorTempVar.createReadNode(), writeDone);
                        lhsExpr = ((UnaryNode) lhsExpr).getExpression();
                    }
                    if (lhsExpr != null) {
                        initElements[i] = transformAssignment(lhsExpr, lhsExpr, rhsNode, initializationAssignment);
                    } else {
                        initElements[i] = rhsNode;
                    }
                }
                JavaScriptNode closeIfNotDone = factory.createIteratorCloseIfNotDone(context, createBlock(initElements), iteratorTempVar.createReadNode(), doneVar.createReadNode());
                assignedNode = factory.createExprBlock(initIteratorTempVar, closeIfNotDone, valueTempVar.createReadNode());
                break;
            }
            case LBRACE: {
                assert shortcutOperation == null;
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
                        lhsExpr = ((BinaryNode) property.getValue()).lhs();
                        init = ((BinaryNode) property.getValue()).rhs();
                    } else if (property.isRest()) {
                        assert hasRest;
                        lhsExpr = ((UnaryNode) property.getKey()).getExpression();
                    } else {
                        lhsExpr = property.getValue();
                    }
                    JavaScriptNode rhsNode;
                    if (property.isRest()) {
                        JavaScriptNode restObj = factory.createObjectLiteral(context, new ArrayList<>());
                        JavaScriptNode excludedItemsArray = excludedKeys.length == 0 ? null : factory.createArrayLiteral(context, excludedKeys);
                        rhsNode = factory.createCopyDataProperties(restObj, valueTempVar.createReadNode(), excludedItemsArray);
                    } else if (property.getKey() instanceof IdentNode && !property.isComputed()) {
                        String keyName = property.getKeyName();
                        if (hasRest) {
                            excludedKeys[i] = factory.createConstantString(keyName);
                        }
                        rhsNode = factory.createReadProperty(context, valueTempVar.createReadNode(), keyName);
                    } else {
                        JavaScriptNode key = transform(property.getKey());
                        if (hasRest) {
                            VarRef keyTempVar = environment.createTempVar();
                            excludedKeys[i] = keyTempVar.createReadNode();
                            key = keyTempVar.createWriteNode(factory.createToPropertyKey(key));
                        }
                        rhsNode = factory.createReadElementNode(context, valueTempVar.createReadNode(), key);
                    }
                    if (init != null) {
                        rhsNode = factory.createNotUndefinedOr(rhsNode, transform(init));
                    }
                    initElements[i] = transformAssignment(lhsExpr, lhsExpr, rhsNode, initializationAssignment);
                }
                assignedNode = factory.createExprBlock(initValueTempVar, createBlock(initElements), valueTempVar.createReadNode());
                break;
            }
            default:
                throwUnsupportedAssignmentError(lhsExpression);
        }

        if (shortcutOperation != null) {
            ensureHasSourceSection(assignedNode, assignmentExpression);
        }
        if (prev != null) {
            assignedNode = factory.createDual(context, prev, assignedNode);
        }
        if (resultTemp != null) {
            assignedNode = factory.createDual(context, assignedNode, resultTemp.createReadNode());
        }
        return tagExpression(assignedNode, assignmentExpression);
    }

    private static void throwUnsupportedAssignmentError(Expression lhsExpression) {
        throw Errors.createReferenceError("unsupported assignment to token type: " + lhsExpression.tokenType().toString() + " " + lhsExpression.toString());
    }

    private JavaScriptNode transformAssignmentIdent(IdentNode identNode, JavaScriptNode assignedValue, BinaryOperation shortcutOperation, boolean returnOldValue, boolean convertLHSToNumeric,
                    VarRef resultTemp, boolean initializationAssignment) {
        JavaScriptNode rhs;
        String ident = identNode.getName();
        VarRef scopeVar = findScopeVarCheckTDZ(ident, initializationAssignment);

        if (shortcutOperation != null) {
            JavaScriptNode operand = tagExpression(scopeVar.createReadNode(), identNode);
            JavaScriptNode shortcutNode = convertLHSToNumeric ? factory.numericConversion(operand) : operand;

            if (returnOldValue) {
                shortcutNode = resultTemp.createWriteNode(shortcutNode);
            }
            rhs = tagExpression(factory.createBinary(context, shortcutOperation, shortcutNode, assignedValue), identNode);
        } else {
            rhs = assignedValue;
        }

        if (shortcutOperation != null && scopeVar.isGlobal()) {
            // e.g.: lhs *= rhs => lhs = lhs * rhs
            // If lhs is a side-effecting getter that deletes lhs, we must not throw
            // ReferenceError at the lhs assignment since the lhs reference is already resolved.
            return scopeVar.withRequired(false).createWriteNode(rhs);
        } else {
            // if scopeVar is const, the assignment will never succeed and is only there to perform
            // the temporal dead zone check and throw a ReferenceError instead of a TypeError
            if (!initializationAssignment && scopeVar.isConst()) {
                if (JSTruffleOptions.V8LegacyConst && !environment.isStrictMode()) {
                    // Note that there is no TDZ check for const in this mode either.
                    return rhs;
                }
                rhs = checkMutableBinding(rhs, scopeVar.getName());
            }
            return scopeVar.createWriteNode(rhs);
        }
    }

    /**
     * If this is an attempt to change the value of an immutable binding, throw a runtime TypeError.
     */
    private JavaScriptNode checkMutableBinding(JavaScriptNode rhsNode, Object identifier) {
        if (JSTruffleOptions.V8LegacyConst && !environment.isStrictMode()) {
            return rhsNode;
        }
        // evaluate rhs and throw TypeError
        String message = context.isOptionV8CompatibilityMode() ? "Assignment to constant variable." : "Assignment to constant \"" + identifier + "\"";
        JavaScriptNode throwTypeError = factory.createThrowError(JSErrorType.TypeError, message);
        return isPotentiallySideEffecting(rhsNode) ? createBlock(rhsNode, throwTypeError) : throwTypeError;
    }

    @Override
    public JavaScriptNode enterAccessNode(AccessNode accessNode) {
        String propertyName = accessNode.getProperty();
        JavaScriptNode base = transform(accessNode.getBase());
        return tagExpression(factory.createReadProperty(context, base, propertyName), accessNode);
    }

    @Override
    public JavaScriptNode enterIndexNode(IndexNode indexNode) {
        JavaScriptNode base = transform(indexNode.getBase());
        JavaScriptNode index = transform(indexNode.getIndex());
        return tagExpression(factory.createReadElementNode(context, base, index), indexNode);
    }

    @Override
    public JavaScriptNode enterObjectNode(ObjectNode objectNode) {
        ArrayList<ObjectLiteralMemberNode> members = transformPropertyDefinitionList(objectNode.getElements(), false);
        return tagExpression(factory.createObjectLiteral(context, members), objectNode);
    }

    private ArrayList<ObjectLiteralMemberNode> transformPropertyDefinitionList(List<PropertyNode> properties, boolean isClass) {
        ArrayList<ObjectLiteralMemberNode> members = new ArrayList<>(properties.size());
        for (int i = 0; i < properties.size(); i++) {
            PropertyNode property = properties.get(i);
            String keyName = property.getKeyName();

            final ObjectLiteralMemberNode member;
            if (property.getValue() != null) {
                member = enterObjectPropertyNode(property, keyName, isClass);
            } else if (property.isRest()) {
                member = factory.createSpreadObjectMember(property.isStatic(), transform(((UnaryNode) property.getKey()).getExpression()));
            } else {
                member = enterObjectAccessorNode(property, keyName, isClass);
            }
            members.add(member);
        }
        return members;
    }

    private ObjectLiteralMemberNode enterObjectAccessorNode(PropertyNode property, String keyName, boolean isClass) {
        JavaScriptNode getter = getAccessor(property.getGetter());
        JavaScriptNode setter = getAccessor(property.getSetter());
        boolean enumerable = !isClass;
        if (property.isComputed()) {
            return factory.createComputedAccessorMember(transform(property.getKey()), property.isStatic(), enumerable, getter, setter);
        } else {
            return factory.createAccessorMember(keyName, property.isStatic(), enumerable, getter, setter);
        }
    }

    private JavaScriptNode getAccessor(FunctionNode accessorFunction) {
        if (accessorFunction == null) {
            return null;
        }
        JavaScriptNode function = transform(accessorFunction);
        if (accessorFunction.usesSuper()) {
            assert accessorFunction.isMethod();
            function = factory.createMakeMethod(context, function);
        }
        return function;
    }

    private ObjectLiteralMemberNode enterObjectPropertyNode(PropertyNode property, String keyName, boolean isClass) {
        JavaScriptNode value = transform(property.getValue());
        if (property.getValue() instanceof FunctionNode && ((FunctionNode) property.getValue()).usesSuper()) {
            assert ((FunctionNode) property.getValue()).isMethod();
            value = factory.createMakeMethod(context, value);
        }
        boolean enumerable = !isClass;
        if (property.isComputed()) {
            return factory.createComputedDataMember(transform(property.getKey()), property.isStatic(), enumerable, value);
        } else if (!isClass && property.isProto()) {
            return factory.createProtoMember(keyName, property.isStatic(), value);
        } else {
            setAnonymousFunctionName(value, keyName);
            return factory.createDataMember(keyName, property.isStatic(), enumerable, value);
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
            result = wrapClearAndGetCompletionValue(factory.createTryFinally(result, wrapSaveAndRestoreCompletionValue(finallyBlock)));
        }
        return result;
    }

    @Override
    public JavaScriptNode enterThrowNode(com.oracle.js.parser.ir.ThrowNode throwNode) {
        return tagStatement(factory.createThrow(transform(throwNode.getExpression())), throwNode);
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
            if (JSTruffleOptions.OptimizeNoFallthroughSwitch && isNoFallthroughSwitch(switchNode)) {
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
            curNode = dropTerminalDirectBreakStatement(transformStatements(defaultCase.getStatements(), false));
            ensureHasSourceSection(curNode, defaultCase);
        }

        for (int i = cases.size() - 1; i >= 0; i--) {
            CaseNode caseNode = cases.get(i);
            if (caseNode.getTest() != null) { // default case is already last in the cascade
                JavaScriptNode readSwitchVarNode = switchVar.createReadNode();
                JavaScriptNode test = createSwitchCaseExpr(isSwitchTypeofString, caseNode, readSwitchVarNode);
                if (caseNode.getStatements().isEmpty()) {
                    if (curNode instanceof com.oracle.truffle.js.nodes.control.IfNode) {
                        // if (condition) => if (test || condition)
                        com.oracle.truffle.js.nodes.control.IfNode prevIfNode = (com.oracle.truffle.js.nodes.control.IfNode) curNode;
                        curNode = factory.copyIfWithCondition(prevIfNode, factory.createLogicalOr(test, prevIfNode.getCondition()));
                    } else {
                        // fall through to default case, execute test only for potential side effect
                        if (isPotentiallySideEffecting(test)) {
                            curNode = curNode == null ? discardResult(test) : createBlock(test, curNode);
                        }
                    }
                } else {
                    JavaScriptNode pass = dropTerminalDirectBreakStatement(transformStatements(caseNode.getStatements(), false));
                    ensureHasSourceSection(pass, caseNode);
                    curNode = factory.createIf(test, pass, curNode);
                }
                ensureHasSourceSection(curNode, caseNode);
            }
        }
        return curNode == null ? factory.createEmpty() : curNode;
    }

    private static boolean isPotentiallySideEffecting(JavaScriptNode test) {
        // (GR-2041) improve detection
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
            Statement lastStatement = statements.get(statements.size() - 1);
            if (!isExceptionalControlFlowNode(lastStatement)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isExceptionalControlFlowNode(com.oracle.js.parser.ir.Node node) {
        return node instanceof com.oracle.js.parser.ir.BreakNode || node instanceof com.oracle.js.parser.ir.ReturnNode ||
                        node instanceof com.oracle.js.parser.ir.ContinueNode || node instanceof com.oracle.js.parser.ir.ThrowNode;
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
        JavaScriptNode withExpression = transform(withNode.getExpression());
        JavaScriptNode toObject = factory.createToObjectFromWith(context, withExpression, true);
        String withVarName = makeUniqueTempVarNameForStatement(withNode);
        environment.declareLocalVar(withVarName);
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
    public JavaScriptNode enterRuntimeNode(RuntimeNode runtimeNode) {
        if (runtimeNode.getRequest() == RuntimeNode.Request.REFERENCE_ERROR) {
            String msg = com.oracle.js.parser.ECMAErrors.getMessage("parser.error.invalid.lvalue");
            return factory.createThrowError(JSErrorType.ReferenceError, error(msg, runtimeNode.getToken(), lc));
        } else if (runtimeNode.getRequest() == RuntimeNode.Request.GET_TEMPLATE_OBJECT) {
            JavaScriptNode rawStrings = transform(runtimeNode.getArgs().get(0));
            JavaScriptNode cookedStrings = transform(runtimeNode.getArgs().get(1));
            return tagExpression(factory.createTemplateObject(context, rawStrings, cookedStrings), runtimeNode);
        } else if (runtimeNode.getRequest() == RuntimeNode.Request.TO_STRING) {
            JavaScriptNode value = transform(runtimeNode.getArgs().get(0));
            return tagExpression(factory.createToString(value), runtimeNode);
        }
        throw new UnsupportedOperationException(runtimeNode.toString());
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
        JavaScriptNode classHeritage = transform(classNode.getClassHeritage());
        JavaScriptNode classFunction = transform(classNode.getConstructor().getValue());

        String className = null;
        if (classNode.getIdent() != null) {
            className = classNode.getIdent().getName();
            lc.getCurrentBlock().getExistingSymbol(className).setHasBeenDeclared();
        }

        ArrayList<ObjectLiteralMemberNode> members = transformPropertyDefinitionList(classNode.getClassElements(), true);

        JavaScriptNode classDefinition = factory.createClassDefinition(context, (JSFunctionExpressionNode) classFunction, classHeritage,
                        members.toArray(new ObjectLiteralMemberNode[members.size()]), className);

        return tagExpression(classDefinition, classNode);
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
        int start = functionNode.getStart();
        int finish = functionNode.getFinish();
        return source.createSection(start, finish - start);
    }

    private JavaScriptNode ensureHasSourceSection(JavaScriptNode resultNode, com.oracle.js.parser.ir.Node parseNode) {
        if (!resultNode.hasSourceSection()) {
            assignSourceSection(resultNode, parseNode);
            if (resultNode instanceof GlobalScopeVarWrapperNode) {
                ensureHasSourceSection(((GlobalScopeVarWrapperNode) resultNode).getDelegateNode(), parseNode);
            }
        }
        return resultNode;
    }

    private void assignSourceSection(JavaScriptNode resultNode, com.oracle.js.parser.ir.Node parseNode) {
        resultNode.setSourceSection(source, parseNode.getStart(), parseNode.getFinish() - parseNode.getStart());
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
                    return factory.createBlockScope(blockEnv.getBlockFrameDescriptor(), blockEnv.getParentSlot(), block);
                }
            }
            return block;
        }

        @Override
        public void close() {
            assert environment == newEnv;
            assert prevEnv == newEnv.getParent() || prevEnv == newEnv || prevEnv instanceof EvalEnvironment;
            assert newEnv == prevEnv || !(newEnv instanceof BlockEnvironment) || wrappedInBlockScopeNode == 1;
            environment = prevEnv;
        }
    }
}

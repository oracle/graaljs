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
package com.oracle.truffle.js.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.oracle.js.parser.Lexer;
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
import com.oracle.js.parser.ir.ClassElement;
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
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ImportEntry;
import com.oracle.js.parser.ir.Module.ImportPhase;
import com.oracle.js.parser.ir.Module.ModuleRequest;
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
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.decorators.DecoratorListEvaluationNode;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSFrameDescriptor;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.NodeFactory.BinaryOperation;
import com.oracle.truffle.js.nodes.NodeFactory.UnaryOperation;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.RepeatableNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.DeclareEvalVariableNode;
import com.oracle.truffle.js.nodes.access.DeclareGlobalNode;
import com.oracle.truffle.js.nodes.access.GetIteratorUnaryNode;
import com.oracle.truffle.js.nodes.access.GlobalPropertyNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.ObjectLiteralMemberNode;
import com.oracle.truffle.js.nodes.access.OptionalChainNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.access.SuperPropertyReferenceNode;
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
import com.oracle.truffle.js.nodes.control.GeneratorNode;
import com.oracle.truffle.js.nodes.control.GeneratorWrapperNode;
import com.oracle.truffle.js.nodes.control.ModuleYieldNode;
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
import com.oracle.truffle.js.parser.env.PrivateEnvironment;
import com.oracle.truffle.js.parser.env.WithEnvironment;
import com.oracle.truffle.js.parser.internal.ir.debug.PrintVisitor;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.InternalSlotId;
import com.oracle.truffle.js.runtime.util.Pair;

@SuppressWarnings("try")
abstract class GraalJSTranslator extends com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor<LexicalContext, JavaScriptNode> {

    public static final TruffleString SUPER_CALLED_TWICE = Strings.constant("super() called twice");
    public static final TruffleString UNSUPPORTED_REFERENCE_TO_SUPER = Strings.constant("Unsupported reference to 'super'");
    public static final String LINE__ = "__LINE__";
    public static final String FILE__ = "__FILE__";
    public static final String DIR__ = "__DIR__";
    public static final String IMPORT = "import";
    public static final String IMPORT_META = "import.meta";
    public static final String ARGUMENTS = "arguments";

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
    protected final List<String> argumentNames;
    protected final int sourceLength;
    protected final int prologLength;
    protected final ScriptOrModule activeScriptOrModule;
    private final boolean isParentStrict;

    protected GraalJSTranslator(LexicalContext lc, NodeFactory factory, JSContext context, Source source, List<String> argumentNames, int prologLength, Environment environment,
                    boolean isParentStrict, ScriptOrModule scriptOrModule) {
        super(lc);
        this.context = context;
        this.environment = environment;
        this.factory = factory;
        this.source = source;
        this.argumentNames = argumentNames;
        this.isParentStrict = isParentStrict;
        this.sourceLength = source.getCharacters().length();
        this.prologLength = prologLength;
        this.activeScriptOrModule = scriptOrModule;
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
            if (resultNode instanceof BlockScopeNode) {
                JavaScriptNode blockNode = ((BlockScopeNode) resultNode).getBlock();
                blockNode.addRootBodyTag();
                ensureHasSourceSection(blockNode, parseNode);
            } else {
                resultNode.addRootBodyTag();
            }
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

        boolean lazyTranslation = context.getLanguageOptions().lazyTranslation() && functionMode && !functionNode.isProgram() && !inDirectEval;

        TruffleString functionName = getFunctionName(functionNode);
        JSFunctionData functionData;
        FunctionRootNode functionRoot;
        JSFrameSlot blockScopeSlot;
        if (lazyTranslation) {
            assert functionMode && !functionNode.isProgram() && !functionNode.isModule();

            // function needs parent frame analysis has already been done
            boolean needsParentFrame = functionNode.usesAncestorScope();
            blockScopeSlot = needsParentFrame && environment != null ? environment.getCurrentBlockScopeSlot() : null;

            functionData = factory.createFunctionData(context, functionNode.getLength(), functionName, isConstructor, isDerivedConstructor, isStrict, isBuiltin,
                            needsParentFrame, isGeneratorFunction, isAsyncFunction, isClassConstructor, strictFunctionProperties, needsNewTarget);

            LexicalContext savedLC = lc.copy();
            Environment parentEnv = environment;
            functionData.setLazyInit(new JSFunctionData.Initializer() {
                @Override
                public void initializeRoot(JSFunctionData fd) {
                    synchronized (this) {
                        if (fd.getRootNode() == null) {
                            GraalJSTranslator translator = newTranslator(parentEnv, savedLC);
                            translator.translateFunctionOnDemand(functionNode, fd, isStrict, isGlobal, needsParentFrame, functionName, hasSyntheticArguments);
                            fd.releaseLazyInit();
                        }
                    }
                }
            });
            functionRoot = null;
        } else {
            Environment prevEnv = environment;
            try (EnvironmentCloseable functionEnv = enterFunctionEnvironment(functionNode, isStrict, isGlobal, hasSyntheticArguments)) {
                FunctionEnvironment currentFunction = currentFunction();
                currentFunction.setFunctionName(functionName);
                currentFunction.setInternalFunctionName(functionNode.getInternalNameTS());
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

                JavaScriptNode body = translateFunctionBody(functionNode, declarations);

                needsParentFrame = currentFunction.needsParentFrame();
                blockScopeSlot = needsParentFrame && prevEnv != null ? prevEnv.getCurrentBlockScopeSlot() : null;

                functionData = factory.createFunctionData(context, functionNode.getLength(), functionName, isConstructor, isDerivedConstructor, isStrict, isBuiltin,
                                needsParentFrame, isGeneratorFunction, isAsyncFunction, isClassConstructor, strictFunctionProperties, needsNewTarget);

                if (functionNode.isModule()) {
                    functionRoot = createModuleRoot(functionNode, functionData, currentFunction, body);
                } else {
                    functionRoot = createFunctionRoot(functionNode, functionData, currentFunction, body);
                }

                // Freeze after root creation to allow registration of async/generator variables.
                currentFunction.freeze();

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

    JavaScriptNode translateFunctionBody(FunctionNode functionNode, List<JavaScriptNode> declarations) {
        JavaScriptNode body = transform(functionNode.getBody());

        if (functionNode.isAsync() && !functionNode.isGenerator()) {
            ensureHasSourceSection(body, functionNode);
            body = handleAsyncFunctionBody(body);
            ensureHasSourceSection(body, functionNode);
        }

        if (!declarations.isEmpty()) {
            body = prepareDeclarations(declarations, body);
        }

        JSFrameDescriptor fd = currentFunction().getFunctionFrameDescriptor();
        List<JSFrameSlot> slotsWithTDZ = new ArrayList<>(fd.getSize());
        for (JSFrameSlot slot : fd.getSlots()) {
            if (JSFrameUtil.hasTemporalDeadZone(slot)) {
                slotsWithTDZ.add(slot);
            }
        }
        if (!slotsWithTDZ.isEmpty()) {
            int[] slots = new int[slotsWithTDZ.size()];
            for (int i = 0; i < slotsWithTDZ.size(); i++) {
                slots[i] = slotsWithTDZ.get(i).getIndex();
            }
            body = factory.createExprBlock(factory.createClearFrameSlots(factory.createScopeFrame(0, 0, null), slots, 0, slots.length), body);
        }

        return body;
    }

    private FunctionRootNode translateFunctionOnDemand(FunctionNode functionNode, JSFunctionData functionData, boolean isStrict,
                    boolean isGlobal, boolean needsParentFrame, TruffleString functionName, boolean hasSyntheticArguments) {
        try (EnvironmentCloseable functionEnv = enterFunctionEnvironment(functionNode, isStrict, isGlobal, hasSyntheticArguments)) {
            FunctionEnvironment currentFunction = currentFunction();
            currentFunction.setFunctionName(functionName);
            currentFunction.setInternalFunctionName(functionNode.getInternalNameTS());
            currentFunction.setNamedFunctionExpression(functionNode.isNamedFunctionExpression());

            currentFunction.setNeedsParentFrame(needsParentFrame);

            declareParameters(functionNode);
            functionEnvInit(functionNode);

            JavaScriptNode body = translateFunctionBody(functionNode, Collections.emptyList());

            currentFunction.freeze();
            assert currentFunction.isDeepFrozen();

            return createFunctionRoot(functionNode, functionData, currentFunction, body);
        }
    }

    private FunctionRootNode createFunctionRoot(FunctionNode functionNode, JSFunctionData functionData, FunctionEnvironment currentFunction, JavaScriptNode body) {
        SourceSection functionSourceSection = createSourceSection(functionNode);
        FunctionBodyNode functionBody = factory.createFunctionBody(body);
        FunctionRootNode functionRoot = factory.createFunctionRootNode(functionBody, environment.getFunctionFrameDescriptor().toFrameDescriptor(), functionData, functionSourceSection,
                        activeScriptOrModule, currentFunction.getInternalFunctionName());

        if (JSConfig.PrintAst) {
            printAST(functionRoot);
        }
        return functionRoot;
    }

    /**
     * Creates one or two module root nodes out of a module body. Every module has exactly one yield
     * statement separating the linking and evaluation parts, so we try to split it up into two root
     * nodes, eliminating the yield and the need to suspend and resume (except for top-level await).
     *
     * @see #splitModuleBodyAtYield
     */
    private FunctionRootNode createModuleRoot(FunctionNode functionNode, JSFunctionData functionData, FunctionEnvironment currentFunction, JavaScriptNode body) {
        if (JSConfig.PrintAst) {
            printAST(body);
        }

        SourceSection moduleSourceSection = createSourceSection(functionNode);
        TruffleString internalFunctionName = currentFunction.getInternalFunctionName();
        JavaScriptNode[] statements = null;
        if (body instanceof SequenceNode) {
            statements = ((SequenceNode) body).getStatements();
        } else if (isModuleYieldStatement(body)) {
            statements = new JavaScriptNode[]{body};
        }
        if (JSConfig.SplitModuleRoot && statements != null) {
            for (int i = 0; i < statements.length; i++) {
                JavaScriptNode statement = statements[i];
                if (isModuleYieldStatement(statement)) {
                    // Split the module into two call targets:
                    // 1. InitializeEnvironment()
                    // 2. ExecuteModule() / ExecuteAsyncModule()
                    JavaScriptNode[] linkHalf = Arrays.copyOfRange(statements, 0, i);
                    JavaScriptNode[] evalHalf = Arrays.copyOfRange(statements, i + 1, statements.length);
                    JavaScriptNode linkBlock = tagBody(factory.createModuleInitializeEnvironment(factory.createVoidBlock(linkHalf)), functionNode);
                    JavaScriptNode evalBlock = handleModuleBody(factory.createExprBlock(evalHalf));
                    FunctionBodyNode linkBody = factory.createFunctionBody(linkBlock);
                    FunctionBodyNode evalBody = factory.createFunctionBody(evalBlock);
                    return factory.createModuleRootNode(linkBody, evalBody, environment.getFunctionFrameDescriptor().toFrameDescriptor(), functionData,
                                    moduleSourceSection, activeScriptOrModule, internalFunctionName);
                }
            }
        }

        // Fall back to single call target using generator yield logic.
        currentFunction.addYield(); // yield has not been added yet.
        FunctionBodyNode generatorBody = factory.createFunctionBody(handleModuleBody(body));
        return factory.createModuleRootNode(generatorBody, generatorBody, environment.getFunctionFrameDescriptor().toFrameDescriptor(), functionData,
                        moduleSourceSection, activeScriptOrModule, internalFunctionName);
    }

    private static void printAST(Node functionRoot) {
        NodeUtil.printCompactTree(System.out, functionRoot);
    }

    private static void printParse(FunctionNode functionNode) {
        System.out.printf(new PrintVisitor(functionNode).toString());
    }

    private JavaScriptNode finishDerivedConstructorBody(FunctionNode function, JavaScriptNode body) {
        JavaScriptNode getThisBinding = (function.hasDirectSuper() || function.hasEval() || function.hasArrowEval()) ? environment.findThisVar().createReadNode() : factory.createConstantUndefined();
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
        return factory.createAsyncFunctionBody(context, instrumentedBody, writeContextNode, readContextNode, writeResultNode,
                        createSourceSection(lc.getCurrentFunction()), currentFunction().getExplicitOrInternalFunctionName(), activeScriptOrModule);
    }

    /**
     * Generator function parse-time AST modifications.
     *
     * @return instrumented function body
     */
    private JavaScriptNode finishGeneratorBody(JavaScriptNode body) {
        // Note: parameter initialization must precede (i.e. wrap) the (async) generator body
        assert lc.getCurrentBlock().isFunctionBody();
        assert !currentFunction().isModule();
        if (currentFunction().isAsyncGeneratorFunction()) {
            return handleAsyncGeneratorBody(body);
        } else {
            return handleGeneratorBody(body);
        }
    }

    private JavaScriptNode handleGeneratorBody(JavaScriptNode body) {
        assert currentFunction().isGeneratorFunction() && !currentFunction().isAsyncGeneratorFunction() && !currentFunction().isModule();
        JavaScriptNode instrumentedBody = instrumentSuspendNodes(body);
        VarRef yieldVar = environment.findYieldValueVar();
        JSWriteFrameSlotNode writeYieldValueNode = (JSWriteFrameSlotNode) yieldVar.createWriteNode(null);
        JSReadFrameSlotNode readYieldResultNode = JSConfig.YieldResultInFrame ? (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getYieldResultSlot()).createReadNode() : null;
        return factory.createGeneratorBody(context, instrumentedBody, writeYieldValueNode, readYieldResultNode,
                        createSourceSection(lc.getCurrentFunction()), currentFunction().getExplicitOrInternalFunctionName(), activeScriptOrModule);
    }

    private JavaScriptNode handleAsyncGeneratorBody(JavaScriptNode body) {
        assert currentFunction().isAsyncGeneratorFunction() && !currentFunction().isModule();
        VarRef asyncContextVar = environment.findAsyncContextVar();
        JavaScriptNode instrumentedBody = instrumentSuspendNodes(body);
        VarRef yieldVar = environment.findAsyncResultVar();
        JSWriteFrameSlotNode writeAsyncContextNode = (JSWriteFrameSlotNode) asyncContextVar.createWriteNode(null);
        JSReadFrameSlotNode readAsyncContextNode = (JSReadFrameSlotNode) asyncContextVar.createReadNode();
        JSWriteFrameSlotNode writeYieldValueNode = (JSWriteFrameSlotNode) yieldVar.createWriteNode(null);
        JSReadFrameSlotNode readYieldResultNode = JSConfig.YieldResultInFrame ? (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getYieldResultSlot()).createReadNode() : null;
        return factory.createAsyncGeneratorBody(context, instrumentedBody, writeYieldValueNode, readYieldResultNode, writeAsyncContextNode, readAsyncContextNode,
                        createSourceSection(lc.getCurrentFunction()), currentFunction().getExplicitOrInternalFunctionName(), activeScriptOrModule);
    }

    private JavaScriptNode handleModuleBody(JavaScriptNode body) {
        assert currentFunction().isModule();
        if (currentFunction().isAsyncGeneratorFunction()) {
            VarRef asyncContextVar = environment.findAsyncContextVar();
            JavaScriptNode instrumentedBody = instrumentSuspendNodes(body);
            VarRef yieldVar = environment.findAsyncResultVar();
            JSWriteFrameSlotNode writeAsyncContextNode = (JSWriteFrameSlotNode) asyncContextVar.createWriteNode(null);
            JSWriteFrameSlotNode writeYieldValueNode = (JSWriteFrameSlotNode) yieldVar.createWriteNode(null);
            return factory.createTopLevelAsyncModuleBody(context, instrumentedBody, writeYieldValueNode, writeAsyncContextNode,
                            createSourceSection(lc.getCurrentFunction()), activeScriptOrModule);
        } else {
            JavaScriptNode instrumentedBody = instrumentSuspendNodes(body);
            return factory.createModuleBody(instrumentedBody);
        }
    }

    /**
     * Instrument code paths leading to yield and await expressions.
     */
    private JavaScriptNode instrumentSuspendNodes(JavaScriptNode body) {
        if (!currentFunction().hasYield() && !currentFunction().hasAwait()) {
            return body;
        }
        JavaScriptNode newBody = (JavaScriptNode) instrumentSuspendHelper(body, null);
        if (newBody == null) {
            // No suspendable children found. They could have been eliminated
            // as a dead code during translation (for example, 'while (false) yield').
            return body;
        } else {
            return newBody;
        }
    }

    private Node instrumentSuspendHelper(Node parent, Node grandparent) {
        boolean hasSuspendChild = false;
        BitSet suspendableIndices = null;
        if (parent instanceof AbstractBlockNode) {
            AbstractBlockNode blockNode = ((AbstractBlockNode) parent);
            Node[] statements = blockNode.getStatements();
            for (int i = 0; i < statements.length; i++) {
                Node oldChild = statements[i];
                Node newChild = instrumentSuspendHelper(oldChild, parent);
                if (newChild != null) {
                    hasSuspendChild = true;
                    if (newChild != oldChild) {
                        factory.fixBlockNodeChild(blockNode, i, (JavaScriptNode) newChild);
                    }
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
                    if (child != newChild) {
                        factory.fixNodeChild(parent, child, newChild);
                    }
                    assert !(child instanceof ResumableNode) || newChild instanceof GeneratorNode || newChild instanceof SuspendNode : "resumable node not wrapped: " + child;
                }
            }
        }
        if (parent instanceof SuspendNode) {
            return parent;
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
        assert !(resumableNode instanceof SuspendNode) : resumableNode;
        JSFrameSlot stateSlot = addGeneratorStateSlot(environment.getFunctionFrameDescriptor(), ((ResumableNode) resumableNode).getStateSlotKind());
        return factory.createGeneratorWrapper((JavaScriptNode) resumableNode, stateSlot);
    }

    private JavaScriptNode toGeneratorBlockNode(AbstractBlockNode blockNode, BitSet suspendableIndices) {
        JSFrameDescriptor functionFrameDesc = environment.getFunctionFrameDescriptor();
        JavaScriptNode[] statements = blockNode.getStatements();
        boolean returnsResult = !blockNode.isResultAlwaysOfType(Undefined.class);
        JavaScriptNode genBlock;
        // we can resume at index 0 (start state) and every statement that contains a yield
        int resumePoints = suspendableIndices.cardinality() + (suspendableIndices.get(0) ? 0 : 1);
        if (resumePoints == statements.length) {
            // all statements are resume points
            JSFrameSlot stateSlot = addGeneratorStateSlot(functionFrameDesc, FrameSlotKind.Int);
            if (returnsResult) {
                genBlock = factory.createGeneratorExprBlock(statements, stateSlot);
            } else {
                genBlock = factory.createGeneratorVoidBlock(statements, stateSlot);
            }
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
            JSFrameSlot stateSlot = addGeneratorStateSlot(functionFrameDesc, FrameSlotKind.Int);
            if (returnsResult) {
                genBlock = factory.createGeneratorExprBlock(chunks, stateSlot);
            } else {
                genBlock = factory.createGeneratorVoidBlock(chunks, stateSlot);
            }
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
                JSFrameDescriptor functionFrameDescriptor = environment.getFunctionFrameDescriptor();
                InternalSlotId identifier = factory.createInternalSlotId("generatorexpr", functionFrameDescriptor.getSize());
                JSFrameSlot frameSlot = functionFrameDescriptor.addFrameSlot(identifier);
                JavaScriptNode readState = factory.createReadCurrentFrameSlot(frameSlot);
                if (jschild.hasTag(StandardTags.ExpressionTag.class) ||
                                (jschild instanceof GeneratorWrapperNode && ((GeneratorWrapperNode) jschild).getResumableNode().hasTag(StandardTags.ExpressionTag.class))) {
                    tagHiddenExpression(readState);
                }
                JavaScriptNode writeState = factory.createWriteCurrentFrameSlot(frameSlot, jschild);
                extracted.add(writeState);
                // replace child with saved expression result
                factory.fixNodeChild(parent, child, readState);
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
        FunctionEnvironment currentFunction = currentFunction();
        assert (currentFunction.isGlobal() || currentFunction.isEval() || currentFunction.hasSyntheticArguments()) == (functionNode.isScript() || functionNode.isModule());
        if (currentFunction.hasReturn()) {
            if (JSConfig.ReturnValueInFrame) {
                return factory.createFrameReturnTarget(body, factory.createReadCurrentFrameSlot(currentFunction.getReturnSlot()));
            } else {
                return factory.createReturnTarget(body);
            }
        } else if (currentFunction.returnsLastStatementResult()) {
            if (currentFunction.hasReturnSlot()) {
                return wrapGetCompletionValue(body);
            } else {
                return discardResult(body);
            }
        }
        return body;
    }

    private EnvironmentCloseable enterFunctionEnvironment(FunctionNode function, boolean isStrict, boolean isGlobal, boolean hasSyntheticArguments) {
        Scope scope = function.getBody().getScope();
        Environment functionEnv;
        if (environment instanceof EvalEnvironment) {
            assert !function.isArrow() && !function.isGenerator() && !function.isDerivedConstructor() && !function.isAsync();
            functionEnv = new FunctionEnvironment(environment.getParent(), factory, context, scope, isStrict,
                            true, ((EvalEnvironment) environment).isDirectEval(), false, false, false, false, isGlobal, hasSyntheticArguments);
        } else if (environment instanceof DebugEnvironment) {
            assert !function.isArrow() && !function.isGenerator() && !function.isDerivedConstructor() && !function.isAsync();
            functionEnv = new FunctionEnvironment(environment, factory, context, scope, isStrict, true, true, false, false, false, false, isGlobal, hasSyntheticArguments);
        } else {
            functionEnv = new FunctionEnvironment(environment, factory, context, scope, isStrict, false, false,
                            function.isArrow(), function.isGenerator(), function.isDerivedConstructor(), function.isAsync(), isGlobal, hasSyntheticArguments);
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

    private TruffleString getFunctionName(FunctionNode functionNode) {
        if (context.getEcmaScriptVersion() < 6 && (functionNode.isGetter() || functionNode.isSetter())) {
            // strip getter/setter name prefix in ES5 mode
            assert !functionNode.isAnonymous();
            String name = functionNode.getName();
            if (functionNode.isGetter() && name.startsWith(Strings.GET_SPC.toJavaStringUncached()) || (functionNode.isSetter() && name.startsWith(Strings.SET_SPC.toJavaStringUncached()))) {
                return Strings.substring(context, functionNode.getNameTS(), 4);
            }
        }
        return functionNode.getNameTS();
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
                valueNode = tagHiddenExpression(factory.createAccessRestArgument(context, argIndex));
            } else {
                valueNode = tagHiddenExpression(factory.createAccessArgument(argIndex));
            }
            IdentNode param = function.getParameters().get(i);
            if (param.isIgnoredParameter()) {
                // Duplicate parameter names are allowed in non-strict mode but have no binding.
                assert !currentFunction.isStrictMode();
                continue;
            }
            TruffleString paramName = param.getNameTS();
            VarRef paramRef = environment.findLocalVar(paramName);
            init.add(tagHiddenExpression(paramRef.createWriteNode(valueNode)));
            if (hasMappedArguments) {
                currentFunction.addMappedParameter(paramRef.getFrameSlot(), i);
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
        if (!context.getLanguageOptions().lazyTranslation()) {
            return; // nothing to do
        }

        com.oracle.js.parser.ir.visitor.NodeVisitor<LexicalContext> visitor = new com.oracle.js.parser.ir.visitor.NodeVisitor<>(new LexicalContext()) {
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
                    if (node instanceof Block) {
                        Symbol foundSymbol = ((Block) node).getScope().getExistingSymbol(varName);
                        if (foundSymbol != null && !foundSymbol.isGlobal()) {
                            if (!local) {
                                markUsesAncestorScopeUntil(lastFunction, true);
                            }
                            break;
                        }
                    } else if (node instanceof ClassNode) {
                        if (((ClassNode) node).getScope().hasSymbol(varName) || ((ClassNode) node).getClassHeadScope().hasSymbol(varName)) {
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
                        } else if (!function.isProgram() && varName.equals(ARGUMENTS)) {
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
                            assert !varName.equals(ARGUMENTS);
                            FunctionNode nonArrowFunction = lc.getCurrentNonArrowFunction();
                            // `this` is read from the arrow function object,
                            // unless `this` is supplied by a subclass constructor
                            if (!varName.equals("this") || nonArrowFunction.isDerivedConstructor()) {
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
                return ARGUMENTS.equals(varName) || "new.target".equals(varName) || "super".equals(varName) || "this".equals(varName);
            }

            private boolean isImport(String varName) {
                return IMPORT.equals(varName) || IMPORT_META.equals(varName);
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
                if (!identNode.isPropertyName() && !identNode.isApplyArguments() && identNode.getName().equals(ARGUMENTS)) {
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
                    declarations.add(factory.createDeclareGlobalFunction(symbol.getNameTS(), configurable));
                } else {
                    declarations.add(factory.createDeclareGlobalVariable(symbol.getNameTS(), configurable));
                }
            } else if (!configurable) {
                assert symbol.isBlockScoped();
                declarations.add(factory.createDeclareGlobalLexicalVariable(symbol.getNameTS(), symbol.isConst()));
            }
        }
        final List<JavaScriptNode> nodes = new ArrayList<>(2);
        nodes.add(factory.createGlobalDeclarationInstantiation(context, declarations));
        return nodes;
    }

    private JavaScriptNode prepareArguments() {
        VarRef argumentsVar = environment.findLocalVar(Strings.ARGUMENTS);
        boolean unmappedArgumentsObject = currentFunction().isStrictMode() || !currentFunction().hasSimpleParameterList();
        JavaScriptNode argumentsObject = factory.createArgumentsObjectNode(context, unmappedArgumentsObject, currentFunction().getLeadingArgumentCount());
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
            if (block.getScope().hasBlockScopedOrRedeclaredSymbols() && !(environment instanceof GlobalEnvironment)) {
                createTemporalDeadZoneInit(block.getScope(), scopeInit);
            }
            if (block.isModuleBody()) {
                createResolveImports(lc.getCurrentFunction(), scopeInit);
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

                if (block.isModuleBody()) {
                    blockNode = splitModuleBodyAtYield(blockNode, scopeInit);
                }

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
                // Move hoistable declaration to the front of the block
                List<Statement> newBlockStatements = null;
                for (Statement statement : blockStatements) {
                    if (statement instanceof VarNode) {
                        VarNode varNode = (VarNode) statement;
                        if (varNode.isHoistableDeclaration()) {
                            if (newBlockStatements == null) {
                                newBlockStatements = new ArrayList<>();
                            }
                            newBlockStatements.add(statement);
                        }
                    }
                }
                if (newBlockStatements == null) {
                    // no hoistable declarations
                    newBlockStatements = blockStatements;
                } else {
                    // append other statements
                    for (Statement statement : blockStatements) {
                        if (statement instanceof VarNode) {
                            VarNode varNode = (VarNode) statement;
                            if (varNode.isHoistableDeclaration()) {
                                if (annexBBlockToFunctionTransfer(varNode)) {
                                    newBlockStatements.add(varNode.setFlag(VarNode.IS_ANNEXB_BLOCK_TO_FUNCTION_TRANSFER));
                                } // else among declarations already
                                continue;
                            }
                        }
                        newBlockStatements.add(statement);
                    }
                }
                blockNode = transformStatements(newBlockStatements, block.isTerminal(), block.isExpressionBlock() || block.isParameterBlock(), scopeInit);
            }

            result = blockEnv.wrapBlockScope(blockNode);
        }
        // Parameter initialization must precede (i.e. wrap) the (async) generator function body
        if (block.isFunctionBody()) {
            if (currentFunction.isGeneratorFunction() && !currentFunction.isModule()) {
                result = finishGeneratorBody(result);
            }
        }
        ensureHasSourceSection(result, block);
        return result;
    }

    private boolean annexBBlockToFunctionTransfer(VarNode varNode) {
        return context.isOptionAnnexB() && !environment.isStrictMode() && varNode.isFunctionDeclaration();
    }

    private boolean allowScopeOptimization() {
        return context.getLanguageOptions().scopeOptimization();
    }

    @SuppressWarnings("static-method")
    private boolean allowTDZOptimization() {
        return false;
    }

    /**
     * Initialize block-scoped symbols with a <i>dead</i> marker value.
     */
    private void createTemporalDeadZoneInit(Scope blockScope, List<JavaScriptNode> blockWithInit) {
        assert blockScope.hasBlockScopedOrRedeclaredSymbols() && !(environment instanceof GlobalEnvironment);

        List<FrameSlotVarRef> slotsWithTDZ = new ArrayList<>();
        for (Symbol symbol : blockScope.getSymbols()) {
            if (symbol.isImportBinding()) {
                continue;
            }
            if (symbol.isBlockScoped() && !symbol.hasBeenDeclared()) {
                // Exported variables always need a temporal dead zone. We do not track which
                // bindings are exported, so we assume all bindings in the module scope may be.
                // Also, variables declared in a switch block always need a temporal dead zone since
                // generally, there is no dominance relationship between declaration and use.
                // In other cases, we can statically determine if a local use is in the TDZ,
                // so we can skip the initialization if there is no non-local use (or eval).
                // However, we currently need to initialize the variable for instrumentation.
                if (symbol.isClosedOver() || symbol.isDeclaredInSwitchBlock() || blockScope.isModuleScope() || blockScope.hasNestedEval() || !allowScopeOptimization() || !allowTDZOptimization()) {
                    FrameSlotVarRef slotRef = (FrameSlotVarRef) findScopeVar(symbol.getNameTS(), true);
                    assert JSFrameUtil.hasTemporalDeadZone(slotRef.getFrameSlot()) : slotRef.getFrameSlot();
                    slotsWithTDZ.add(slotRef);
                }
            }
            if (symbol.isVarRedeclaredHere()) {
                // redeclaration of parameter binding; initial value is copied from outer scope.
                assert blockScope.isFunctionBodyScope();
                VarRef outerVarRef = environment.findBlockScopedVar(symbol.getNameTS());
                VarRef innerVarRef = findScopeVar(symbol.getNameTS(), true);
                JavaScriptNode outerVar = outerVarRef.createReadNode();
                blockWithInit.add(innerVarRef.createWriteNode(outerVar));
            }
        }

        if (!slotsWithTDZ.isEmpty()) {
            slotsWithTDZ.sort(Comparator.comparingInt(FrameSlotVarRef::getScopeLevel));
            if (slotsWithTDZ.size() == 1 || slotsWithTDZ.get(0).getScopeLevel() == slotsWithTDZ.get(slotsWithTDZ.size() - 1).getScopeLevel()) {
                // all slots are in the same frame
                int[] slots = new int[slotsWithTDZ.size()];
                ScopeFrameNode scope = slotsWithTDZ.get(0).createScopeFrameNode();
                for (int i = 0; i < slots.length; i++) {
                    slots[i] = slotsWithTDZ.get(i).getFrameSlot().getIndex();
                }
                blockWithInit.add(factory.createClearFrameSlots(scope, slots, 0, slots.length));
            } else {
                // we have slots in separate frames
                int[] slots = new int[slotsWithTDZ.size()];
                for (int from = 0; from < slots.length;) {
                    FrameSlotVarRef first = slotsWithTDZ.get(from);
                    ScopeFrameNode scope = first.createScopeFrameNode();
                    slots[from] = slotsWithTDZ.get(from).getFrameSlot().getIndex();
                    int to = from + 1;
                    while (to < slots.length) {
                        FrameSlotVarRef next = slotsWithTDZ.get(to);
                        if (next.getScopeLevel() != first.getScopeLevel()) {
                            break;
                        }
                        slots[to++] = next.getFrameSlot().getIndex();
                    }
                    blockWithInit.add(factory.createClearFrameSlots(scope, slots, from, to));
                    from = to;
                }
            }
        }
    }

    private JavaScriptNode wrapTemporalDeadZoneInit(Scope scope, JavaScriptNode blockBody) {
        if (!scope.hasBlockScopedOrRedeclaredSymbols()) {
            return blockBody;
        }
        List<JavaScriptNode> init = new ArrayList<>(4);
        createTemporalDeadZoneInit(scope, init);
        if (init.isEmpty()) {
            return blockBody;
        } else {
            init.add(blockBody);
            return factory.createExprBlock(init.toArray(EMPTY_NODE_ARRAY));
        }
    }

    private void createResolveImports(FunctionNode functionNode, List<JavaScriptNode> declarations) {
        assert functionNode.isModule();

        // Assert: all named exports from module are resolvable.
        for (ImportEntry importEntry : functionNode.getModule().getImportEntries()) {
            ModuleRequest moduleRequest = importEntry.getModuleRequest();
            TruffleString localName = importEntry.getLocalName();
            String localNameJS = localName.toJavaStringUncached();
            JSWriteFrameSlotNode writeLocalNode = (JSWriteFrameSlotNode) environment.findLocalVar(localName).createWriteNode(null);
            JavaScriptNode thisModule = getActiveModule();
            if (importEntry.getImportName().equals(Module.STAR_NAME)) { // namespace-object
                assert functionNode.getBody().getScope().hasSymbol(localNameJS) && functionNode.getBody().getScope().getExistingSymbol(localNameJS).hasBeenDeclared();
                declarations.add(factory.createResolveStarImport(context, thisModule, moduleRequest, writeLocalNode));
            } else if (importEntry.getImportName().equals(Module.SOURCE_IMPORT_NAME)) { // source
                assert functionNode.getBody().getScope().hasSymbol(localNameJS) && functionNode.getBody().getScope().getExistingSymbol(localNameJS).hasBeenDeclared();
                declarations.add(factory.createResolveSourceImport(context, thisModule, moduleRequest, writeLocalNode));
            } else {
                assert functionNode.getBody().getScope().hasSymbol(localNameJS) && functionNode.getBody().getScope().getExistingSymbol(localNameJS).isImportBinding();
                declarations.add(factory.createResolveNamedImport(context, thisModule, moduleRequest, importEntry.getImportName(), writeLocalNode));
            }
        }
    }

    /**
     * Moves all statements up to, and including, module yield to scopeInit and returns a new block
     * containing the rest of the body. This ensures a flat block up to the yield, followed by a
     * RootBody-tagged (block) node. Otherwise we might end up with a nested block that contains the
     * yield, making it more involved to split up the module at the yield into link() and execute();
     * but even if we kept the yield, we'd still have two blocks to resume into instead of one.
     *
     * <pre>
     * [ScopeInit] | [ModuleLink Yield ModuleExecute]:body =>
     * [ScopeInit ModuleLink Yield] | [ModuleExecute]:body
     * </pre>
     */
    private JavaScriptNode splitModuleBodyAtYield(JavaScriptNode blockNode, List<JavaScriptNode> scopeInit) {
        if (blockNode instanceof SequenceNode) {
            JavaScriptNode[] statements = ((SequenceNode) blockNode).getStatements();
            for (int i = 0; i < statements.length; i++) {
                JavaScriptNode statement = statements[i];
                if (isModuleYieldStatement(statement)) {
                    scopeInit.addAll(Arrays.asList(statements).subList(0, i + 1));
                    return factory.createExprBlock(Arrays.copyOfRange(statements, i + 1, statements.length));
                }
            }
        } else if (isModuleYieldStatement(blockNode)) {
            scopeInit.add(blockNode);
            return factory.createEmpty();
        }
        // If yield has not been found (unexpected), keep everything as it is.
        return blockNode;
    }

    /**
     * Detects a module yield, optionally wrapped in a return value slot assignment.
     */
    private static boolean isModuleYieldStatement(JavaScriptNode statement) {
        return statement instanceof ModuleYieldNode || (statement instanceof JSWriteFrameSlotNode && ((JSWriteFrameSlotNode) statement).getRhs() instanceof ModuleYieldNode);
    }

    /**
     * Create var-declared dynamic scope bindings in the variable environment of the caller.
     */
    private void prependDynamicScopeBindingInit(Block block, List<JavaScriptNode> blockWithInit) {
        assert currentFunction().isCallerContextEval();
        for (Symbol symbol : block.getSymbols()) {
            if (symbol.isVar() && !environment.getVariableEnvironment().hasLocalVar(symbol.getName())) {
                blockWithInit.add(createDynamicScopeBinding(symbol.getNameTS(), true));
            }
        }
    }

    private JavaScriptNode createDynamicScopeBinding(TruffleString varName, boolean deleteable) {
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
        boolean returnsLastStatementResult = currentFunction().returnsLastStatementResult();
        for (int i = 0; i < blockStatements.size(); i++) {
            Statement statement = blockStatements.get(i);
            JavaScriptNode statementNode = transformStatementInBlock(statement);
            if (returnsLastStatementResult) {
                if (statement.isCompletionValueNeverEmpty()) {
                    lastNonEmptyIndex = pos;
                } else {
                    if (lastNonEmptyIndex >= 0) {
                        statements[lastNonEmptyIndex] = wrapSetCompletionValue(statements[lastNonEmptyIndex]);
                        lastNonEmptyIndex = -1;
                    }
                }
            }
            statements[pos++] = statementNode;
        }
        if (returnsLastStatementResult && lastNonEmptyIndex >= 0) {
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
                BlockEnvironment blockEnv = new BlockEnvironment(globalEnv, factory, context, block.getScope());
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
                if (!function.isModule() && (allowScopeOptimization()
                                ? BlockEnvironment.isScopeCaptured(scope)
                                : (function.hasClosures() || function.hasEval()))) {
                    functionEnv = new BlockEnvironment(environment, factory, context, scope);
                }

                boolean onlyBlockScoped = currentFunction().isCallerContextEval();

                addFunctionFrameSlots(functionEnv, function);

                functionEnv.addFrameSlotsFromSymbols(scope.getSymbols(), onlyBlockScoped, null);

                return new EnvironmentCloseable(functionEnv);
            } else if (scope.hasDeclarations() || JSConfig.ManyBlockScopes) {
                BlockEnvironment blockEnv = new BlockEnvironment(environment, factory, context, scope);
                blockEnv.addFrameSlotsFromSymbols(scope.getSymbols());
                return new EnvironmentCloseable(blockEnv);
            }
        }
        return new EnvironmentCloseable(environment);
    }

    private Environment newPerIterationEnvironment(Scope scope) {
        BlockEnvironment blockEnv = new BlockEnvironment(environment, factory, context, scope);
        blockEnv.addFrameSlotsFromSymbols(scope.getSymbols(), true, (!scope.hasNestedEval() && allowScopeOptimization()) ? Symbol::isClosedOver : null);
        return blockEnv;
    }

    private void addFunctionFrameSlots(Environment env, FunctionNode function) {
        if (function.needsArguments()) {
            assert function.getBody().getScope().hasSymbol(ARGUMENTS) : function;
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
        if (function.isClassConstructor() && (lc.getCurrentClass().needsInitializeInstanceElements())) {
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
                globalEnv.addLexicalDeclaration(symbol.getNameTS(), symbol.isConst());
            } else if (symbol.isGlobal() && symbol.isVar()) {
                globalEnv.addVarDeclaration(symbol.getNameTS());
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
            return factory.createRegExpLiteral(context, ((Lexer.RegexToken) value).getExpressionTS(), ((Lexer.RegexToken) value).getOptionsTS());
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
        } else if (identNode.isPrivateInCheck()) {
            TruffleString privateVarName = identNode.getNameTS();
            VarRef privateVarRef = environment.findLocalVar(privateVarName);
            JavaScriptNode readNode = privateVarRef.createReadNode();
            JSFrameSlot frameSlot = privateVarRef.getFrameSlot();
            if (JSFrameUtil.needsPrivateBrandCheck(frameSlot)) {
                // Create a brand node so that a brand check can be performed in the InNode.
                result = getPrivateBrandNode(frameSlot, privateVarRef);
            } else {
                result = readNode;
            }
        } else {
            TruffleString varName = identNode.getNameTS();
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
        return environment.findActiveModule().createReadNode();
    }

    private VarRef findScopeVar(TruffleString name, boolean skipWith) {
        return environment.findVar(name, skipWith);
    }

    private VarRef findScopeVarCheckTDZ(TruffleString name, boolean initializationAssignment) {
        VarRef varRef = findScopeVar(name, false);
        if (varRef.isFunctionLocal()) {
            if (varRef.hasBeenDeclared()) {
                return varRef;
            }
            Symbol symbol = lc.getCurrentScope().findBlockScopedSymbolInFunction(varRef.getName().toJavaStringUncached());
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
            } else if (initializationAssignment) {
                varRef.setHasBeenDeclared(true);
                return varRef;
            } else {
                // variable reference is unconditionally in the temporal dead zone, i.e.,
                // var ref is in declaring function and in scope but before the actual declaration
                // Note: may not throw a ReferenceError if shadowed by a with statement.
                return varRef.withTDZCheck();
            }
        }
        return varRef.withTDZCheck();
    }

    @Override
    public JavaScriptNode enterVarNode(VarNode varNode) {
        TruffleString varName = varNode.getName().getNameTS();
        assert currentFunction().isGlobal() && (!varNode.isBlockScoped() || lc.getCurrentBlock().isFunctionBody()) || !findScopeVar(varName, true).isGlobal() ||
                        currentFunction().isCallerContextEval() : varNode;

        Symbol symbol = null;
        VarRef varRef = null;
        if (varNode.isBlockScoped()) {
            // below, `symbol!=null` implies `isBlockScoped`
            symbol = lc.getCurrentScope().getExistingSymbol(varNode.getName().getName());
            varRef = findScopeVar(varName, true);
            assert symbol != null && varRef != null : varName;
        }

        JavaScriptNode assignment;
        if (varNode.isAssignment()) {
            assignment = createVarAssignNode(varNode, varName);
        } else if (symbol != null && (!varNode.isDestructuring() || symbol.isDeclaredInSwitchBlock()) && !symbol.hasBeenDeclared()) {
            assert varNode.isBlockScoped();
            assignment = varRef.createWriteNode(factory.createConstantUndefined());
        } else {
            assignment = factory.createEmpty();
        }
        // mark block-scoped symbols as declared, except:
        // (a) symbols declared in a switch case always need the dynamic TDZ check
        // (b) destructuring: the symbol does not come alive until the destructuring assignment
        if (symbol != null && (!symbol.isDeclaredInSwitchBlock() && !varNode.isDestructuring())) {
            assert varNode.isBlockScoped();
            varRef.setHasBeenDeclared(true);
        }
        return assignment;
    }

    private JavaScriptNode createVarAssignNode(VarNode varNode, TruffleString varName) {
        JavaScriptNode assignment = null;
        if (varNode.isBlockScoped() && varNode.isFunctionDeclaration() && context.isOptionAnnexB()) {
            // B.3.3 Block-Level Function Declarations Web Legacy Compatibility Semantics
            FunctionNode fn = lc.getCurrentFunction();
            if (!fn.isStrict()) {
                Symbol symbol = lc.getCurrentScope().getExistingSymbol(varName.toJavaStringUncached());
                if (symbol.isHoistedBlockFunctionDeclaration()) {
                    if (varNode.getFlag(VarNode.IS_ANNEXB_BLOCK_TO_FUNCTION_TRANSFER)) {
                        assert hasVarSymbol(fn.getVarDeclarationBlock().getScope(), varName) : varName;
                        JavaScriptNode blockScopeValue = findScopeVar(varName, false).createReadNode();
                        assignment = environment.findVar(varName, true, false, true, false, false).withRequired(false).createWriteNode(blockScopeValue);
                        tagExpression(assignment, varNode);
                    }
                }
            }
        }
        if (assignment == null) {
            JavaScriptNode rhs = transform(varNode.getAssignmentSource());
            assignment = findScopeVar(varName, false).createWriteNode(rhs);
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

    private static boolean hasVarSymbol(Scope scope, TruffleString varName) {
        Symbol varSymbol = scope.getExistingSymbol(varName.toJavaStringUncached());
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

    private static boolean isConstantFalse(JavaScriptNode condition) {
        return condition instanceof JSConstantNode && !JSRuntime.toBoolean(((JSConstantNode) condition).getValue());
    }

    private JavaScriptNode createDoWhile(JavaScriptNode condition, JavaScriptNode body) {
        if (isConstantFalse(condition)) {
            // do {} while (0); happens 336 times in Mandreel
            return body;
        }
        RepeatingNode repeatingNode = factory.createDoWhileRepeatingNode(condition, body);
        return factory.createDoWhile(factory.createLoopNode(repeatingNode));
    }

    private JavaScriptNode createWhileDo(JavaScriptNode condition, JavaScriptNode body) {
        if (isConstantFalse(condition)) {
            return factory.createEmpty();
        }
        RepeatingNode repeatingNode = factory.createWhileDoRepeatingNode(condition, body);
        return factory.createWhileDo(factory.createLoopNode(repeatingNode));
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
            JSFrameDescriptor iterationBlockFrameDescriptor = environment.getBlockFrameDescriptor();
            RepeatingNode repeatingNode = factory.createForRepeatingNode(test, wrappedBody, modify, iterationBlockFrameDescriptor.toFrameDescriptor(), firstTempVar.createReadNode(),
                            firstTempVar.createWriteNode(factory.createConstantBoolean(false)), environment.getCurrentBlockScopeSlot());
            StatementNode newFor = factory.createFor(factory.createLoopNode(repeatingNode));
            ensureHasSourceSection(newFor, forNode);
            return createBlock(init, firstTempVar.createWriteNode(factory.createConstantBoolean(true)), newFor);
        }
        RepeatingNode repeatingNode = factory.createWhileDoRepeatingNode(test, createBlock(wrappedBody, modify));
        JavaScriptNode whileDo = factory.createDesugaredFor(factory.createLoopNode(repeatingNode));
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
        return desugarForInOrOfBody(forNode, factory.createGetIterator(createIteratorNode), jumpTarget);
    }

    private JavaScriptNode desugarForOf(ForNode forNode, JavaScriptNode modify, JumpTargetCloseable<ContinueTarget> jumpTarget) {
        assert forNode.isForOf();
        JavaScriptNode getIterator = factory.createGetIterator(modify);
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
        try (EnvironmentCloseable blockEnv = new EnvironmentCloseable(needsPerIterationScope(forNode) ? newPerIterationEnvironment(lc.getCurrentBlock().getScope()) : environment)) {
            // var nextValue = IteratorValue(nextResult);
            VarRef nextResultVar2 = environment.findTempVar(nextResultVar.getFrameSlot());
            VarRef nextValueVar = environment.createTempVar();
            VarRef iteratorVar2 = environment.findTempVar(iteratorVar.getFrameSlot());
            JavaScriptNode nextResult = nextResultVar2.createReadNode();
            JavaScriptNode nextValue = factory.createIteratorValue(nextResult);
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
        RepeatingNode repeatingNode = factory.createWhileDoRepeatingNode(condition, wrappedBody);
        LoopNode loopNode = factory.createLoopNode(repeatingNode);
        JavaScriptNode whileNode = forNode.isForOf() ? factory.createDesugaredForOf(loopNode) : factory.createDesugaredForIn(loopNode);
        JavaScriptNode wrappedWhile = factory.createIteratorCloseIfNotDone(context, jumpTarget.wrapBreakTargetNode(whileNode), iteratorVar.createReadNode());
        JavaScriptNode resetIterator = iteratorVar.createWriteNode(factory.createConstant(JSFrameUtil.DEFAULT_VALUE));
        wrappedWhile = factory.createTryFinally(wrappedWhile, resetIterator);
        ensureHasSourceSection(whileNode, forNode);
        return createBlock(iteratorInit, wrappedWhile);
    }

    private JavaScriptNode desugarForHeadAssignment(ForNode forNode, JavaScriptNode next) {
        boolean lexicalBindingInit = forNode.hasPerIterationScope();
        if (forNode.getInit() instanceof IdentNode && lexicalBindingInit) {
            return tagExpression(findScopeVarCheckTDZ(((IdentNode) forNode.getInit()).getNameTS(), lexicalBindingInit).createWriteNode(next), forNode);
        } else {
            // transform destructuring assignment
            return tagExpression(transformAssignment(forNode.getInit(), forNode.getInit(), next, lexicalBindingInit), forNode);
        }
    }

    private JavaScriptNode desugarForAwaitOf(ForNode forNode, JavaScriptNode modify, JumpTargetCloseable<ContinueTarget> jumpTarget) {
        assert forNode.isForAwaitOf();
        JavaScriptNode getIterator = factory.createGetAsyncIterator(modify);
        VarRef iteratorVar = environment.createTempVar();
        JavaScriptNode iteratorInit = iteratorVar.createWriteNode(getIterator);
        VarRef nextResultVar = environment.createTempVar();

        currentFunction().addAwait();
        JSReadFrameSlotNode asyncResultNode = (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getAsyncResultSlot()).createReadNode();
        JSReadFrameSlotNode asyncContextNode = (JSReadFrameSlotNode) environment.findTempVar(currentFunction().getAsyncContextSlot()).createReadNode();
        JSFrameDescriptor functionFrameDesc = environment.getFunctionFrameDescriptor();
        JSFrameSlot stateSlot = addGeneratorStateSlot(functionFrameDesc, FrameSlotKind.Int);
        JavaScriptNode iteratorNext = factory.createAsyncIteratorNext(context, stateSlot, iteratorVar.createReadNode(),
                        asyncContextNode, asyncResultNode);
        // nextResult = Await(IteratorNext(iterator))
        // while(!(done = IteratorComplete(nextResult)))
        JavaScriptNode condition = factory.createDual(context,
                        factory.createIteratorSetDone(iteratorVar.createReadNode(), factory.createConstantBoolean(true)),
                        factory.createUnary(UnaryOperation.NOT, factory.createIteratorComplete(context, nextResultVar.createWriteNode(iteratorNext))));
        JavaScriptNode wrappedBody;
        try (EnvironmentCloseable blockEnv = new EnvironmentCloseable(needsPerIterationScope(forNode) ? newPerIterationEnvironment(lc.getCurrentBlock().getScope()) : environment)) {
            // var nextValue = IteratorValue(nextResult);
            VarRef nextResultVar2 = environment.findTempVar(nextResultVar.getFrameSlot());
            VarRef nextValueVar = environment.createTempVar();
            VarRef iteratorVar2 = environment.findTempVar(iteratorVar.getFrameSlot());
            JavaScriptNode nextResult = nextResultVar2.createReadNode();
            JavaScriptNode nextValue = factory.createIteratorValue(nextResult);
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
        RepeatingNode repeatingNode = factory.createWhileDoRepeatingNode(condition, wrappedBody);
        LoopNode loopNode = factory.createLoopNode(repeatingNode);
        JavaScriptNode whileNode = factory.createDesugaredForAwaitOf(loopNode);
        currentFunction().addAwait();
        stateSlot = addGeneratorStateSlot(functionFrameDesc, FrameSlotKind.Object);
        JavaScriptNode wrappedWhile = factory.createAsyncIteratorCloseWrapper(context, stateSlot, jumpTarget.wrapBreakTargetNode(whileNode), iteratorVar.createReadNode(),
                        asyncContextNode, asyncResultNode);
        JavaScriptNode resetIterator = iteratorVar.createWriteNode(factory.createConstant(JSFrameUtil.DEFAULT_VALUE));
        wrappedWhile = factory.createTryFinally(wrappedWhile, resetIterator);
        ensureHasSourceSection(whileNode, forNode);
        return createBlock(iteratorInit, wrappedWhile);
    }

    private boolean needsPerIterationScope(ForNode forNode) {
        // for loop init block may contain closures, too; that's why we check the surrounding block.
        if (forNode.hasPerIterationScope()) {
            if (allowScopeOptimization()) {
                Scope forScope = lc.getCurrentScope();
                // assert forScope.hasDeclarations(); // implied by per-iteration scope flag.
                if (!forScope.hasDeclarations()) {
                    return false;
                }
                if (forScope.hasClosures() || forScope.hasNestedEval()) {
                    return true;
                }
            } else {
                FunctionNode function = lc.getCurrentFunction();
                if (function.hasClosures() && hasClosures(lc.getCurrentBlock())) {
                    return true;
                } else if (function.hasEval()) {
                    return true;
                }
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
            case SPREAD_ARGUMENT: {
                JavaScriptNode argument = transform(unaryNode.getExpression());
                GetIteratorUnaryNode getIterator = factory.createGetIterator(argument);
                return tagExpression(factory.createSpreadArgument(context, getIterator), unaryNode);
            }
            case SPREAD_ARRAY: {
                JavaScriptNode array = transform(unaryNode.getExpression());
                GetIteratorUnaryNode getIterator = factory.createGetIterator(array);
                return tagExpression(factory.createSpreadArray(context, getIterator), unaryNode);
            }
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

    public JSFrameSlot addGeneratorStateSlot(JSFrameDescriptor functionFrameDescriptor, FrameSlotKind slotKind) {
        InternalSlotId identifier = factory.createInternalSlotId("generatorstate", functionFrameDescriptor.getSize());
        return functionFrameDescriptor.addFrameSlot(identifier, slotKind);
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
        JSFrameSlot stateSlot = addGeneratorStateSlot(currentFunction.getFunctionFrameDescriptor(), FrameSlotKind.Int);
        return factory.createAwait(context, stateSlot, expression, asyncContextNode, asyncResultNode);
    }

    private JavaScriptNode createYieldNode(UnaryNode unaryNode) {
        FunctionEnvironment currentFunction = currentFunction();
        assert currentFunction.isGeneratorFunction();
        JSFrameDescriptor functionFrameDesc = currentFunction.getFunctionFrameDescriptor();
        if (lc.getCurrentFunction().isModule()) {
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
            JSFrameSlot stateSlot = addGeneratorStateSlot(functionFrameDesc, FrameSlotKind.Int);
            if (yieldStar) {
                JSFrameSlot iteratorTempSlot = addGeneratorStateSlot(functionFrameDesc, FrameSlotKind.Object);
                return factory.createAsyncGeneratorYieldStar(context, stateSlot, iteratorTempSlot, expression, asyncContextNode, asyncResultNode, returnNode);
            } else {
                return factory.createAsyncGeneratorYield(context, stateSlot, expression, asyncContextNode, asyncResultNode, returnNode);
            }
        } else {
            currentFunction.addYield();
            JSWriteFrameSlotNode writeYieldResultNode = JSConfig.YieldResultInFrame ? (JSWriteFrameSlotNode) environment.findTempVar(currentFunction.getYieldResultSlot()).createWriteNode(null)
                            : null;
            JSFrameSlot stateSlot = addGeneratorStateSlot(functionFrameDesc, yieldStar ? FrameSlotKind.Object : FrameSlotKind.Int);
            return factory.createYield(context, stateSlot, expression, environment.findYieldValueVar().createReadNode(), yieldStar, returnNode, writeYieldResultNode);
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
            TruffleString identNodeNameTS = identNode.getNameTS();
            if (context.isOptionNashornCompatibilityMode() && (identNodeName.equals(LINE__) || identNodeName.equals(FILE__) || identNodeName.equals(DIR__))) {
                operand = GlobalPropertyNode.createPropertyNode(context, identNodeNameTS);
            } else if (!identNode.isThis() && !identNode.isMetaProperty()) {
                // typeof globalVar must not throw ReferenceError if globalVar does not exist
                operand = findScopeVarCheckTDZ(identNodeNameTS, false).withRequired(false).createReadNode();
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
            VarRef varRef = findScopeVarCheckTDZ(identNode.getNameTS(), false);
            if (varRef instanceof FrameSlotVarRef) {
                FrameSlotVarRef frameVarRef = (FrameSlotVarRef) varRef;
                JSFrameSlot frameSlot = frameVarRef.getFrameSlot();
                if (JSFrameUtil.isConst(frameSlot)) {
                    // we know this is going to throw. do the read and throw TypeError.
                    return tagExpression(checkMutableBinding(frameVarRef.createReadNode(), frameSlot.getIdentifier()), unaryNode);
                }
                return tagExpression(factory.createLocalVarInc(tokenTypeToUnaryOperation(unaryNode.tokenType()), frameSlot, frameVarRef.hasTDZCheck(),
                                frameVarRef.createScopeFrameNode()), unaryNode);
            }
        }

        BinaryOperation operation = unaryNode.tokenType() == TokenType.INCPREFIX || unaryNode.tokenType() == TokenType.INCPOSTFIX ? BinaryOperation.INCREMENT : BinaryOperation.DECREMENT;
        boolean isPostfix = unaryNode.tokenType() == TokenType.INCPOSTFIX || unaryNode.tokenType() == TokenType.DECPOSTFIX;
        return tagExpression(transformCompoundAssignment(unaryNode, unaryNode.getExpression(), null, operation, isPostfix, true), unaryNode);
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
        return factory.createNamedEvaluation(transform(unaryNode.getExpression()), factory.createAccessArgument(1));
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
            TruffleString varName = ((IdentNode) rhs).getNameTS();
            VarRef varRef = findScopeVar(varName, varName.equals(Strings.THIS));
            result = varRef.createDeleteNode();
        } else {
            // deleting a non-reference, always returns true
            if (rhs instanceof LiteralNode.PrimitiveLiteralNode<?>) {
                result = factory.createConstantBoolean(true);
            } else {
                result = factory.createDual(context, transform(rhs), factory.createConstantBoolean(true));
            }
        }
        return tagExpression(result, unaryNode);
    }

    private JavaScriptNode enterDeleteProperty(UnaryNode deleteNode) {
        BaseNode baseNode = (BaseNode) deleteNode.getExpression();

        JavaScriptNode target = transform(baseNode.getBase());
        JavaScriptNode key;
        if (baseNode instanceof AccessNode) {
            AccessNode accessNode = (AccessNode) baseNode;
            assert !accessNode.isPrivate();
            key = factory.createConstantString(accessNode.getPropertyTS());
        } else {
            assert baseNode instanceof IndexNode;
            IndexNode indexNode = (IndexNode) baseNode;
            key = transform(indexNode.getIndex());
        }

        if (baseNode.isSuper()) {
            // If IsSuperReference(ref) is true, throw a ReferenceError exception.
            return tagExpression(factory.createThrowError(JSErrorType.ReferenceError, UNSUPPORTED_REFERENCE_TO_SUPER), deleteNode);
        }

        if (baseNode.isOptionalChain()) {
            target = filterOptionalChainTarget(target, baseNode.isOptional());
        }
        JavaScriptNode delete = factory.createDeleteProperty(target, key, environment.isStrictMode());
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
        AbstractFunctionArgumentsNode arguments = factory.createFunctionArguments(context, args);
        JavaScriptNode call = factory.createNew(context, function, arguments);
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
            call = createCallDirectSuper(function, args, callNode.isDefaultDerivedConstructorSuperCall());
        } else if (callNode.isImport()) {
            call = createImportCallNode(args, callNode.isImportSource() ? ImportPhase.Source : ImportPhase.Evaluation);
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
                        factory.createThrowError(JSErrorType.ReferenceError, SUPER_CALLED_TWICE));
    }

    private JavaScriptNode initializeInstanceElements(JavaScriptNode thisValueNode) {
        ClassNode classNode = lc.getCurrentClass();
        if (!classNode.needsInitializeInstanceElements()) {
            return thisValueNode;
        }

        JavaScriptNode constructor = factory.createAccessCallee(currentFunction().getThisFunctionLevel());
        return factory.createInitializeInstanceElements(context, thisValueNode, constructor);
    }

    private JavaScriptNode createCallEvalNode(JavaScriptNode function, JavaScriptNode[] args) {
        assert (currentFunction().isGlobal() || currentFunction().isStrictMode() || currentFunction().isDirectEval()) || currentFunction().isDynamicallyScoped();
        currentFunction().prepareForDirectEval();
        return EvalNode.create(context, function, args, createThisNodeUnchecked(),
                        new DirectEvalContext(lc.getCurrentScope(), environment, lc.getCurrentClass(), activeScriptOrModule),
                        environment.getCurrentBlockScopeSlot());
    }

    private JavaScriptNode createCallApplyArgumentsNode(JavaScriptNode function, JavaScriptNode[] args) {
        return factory.createCallApplyArguments((JSFunctionCallNode) factory.createFunctionCall(context, function, args));
    }

    private JavaScriptNode createCallDirectSuper(JavaScriptNode function, JavaScriptNode[] args, boolean inDefaultDerivedConstructor) {
        if (inDefaultDerivedConstructor) {
            VarRef thisVar = environment.findThisVar();
            return initializeInstanceElements(thisVar.createWriteNode(factory.createDefaultDerivedConstructorSuperCall(function)));
        } else {
            return initializeThis(factory.createFunctionCallWithNewTarget(context, function, insertNewTargetArg(args)));
        }
    }

    private JavaScriptNode createImportCallNode(JavaScriptNode[] args, ImportPhase phase) {
        if (args.length == 2 && (context.getLanguageOptions().importAttributes() || context.getLanguageOptions().importAssertions())) {
            return factory.createImportCall(context, phase, args[0], args[1], activeScriptOrModule);
        }
        assert args.length == 1 : args.length;
        return factory.createImportCall(context, phase, args[0], null, activeScriptOrModule);
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
        Expression lhsExpr = binaryNode.getLhs();
        JavaScriptNode lhs = transform(lhsExpr);
        JavaScriptNode rhs = transform(binaryNode.getRhs());
        JavaScriptNode result;
        if (lhsExpr instanceof IdentNode && ((IdentNode) lhsExpr).isPrivateInCheck()) {
            result = factory.createPrivateFieldIn(lhs, rhs);
        } else {
            result = factory.createBinary(context, tokenTypeToBinaryOperation(binaryNode.tokenType()), lhs, rhs);
        }
        return tagExpression(result, binaryNode);
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

    @SuppressWarnings("fallthrough")
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
        TruffleString ident = identNode.getNameTS();
        VarRef scopeVar = findScopeVarCheckTDZ(ident, initializationAssignment);

        // if scopeVar is const, the assignment will never succeed and is only there to perform
        // the temporal dead zone check and throw a ReferenceError instead of a TypeError
        boolean constAssignment = !initializationAssignment && scopeVar.isConst();

        if (binaryOp == null) {
            assert assignedValue != null;
            if (constAssignment) {
                rhs = checkMutableBinding(rhs, scopeVar.getName());
            }
            return scopeVar.createWriteNode(rhs);
        } else {
            if (isLogicalOp(binaryOp)) {
                assert !convertLHSToNumeric && !returnOldValue && assignedValue != null;
                if (constAssignment) {
                    rhs = checkMutableBinding(rhs, scopeVar.getName());
                }
                JavaScriptNode readNode = tagExpression(scopeVar.createReadNode(), identNode);
                JavaScriptNode writeNode = scopeVar.createWriteNode(rhs);
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
                if (constAssignment) {
                    binOpNode = checkMutableBinding(binOpNode, scopeVar.getName());
                }
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
        // evaluate rhs and throw TypeError
        return factory.createWriteConstantVariable(rhsNode, true, identifier);
    }

    private JavaScriptNode transformPropertyAssignment(AccessNode accessNode, JavaScriptNode assignedValue, BinaryOperation binaryOp, boolean returnOldValue, boolean convertToNumeric) {
        JavaScriptNode assignedNode;
        JavaScriptNode target = transform(accessNode.getBase());

        if (binaryOp == null) {
            assert assignedValue != null;
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
                assert !convertToNumeric && !returnOldValue && assignedValue != null;
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
            assert assignedValue != null;
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
            if ((elem instanceof JSConstantNode && target instanceof RepeatableNode)) {
                // Cannot be used for any indexNode and any target RepeatableNode
                // because there is an invocation of indexNode in between targets
                target1 = target;
                target2 = factory.copy(target);
            } else {
                VarRef targetTemp = environment.createTempVar();
                if (target instanceof SuperPropertyReferenceNode superTarget) {
                    // Various places depend on 'instanceof SuperPropertyReferenceNode',
                    // so we cannot use the branch below, but we can use a similar
                    // SuperPropertyReferenceNode-specific approach
                    target1 = tagExpression(factory.createSuperPropertyReference(targetTemp.createWriteNode(superTarget.getBaseValue()), superTarget.getThisValue()), indexNode.getBase());
                    target2 = tagExpression(factory.createSuperPropertyReference(targetTemp.createReadNode(), superTarget.getThisValue()), indexNode.getBase());
                } else {
                    target1 = targetTemp.createWriteNode(target);
                    target2 = targetTemp.createReadNode();
                }
            }

            if (isLogicalOp(binaryOp)) {
                assert !convertToNumeric && !returnOldValue && assignedValue != null;
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
        JavaScriptNode getIterator = factory.createGetIterator(initValue);
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
            JavaScriptNode iteratorGetNextValueNode = factory.createIteratorGetNextValue(context, iteratorTempVar.createReadNode(), factory.createConstantUndefined(), true, element != null);
            JavaScriptNode iteratorIsDoneNode = factory.createIteratorIsDone(iteratorTempVar.createReadNode());
            JavaScriptNode rhsNode = factory.createIf(iteratorIsDoneNode, factory.createConstantUndefined(), iteratorGetNextValueNode);
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
                TruffleString keyName = property.getKeyNameTS();
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
            return factory.createReadProperty(context, base, accessNode.getPropertyTS(), accessNode.isFunction());
        }
    }

    private JavaScriptNode createWriteProperty(AccessNode accessNode, JavaScriptNode base, JavaScriptNode rhs) {
        if (accessNode.isPrivate()) {
            return createPrivateFieldSet(accessNode, base, rhs);
        } else {
            return factory.createWriteProperty(base, accessNode.getPropertyTS(), rhs, context, environment.isStrictMode());
        }
    }

    private JavaScriptNode createPrivateFieldGet(AccessNode accessNode, JavaScriptNode base) {
        VarRef privateNameVar = environment.findLocalVar(accessNode.getPrivateNameTS());
        JavaScriptNode privateName = privateNameVar.createReadNode();
        return factory.createPrivateFieldGet(context, insertPrivateBrandCheck(base, privateNameVar), privateName);
    }

    private JavaScriptNode createPrivateFieldSet(AccessNode accessNode, JavaScriptNode base, JavaScriptNode rhs) {
        VarRef privateNameVar = environment.findLocalVar(accessNode.getPrivateNameTS());
        JavaScriptNode privateName = privateNameVar.createReadNode();
        return factory.createPrivateFieldSet(context, insertPrivateBrandCheck(base, privateNameVar), privateName, rhs);
    }

    private JavaScriptNode insertPrivateBrandCheck(JavaScriptNode base, VarRef privateNameVar) {
        JSFrameSlot frameSlot = privateNameVar.getFrameSlot();
        if (JSFrameUtil.needsPrivateBrandCheck(frameSlot)) {
            JavaScriptNode brand = getPrivateBrandNode(frameSlot, privateNameVar);
            return factory.createPrivateBrandCheck(base, brand);
        } else {
            return base;
        }
    }

    private JSFrameSlot getConstructorFrameSlotForVariable(VarRef privateNameVar) {
        int frameLevel = ((AbstractFrameVarRef) privateNameVar).getFrameLevel();
        int scopeLevel = ((AbstractFrameVarRef) privateNameVar).getScopeLevel();
        Environment memberEnv = environment.getParentAt(frameLevel, scopeLevel);
        return memberEnv.findBlockFrameSlot(ClassNode.PRIVATE_CONSTRUCTOR_BINDING_NAME);
    }

    private JavaScriptNode getPrivateBrandNode(JSFrameSlot frameSlot, VarRef privateNameVar) {
        int frameLevel = ((AbstractFrameVarRef) privateNameVar).getFrameLevel();
        int scopeLevel = ((AbstractFrameVarRef) privateNameVar).getScopeLevel();
        Environment memberEnv = environment.getParentAt(frameLevel, scopeLevel);
        JSFrameSlot constructorSlot = memberEnv.findBlockFrameSlot(ClassNode.PRIVATE_CONSTRUCTOR_BINDING_NAME);
        JavaScriptNode constructor = environment.createLocal(constructorSlot, frameLevel, scopeLevel);
        if (JSFrameUtil.isPrivateNameStatic(frameSlot)) {
            return constructor;
        } else {
            return factory.createGetPrivateBrand(context, constructor);
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

    private ArrayList<ObjectLiteralMemberNode> transformPropertyDefinitionList(List<? extends PropertyNode> properties, boolean isClass, Symbol classNameSymbol) {
        ArrayList<ObjectLiteralMemberNode> members = new ArrayList<>(properties.size());
        for (int i = 0; i < properties.size(); i++) {
            PropertyNode property = properties.get(i);
            assert !property.isCoverInitializedName();

            final ObjectLiteralMemberNode member;
            if (property.getValue() != null || (isClass && ((ClassElement) property).isClassFieldOrAutoAccessor())) {
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

    private DecoratorListEvaluationNode[] transformClassElementsDecorators(List<ClassElement> elements) {
        DecoratorListEvaluationNode[] decoratedElements = new DecoratorListEvaluationNode[elements.size()];
        int i = 0;
        for (ClassElement element : elements) {
            JavaScriptNode[] decorators = null;
            if (element.getDecorators() != null) {
                List<Expression> d = element.getDecorators();
                decorators = new JavaScriptNode[d.size()];
                for (int j = 0; j < d.size(); j++) {
                    decorators[j] = transform(d.get(j));
                }
            }
            decoratedElements[i++] = decorators != null ? factory.createDecoratorListEvaluation(decorators) : null;
        }
        return decoratedElements;
    }

    private ObjectLiteralMemberNode enterObjectAccessorNode(PropertyNode property, boolean isClass) {
        assert property.getGetter() != null || property.getSetter() != null;
        JavaScriptNode getter = getAccessor(property.getGetter());
        JavaScriptNode setter = getAccessor(property.getSetter());
        boolean enumerable = !isClass;
        if (property.isComputed()) {
            JavaScriptNode key = transform(property.getKey());
            JavaScriptNode keyWrapper = factory.createToPropertyKey(key);
            return factory.createComputedAccessorMember(keyWrapper, property.isStatic(), enumerable, getter, setter);
        } else if (property.isPrivate()) {
            VarRef privateVar = environment.findLocalVar(property.getPrivateNameTS());
            JSWriteFrameSlotNode writePrivateNode = (JSWriteFrameSlotNode) privateVar.createWriteNode(null);
            JSFrameSlot constructorSlot = getConstructorFrameSlotForVariable(privateVar);
            return factory.createPrivateAccessorMember(property.isStatic(), getter, setter, writePrivateNode, constructorSlot.getIndex());
        } else {
            return factory.createAccessorMember(property.getKeyNameTS(), property.isStatic(), enumerable, getter, setter);
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
        VarRef classNameVarRef = null;
        if (classNameSymbol != null) {
            classNameVarRef = findScopeVar(classNameSymbol.getNameTS(), true);
            assert classNameVarRef.isFrameVar() : classNameSymbol;
            assert !classNameVarRef.hasBeenDeclared() : classNameSymbol;
            classNameVarRef.setHasBeenDeclared(true);
        }
        JavaScriptNode value = transform(propertyValue);
        if (classNameSymbol != null) {
            classNameVarRef.setHasBeenDeclared(false);
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
        if (isClass && property.isPrivate()) {
            VarRef privateVar = environment.findLocalVar(property.getPrivateNameTS());
            if (((ClassElement) property).isAutoAccessor()) {
                JSWriteFrameSlotNode writePrivateAccessor = (JSWriteFrameSlotNode) privateVar.createWriteNode(null);
                JavaScriptNode fieldStorageKey = factory.createNewPrivateName(property.getPrivateNameTS());
                JSFrameSlot constructorSlot = getConstructorFrameSlotForVariable(privateVar);
                return factory.createPrivateAutoAccessorMember(property.isStatic(), value, writePrivateAccessor, fieldStorageKey, constructorSlot.getIndex());
            } else if (property.isClassField()) {
                JSWriteFrameSlotNode writePrivateNode = (JSWriteFrameSlotNode) privateVar.createWriteNode(factory.createNewPrivateName(property.getPrivateNameTS()));
                return factory.createPrivateFieldMember(privateVar.createReadNode(), property.isStatic(), value, writePrivateNode);
            } else {
                JSWriteFrameSlotNode writePrivateNode = (JSWriteFrameSlotNode) privateVar.createWriteNode(null);
                JSFrameSlot constructorSlot = getConstructorFrameSlotForVariable(privateVar);
                return factory.createPrivateMethodMember(property.getPrivateNameTS(), property.isStatic(), value, writePrivateNode, constructorSlot.getIndex());
            }
        } else if (isClass && ((ClassElement) property).isAutoAccessor()) {
            if (property.isComputed()) {
                JavaScriptNode computedKey = transform(property.getKey());
                return factory.createComputedAutoAccessor(computedKey, property.isStatic(), enumerable, value);
            } else {
                return factory.createAutoAccessor(property.getKeyNameTS(), property.isStatic(), enumerable, value);
            }
        } else if (property.isComputed()) {
            JavaScriptNode computedKey = transform(property.getKey());
            return factory.createComputedDataMember(computedKey, property.isStatic(), enumerable, value, property.isClassField(), property.isAnonymousFunctionDefinition());
        } else if (!isClass && property.isProto()) {
            return factory.createProtoMember(property.getKeyNameTS(), property.isStatic(), value);
        } else if (isClass && property.isClassStaticBlock()) {
            return factory.createStaticBlockMember(value);
        } else {
            assert property.getKey() != null;
            return factory.createDataMember(property.getKeyNameTS(), property.isStatic(), enumerable, value, property.isClassField());
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
                            TruffleString errorVarName = ((IdentNode) catchParameter).getNameTS();
                            VarRef errorVar = environment.findLocalVar(errorVarName);
                            writeErrorVar = errorVar.createWriteNode(null);
                            if (pattern != null) {
                                // exception is being destructured
                                destructuring = transformAssignment(pattern, pattern, errorVar.createReadNode(), true);
                                destructuring = wrapTemporalDeadZoneInit(catchParamBlock.getScope(), destructuring);
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

        InternalSlotId switchVarName = makeUniqueTempVarNameForStatement("switch", switchNode.getLineNumber());
        environment.declareLocalVar(switchVarName);

        JavaScriptNode switchExpression = transform(switchNode.getExpression());
        boolean isSwitchTypeofString = isSwitchTypeofStringConstant(switchNode, switchExpression);
        if (isSwitchTypeofString) {
            switchExpression = ((TypeOfNode) switchExpression).getOperand();
        }

        VarRef switchVar = environment.findInternalSlot(switchVarName);
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
        List<JavaScriptNode> declarationList = new ArrayList<>();
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
                    if (statement instanceof VarNode) {
                        VarNode varNode = (VarNode) statement;
                        if (varNode.isHoistableDeclaration()) {
                            declarationList.add(transform(statement));
                            if (annexBBlockToFunctionTransfer(varNode)) {
                                statement = varNode.setFlag(VarNode.IS_ANNEXB_BLOCK_TO_FUNCTION_TRANSFER);
                            } else {
                                continue;
                            }
                        }
                    }
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
        return factory.createSwitch(declarationList.toArray(EMPTY_NODE_ARRAY), caseExprList.toArray(EMPTY_NODE_ARRAY), jumptable, statementList.toArray(EMPTY_NODE_ARRAY));
    }

    private JavaScriptNode createSwitchCaseExpr(boolean isSwitchTypeofString, CaseNode switchCase, JavaScriptNode readSwitchVarNode) {
        tagHiddenExpression(readSwitchVarNode);
        if (isSwitchTypeofString) {
            TruffleString typeString = (TruffleString) ((LiteralNode<?>) switchCase.getTest()).getValue();
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
            if (!(test == null || (test instanceof LiteralNode && Strings.isTString(((LiteralNode<?>) test).getValue())))) {
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
        // Store with object in synthetic block environment that can be captured by closures/eval.
        FunctionNode function = lc.getCurrentFunction();
        Environment withParentEnv = (function.hasClosures() || function.hasEval()) ? new BlockEnvironment(environment, factory, context) : environment;
        try (EnvironmentCloseable withParent = new EnvironmentCloseable(withParentEnv)) {
            JavaScriptNode withExpression = transform(withNode.getExpression());
            JavaScriptNode toObject = factory.createToObjectForWithStatement(context, withExpression);
            InternalSlotId withVarName = makeUniqueTempVarNameForStatement("with", withNode.getLineNumber());
            environment.declareInternalSlot(withVarName);
            JavaScriptNode writeWith = environment.findInternalSlot(withVarName).createWriteNode(toObject);
            JavaScriptNode withStatement;
            try (EnvironmentCloseable withEnv = enterWithEnvironment(withVarName)) {
                JavaScriptNode withBody = transform(withNode.getBody());
                withStatement = tagStatement(factory.createWith(writeWith, wrapClearAndGetCompletionValue(withBody)), withNode);
            }
            return withParent.wrapBlockScope(withStatement);
        }
    }

    private EnvironmentCloseable enterWithEnvironment(Object withVarName) {
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
        Scope classHeadScope = classNode.getClassHeadScope();
        Scope classBodyScope = classNode.getScope();
        boolean needsScope = classHeadScope.hasDeclarations() || classBodyScope.hasDeclarations();
        try (EnvironmentCloseable blockEnv = new EnvironmentCloseable(needsScope ? newClassEnvironment(classHeadScope) : environment)) {
            TruffleString className = null;
            Symbol classNameSymbol = null;
            IdentNode ident = classNode.getIdent();
            if (ident != null) {
                className = ident.getNameTS();
                classNameSymbol = classHeadScope.getExistingSymbol(ident.getName());
            }

            JavaScriptNode classHeritage = transform(classNode.getClassHeritage());

            JavaScriptNode classDefinition;
            try (EnvironmentCloseable privateEnv = new EnvironmentCloseable(newPrivateEnvironment(classBodyScope))) {
                JavaScriptNode classFunction = transform(classNode.getConstructor().getValue());

                List<ObjectLiteralMemberNode> members = transformPropertyDefinitionList(classNode.getClassElements(), true, classNameSymbol);

                DecoratorListEvaluationNode[] decoratedElementNodes = classNode.hasClassElementDecorators() ? transformClassElementsDecorators(classNode.getClassElements()) : null;
                JavaScriptNode[] classDecorators = transformClassDecorators(classNode.getDecorators());

                JSWriteFrameSlotNode writeClassBinding = className == null ? null : (JSWriteFrameSlotNode) findScopeVar(className, true).createWriteNode(null);

                // internal constructor binding used for private brand checks.
                JSWriteFrameSlotNode writeInternalConstructorBrand = classNode.hasPrivateMethods()
                                ? (JSWriteFrameSlotNode) environment.findLocalVar(ClassNode.PRIVATE_CONSTRUCTOR_BINDING_NAME).createWriteNode(null)
                                : null;

                classDefinition = factory.createClassDefinition(context, (JSFunctionExpressionNode) classFunction, classHeritage,
                                members.toArray(ObjectLiteralMemberNode.EMPTY), writeClassBinding, writeInternalConstructorBrand,
                                classDecorators, decoratedElementNodes, className,
                                classNode.getInstanceElementCount(), classNode.getStaticElementCount(), classNode.hasPrivateInstanceMethods(), classNode.hasInstanceFieldsOrAccessors(),
                                environment.getCurrentBlockScopeSlot());
                classDefinition = privateEnv.wrapBlockScope(classDefinition);
            }

            if (ident != null) {
                classDefinition = wrapTemporalDeadZoneInit(classHeadScope, classDefinition);
            }
            return tagExpression(blockEnv.wrapBlockScope(classDefinition), classNode);
        }
    }

    private JavaScriptNode[] transformClassDecorators(List<Expression> decorators) {
        if (decorators == null) {
            return EMPTY_NODE_ARRAY;
        }
        JavaScriptNode[] transformedDecorators = new JavaScriptNode[decorators.size()];
        int i = 0;
        for (Expression e : decorators) {
            transformedDecorators[i++] = transform(e);
        }
        return transformedDecorators;
    }

    private Environment newClassEnvironment(Scope scope) {
        assert scope.isClassHeadScope();
        BlockEnvironment classEnv = new BlockEnvironment(environment, factory, context, scope);
        classEnv.addFrameSlotsFromSymbols(scope.getSymbols());
        return classEnv;
    }

    private Environment newPrivateEnvironment(Scope scope) {
        assert scope.isClassBodyScope();
        if (scope.hasPrivateNames()) {
            PrivateEnvironment privateEnv = new PrivateEnvironment(environment, factory, context, scope);
            privateEnv.addFrameSlotsFromSymbols(scope.getSymbols());
            return privateEnv;
        }
        return environment;
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
            valueNode = factory.createAccessRestArgument(context, currentFunction.getLeadingArgumentCount() + paramNode.getIndex());
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

    private InternalSlotId makeUniqueTempVarNameForStatement(String prefix, int lineNumber) {
        InternalSlotId name = factory.createInternalSlotId(prefix, lineNumber);
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
                    if (blockEnv.hasScopeFrame()) {
                        return factory.createBlockScope(block, blockEnv.getCurrentBlockScopeSlot(), blockEnv.getBlockFrameDescriptor().toFrameDescriptor(),
                                        blockEnv.getParentSlot(), blockEnv.isFunctionBlock(), blockEnv.capturesFunctionFrame(), blockEnv.isGeneratorFunctionBlock(), blockEnv.getScopeLevel() > 1,
                                        blockEnv.getStart(), blockEnv.getEnd());
                    } else if (blockEnv.getStart() < blockEnv.getEnd() && !blockEnv.isFunctionBlock()) {
                        return factory.createVirtualBlockScope(block, blockEnv.getStart(), blockEnv.getEnd());
                    }
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

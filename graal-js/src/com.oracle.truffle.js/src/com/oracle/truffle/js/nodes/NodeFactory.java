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
package com.oracle.truffle.js.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.js.parser.ir.Module.ImportPhase;
import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.annotations.GenerateDecoder;
import com.oracle.truffle.js.annotations.GenerateProxy;
import com.oracle.truffle.js.decorators.DecoratorListEvaluationNode;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode;
import com.oracle.truffle.js.nodes.access.AsyncIteratorNextNode;
import com.oracle.truffle.js.nodes.access.ClearFrameSlotsNode;
import com.oracle.truffle.js.nodes.access.CompoundWriteElementNode;
import com.oracle.truffle.js.nodes.access.ConstantVariableWriteNode;
import com.oracle.truffle.js.nodes.access.DebugScopeVarWrapperNode;
import com.oracle.truffle.js.nodes.access.DeclareGlobalFunctionNode;
import com.oracle.truffle.js.nodes.access.DeclareGlobalLexicalVariableNode;
import com.oracle.truffle.js.nodes.access.DeclareGlobalNode;
import com.oracle.truffle.js.nodes.access.DeclareGlobalVariableNode;
import com.oracle.truffle.js.nodes.access.EnumerateNode;
import com.oracle.truffle.js.nodes.access.GetAsyncIteratorNode;
import com.oracle.truffle.js.nodes.access.GetIteratorUnaryNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.GetTemplateObjectNode;
import com.oracle.truffle.js.nodes.access.GlobalDeclarationInstantiationNode;
import com.oracle.truffle.js.nodes.access.GlobalObjectNode;
import com.oracle.truffle.js.nodes.access.GlobalPropertyNode;
import com.oracle.truffle.js.nodes.access.GlobalScopeNode;
import com.oracle.truffle.js.nodes.access.GlobalScopeVarWrapperNode;
import com.oracle.truffle.js.nodes.access.InitializeInstanceElementsNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteUnaryNode;
import com.oracle.truffle.js.nodes.access.IteratorGetNextValueNode;
import com.oracle.truffle.js.nodes.access.IteratorIsDoneNode;
import com.oracle.truffle.js.nodes.access.IteratorNextUnaryNode;
import com.oracle.truffle.js.nodes.access.IteratorSetDoneNode;
import com.oracle.truffle.js.nodes.access.IteratorToArrayNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantStringNode;
import com.oracle.truffle.js.nodes.access.JSGuardDisconnectedArgumentRead;
import com.oracle.truffle.js.nodes.access.JSGuardDisconnectedArgumentWrite;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.access.JSTargetableWrapperNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.LocalVarIncNode;
import com.oracle.truffle.js.nodes.access.NewPrivateNameNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.MakeMethodNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.ObjectLiteralMemberNode;
import com.oracle.truffle.js.nodes.access.OptionalChainNode;
import com.oracle.truffle.js.nodes.access.PrivateBrandCheckNode;
import com.oracle.truffle.js.nodes.access.PrivateFieldGetNode;
import com.oracle.truffle.js.nodes.access.PrivateFieldSetNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.RegExpLiteralNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNode.RequireObjectCoercibleWrapperNode;
import com.oracle.truffle.js.nodes.access.RestObjectNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.access.SuperPropertyReferenceNode;
import com.oracle.truffle.js.nodes.access.WithTargetNode;
import com.oracle.truffle.js.nodes.access.WithVarWrapperNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.access.WritePropertyNode;
import com.oracle.truffle.js.nodes.arguments.AccessArgumentsArrayDirectlyNode;
import com.oracle.truffle.js.nodes.arguments.AccessDerivedConstructorThisNode;
import com.oracle.truffle.js.nodes.arguments.AccessFrameArgumentNode;
import com.oracle.truffle.js.nodes.arguments.AccessFunctionNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.arguments.AccessLevelFunctionNode;
import com.oracle.truffle.js.nodes.arguments.AccessLexicalThisNode;
import com.oracle.truffle.js.nodes.arguments.AccessRestArgumentsNode;
import com.oracle.truffle.js.nodes.arguments.AccessThisNode;
import com.oracle.truffle.js.nodes.arguments.AccessVarArgsNode;
import com.oracle.truffle.js.nodes.arguments.ArgumentsObjectNode;
import com.oracle.truffle.js.nodes.binary.DualNode;
import com.oracle.truffle.js.nodes.binary.InNode;
import com.oracle.truffle.js.nodes.binary.InstanceofNode;
import com.oracle.truffle.js.nodes.binary.JSAddNode;
import com.oracle.truffle.js.nodes.binary.JSAndNode;
import com.oracle.truffle.js.nodes.binary.JSBitwiseAndNode;
import com.oracle.truffle.js.nodes.binary.JSBitwiseOrNode;
import com.oracle.truffle.js.nodes.binary.JSBitwiseXorNode;
import com.oracle.truffle.js.nodes.binary.JSDivideNode;
import com.oracle.truffle.js.nodes.binary.JSEqualNode;
import com.oracle.truffle.js.nodes.binary.JSExponentiateNode;
import com.oracle.truffle.js.nodes.binary.JSGreaterOrEqualNode;
import com.oracle.truffle.js.nodes.binary.JSGreaterThanNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.binary.JSLeftShiftNode;
import com.oracle.truffle.js.nodes.binary.JSLessOrEqualNode;
import com.oracle.truffle.js.nodes.binary.JSLessThanNode;
import com.oracle.truffle.js.nodes.binary.JSModuloNode;
import com.oracle.truffle.js.nodes.binary.JSMultiplyNode;
import com.oracle.truffle.js.nodes.binary.JSNullishCoalescingNode;
import com.oracle.truffle.js.nodes.binary.JSOrNode;
import com.oracle.truffle.js.nodes.binary.JSRightShiftNode;
import com.oracle.truffle.js.nodes.binary.JSSubtractNode;
import com.oracle.truffle.js.nodes.binary.JSTypeofIdenticalNode;
import com.oracle.truffle.js.nodes.binary.JSUnsignedRightShiftNode;
import com.oracle.truffle.js.nodes.binary.PrivateFieldInNode;
import com.oracle.truffle.js.nodes.cast.JSPrepareThisNode;
import com.oracle.truffle.js.nodes.cast.JSToNumericNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode.JSToPropertyKeyWrapperNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode.JSToStringWrapperNode;
import com.oracle.truffle.js.nodes.cast.WithStatementToObjectNode;
import com.oracle.truffle.js.nodes.control.AbstractBlockNode;
import com.oracle.truffle.js.nodes.control.AsyncFunctionBodyNode;
import com.oracle.truffle.js.nodes.control.AsyncGeneratorBodyNode;
import com.oracle.truffle.js.nodes.control.AsyncGeneratorYieldNode;
import com.oracle.truffle.js.nodes.control.AsyncIteratorCloseWrapperNode;
import com.oracle.truffle.js.nodes.control.AwaitNode;
import com.oracle.truffle.js.nodes.control.BreakNode;
import com.oracle.truffle.js.nodes.control.BreakTarget;
import com.oracle.truffle.js.nodes.control.ContinueNode;
import com.oracle.truffle.js.nodes.control.ContinueTarget;
import com.oracle.truffle.js.nodes.control.ContinueTargetNode;
import com.oracle.truffle.js.nodes.control.DebuggerNode;
import com.oracle.truffle.js.nodes.control.DeletePropertyNode;
import com.oracle.truffle.js.nodes.control.DirectBreakTargetNode;
import com.oracle.truffle.js.nodes.control.EmptyNode;
import com.oracle.truffle.js.nodes.control.ExprBlockNode;
import com.oracle.truffle.js.nodes.control.ForNode;
import com.oracle.truffle.js.nodes.control.GeneratorBodyNode;
import com.oracle.truffle.js.nodes.control.GeneratorExprBlockNode;
import com.oracle.truffle.js.nodes.control.GeneratorVoidBlockNode;
import com.oracle.truffle.js.nodes.control.IfNode;
import com.oracle.truffle.js.nodes.control.IteratorCloseWrapperNode;
import com.oracle.truffle.js.nodes.control.LabelNode;
import com.oracle.truffle.js.nodes.control.ModuleBodyNode;
import com.oracle.truffle.js.nodes.control.ModuleInitializeEnvironmentNode;
import com.oracle.truffle.js.nodes.control.ModuleYieldNode;
import com.oracle.truffle.js.nodes.control.ResumableNode;
import com.oracle.truffle.js.nodes.control.ReturnNode;
import com.oracle.truffle.js.nodes.control.ReturnTargetNode;
import com.oracle.truffle.js.nodes.control.RuntimeErrorNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.nodes.control.SwitchNode;
import com.oracle.truffle.js.nodes.control.ThrowNode;
import com.oracle.truffle.js.nodes.control.TopLevelAwaitModuleBodyNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.control.TryFinallyNode;
import com.oracle.truffle.js.nodes.control.VoidBlockNode;
import com.oracle.truffle.js.nodes.control.WhileNode;
import com.oracle.truffle.js.nodes.control.WithNode;
import com.oracle.truffle.js.nodes.control.YieldNode;
import com.oracle.truffle.js.nodes.function.AbstractBodyNode;
import com.oracle.truffle.js.nodes.function.AbstractFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.BlockScopeNode;
import com.oracle.truffle.js.nodes.function.CallApplyArgumentsNode;
import com.oracle.truffle.js.nodes.function.ClassDefinitionNode;
import com.oracle.truffle.js.nodes.function.ConstructorResultNode;
import com.oracle.truffle.js.nodes.function.ConstructorRootNode;
import com.oracle.truffle.js.nodes.function.DefaultDerivedConstructorSuperCallNode;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.function.IterationScopeNode;
import com.oracle.truffle.js.nodes.function.JSFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.function.JSFunctionExpressionNode;
import com.oracle.truffle.js.nodes.function.JSNewNode;
import com.oracle.truffle.js.nodes.function.NamedEvaluationNode;
import com.oracle.truffle.js.nodes.function.NewTargetRootNode;
import com.oracle.truffle.js.nodes.function.SpreadArgumentNode;
import com.oracle.truffle.js.nodes.module.ImportMetaNode;
import com.oracle.truffle.js.nodes.module.ReadImportBindingNode;
import com.oracle.truffle.js.nodes.module.ResolveNamedImportNode;
import com.oracle.truffle.js.nodes.module.ResolveSourceImportNode;
import com.oracle.truffle.js.nodes.module.ResolveStarImportNode;
import com.oracle.truffle.js.nodes.promise.ImportCallNode;
import com.oracle.truffle.js.nodes.unary.JSComplementNode;
import com.oracle.truffle.js.nodes.unary.JSNotNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryMinusNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryPlusNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.nodes.unary.VoidNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.InternalSlotId;

@GenerateDecoder
@GenerateProxy
public class NodeFactory {

    private static final NodeFactory FACTORY = new NodeFactory();

    public enum BinaryOperation {
        ADD,
        DIVIDE,
        MODULO,
        MULTIPLY,
        EXPONENTIATE,
        SUBTRACT,
        EQUAL,
        GREATER_OR_EQUAL,
        GREATER,
        IDENTICAL,
        LESS_OR_EQUAL,
        LESS,
        NOT_EQUAL,
        NOT_IDENTICAL,
        BITWISE_XOR,
        BITWISE_AND,
        BITWISE_OR,
        BITWISE_LEFT_SHIFT,
        BITWISE_RIGHT_SHIFT,
        BITWISE_UNSIGNED_RIGHT_SHIFT,
        LOGICAL_AND,
        LOGICAL_OR,
        INSTANCEOF,
        IN,
        DUAL,
        NULLISH_COALESCING
    }

    public enum UnaryOperation {
        MINUS,
        PLUS,
        BITWISE_COMPLEMENT,
        NOT,
        POSTFIX_LOCAL_INCREMENT,
        PREFIX_LOCAL_INCREMENT,
        POSTFIX_LOCAL_DECREMENT,
        PREFIX_LOCAL_DECREMENT,
        TYPE_OF,
        VOID
    }

    public JavaScriptNode createUnary(UnaryOperation operation, JavaScriptNode operand) {
        switch (operation) {
            case BITWISE_COMPLEMENT:
                return JSComplementNode.create(operand);
            case MINUS:
                return JSUnaryMinusNode.create(operand);
            case PLUS:
                return JSUnaryPlusNode.create(operand);
            case NOT:
                return JSNotNode.create(operand);
            case TYPE_OF:
                return TypeOfNode.create(operand);
            case VOID:
                return VoidNode.create(operand);
            default:
                throw new IllegalArgumentException();
        }
    }

    public JavaScriptNode createLocalVarInc(UnaryOperation operation, JSFrameSlot frameSlot, boolean hasTemporalDeadZone, ScopeFrameNode scopeFrameNode) {
        switch (operation) {
            case POSTFIX_LOCAL_INCREMENT:
                return LocalVarIncNode.createPostfix(LocalVarIncNode.Op.Inc, frameSlot, hasTemporalDeadZone, scopeFrameNode);
            case PREFIX_LOCAL_INCREMENT:
                return LocalVarIncNode.createPrefix(LocalVarIncNode.Op.Inc, frameSlot, hasTemporalDeadZone, scopeFrameNode);
            case POSTFIX_LOCAL_DECREMENT:
                return LocalVarIncNode.createPostfix(LocalVarIncNode.Op.Dec, frameSlot, hasTemporalDeadZone, scopeFrameNode);
            case PREFIX_LOCAL_DECREMENT:
                return LocalVarIncNode.createPrefix(LocalVarIncNode.Op.Dec, frameSlot, hasTemporalDeadZone, scopeFrameNode);
            default:
                throw new IllegalArgumentException();
        }
    }

    public JavaScriptNode createToNumericOperand(JavaScriptNode operand) {
        return JSToNumericNode.createToNumericOperand(operand);
    }

    public JavaScriptNode createDual(JSContext context, JavaScriptNode left, JavaScriptNode right) {
        if (left instanceof EmptyNode) {
            return right;
        }
        return createBinary(context, BinaryOperation.DUAL, left, right);
    }

    public JavaScriptNode createBinary(JSContext context, BinaryOperation operation, JavaScriptNode left, JavaScriptNode right) {
        switch (operation) {
            case ADD:
                return JSAddNode.create(left, right, false);
            case SUBTRACT:
                return JSSubtractNode.create(left, right, false);
            case MULTIPLY:
                return JSMultiplyNode.create(left, right);
            case EXPONENTIATE:
                return JSExponentiateNode.create(left, right);
            case DIVIDE:
                return JSDivideNode.create(left, right);
            case MODULO:
                return JSModuloNode.create(left, right);
            case EQUAL:
                return createBinaryEqual(left, right);
            case GREATER:
                return JSGreaterThanNode.create(left, right);
            case GREATER_OR_EQUAL:
                return JSGreaterOrEqualNode.create(left, right);
            case IDENTICAL:
                return createBinaryIdentical(left, right);
            case LESS:
                return JSLessThanNode.create(left, right);
            case LESS_OR_EQUAL:
                return JSLessOrEqualNode.create(left, right);
            case NOT_EQUAL:
                return JSNotNode.create(createBinaryEqual(left, right));
            case NOT_IDENTICAL:
                return JSNotNode.create(createBinaryIdentical(left, right));
            case LOGICAL_AND:
                return JSAndNode.create(left, right);
            case LOGICAL_OR:
                return JSOrNode.create(left, right);
            case NULLISH_COALESCING:
                return JSNullishCoalescingNode.create(left, right);
            case BITWISE_AND:
                return JSBitwiseAndNode.create(left, right);
            case BITWISE_OR:
                return JSBitwiseOrNode.create(left, right);
            case BITWISE_XOR:
                return JSBitwiseXorNode.create(left, right);
            case BITWISE_LEFT_SHIFT:
                return JSLeftShiftNode.create(left, right);
            case BITWISE_RIGHT_SHIFT:
                return JSRightShiftNode.create(left, right);
            case BITWISE_UNSIGNED_RIGHT_SHIFT:
                return JSUnsignedRightShiftNode.create(left, right);
            case INSTANCEOF:
                return InstanceofNode.create(context, left, right);
            case IN:
                return InNode.create(context, left, right);
            case DUAL:
                return DualNode.create(left, right);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static JavaScriptNode createBinaryIdentical(JavaScriptNode left, JavaScriptNode right) {
        JavaScriptNode node = createIdenticalSpecial(left, right);
        if (node != null) {
            return node;
        }
        return JSIdenticalNode.create(left, right);
    }

    private static JavaScriptNode createBinaryEqual(JavaScriptNode left, JavaScriptNode right) {
        JavaScriptNode node = createIdenticalSpecial(left, right);
        if (node != null) {
            return node;
        }
        return JSEqualNode.create(left, right);
    }

    private static JavaScriptNode createIdenticalSpecial(JavaScriptNode left, JavaScriptNode right) {
        if (left instanceof TypeOfNode && right instanceof JSConstantStringNode) {
            return JSTypeofIdenticalNode.create(((TypeOfNode) left).getOperand(), (JSConstantStringNode) right);
        } else if (right instanceof TypeOfNode && left instanceof JSConstantStringNode) {
            return JSTypeofIdenticalNode.create(((TypeOfNode) right).getOperand(), (JSConstantStringNode) left);
        }
        return null;
    }

    public JavaScriptNode createTypeofIdentical(JavaScriptNode subject, TruffleString typeString) {
        return JSTypeofIdenticalNode.create(subject, typeString);
    }

    public JavaScriptNode createLogicalOr(JavaScriptNode left, JavaScriptNode right) {
        return JSOrNode.create(left, right);
    }

    public JavaScriptNode createNotUndefinedOr(JavaScriptNode left, JavaScriptNode right) {
        return JSOrNode.createNotUndefinedOr(left, right);
    }

    public JavaScriptNode createConstant(Object value) {
        return JSConstantNode.create(value);
    }

    public JavaScriptNode createConstantBoolean(boolean value) {
        return JSConstantNode.createBoolean(value);
    }

    public JavaScriptNode createConstantInteger(int value) {
        return JSConstantNode.createInt(value);
    }

    public JavaScriptNode createConstantSafeInteger(long value) {
        return JSConstantNode.createSafeInteger(SafeInteger.valueOf(value));
    }

    public JavaScriptNode createConstantNumericUnit() {
        return JSConstantNode.createConstantNumericUnit();
    }

    public JavaScriptNode createConstantDouble(double value) {
        return JSConstantNode.createDouble(value);
    }

    public JavaScriptNode createConstantString(TruffleString value) {
        return JSConstantNode.createString(value);
    }

    public JavaScriptNode createConstantUndefined() {
        return JSConstantNode.createUndefined();
    }

    public JavaScriptNode createConstantNull() {
        return JSConstantNode.createNull();
    }

    public IfNode createIf(JavaScriptNode condition, JavaScriptNode pass, JavaScriptNode fail) {
        return IfNode.create(condition, pass, fail);
    }

    // ##### Control nodes

    public SwitchNode createSwitch(JavaScriptNode[] declarations, JavaScriptNode[] caseExpressions, int[] jumptable, JavaScriptNode[] statements) {
        return SwitchNode.create(declarations, caseExpressions, jumptable, statements);
    }

    public LoopNode createLoopNode(RepeatingNode repeatingNode) {
        return Truffle.getRuntime().createLoopNode(repeatingNode);
    }

    public RepeatingNode createWhileDoRepeatingNode(JavaScriptNode condition, JavaScriptNode body) {
        return WhileNode.createWhileDoRepeatingNode(condition, body);
    }

    public JavaScriptNode createWhileDo(LoopNode loopNode) {
        return WhileNode.createWhileDo(loopNode);
    }

    public AbstractBlockNode fixBlockNodeChild(AbstractBlockNode blockNode, int index, JavaScriptNode newChild) {
        assert blockNode.getStatements()[index] != newChild;
        blockNode.getStatements()[index] = newChild;
        return blockNode;
    }

    public Node fixNodeChild(Node parent, Node child, Node newChild) {
        assert child != newChild;
        boolean ok = NodeUtil.replaceChild(parent, child, newChild);
        assert ok;
        return parent;
    }

    public RepeatingNode createDoWhileRepeatingNode(JavaScriptNode condition, JavaScriptNode body) {
        return WhileNode.createDoWhileRepeatingNode(condition, body);
    }

    public JavaScriptNode createDoWhile(LoopNode loopNode) {
        return WhileNode.createDoWhile(loopNode);
    }

    public JavaScriptNode createDesugaredFor(LoopNode loopNode) {
        return WhileNode.createDesugaredFor(loopNode);
    }

    public JavaScriptNode createDesugaredForOf(LoopNode loopNode) {
        return WhileNode.createDesugaredForOf(loopNode);
    }

    public JavaScriptNode createDesugaredForIn(LoopNode loopNode) {
        return WhileNode.createDesugaredForIn(loopNode);
    }

    public JavaScriptNode createDesugaredForAwaitOf(LoopNode loopNode) {
        return WhileNode.createDesugaredForAwaitOf(loopNode);
    }

    public RepeatingNode createForRepeatingNode(JavaScriptNode condition, JavaScriptNode body, JavaScriptNode modify, FrameDescriptor frameDescriptor, JavaScriptNode isFirstNode,
                    JavaScriptNode setNotFirstNode, JSFrameSlot blockScopeSlot) {
        IterationScopeNode perIterationScope = createIterationScope(frameDescriptor, blockScopeSlot);
        return ForNode.createForRepeatingNode(condition, body, modify, perIterationScope, isFirstNode, setNotFirstNode);
    }

    public StatementNode createFor(LoopNode loopNode) {
        return ForNode.createFor(loopNode);
    }

    public IterationScopeNode createIterationScope(FrameDescriptor frameDescriptor, JSFrameSlot blockScopeSlot) {
        int numberOfSlots = frameDescriptor.getNumberOfSlots();
        assert numberOfSlots > ScopeFrameNode.PARENT_SCOPE_SLOT_INDEX && ScopeFrameNode.PARENT_SCOPE_IDENTIFIER.equals(frameDescriptor.getSlotName(ScopeFrameNode.PARENT_SCOPE_SLOT_INDEX));
        int numberOfSlotsToCopy = numberOfSlots - 1;
        JSReadFrameSlotNode[] reads = new JSReadFrameSlotNode[numberOfSlotsToCopy];
        JSWriteFrameSlotNode[] writes = new JSWriteFrameSlotNode[numberOfSlotsToCopy];
        int slotIndex = 0;
        for (int i = 0; i < numberOfSlots; i++) {
            if (i == ScopeFrameNode.PARENT_SCOPE_SLOT_INDEX) {
                continue;
            }
            JSFrameSlot slot = JSFrameSlot.fromIndexedFrameSlot(frameDescriptor, i);
            reads[slotIndex] = JSReadFrameSlotNode.create(slot, false);
            writes[slotIndex] = JSWriteFrameSlotNode.create(slot, null, false);
            slotIndex++;
        }
        assert slotIndex == numberOfSlotsToCopy;
        return IterationScopeNode.create(frameDescriptor, reads, writes, blockScopeSlot.getIndex());
    }

    public BreakNode createBreak(BreakTarget breakTarget) {
        return BreakNode.create(breakTarget);
    }

    public ContinueNode createContinue(ContinueTarget continueTarget) {
        return ContinueNode.create(continueTarget);
    }

    public LabelNode createLabel(JavaScriptNode block, BreakTarget target) {
        return LabelNode.create(block, target);
    }

    public JavaScriptNode createEmpty() {
        return EmptyNode.create();
    }

    public JavaScriptNode createVoidBlock(JavaScriptNode... statements) {
        return VoidBlockNode.createVoidBlock(statements);
    }

    public JavaScriptNode createExprBlock(JavaScriptNode... statements) {
        return ExprBlockNode.createExprBlock(statements);
    }

    public ReturnTargetNode createReturnTarget(JavaScriptNode body) {
        return ReturnTargetNode.create(body);
    }

    public ReturnTargetNode createFrameReturnTarget(JavaScriptNode body, JavaScriptNode returnValue) {
        return ReturnTargetNode.createFrameReturnTarget(body, returnValue);
    }

    public ContinueTargetNode createContinueTarget(JavaScriptNode block, ContinueTarget continueTarget) {
        return ContinueTargetNode.create(block, continueTarget);
    }

    public DirectBreakTargetNode createDirectBreakTarget(JavaScriptNode block) {
        return DirectBreakTargetNode.create(block);
    }

    public JavaScriptNode createDebugger() {
        return DebuggerNode.create();
    }

    public JavaScriptNode createLocal(JSFrameSlot frameSlot, int frameLevel, int scopeLevel) {
        return createReadFrameSlot(frameSlot, createScopeFrame(frameLevel, scopeLevel, null), false);
    }

    public JavaScriptNode createReadFrameSlot(JSFrameSlot frameSlot, ScopeFrameNode scope) {
        return createReadFrameSlot(frameSlot, scope, false);
    }

    public JavaScriptNode createReadFrameSlot(JSFrameSlot frameSlot, ScopeFrameNode scope, boolean hasTemporalDeadZone) {
        return JSReadFrameSlotNode.create(frameSlot, scope, hasTemporalDeadZone);
    }

    public JavaScriptNode createReadCurrentFrameSlot(JSFrameSlot frameSlot) {
        return JSReadFrameSlotNode.create(frameSlot, false);
    }

    public JSWriteFrameSlotNode createWriteFrameSlot(JSFrameSlot frameSlot, ScopeFrameNode scope, JavaScriptNode rhs) {
        return JSWriteFrameSlotNode.create(frameSlot, scope, rhs, false);
    }

    public JSWriteFrameSlotNode createWriteFrameSlot(JSFrameSlot frameSlot, ScopeFrameNode scope, JavaScriptNode rhs, boolean hasTemporalDeadZone) {
        return JSWriteFrameSlotNode.create(frameSlot, scope, rhs, hasTemporalDeadZone);
    }

    public JSWriteFrameSlotNode createWriteCurrentFrameSlot(JSFrameSlot frameSlot, JavaScriptNode rhs) {
        return JSWriteFrameSlotNode.create(frameSlot, rhs, false);
    }

    public ScopeFrameNode createScopeFrame(int frameLevel, int scopeLevel, JSFrameSlot blockScopeSlot) {
        return ScopeFrameNode.create(frameLevel, scopeLevel, blockScopeSlot);
    }

    public JavaScriptNode createReadLexicalGlobal(TruffleString name, boolean hasTemporalDeadZone, JSContext context) {
        return GlobalPropertyNode.createLexicalGlobal(context, name, hasTemporalDeadZone);
    }

    public JavaScriptNode createGlobalScope(JSContext context) {
        return GlobalScopeNode.create(context);
    }

    public JavaScriptNode createGlobalScopeTDZCheck(JSContext context, TruffleString name, boolean checkTDZ) {
        if (!checkTDZ) {
            return createGlobalScope(context);
        }
        return GlobalScopeNode.createWithTDZCheck(context, name);
    }

    public JavaScriptNode createGlobalVarWrapper(TruffleString varName, JavaScriptNode defaultDelegate, JavaScriptNode dynamicScope, JSTargetableNode scopeAccessNode) {
        return new GlobalScopeVarWrapperNode(varName, defaultDelegate, dynamicScope, scopeAccessNode);
    }

    /** Check if the indices can be represented as closed range [slots[from], slots[to - 1]]. */
    private static boolean isIndexRange(int[] slots, int from, int to) {
        if (to - from >= 2) {
            for (int i = from + 1; i < to; i++) {
                if (slots[i - 1] != slots[i] - 1) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public JavaScriptNode createClearFrameSlots(ScopeFrameNode scope, int[] slots) {
        return ClearFrameSlotsNode.create(scope, slots);
    }

    public JavaScriptNode createClearFrameSlotRange(ScopeFrameNode scope, int start, int end) {
        return ClearFrameSlotsNode.createRange(scope, start, end);
    }

    public final JavaScriptNode createClearFrameSlots(ScopeFrameNode scope, int[] slots, int from, int to) {
        if (isIndexRange(slots, from, to)) {
            return createClearFrameSlotRange(scope, slots[from], slots[to - 1] + 1);
        }
        return createClearFrameSlots(scope, (from == 0 && to == slots.length) ? slots : Arrays.copyOfRange(slots, from, to));
    }

    public JavaScriptNode createThrow(JSContext context, JavaScriptNode expression) {
        return ThrowNode.create(expression, context);
    }

    public JavaScriptNode createTryCatch(JSContext context, JavaScriptNode tryNode, JavaScriptNode catchBlock, JavaScriptNode writeErrorVar, BlockScopeNode blockScope,
                    JavaScriptNode destructuring, JavaScriptNode conditionExpression) {
        return TryCatchNode.create(context, tryNode, catchBlock, (JSWriteFrameSlotNode) writeErrorVar, blockScope, destructuring, conditionExpression);
    }

    public JavaScriptNode createTryFinally(JavaScriptNode tryNode, JavaScriptNode finallyBlock) {
        return TryFinallyNode.create(tryNode, finallyBlock);
    }

    public JavaScriptNode createFunctionCall(@SuppressWarnings("unused") JSContext context, JavaScriptNode expression, JavaScriptNode[] arguments) {
        if (expression instanceof PropertyNode || expression instanceof ReadElementNode || expression instanceof WithVarWrapperNode || expression instanceof PrivateFieldGetNode ||
                        expression instanceof OptionalChainNode.ShortCircuitTargetableNode || expression instanceof OptionalChainNode.OptionalTargetableNode) {
            assert !(expression instanceof PropertyNode) || ((PropertyNode) expression).isMethod();
            return JSFunctionCallNode.createInvoke((JSTargetableNode) expression, arguments, false, false);
        } else if (expression instanceof JSTargetableWrapperNode) {
            JavaScriptNode function = ((JSTargetableWrapperNode) expression).getDelegate();
            JavaScriptNode target = ((JSTargetableWrapperNode) expression).getTarget();
            return JSFunctionCallNode.createCall(function, target, arguments, false, false);
        } else {
            assert expression != null;
            JavaScriptNode target = null;
            JavaScriptNode function = expression;
            if (function instanceof GlobalPropertyNode) {
                ((GlobalPropertyNode) function).setMethod();
            } else if (function instanceof GlobalScopeVarWrapperNode) {
                ((GlobalScopeVarWrapperNode) function).setMethod();
            }
            return JSFunctionCallNode.createCall(function, target, arguments, false, false);
        }
    }

    public JavaScriptNode createFunctionCallWithNewTarget(@SuppressWarnings("unused") JSContext context, JavaScriptNode expression, JavaScriptNode[] arguments) {
        assert expression instanceof JSTargetableWrapperNode;
        JavaScriptNode function = ((JSTargetableWrapperNode) expression).getDelegate();
        JavaScriptNode target = ((JSTargetableWrapperNode) expression).getTarget();
        return JSFunctionCallNode.createCall(function, target, arguments, false, true);
    }

    public AbstractFunctionArgumentsNode createFunctionArguments(JSContext context, JavaScriptNode[] arguments) {
        return JSFunctionArgumentsNode.create(context, arguments);
    }

    public JavaScriptNode createNew(JSContext context, JavaScriptNode function, AbstractFunctionArgumentsNode arguments) {
        assert !(function instanceof PropertyNode) || !((PropertyNode) function).isMethod();
        return JSNewNode.create(context, function, arguments);
    }

    // ##### Argument nodes

    public JavaScriptNode createAccessThis() {
        return AccessThisNode.create();
    }

    public JavaScriptNode createAccessCallee(int level) {
        if (level == 0) {
            return AccessFunctionNode.create();
        } else {
            return AccessLevelFunctionNode.create(level);
        }
    }

    public JavaScriptNode createAccessLexicalThis() {
        return AccessLexicalThisNode.create(createAccessCallee(0));
    }

    public JavaScriptNode createAccessArgument(int index) {
        return AccessIndexedArgumentNode.create(index);
    }

    public JavaScriptNode createAccessVarArgs(int startIndex) {
        return AccessVarArgsNode.create(startIndex);
    }

    public JavaScriptNode createAccessRestArgument(JSContext context, int index) {
        return AccessRestArgumentsNode.create(context, index);
    }

    public JavaScriptNode createAccessNewTarget() {
        return AccessIndexedArgumentNode.create(0);
    }

    public JavaScriptNode createAccessFrameArgument(ScopeFrameNode accessFrame, int argIndex) {
        return AccessFrameArgumentNode.create(accessFrame, argIndex);
    }

    public JavaScriptNode createAccessHomeObject(JSContext context) {
        return PropertyNode.createGetHidden(context, createAccessCallee(0), JSFunction.HOME_OBJECT_ID);
    }

    // ##### Element nodes

    public ReadElementNode createReadElementNode(JSContext context, JavaScriptNode target, JavaScriptNode element) {
        return ReadElementNode.create(target, element, context);
    }

    public WriteElementNode createWriteElementNode(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSContext context, boolean isStrict) {
        return WriteElementNode.create(targetNode, indexNode, valueNode, context, isStrict);
    }

    public WriteElementNode createCompoundWriteElementNode(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSWriteFrameSlotNode writeIndex, JSContext context,
                    boolean isStrict) {
        return CompoundWriteElementNode.create(targetNode, indexNode, valueNode, writeIndex, context, isStrict);
    }

    // ##### Property nodes

    public JSTargetableNode createReadProperty(JSContext context, JavaScriptNode base, TruffleString propertyName) {
        return PropertyNode.createProperty(context, base, propertyName);
    }

    public JSTargetableNode createReadProperty(JSContext context, JavaScriptNode base, TruffleString propertyName, boolean method) {
        return PropertyNode.createProperty(context, base, propertyName, method);
    }

    public WritePropertyNode createWriteProperty(JavaScriptNode target, TruffleString name, JavaScriptNode rhs, JSContext context, boolean strictMode) {
        return WritePropertyNode.create(target, name, rhs, context, strictMode);
    }

    public WritePropertyNode createWriteProperty(JavaScriptNode target, TruffleString name, JavaScriptNode rhs, JSContext context, boolean isStrict, boolean isGlobal, boolean verifyHasProperty) {
        return WritePropertyNode.create(target, name, rhs, context, isStrict, isGlobal, verifyHasProperty);
    }

    public ConstantVariableWriteNode createWriteConstantVariable(JavaScriptNode rhs, boolean doThrow, Object name) {
        return ConstantVariableWriteNode.create(rhs, doThrow, name);
    }

    public JSTargetableNode createReadGlobalProperty(JSContext context, TruffleString name) {
        return GlobalPropertyNode.createPropertyNode(context, name);
    }

    public JSTargetableNode createDeleteProperty(JavaScriptNode target, JavaScriptNode property, boolean strictMode) {
        return DeletePropertyNode.create(target, property, strictMode);
    }

    // ##### Function nodes

    public FunctionRootNode createFunctionRootNode(AbstractBodyNode body, FrameDescriptor frameDescriptor, JSFunctionData functionData,
                    SourceSection sourceSection, ScriptOrModule activeScriptOrModule, TruffleString internalFunctionName) {
        FunctionRootNode functionRoot = FunctionRootNode.create(body, frameDescriptor, functionData, sourceSection, activeScriptOrModule, internalFunctionName);
        functionData.setRootNode(functionRoot);

        // Lazy initialization is performed under a lock, so the root node should only be set once.
        assert functionData.getRootNode() == functionRoot : "RootNode created more than once";

        if (!JSConfig.LazyFunctionData) {
            functionRoot.initializeCallTargets(functionData);
        }

        return functionRoot;
    }

    public FunctionRootNode createModuleRootNode(AbstractBodyNode linkBody, AbstractBodyNode evalBody, FrameDescriptor frameDescriptor, JSFunctionData functionData,
                    SourceSection sourceSection, ScriptOrModule activeScriptOrModule, TruffleString internalFunctionName) {
        FunctionRootNode linkRoot;
        FunctionRootNode evalRoot;
        if (linkBody == evalBody) {
            evalRoot = linkRoot = FunctionRootNode.create(linkBody, frameDescriptor, functionData, sourceSection, activeScriptOrModule, internalFunctionName);
        } else {
            linkRoot = FunctionRootNode.create(linkBody, frameDescriptor, functionData, sourceSection, activeScriptOrModule,
                            Strings.concat(internalFunctionName, Evaluator.MODULE_LINK_SUFFIX));
            evalRoot = FunctionRootNode.create(evalBody, JavaScriptRootNode.MODULE_DUMMY_FRAMEDESCRIPTOR, functionData, sourceSection, activeScriptOrModule,
                            Strings.concat(internalFunctionName, Evaluator.MODULE_EVAL_SUFFIX));
        }

        // Note: RootNode is used to get the module environment FrameDescriptor.
        functionData.setRootNode(linkRoot);

        RootCallTarget linkCallTarget = linkRoot.getCallTarget();
        RootCallTarget evalCallTarget = evalRoot.getCallTarget();
        // The [[Construct]] target is used to represent InitializeEnvironment().
        // The [[Call]] target is used to represent ExecuteModule().
        functionData.setCallTarget(evalCallTarget);
        functionData.setConstructTarget(linkCallTarget);
        functionData.setConstructNewTarget(linkCallTarget); // unused

        return linkRoot;
    }

    public ConstructorRootNode createConstructorRootNode(JSFunctionData functionData, CallTarget callTarget, boolean newTarget) {
        return ConstructorRootNode.create(functionData, callTarget, newTarget);
    }

    public FunctionBodyNode createFunctionBody(JavaScriptNode body) {
        return FunctionBodyNode.create(body);
    }

    /**
     * @param functionNode used by snapshot recording.
     */
    public JSFunctionExpressionNode createFunctionExpression(JSFunctionData function, FunctionRootNode functionNode, JSFrameSlot blockScopeSlot) {
        return JSFunctionExpressionNode.create(function, blockScopeSlot);
    }

    /**
     * @param functionNode used by snapshot recording.
     */
    public JSFunctionExpressionNode createFunctionExpressionLexicalThis(JSFunctionData function, FunctionRootNode functionNode, JSFrameSlot blockScopeSlot, JavaScriptNode thisNode) {
        return JSFunctionExpressionNode.createLexicalThis(function, blockScopeSlot, thisNode);
    }

    public JavaScriptNode createPrepareThisBinding(JSContext context, JavaScriptNode child) {
        return JSPrepareThisNode.createPrepareThisBinding(context, child);
    }

    public JavaScriptNode createGlobalObject() {
        return GlobalObjectNode.create();
    }

    public JavaScriptNode createArgumentsObjectNode(JSContext context, boolean unmapped, int leadingArgumentCount) {
        return ArgumentsObjectNode.create(context, unmapped, leadingArgumentCount);
    }

    public JavaScriptNode createThrowError(JSErrorType errorType, TruffleString message) {
        return RuntimeErrorNode.create(errorType, Strings.toJavaString(message));
    }

    public JavaScriptNode createObjectLiteral(JSContext context, ArrayList<ObjectLiteralMemberNode> members) {
        return ObjectLiteralNode.create(context, members.toArray(ObjectLiteralMemberNode.EMPTY));
    }

    public JavaScriptNode createArrayLiteral(JSContext context, JavaScriptNode[] elements) {
        return ArrayLiteralNode.create(context, elements);
    }

    public JavaScriptNode createArrayLiteralWithSpread(JSContext context, JavaScriptNode[] elements) {
        return ArrayLiteralNode.createWithSpread(context, elements);
    }

    public ObjectLiteralMemberNode createAccessorMember(TruffleString keyName, boolean isStatic, boolean enumerable, JavaScriptNode getter, JavaScriptNode setter) {
        return ObjectLiteralNode.newAccessorMember(keyName, isStatic, enumerable, getter, setter);
    }

    public ObjectLiteralMemberNode createDataMember(TruffleString keyName, boolean isStatic, boolean enumerable, JavaScriptNode value, boolean isField) {
        return ObjectLiteralNode.newDataMember(keyName, isStatic, enumerable, value, isField);
    }

    public ObjectLiteralMemberNode createAutoAccessor(TruffleString keyName, boolean isStatic, boolean enumerable, JavaScriptNode value) {
        return ObjectLiteralNode.newAutoAccessor(keyName, isStatic, enumerable, value);
    }

    public ObjectLiteralMemberNode createComputedAutoAccessor(JavaScriptNode key, boolean isStatic, boolean enumerable, JavaScriptNode value) {
        return ObjectLiteralNode.newComputedAutoAccessor(key, isStatic, enumerable, value);
    }

    public ObjectLiteralMemberNode createProtoMember(TruffleString keyName, boolean isStatic, JavaScriptNode value) {
        return ObjectLiteralNode.newProtoMember(keyName, isStatic, value);
    }

    public ObjectLiteralMemberNode createComputedDataMember(JavaScriptNode key, boolean isStatic, boolean enumerable, JavaScriptNode value, boolean isField,
                    boolean isAnonymousFunctionDefinition) {
        return ObjectLiteralNode.newComputedDataMember(key, isStatic, enumerable, value, isField, isAnonymousFunctionDefinition);
    }

    public ObjectLiteralMemberNode createComputedAccessorMember(JavaScriptNode key, boolean isStatic, boolean enumerable, JavaScriptNode getter, JavaScriptNode setter) {
        return ObjectLiteralNode.newComputedAccessorMember(key, isStatic, enumerable, getter, setter);
    }

    public ObjectLiteralMemberNode createSpreadObjectMember(boolean isStatic, JavaScriptNode value) {
        return ObjectLiteralNode.newSpreadObjectMember(isStatic, value);
    }

    public ObjectLiteralMemberNode createStaticBlockMember(JavaScriptNode value) {
        return ObjectLiteralNode.newStaticBlockMember(value);
    }

    public DecoratorListEvaluationNode createDecoratorListEvaluation(JavaScriptNode[] decorators) {
        return DecoratorListEvaluationNode.create(decorators);
    }

    public JavaScriptNode createClassDefinition(JSContext context, JSFunctionExpressionNode constructorFunction, JavaScriptNode classHeritage, ObjectLiteralMemberNode[] members,
                    JSWriteFrameSlotNode writeClassBinding, JSWriteFrameSlotNode writeInternalConstructorBrand, JavaScriptNode[] classDecorators, DecoratorListEvaluationNode[] memberDecorators,
                    TruffleString className, int instanceFieldCount, int staticFieldCount, boolean hasPrivateInstanceMethods, boolean hasInstanceFieldsOrAccessors, JSFrameSlot blockScopeSlot) {
        return ClassDefinitionNode.create(context, constructorFunction, classHeritage, members,
                        writeClassBinding, writeInternalConstructorBrand, className, classDecorators, memberDecorators,
                        instanceFieldCount, staticFieldCount, hasPrivateInstanceMethods, hasInstanceFieldsOrAccessors, blockScopeSlot);
    }

    public JavaScriptNode createMakeMethod(JSContext context, JavaScriptNode function) {
        return MakeMethodNode.create(context, function);
    }

    public JavaScriptNode createSpreadArgument(JSContext context, GetIteratorUnaryNode getIterator) {
        return SpreadArgumentNode.create(context, getIterator);
    }

    public JavaScriptNode createSpreadArray(JSContext context, GetIteratorUnaryNode getIterator) {
        return ArrayLiteralNode.SpreadArrayNode.create(context, getIterator);
    }

    public ReturnNode createReturn(JavaScriptNode expression) {
        return ReturnNode.create(expression);
    }

    public ReturnNode createFrameReturn(JavaScriptNode expression) {
        return ReturnNode.createFrameReturn(expression);
    }

    public ReturnNode createTerminalPositionReturn(JavaScriptNode expression) {
        return ReturnNode.createTerminalPositionReturn(expression);
    }

    public JSFunctionData createFunctionData(JSContext context, int length, TruffleString name, boolean isConstructor, boolean isDerived, boolean isStrict, boolean isBuiltin, boolean needsParentFrame,
                    boolean isGenerator, boolean isAsync, boolean isClassConstructor, boolean strictProperties, boolean needsNewTarget) {
        return JSFunctionData.create(context, null, null, null, length, name, isConstructor, isDerived, isStrict, isBuiltin, needsParentFrame, isGenerator, isAsync,
                        isClassConstructor,
                        strictProperties, needsNewTarget, false);
    }

    public JavaScriptNode createAwait(JSContext context, JSFrameSlot stateSlot, JavaScriptNode expression,
                    JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        return AwaitNode.create(context, stateSlot.getIndex(), expression, asyncContextNode, asyncResultNode);
    }

    // ##### Generator nodes

    public JavaScriptNode createYield(JSContext context, JSFrameSlot stateSlot, JavaScriptNode expression, JavaScriptNode yieldValue, boolean yieldStar,
                    ReturnNode returnNode, JSWriteFrameSlotNode writeYieldResultNode) {
        if (yieldStar) {
            return YieldNode.createYieldStar(context, stateSlot.getIndex(), expression, yieldValue, returnNode, writeYieldResultNode);
        } else {
            return YieldNode.createYield(context, stateSlot.getIndex(), expression, yieldValue, returnNode, writeYieldResultNode);
        }
    }

    public JavaScriptNode createAsyncGeneratorYield(JSContext context, JSFrameSlot stateSlot, JavaScriptNode expression,
                    JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode, ReturnNode returnNode) {
        return AsyncGeneratorYieldNode.createYield(context, stateSlot.getIndex(), expression, asyncContextNode, asyncResultNode, returnNode);
    }

    public JavaScriptNode createAsyncGeneratorYieldStar(JSContext context, JSFrameSlot stateSlot, JSFrameSlot iteratorTempSlot, JavaScriptNode expression,
                    JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode, ReturnNode returnNode) {
        return AsyncGeneratorYieldNode.createYieldStar(context, stateSlot.getIndex(), expression, asyncContextNode, asyncResultNode, returnNode, iteratorTempSlot.getIndex());
    }

    public JavaScriptNode createAsyncFunctionBody(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeAsyncContext, JSReadFrameSlotNode readAsyncContext,
                    JSWriteFrameSlotNode writeAsyncResult, SourceSection functionSourceSection, TruffleString functionName, ScriptOrModule activeScriptOrModule) {
        return AsyncFunctionBodyNode.create(context, body, writeAsyncContext, readAsyncContext, writeAsyncResult, functionSourceSection, functionName, activeScriptOrModule);
    }

    public JavaScriptNode createGeneratorBody(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeYieldValue, JSReadFrameSlotNode readYieldResult,
                    SourceSection functionSourceSection, TruffleString functionName, ScriptOrModule activeScriptOrModule) {
        return GeneratorBodyNode.create(context, body, writeYieldValue, readYieldResult, functionSourceSection, functionName, activeScriptOrModule);
    }

    public JavaScriptNode createAsyncGeneratorBody(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeYieldValue, JSReadFrameSlotNode readYieldResult,
                    JSWriteFrameSlotNode writeAsyncContext, JSReadFrameSlotNode readAsyncContext,
                    SourceSection functionSourceSection, TruffleString functionName, ScriptOrModule activeScriptOrModule) {
        return AsyncGeneratorBodyNode.create(context, body, writeYieldValue, readYieldResult, writeAsyncContext, readAsyncContext, functionSourceSection, functionName, activeScriptOrModule);
    }

    public JavaScriptNode createGeneratorWrapper(JavaScriptNode child, JSFrameSlot stateSlot) {
        return ResumableNode.createResumableNode((ResumableNode) child, stateSlot.getIndex());
    }

    public JavaScriptNode createGeneratorVoidBlock(JavaScriptNode[] statements, JSFrameSlot stateSlot) {
        return GeneratorVoidBlockNode.create(statements, stateSlot.getIndex());
    }

    public JavaScriptNode createGeneratorExprBlock(JavaScriptNode[] statements, JSFrameSlot stateSlot) {
        return GeneratorExprBlockNode.create(statements, stateSlot.getIndex());
    }

    public JavaScriptNode createBlockScope(JavaScriptNode block, JSFrameSlot blockScopeSlot, FrameDescriptor blockFrameDescriptor, JSFrameSlot parentSlot,
                    boolean functionBlock, boolean captureFunctionFrame, boolean generatorFunctionBlock, boolean hasParentBlock, int frameStart, int frameEnd) {
        return BlockScopeNode.create(block, blockScopeSlot, blockFrameDescriptor, parentSlot, functionBlock, captureFunctionFrame, generatorFunctionBlock, hasParentBlock, frameStart, frameEnd);
    }

    public JavaScriptNode createVirtualBlockScope(JavaScriptNode block, int start, int end) {
        return BlockScopeNode.createVirtual(block, start, end);
    }

    public JavaScriptNode createTemplateObject(JSContext context, JavaScriptNode rawStrings, JavaScriptNode cookedStrings) {
        return GetTemplateObjectNode.create(context, (ArrayLiteralNode) rawStrings, (ArrayLiteralNode) cookedStrings);
    }

    public JavaScriptNode createToString(JavaScriptNode operand) {
        return JSToStringWrapperNode.create(operand);
    }

    public JavaScriptNode createRegExpLiteral(JSContext context, TruffleString pattern, TruffleString flags) {
        return RegExpLiteralNode.create(context, Strings.toJavaString(pattern), Strings.toJavaString(flags));
    }

    // ##### Iterator nodes

    public GetIteratorUnaryNode createGetIterator(JavaScriptNode iteratedObject) {
        return GetIteratorUnaryNode.create(iteratedObject);
    }

    public JavaScriptNode createGetAsyncIterator(JavaScriptNode iteratedObject) {
        return GetAsyncIteratorNode.create(iteratedObject);
    }

    public JavaScriptNode createEnumerate(JSContext context, JavaScriptNode iteratedObject, boolean values) {
        return EnumerateNode.create(context, iteratedObject, values);
    }

    public JavaScriptNode createIteratorNext(JavaScriptNode iterator) {
        return IteratorNextUnaryNode.create(iterator);
    }

    public JavaScriptNode createIteratorComplete(JSContext context, JavaScriptNode iterResult) {
        return IteratorCompleteUnaryNode.create(context, iterResult);
    }

    public JavaScriptNode createIteratorGetNextValue(JSContext context, JavaScriptNode iterator, JavaScriptNode doneNode, boolean setDoneOnError, boolean readValue) {
        return IteratorGetNextValueNode.create(context, iterator, doneNode, setDoneOnError, readValue);
    }

    public JavaScriptNode createIteratorSetDone(JavaScriptNode iterator, JavaScriptNode isDone) {
        return IteratorSetDoneNode.create(iterator, isDone);
    }

    public JavaScriptNode createIteratorIsDone(JavaScriptNode iterator) {
        return IteratorIsDoneNode.create(iterator);
    }

    public JavaScriptNode createAsyncIteratorNext(JSContext context, JSFrameSlot stateSlot, JavaScriptNode createReadNode,
                    JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        return AsyncIteratorNextNode.create(context, stateSlot.getIndex(), createReadNode, asyncContextNode, asyncResultNode);
    }

    public JavaScriptNode createIteratorValue(JavaScriptNode iterator) {
        return IteratorValueNode.create(iterator);
    }

    public JavaScriptNode createAsyncIteratorCloseWrapper(JSContext context, JSFrameSlot stateSlot, JavaScriptNode loopNode, JavaScriptNode iterator,
                    JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        return AsyncIteratorCloseWrapperNode.create(context, stateSlot.getIndex(), loopNode, iterator, asyncContextNode, asyncResultNode);
    }

    public JavaScriptNode createIteratorCloseIfNotDone(JSContext context, JavaScriptNode block, JavaScriptNode iterator) {
        return IteratorCloseWrapperNode.create(context, block, iterator);
    }

    public IteratorToArrayNode createIteratorToArray(JSContext context, JavaScriptNode iterator) {
        return IteratorToArrayNode.create(context, iterator);
    }

    public JavaScriptNode createGetPrototype(JavaScriptNode object) {
        return GetPrototypeNode.create(object);
    }

    public JSTargetableNode createSuperPropertyReference(JavaScriptNode delegate, JavaScriptNode target) {
        return SuperPropertyReferenceNode.create(delegate, target);
    }

    public JSTargetableNode createTargetableWrapper(JavaScriptNode delegate, JavaScriptNode target) {
        return JSTargetableWrapperNode.create(delegate, target);
    }

    public JavaScriptNode createWith(JavaScriptNode expression, JavaScriptNode statement) {
        return WithNode.create(expression, statement);
    }

    public JavaScriptNode createWithVarWrapper(TruffleString propertyName, JavaScriptNode withTarget, JSTargetableNode withAccessNode, JavaScriptNode globalDelegate) {
        return WithVarWrapperNode.create(propertyName, withTarget, withAccessNode, globalDelegate);
    }

    public JavaScriptNode createWithTarget(JSContext context, TruffleString propertyName, JavaScriptNode withVariable) {
        return WithTargetNode.create(context, propertyName, withVariable);
    }

    public JavaScriptRootNode createNewTargetConstruct(JSContext context, CallTarget callTarget) {
        return NewTargetRootNode.createNewTargetConstruct(context.getLanguage(), callTarget);
    }

    public JavaScriptRootNode createNewTargetCall(JSContext context, CallTarget callTarget) {
        return NewTargetRootNode.createNewTargetCall(context.getLanguage(), callTarget);
    }

    public JavaScriptRootNode createDropNewTarget(JSContext context, CallTarget callTarget) {
        return NewTargetRootNode.createDropNewTarget(context.getLanguage(), callTarget);
    }

    public JavaScriptRootNode createConstructorRequiresNewRoot(JSFunctionData functionData, SourceSection sourceSection) {
        return new JavaScriptRealmBoundaryRootNode(functionData.getContext().getLanguage(), sourceSection) {
            @Override
            protected Object executeInRealm(VirtualFrame frame) {
                JSFunctionObject constructor = JSFrameUtil.getFunctionObject(frame);
                TruffleString className = JSFunction.getFunctionData(constructor).getName();
                throw Errors.createTypeErrorClassConstructorRequiresNew(className, this);
            }
        };
    }

    public JavaScriptNode createDerivedConstructorResult(JavaScriptNode bodyNode, JavaScriptNode thisNode) {
        return ConstructorResultNode.createDerived(bodyNode, thisNode);
    }

    public JavaScriptNode createDerivedConstructorThis(JavaScriptNode thisNode) {
        return AccessDerivedConstructorThisNode.create(thisNode);
    }

    public JavaScriptNode createDefaultDerivedConstructorSuperCall(JavaScriptNode function) {
        assert function instanceof JSTargetableWrapperNode;
        JavaScriptNode superFunction = ((JSTargetableWrapperNode) function).getDelegate();
        JavaScriptNode target = ((JSTargetableWrapperNode) function).getTarget();
        return DefaultDerivedConstructorSuperCallNode.create(superFunction, target);
    }

    public JavaScriptNode createRequireObjectCoercible(JavaScriptNode argument) {
        return RequireObjectCoercibleWrapperNode.create(argument);
    }

    public JSFrameDescriptor createFunctionFrameDescriptor() {
        return new JSFrameDescriptor(Undefined.instance);
    }

    public JSFrameDescriptor createBlockFrameDescriptor() {
        JSFrameDescriptor desc = new JSFrameDescriptor(Undefined.instance);
        desc.addFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER, FrameSlotKind.Object);
        return desc;
    }

    public DeclareGlobalNode createDeclareGlobalVariable(TruffleString varName, boolean configurable) {
        return DeclareGlobalVariableNode.create(varName, configurable);
    }

    public DeclareGlobalNode createDeclareGlobalFunction(TruffleString varName, boolean configurable) {
        return DeclareGlobalFunctionNode.create(varName, configurable);
    }

    public DeclareGlobalNode createDeclareGlobalLexicalVariable(TruffleString varName, boolean isConst) {
        return DeclareGlobalLexicalVariableNode.create(varName, isConst);
    }

    public JavaScriptNode createGlobalDeclarationInstantiation(JSContext context, List<DeclareGlobalNode> declarations) {
        return GlobalDeclarationInstantiationNode.create(context, declarations);
    }

    public JavaScriptNode copy(JavaScriptNode node) {
        return node.copy();
    }

    public JavaScriptNode createToObject(JavaScriptNode operand) {
        return JSToObjectNode.JSToObjectWrapperNode.createToObject(operand);
    }

    public JavaScriptNode createAccessArgumentsArrayDirectly(JavaScriptNode writeArguments, JavaScriptNode readArguments, int leadingArgCount) {
        return new AccessArgumentsArrayDirectlyNode(writeArguments, readArguments, leadingArgCount);
    }

    public JavaScriptNode createCallApplyArguments(JSFunctionCallNode callNode) {
        return CallApplyArgumentsNode.create(callNode);
    }

    public JavaScriptNode createGuardDisconnectedArgumentRead(int index, ReadElementNode readElementNode, JavaScriptNode argumentsArray, JSFrameSlot slot) {
        return JSGuardDisconnectedArgumentRead.create(index, readElementNode, argumentsArray, (TruffleString) slot.getIdentifier());
    }

    public JavaScriptNode createGuardDisconnectedArgumentWrite(int index, WriteElementNode argumentsArrayAccess, JavaScriptNode argumentsArray, JavaScriptNode rhs, JSFrameSlot slot) {
        return JSGuardDisconnectedArgumentWrite.create(index, argumentsArrayAccess, argumentsArray, rhs, (TruffleString) slot.getIdentifier());
    }

    public JavaScriptNode createModuleBody(JavaScriptNode moduleBody) {
        return ModuleBodyNode.create(moduleBody);
    }

    public JavaScriptNode createModuleInitializeEnvironment(JavaScriptNode moduleBody) {
        return ModuleInitializeEnvironmentNode.create(moduleBody);
    }

    public JavaScriptNode createModuleYield() {
        return ModuleYieldNode.create();
    }

    public JavaScriptNode createTopLevelAsyncModuleBody(JSContext context, JavaScriptNode moduleBody, JSWriteFrameSlotNode writeAsyncResult, JSWriteFrameSlotNode writeAsyncContext,
                    SourceSection functionSourceSection, ScriptOrModule activeScriptOrModule) {
        return TopLevelAwaitModuleBodyNode.create(context, moduleBody, writeAsyncResult, writeAsyncContext, functionSourceSection, activeScriptOrModule);
    }

    public JavaScriptNode createImportMeta(JavaScriptNode moduleNode) {
        return ImportMetaNode.create(moduleNode);
    }

    public JavaScriptNode createResolveStarImport(JSContext context, JavaScriptNode moduleNode, ModuleRequest moduleRequest, JSWriteFrameSlotNode writeLocalNode) {
        return ResolveStarImportNode.create(context, moduleNode, moduleRequest, writeLocalNode);
    }

    public JavaScriptNode createResolveNamedImport(JSContext context, JavaScriptNode moduleNode, ModuleRequest moduleRequest, TruffleString importName, JSWriteFrameSlotNode writeLocalNode) {
        return ResolveNamedImportNode.create(context, moduleNode, moduleRequest, importName, writeLocalNode);
    }

    public JavaScriptNode createReadImportBinding(JavaScriptNode readLocal) {
        return ReadImportBindingNode.create(readLocal);
    }

    public JavaScriptNode createImportCall(JSContext context, ImportPhase phase, JavaScriptNode argument, JavaScriptNode options, ScriptOrModule activeScriptOrModule) {
        return ImportCallNode.create(context, phase, argument, options, activeScriptOrModule);
    }

    public JavaScriptNode createResolveSourceImport(JSContext context, JavaScriptNode moduleNode, ModuleRequest moduleRequest, JSWriteFrameSlotNode writeLocalNode) {
        return ResolveSourceImportNode.create(context, moduleNode, moduleRequest, writeLocalNode);
    }

    public JavaScriptNode createRestObject(JSContext context, JavaScriptNode source, JavaScriptNode excludedNames) {
        JavaScriptNode restObj = ObjectLiteralNode.create(context, ObjectLiteralMemberNode.EMPTY);
        return RestObjectNode.create(context, restObj, source, excludedNames);
    }

    public JavaScriptNode createInitializeInstanceElements(JSContext context, JavaScriptNode target, JavaScriptNode constructor) {
        return InitializeInstanceElementsNode.create(context, target, constructor);
    }

    public JavaScriptNode createNewPrivateName(TruffleString description) {
        return NewPrivateNameNode.create(Strings.toJavaString(description));
    }

    public JavaScriptNode createPrivateFieldGet(JSContext context, JavaScriptNode target, JavaScriptNode key) {
        return PrivateFieldGetNode.create(target, key, context);
    }

    public JavaScriptNode createPrivateFieldSet(JSContext context, JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode) {
        return PrivateFieldSetNode.create(targetNode, indexNode, valueNode, context);
    }

    public ObjectLiteralMemberNode createPrivateFieldMember(JavaScriptNode keyNode, boolean isStatic, JavaScriptNode valueNode, JSWriteFrameSlotNode writePrivateNode) {
        return ObjectLiteralNode.newPrivateFieldMember(keyNode, isStatic, valueNode, writePrivateNode);
    }

    public ObjectLiteralMemberNode createPrivateMethodMember(TruffleString privateName, boolean isStatic, JavaScriptNode valueNode, JSWriteFrameSlotNode writePrivateNode, int privateBrandSlotIndex) {
        return ObjectLiteralNode.newPrivateMethodMember(privateName, isStatic, valueNode, writePrivateNode, privateBrandSlotIndex);
    }

    public ObjectLiteralMemberNode createPrivateAccessorMember(boolean isStatic, JavaScriptNode getterNode, JavaScriptNode setterNode, JSWriteFrameSlotNode writePrivateNode,
                    int privateBrandSlotIndex) {
        return ObjectLiteralNode.newPrivateAccessorMember(isStatic, getterNode, setterNode, writePrivateNode, privateBrandSlotIndex);
    }

    public ObjectLiteralMemberNode createPrivateAutoAccessorMember(boolean isStatic, JavaScriptNode valueNode,
                    JSWriteFrameSlotNode writePrivateAccessor, JavaScriptNode storageKey, int privateBrandSlotIndex) {
        return ObjectLiteralNode.newPrivateAutoAccessorMember(isStatic, valueNode, writePrivateAccessor, storageKey, privateBrandSlotIndex);
    }

    public JavaScriptNode createPrivateBrandCheck(JavaScriptNode targetNode, JavaScriptNode brandNode) {
        return PrivateBrandCheckNode.create(targetNode, brandNode);
    }

    public JavaScriptNode createGetPrivateBrand(JSContext context, JavaScriptNode constructorNode) {
        return JSAndNode.create(constructorNode, PropertyNode.createGetHidden(context, constructorNode, JSFunction.PRIVATE_BRAND_ID));
    }

    public JavaScriptNode createToPropertyKey(JavaScriptNode key) {
        return JSToPropertyKeyWrapperNode.create(key);
    }

    public JavaScriptNode createOptionalChain(JavaScriptNode accessNode) {
        return OptionalChainNode.createTarget(accessNode);
    }

    public JavaScriptNode createOptionalChainShortCircuit(JavaScriptNode valueNode) {
        return OptionalChainNode.createShortCircuit(valueNode);
    }

    public JavaScriptNode createNamedEvaluation(JavaScriptNode expressionNode, JavaScriptNode nameNode) {
        return NamedEvaluationNode.create(expressionNode, nameNode);
    }

    public IfNode copyIfWithCondition(IfNode origIfNode, JavaScriptNode condition) {
        return IfNode.create(condition, origIfNode.getThenPart(), origIfNode.getElsePart());
    }

    public JavaScriptNode createDebugScope(JSContext context, JavaScriptNode function) {
        return PropertyNode.createGetHidden(context, function, JSFunction.DEBUG_SCOPE_ID);
    }

    public JavaScriptNode createDebugVarWrapper(TruffleString varName, JavaScriptNode defaultDelegate, JavaScriptNode dynamicScope, JSTargetableNode scopeAccessNode) {
        return new DebugScopeVarWrapperNode(varName, defaultDelegate, dynamicScope, scopeAccessNode);
    }

    public InternalSlotId createInternalSlotId(String description, int ordinal) {
        return new InternalSlotId(description, ordinal);
    }

    public JavaScriptNode createPrivateFieldIn(JavaScriptNode left, JavaScriptNode right) {
        return PrivateFieldInNode.create(left, right);
    }

    public ScriptOrModule createScript(JSContext context, Source source) {
        return new ScriptOrModule(context, source);
    }

    public JavaScriptNode createToObjectForWithStatement(JSContext context, JavaScriptNode operand) {
        if (context.isOptionNashornCompatibilityMode()) {
            return WithStatementToObjectNode.create(operand);
        } else {
            return createToObject(operand);
        }
    }

    // #####

    public static NodeFactory getDefaultInstance() {
        return FACTORY;
    }

    public static NodeFactory getInstance(JSContext context) {
        return (NodeFactory) context.getNodeFactory();
    }
}

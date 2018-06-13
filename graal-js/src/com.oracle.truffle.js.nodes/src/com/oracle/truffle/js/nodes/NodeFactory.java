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
package com.oracle.truffle.js.nodes;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.annotations.GenerateDecoder;
import com.oracle.truffle.js.annotations.GenerateProxy;
import com.oracle.truffle.js.nodes.access.ArrayLiteralNode;
import com.oracle.truffle.js.nodes.access.AsyncIteratorNextNode;
import com.oracle.truffle.js.nodes.access.CopyDataPropertiesNode;
import com.oracle.truffle.js.nodes.access.DeclareGlobalFunctionNode;
import com.oracle.truffle.js.nodes.access.DeclareGlobalLexicalVariableNode;
import com.oracle.truffle.js.nodes.access.DeclareGlobalNode;
import com.oracle.truffle.js.nodes.access.DeclareGlobalVariableNode;
import com.oracle.truffle.js.nodes.access.DoWithNode;
import com.oracle.truffle.js.nodes.access.EnumerateNode;
import com.oracle.truffle.js.nodes.access.FrameSlotNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.GetTemplateObjectNode;
import com.oracle.truffle.js.nodes.access.GlobalDeclarationInstantiationNode;
import com.oracle.truffle.js.nodes.access.GlobalObjectNode;
import com.oracle.truffle.js.nodes.access.GlobalPropertyNode;
import com.oracle.truffle.js.nodes.access.GlobalScopeNode;
import com.oracle.truffle.js.nodes.access.GlobalScopeVarWrapperNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteUnaryNode;
import com.oracle.truffle.js.nodes.access.IteratorNextUnaryNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorStepSpecialNode;
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
import com.oracle.truffle.js.nodes.access.LazyReadFrameSlotNode;
import com.oracle.truffle.js.nodes.access.LazyWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.LocalVarIncNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.MakeMethodNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.ObjectLiteralMemberNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.access.RegExpLiteralNode;
import com.oracle.truffle.js.nodes.access.RequireObjectCoercibleNode.RequireObjectCoercibleWrapperNode;
import com.oracle.truffle.js.nodes.access.ScopeFrameNode;
import com.oracle.truffle.js.nodes.access.SuperPropertyReferenceNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.access.WriteNode;
import com.oracle.truffle.js.nodes.access.WritePropertyNode;
import com.oracle.truffle.js.nodes.arguments.AccessArgumentsArrayDirectlyNode;
import com.oracle.truffle.js.nodes.arguments.AccessDerivedConstructorThisNode;
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
import com.oracle.truffle.js.nodes.binary.JSOrNode;
import com.oracle.truffle.js.nodes.binary.JSRightShiftNode;
import com.oracle.truffle.js.nodes.binary.JSSubtractNode;
import com.oracle.truffle.js.nodes.binary.JSTypeofIdenticalNode;
import com.oracle.truffle.js.nodes.binary.JSUnsignedRightShiftNode;
import com.oracle.truffle.js.nodes.cast.JSPrepareThisNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode.JSToPropertyKeyWrapperNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode.JSToStringWrapperNode;
import com.oracle.truffle.js.nodes.cast.ToArrayIndexNode;
import com.oracle.truffle.js.nodes.control.AsyncFunctionBodyNode;
import com.oracle.truffle.js.nodes.control.AsyncGeneratorBodyNode;
import com.oracle.truffle.js.nodes.control.AsyncGeneratorYieldNode;
import com.oracle.truffle.js.nodes.control.AsyncIteratorCloseWrapperNode;
import com.oracle.truffle.js.nodes.control.AwaitNode;
import com.oracle.truffle.js.nodes.control.BlockNode;
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
import com.oracle.truffle.js.nodes.control.GeneratorWrapperNode;
import com.oracle.truffle.js.nodes.control.IfNode;
import com.oracle.truffle.js.nodes.control.IteratorCloseIfNotDoneNode;
import com.oracle.truffle.js.nodes.control.LabelNode;
import com.oracle.truffle.js.nodes.control.ReturnNode;
import com.oracle.truffle.js.nodes.control.ReturnTargetNode;
import com.oracle.truffle.js.nodes.control.RuntimeErrorNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.nodes.control.SwitchNode;
import com.oracle.truffle.js.nodes.control.ThrowNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.control.TryFinallyNode;
import com.oracle.truffle.js.nodes.control.WhileNode;
import com.oracle.truffle.js.nodes.control.WithNode;
import com.oracle.truffle.js.nodes.control.YieldNode;
import com.oracle.truffle.js.nodes.function.AbstractBodyNode;
import com.oracle.truffle.js.nodes.function.BlockScopeNode;
import com.oracle.truffle.js.nodes.function.CallApplyArgumentsNode;
import com.oracle.truffle.js.nodes.function.ClassDefinitionNode;
import com.oracle.truffle.js.nodes.function.ConstructorResultNode;
import com.oracle.truffle.js.nodes.function.ConstructorRootNode;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.function.IterationScopeNode;
import com.oracle.truffle.js.nodes.function.JSFunctionArgumentsNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.function.JSFunctionExpressionNode;
import com.oracle.truffle.js.nodes.function.JSNewNode;
import com.oracle.truffle.js.nodes.function.NewTargetRootNode;
import com.oracle.truffle.js.nodes.function.SpreadArgumentNode;
import com.oracle.truffle.js.nodes.unary.JSComplementNode;
import com.oracle.truffle.js.nodes.unary.JSNotNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryMinusNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryPlusNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.nodes.unary.VoidNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

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
            case POSTFIX_LOCAL_INCREMENT:
                return LocalVarIncNode.createPostfix(LocalVarIncNode.Op.Inc, ((FrameSlotNode) operand));
            case PREFIX_LOCAL_INCREMENT:
                return LocalVarIncNode.createPrefix(LocalVarIncNode.Op.Inc, ((FrameSlotNode) operand));
            case POSTFIX_LOCAL_DECREMENT:
                return LocalVarIncNode.createPostfix(LocalVarIncNode.Op.Dec, ((FrameSlotNode) operand));
            case PREFIX_LOCAL_DECREMENT:
                return LocalVarIncNode.createPrefix(LocalVarIncNode.Op.Dec, ((FrameSlotNode) operand));
            case TYPE_OF:
                return TypeOfNode.create(operand);
            case VOID:
                return VoidNode.create(operand);
            default:
                throw new IllegalArgumentException();
        }
    }

    public JavaScriptNode createUnaryPlus(JavaScriptNode operand) {
        return JSUnaryPlusNode.create(operand);
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

    public JavaScriptNode createTypeofIdentical(JavaScriptNode subject, String typeString) {
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

    public JavaScriptNode createConstantDouble(double value) {
        return JSConstantNode.createDouble(value);
    }

    public JavaScriptNode createConstantString(String value) {
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

    public SwitchNode createSwitch(JavaScriptNode[] caseExpressions, int[] jumptable, JavaScriptNode[] statements) {
        return SwitchNode.create(caseExpressions, jumptable, statements);
    }

    public JavaScriptNode createWhileDo(JavaScriptNode condition, JavaScriptNode body) {
        return WhileNode.createWhileDo(condition, body);
    }

    public JavaScriptNode createDoWhile(JavaScriptNode condition, JavaScriptNode body) {
        return WhileNode.createDoWhile(condition, body);
    }

    public StatementNode createFor(JavaScriptNode condition, JavaScriptNode body, JavaScriptNode modify, FrameDescriptor frameDescriptor, JavaScriptNode isFirstNode, JavaScriptNode setNotFirstNode) {
        IterationScopeNode perIterationScope = createIterationScope(frameDescriptor);
        return ForNode.createFor(condition, body, modify, perIterationScope, isFirstNode, setNotFirstNode);
    }

    public IterationScopeNode createIterationScope(FrameDescriptor frameDescriptor) {
        assert frameDescriptor.getSize() > 0 && frameDescriptor.getSlots().get(0).getIdentifier() == ScopeFrameNode.PARENT_SCOPE_IDENTIFIER;
        List<? extends FrameSlot> slots = frameDescriptor.getSlots();
        JSReadFrameSlotNode[] reads = new JSReadFrameSlotNode[slots.size()];
        JSWriteFrameSlotNode[] writes = new JSWriteFrameSlotNode[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            FrameSlot slot = slots.get(i);
            reads[i] = JSReadFrameSlotNode.create(slot, 0, 0, ScopeFrameNode.EMPTY_FRAME_SLOT_ARRAY, false);
            writes[i] = JSWriteFrameSlotNode.create(slot, 0, 0, ScopeFrameNode.EMPTY_FRAME_SLOT_ARRAY, null, false);
        }
        return IterationScopeNode.create(frameDescriptor, reads, writes);
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
        return BlockNode.createVoidBlock(statements);
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

    public JavaScriptNode createLocal(FrameSlot frameSlot, int frameLevel, int scopeLevel, FrameSlot[] parentSlots) {
        return createLocal(frameSlot, frameLevel, scopeLevel, parentSlots, false);
    }

    public JavaScriptNode createLocal(FrameSlot frameSlot, int frameLevel, int scopeLevel, FrameSlot[] parentSlots, boolean hasTemporalDeadZone) {
        return JSReadFrameSlotNode.create(frameSlot, frameLevel, scopeLevel, parentSlots, hasTemporalDeadZone);
    }

    public JSWriteFrameSlotNode createWriteFrameSlot(FrameSlot frameSlot, int frameLevel, int scopeLevel, FrameSlot[] parentSlots, JavaScriptNode rhs) {
        return JSWriteFrameSlotNode.create(frameSlot, frameLevel, scopeLevel, parentSlots, rhs, false);
    }

    public JSWriteFrameSlotNode createWriteFrameSlot(FrameSlot frameSlot, int frameLevel, int scopeLevel, FrameSlot[] parentSlots, JavaScriptNode rhs, boolean hasTemporalDeadZone) {
        return JSWriteFrameSlotNode.create(frameSlot, frameLevel, scopeLevel, parentSlots, rhs, hasTemporalDeadZone);
    }

    public JavaScriptNode createReadLexicalGlobal(String name, boolean hasTemporalDeadZone, JSContext context) {
        return GlobalPropertyNode.createLexicalGlobal(context, name, hasTemporalDeadZone);
    }

    public JavaScriptNode createGlobalScope(JSContext context) {
        return GlobalScopeNode.create(context);
    }

    public JavaScriptNode createGlobalScopeTDZCheck(JSContext context, String name, boolean checkTDZ) {
        if (!checkTDZ) {
            return createGlobalScope(context);
        }
        return GlobalScopeNode.createWithTDZCheck(context, name);
    }

    public JavaScriptNode createGlobalVarWrapper(JSContext context, String varName, JavaScriptNode defaultDelegate, JavaScriptNode dynamicScope, JSTargetableNode scopeAccessNode) {
        return new GlobalScopeVarWrapperNode(context, varName, defaultDelegate, dynamicScope, scopeAccessNode);
    }

    public JavaScriptNode createThrow(JavaScriptNode expression) {
        return ThrowNode.create(expression);
    }

    public JavaScriptNode createTryCatch(JSContext context, JavaScriptNode tryNode, JavaScriptNode catchBlock, JavaScriptNode writeErrorVar, BlockScopeNode blockScope,
                    JavaScriptNode destructuring, JavaScriptNode conditionExpression) {
        return TryCatchNode.create(context, tryNode, catchBlock, (JSWriteFrameSlotNode) writeErrorVar, blockScope, destructuring, conditionExpression);
    }

    public JavaScriptNode createTryFinally(JavaScriptNode tryNode, JavaScriptNode finallyBlock) {
        return TryFinallyNode.create(tryNode, finallyBlock);
    }

    public JavaScriptNode createFunctionCall(@SuppressWarnings("unused") JSContext context, JavaScriptNode expression, JavaScriptNode[] arguments) {
        if (expression instanceof PropertyNode || expression instanceof ReadElementNode || expression instanceof DoWithNode) {
            if (expression instanceof PropertyNode) {
                ((PropertyNode) expression).setMethod();
            }
            return JSFunctionCallNode.createInvoke((JSTargetableNode) expression, JSFunctionArgumentsNode.create(arguments), false, false);
        } else if (expression instanceof JSTargetableWrapperNode) {
            JavaScriptNode function = ((JSTargetableWrapperNode) expression).getDelegate();
            JavaScriptNode target = ((JSTargetableWrapperNode) expression).getTarget();
            return JSFunctionCallNode.create(function, target, JSFunctionArgumentsNode.create(arguments), false, false);
        } else {
            assert expression != null;
            JavaScriptNode target = null;
            JavaScriptNode function = expression;
            if (function instanceof GlobalPropertyNode) {
                ((GlobalPropertyNode) function).setMethod();
            } else if (function instanceof GlobalScopeVarWrapperNode) {
                ((GlobalScopeVarWrapperNode) function).setMethod();
            }
            return JSFunctionCallNode.create(function, target, JSFunctionArgumentsNode.create(arguments), false, false);
        }
    }

    public JavaScriptNode createFunctionCallWithNewTarget(@SuppressWarnings("unused") JSContext context, JavaScriptNode expression, JavaScriptNode[] arguments) {
        assert expression instanceof JSTargetableWrapperNode;
        JavaScriptNode function = ((JSTargetableWrapperNode) expression).getDelegate();
        JavaScriptNode target = ((JSTargetableWrapperNode) expression).getTarget();
        return JSFunctionCallNode.create(function, target, JSFunctionArgumentsNode.create(arguments), false, true);
    }

    public JavaScriptNode createNew(JSContext context, JavaScriptNode function, JavaScriptNode[] arguments) {
        return JSNewNode.create(context, function, JSFunctionArgumentsNode.create(arguments));
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

    public JavaScriptNode createAccessRestArgument(JSContext context, int index, int trailingArgCount) {
        return AccessRestArgumentsNode.create(context, index, trailingArgCount);
    }

    public JavaScriptNode createAccessNewTarget() {
        return AccessIndexedArgumentNode.create(0);
    }

    // ##### Element nodes

    public ReadElementNode createReadElementNode(JSContext context, JavaScriptNode target, JavaScriptNode element) {
        return ReadElementNode.create(target, element, context);
    }

    public WriteElementNode createWriteElementNode(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSContext context, boolean isStrict) {
        return WriteElementNode.create(targetNode, indexNode, valueNode, context, isStrict);
    }

    public WriteElementNode createWriteElementNode(JSContext context, boolean throwError) {
        return WriteElementNode.create(context, throwError);
    }

    public WriteElementNode createWriteElementNode(JSContext context, boolean isStrict, boolean writeOwn) {
        return WriteElementNode.create(context, isStrict, writeOwn);
    }

    // ##### Property nodes

    public JavaScriptNode createReadProperty(JSContext context, JavaScriptNode base, String propertyName) {
        return PropertyNode.createProperty(context, base, propertyName);
    }

    public PropertyNode createProperty(JSContext context, JavaScriptNode base, Object propertyKey) {
        return PropertyNode.createProperty(context, base, propertyKey);
    }

    public PropertyNode createMethod(JSContext jsContext, JavaScriptNode object, Object string) {
        return PropertyNode.createMethod(jsContext, object, string);
    }

    public WritePropertyNode createWriteProperty(JavaScriptNode target, Object propertyKey, JavaScriptNode rhs, JSContext context, boolean strictMode) {
        return WritePropertyNode.create(target, propertyKey, rhs, context, strictMode);
    }

    public WritePropertyNode createWriteProperty(JavaScriptNode target, String name, JavaScriptNode rhs, boolean isGlobal, JSContext context, boolean isStrict) {
        return WritePropertyNode.create(target, name, rhs, isGlobal, context, isStrict);
    }

    public JSTargetableNode createReadGlobalProperty(JSContext context, String name) {
        return GlobalPropertyNode.createPropertyNode(context, name);
    }

    public JSTargetableNode createDeleteProperty(JavaScriptNode target, JavaScriptNode property, boolean strictMode, JSContext context) {
        return DeletePropertyNode.create(target, property, strictMode, context);
    }

    // ##### Function nodes

    public FunctionRootNode createFunctionRootNode(AbstractBodyNode body, FrameDescriptor frameDescriptor, JSFunctionData functionData, SourceSection sourceSection, String internalFunctionName) {
        FunctionRootNode functionRoot = FunctionRootNode.create(body, frameDescriptor, functionData, sourceSection, internalFunctionName);

        if (JSTruffleOptions.LazyFunctionData) {
            if (!functionData.hasLazyInit()) {
                functionData.setLazyInit(functionRoot);
            } else {
                functionRoot.initializeRoot(functionData);
            }
        } else {
            functionRoot.initializeEager(functionData);
        }

        return functionRoot;
    }

    public ConstructorRootNode createConstructorRootNode(JSFunctionData functionData, CallTarget callTarget, boolean newTarget) {
        return ConstructorRootNode.create(functionData, callTarget, newTarget);
    }

    public FunctionBodyNode createFunctionBody(JavaScriptNode body) {
        return FunctionBodyNode.create(body);
    }

    public JSFunctionExpressionNode createFunctionExpression(JSFunctionData function, FunctionRootNode functionNode) {
        return JSFunctionExpressionNode.create(function, functionNode);
    }

    public JSFunctionExpressionNode createFunctionExpressionLexicalThis(JSFunctionData function, FunctionRootNode functionNode, JavaScriptNode thisNode) {
        return JSFunctionExpressionNode.createLexicalThis(function, functionNode, thisNode);
    }

    public JavaScriptNode createPrepareThisBinding(JSContext context, JavaScriptNode child) {
        return JSPrepareThisNode.createPrepareThisBinding(context, child);
    }

    public JavaScriptNode createGlobalObject(JSContext context) {
        return GlobalObjectNode.create(context);
    }

    public JavaScriptNode createArgumentsObjectNode(JSContext context, boolean unmapped, int leadingArgumentCount, int trailingArgumentCount) {
        return ArgumentsObjectNode.create(context, unmapped, leadingArgumentCount, trailingArgumentCount);
    }

    public JavaScriptNode createThrowError(JSErrorType errorType, String message) {
        return RuntimeErrorNode.create(errorType, message);
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

    public ObjectLiteralMemberNode createAccessorMember(String keyName, boolean isStatic, boolean enumerable, JavaScriptNode getter, JavaScriptNode setter) {
        return ObjectLiteralNode.newAccessorMember(keyName, isStatic, enumerable, getter, setter);
    }

    public ObjectLiteralMemberNode createDataMember(String keyName, boolean isStatic, boolean enumerable, JavaScriptNode value) {
        return ObjectLiteralNode.newDataMember(keyName, isStatic, enumerable, value);
    }

    public ObjectLiteralMemberNode createProtoMember(String keyName, boolean isStatic, JavaScriptNode value) {
        return ObjectLiteralNode.newProtoMember(keyName, isStatic, value);
    }

    public ObjectLiteralMemberNode createComputedDataMember(JavaScriptNode key, boolean isStatic, boolean enumerable, JavaScriptNode value) {
        return ObjectLiteralNode.newComputedDataMember(key, isStatic, enumerable, value);
    }

    public ObjectLiteralMemberNode createComputedAccessorMember(JavaScriptNode key, boolean isStatic, boolean enumerable, JavaScriptNode getter, JavaScriptNode setter) {
        return ObjectLiteralNode.newComputedAccessorMember(key, isStatic, enumerable, getter, setter);
    }

    public ObjectLiteralMemberNode createSpreadObjectMember(boolean isStatic, JavaScriptNode value) {
        return ObjectLiteralNode.newSpreadObjectMember(isStatic, value);
    }

    public JavaScriptNode createClassDefinition(JSContext context, JSFunctionExpressionNode constructorFunction, JavaScriptNode classHeritage, ObjectLiteralMemberNode[] members, String className) {
        if (className != null) {
            constructorFunction.setFunctionName(className);
        }
        return ClassDefinitionNode.create(context, constructorFunction, classHeritage, members, className != null);
    }

    public JavaScriptNode createMakeMethod(JSContext context, JavaScriptNode function) {
        return MakeMethodNode.create(context, function);
    }

    public JavaScriptNode createSpreadArgument(JSContext context, JavaScriptNode argument) {
        return SpreadArgumentNode.create(context, argument);
    }

    public JavaScriptNode createSpreadArray(JSContext context, JavaScriptNode argument) {
        return ArrayLiteralNode.SpreadArrayNode.create(context, argument);
    }

    public ReturnNode createReturn(JavaScriptNode expression) {
        return ReturnNode.create(expression);
    }

    public ReturnNode createFrameReturn(JavaScriptNode expression) {
        return ReturnNode.createFrameReturn(expression);
    }

    public JSFunctionData createFunctionData(JSContext context, int length, String name, boolean isConstructor, boolean isDerived, boolean isStrict, boolean isBuiltin, boolean needsParentFrame,
                    boolean isGenerator, boolean isAsync, boolean isClassConstructor, boolean strictProperties, boolean needsNewTarget) {
        return JSFunctionData.create(context, null, null, null, length, name, isConstructor, isDerived, isStrict, isBuiltin, needsParentFrame, isGenerator, isAsync, isClassConstructor,
                        strictProperties, needsNewTarget, false);
    }

    public JavaScriptNode createAwait(JSContext context, JavaScriptNode expression, JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        return AwaitNode.create(context, expression, asyncContextNode, asyncResultNode);
    }

    // ##### Generator nodes

    public JavaScriptNode createYield(JSContext context, JavaScriptNode expression, JavaScriptNode yieldValue, boolean yieldStar, ReturnNode returnNode, JSWriteFrameSlotNode writeYieldResultNode) {
        if (yieldStar) {
            return YieldNode.createYieldStar(context, expression, yieldValue, returnNode, writeYieldResultNode);
        } else {
            return YieldNode.createYield(context, expression, yieldValue, returnNode, writeYieldResultNode);
        }
    }

    public JavaScriptNode createAsyncGeneratorYield(JSContext context, JavaScriptNode expression, JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode, ReturnNode returnNode) {
        return AsyncGeneratorYieldNode.createYield(context, expression, asyncContextNode, asyncResultNode, returnNode);
    }

    public JavaScriptNode createAsyncGeneratorYieldStar(JSContext context, JavaScriptNode expression, JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode,
                    ReturnNode returnNode, JavaScriptNode readTemp, WriteNode writeTemp) {
        return AsyncGeneratorYieldNode.createYieldStar(context, expression, asyncContextNode, asyncResultNode, returnNode, readTemp, writeTemp);
    }

    public JavaScriptNode createAsyncFunctionBody(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode asyncContext, JSWriteFrameSlotNode asyncResult) {
        return AsyncFunctionBodyNode.create(context, body, asyncContext, asyncResult);
    }

    public JavaScriptNode createGeneratorBody(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeYieldValue, JSReadFrameSlotNode readYieldResult) {
        return GeneratorBodyNode.create(context, body, writeYieldValue, readYieldResult);
    }

    public JavaScriptNode createAsyncGeneratorBody(JSContext context, JavaScriptNode body, JSWriteFrameSlotNode writeYieldValue, JSReadFrameSlotNode readYieldResult,
                    JSWriteFrameSlotNode writeAsyncContext) {
        return AsyncGeneratorBodyNode.create(context, body, writeYieldValue, readYieldResult, writeAsyncContext);
    }

    public JavaScriptNode createGeneratorWrapper(JavaScriptNode child, JavaScriptNode state, WriteNode writeStateNode) {
        return GeneratorWrapperNode.createWrapper(child, state, writeStateNode);
    }

    public LazyReadFrameSlotNode createLazyReadFrameSlot(Object identifier) {
        return LazyReadFrameSlotNode.create(identifier);
    }

    public LazyWriteFrameSlotNode createLazyWriteFrameSlot(Object identifier, JavaScriptNode rhs) {
        return LazyWriteFrameSlotNode.create(identifier, rhs);
    }

    public JavaScriptNode createBlockScope(FrameDescriptor blockFrameDescriptor, FrameSlot parentSlot, JavaScriptNode block) {
        return BlockScopeNode.create(blockFrameDescriptor, parentSlot, block);
    }

    public JavaScriptNode createTemplateObject(JSContext context, JavaScriptNode rawStrings, JavaScriptNode cookedStrings) {
        return GetTemplateObjectNode.create(context, (ArrayLiteralNode) rawStrings, (ArrayLiteralNode) cookedStrings);
    }

    public JavaScriptNode createToString(JavaScriptNode operand) {
        return JSToStringWrapperNode.create(operand);
    }

    public JavaScriptNode createRegExpLiteral(JSContext context, String pattern, String flags) {
        return RegExpLiteralNode.create(context, pattern, flags);
    }

    // ##### Iterator nodes

    public JavaScriptNode createGetIterator(JSContext context, JavaScriptNode iteratedObject) {
        return GetIteratorNode.create(context, iteratedObject);
    }

    public JavaScriptNode createGetAsyncIterator(JSContext context, JavaScriptNode iteratedObject) {
        return GetIteratorNode.createAsync(context, iteratedObject);
    }

    public JavaScriptNode createEnumerate(JSContext context, JavaScriptNode iteratedObject, boolean values) {
        return EnumerateNode.create(context, iteratedObject, values);
    }

    public JavaScriptNode createIteratorNext(JSContext context, JavaScriptNode iterator) {
        return IteratorNextUnaryNode.create(context, iterator);
    }

    public JavaScriptNode createIteratorComplete(JSContext context, JavaScriptNode iterResult) {
        return IteratorCompleteUnaryNode.create(context, iterResult);
    }

    public JavaScriptNode createIteratorStep(JSContext context, JavaScriptNode iterator) {
        return IteratorStepNode.create(context, iterator);
    }

    public IteratorStepSpecialNode createIteratorStepSpecial(JSContext context, JavaScriptNode iterator, JavaScriptNode doneNode, boolean setDoneOnError) {
        return IteratorStepSpecialNode.create(context, iterator, doneNode, setDoneOnError);
    }

    public JavaScriptNode createAsyncIteratorNext(JSContext context, JavaScriptNode createReadNode, JSReadFrameSlotNode asyncContextNode, JSReadFrameSlotNode asyncResultNode) {
        return AsyncIteratorNextNode.create(context, createReadNode, asyncContextNode, asyncResultNode);
    }

    public JavaScriptNode createIteratorValue(JSContext context, JavaScriptNode iterator) {
        return IteratorValueNode.create(context, iterator);
    }

    public JavaScriptNode createAsyncIteratorCloseWrapper(JSContext context, JavaScriptNode loopNode, JavaScriptNode iterator, JSReadFrameSlotNode asyncContextNode,
                    JSReadFrameSlotNode asyncResultNode, JavaScriptNode doneNode) {
        return AsyncIteratorCloseWrapperNode.create(context, loopNode, iterator, asyncContextNode, asyncResultNode, doneNode);
    }

    public JavaScriptNode createIteratorClose(JSContext context, JavaScriptNode iterator) {
        return IteratorCloseNode.create(context, iterator);
    }

    public JavaScriptNode createIteratorCloseIfNotDone(JSContext context, JavaScriptNode block, JavaScriptNode iterator, JavaScriptNode isDoneNode) {
        return IteratorCloseIfNotDoneNode.create(context, block, iterator, isDoneNode);
    }

    public IteratorToArrayNode createIteratorToArray(JSContext context, JavaScriptNode iterator, JavaScriptNode doneNode) {
        return IteratorToArrayNode.create(context, iterator, doneNode);
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

    public JavaScriptRootNode createNewTargetConstruct(CallTarget callTarget) {
        return NewTargetRootNode.createNewTargetConstruct(callTarget);
    }

    public JavaScriptRootNode createNewTargetCall(CallTarget callTarget) {
        return NewTargetRootNode.createNewTargetCall(callTarget);
    }

    public JavaScriptRootNode createDropNewTarget(CallTarget callTarget) {
        return NewTargetRootNode.createDropNewTarget(callTarget);
    }

    public JavaScriptRootNode createConstructorRequiresNewRoot(JSContext context, SourceSection sourceSection) {
        // no JavaScriptRealmBoundaryRootNode: error should be thrown in the context of the caller!
        // ES6: 9.2.1. line 2.
        return new JavaScriptRootNode(context.getLanguage(), sourceSection, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                throw Errors.createTypeError("Class constructor requires 'new'");
            }
        };
    }

    public JavaScriptNode createDerivedConstructorResult(JavaScriptNode bodyNode, JavaScriptNode thisNode) {
        return ConstructorResultNode.createDerived(bodyNode, thisNode);
    }

    public JavaScriptNode createDerivedConstructorThis(JavaScriptNode thisNode) {
        return AccessDerivedConstructorThisNode.create(thisNode);
    }

    public JavaScriptNode createRequireObjectCoercible(JavaScriptNode argument) {
        return RequireObjectCoercibleWrapperNode.create(argument);
    }

    public FrameDescriptor createFrameDescriptor() {
        return new FrameDescriptor(Undefined.instance);
    }

    public FrameDescriptor createBlockFrameDescriptor() {
        FrameDescriptor desc = new FrameDescriptor(Undefined.instance);
        desc.addFrameSlot(ScopeFrameNode.PARENT_SCOPE_IDENTIFIER, FrameSlotKind.Object);
        return desc;
    }

    public DeclareGlobalNode createDeclareGlobalVariable(String varName, boolean configurable) {
        return new DeclareGlobalVariableNode(varName, configurable);
    }

    public DeclareGlobalNode createDeclareGlobalFunction(String varName, boolean configurable, JavaScriptNode valueNode) {
        return new DeclareGlobalFunctionNode(varName, configurable, valueNode);
    }

    public DeclareGlobalNode createDeclareGlobalLexicalVariable(String varName, boolean isConst) {
        return new DeclareGlobalLexicalVariableNode(varName, isConst);
    }

    public JavaScriptNode createGlobalDeclarationInstantiation(JSContext context, List<DeclareGlobalNode> declarations) {
        return GlobalDeclarationInstantiationNode.create(context, declarations);
    }

    public JavaScriptNode copy(JavaScriptNode node) {
        return node.copy();
    }

    public JavaScriptNode createToArrayIndex(JavaScriptNode operand) {
        return ToArrayIndexNode.ToArrayIndexWrapperNode.create(operand);
    }

    public JavaScriptNode createToObject(JSContext context, JavaScriptNode operand) {
        return JSToObjectNode.JSToObjectWrapperNode.createToObject(context, operand);
    }

    public JavaScriptNode createToObjectFromWith(JSContext context, JavaScriptNode operand, boolean checkForNullOrUndefined) {
        return JSToObjectNode.JSToObjectWrapperNode.createToObjectFromWith(context, operand, checkForNullOrUndefined);
    }

    public JavaScriptNode createAccessArgumentsArrayDirectly(JavaScriptNode writeArguments, JavaScriptNode readArguments, int leadingArgCount, int trailingArgCount) {
        return new AccessArgumentsArrayDirectlyNode(writeArguments, readArguments, leadingArgCount, trailingArgCount);
    }

    public JavaScriptNode createCallApplyArguments(JSContext context, JSFunctionCallNode callNode) {
        return CallApplyArgumentsNode.create(context, callNode);
    }

    public JavaScriptNode createGuardDisconnectedArgumentRead(int index, ReadElementNode readElementNode, JavaScriptNode argumentsArray, FrameSlot slot) {
        return JSGuardDisconnectedArgumentRead.create(index, readElementNode, argumentsArray, slot);
    }

    public JavaScriptNode createGuardDisconnectedArgumentWrite(int index, WriteElementNode argumentsArrayAccess, JavaScriptNode argumentsArray, JavaScriptNode rhs, FrameSlot slot) {
        return JSGuardDisconnectedArgumentWrite.create(index, argumentsArrayAccess, argumentsArray, rhs, slot);
    }

    public JavaScriptNode createSetModuleEnvironment(JSModuleRecord moduleRecord) {
        return new StatementNode() {
            private final JSModuleRecord module = moduleRecord;

            @Override
            public Object execute(VirtualFrame frame) {
                module.setEnvironment(frame.materialize());
                return EMPTY;
            }

            @Override
            protected JavaScriptNode copyUninitialized() {
                return copy();
            }
        };
    }

    public JavaScriptNode createReadModuleImportBinding(JSModuleRecord moduleRecord, String bindingName) {
        class ModuleEnvFrameNode extends ScopeFrameNode {
            private final JSModuleRecord module;

            ModuleEnvFrameNode(JSModuleRecord module) {
                this.module = module;
            }

            @Override
            public Frame executeFrame(Frame f) {
                return module.getEnvironment();
            }
        }
        return new JavaScriptNode() {
            private final JSModuleRecord module = moduleRecord;

            @Override
            public Object execute(VirtualFrame frame) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                MaterializedFrame environment = module.getEnvironment();
                FrameSlot frameSlot = environment.getFrameDescriptor().findFrameSlot(bindingName);
                assert frameSlot != null;
                return replace(JSReadFrameSlotNode.create(frameSlot, new ModuleEnvFrameNode(module), JSFrameUtil.hasTemporalDeadZone(frameSlot))).execute(frame);
            }

            @Override
            protected JavaScriptNode copyUninitialized() {
                return copy();
            }
        };
    }

    public JavaScriptNode createCopyDataProperties(JavaScriptNode targetObj, JavaScriptNode source, JavaScriptNode excludedNames) {
        return CopyDataPropertiesNode.create(targetObj, source, excludedNames);
    }

    public JavaScriptNode createToPropertyKey(JavaScriptNode key) {
        return JSToPropertyKeyWrapperNode.create(key);
    }

    public IfNode copyIfWithCondition(IfNode origIfNode, JavaScriptNode condition) {
        return IfNode.create(condition, origIfNode.getThenPart(), origIfNode.getElsePart());
    }

    // #####

    public static NodeFactory getDefaultInstance() {
        return FACTORY;
    }

    public static NodeFactory getInstance(JSContext context) {
        return (NodeFactory) context.getNodeFactory();
    }
}

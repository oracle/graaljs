package com.oracle.truffle.js.nodes.tags;

import com.oracle.truffle.api.instrumentation.Tag;

public class JSSpecificTags {

    /*** Calls ***/
    @Tag.Identifier("FunctionCallTag")
    public static class FunctionCallTag extends Tag {
        /**
         * @Provides "isNew"
         * @Provides "isInvoke"
         */
        private FunctionCallTag() {
        }
    }

    /*** Builtin operations ***/
    @Tag.Identifier("BuiltinRootTag")
    public static class BuiltinRootTag extends Tag {
        /**
         * @Provides "name"
         */
        private BuiltinRootTag() {
        }
    }

    /*** Eval ***/
    @Tag.Identifier("EvalCallTag")
    public static class EvalCallTag extends Tag {

        private EvalCallTag() {
        }
    }

    /*** New object instances ***/
    @Tag.Identifier("ObjectAllocationTag")
    public static class ObjectAllocationTag extends Tag {
        private ObjectAllocationTag() {
        }
    }

    /*** Literals ***/
    @Tag.Identifier("LiteralTag")
    public static class LiteralTag extends Tag {
        public static enum Type {
            ObjectLiteral,
            ArrayLiteral,
            FunctionLiteral,
            NumericLiteral,
            BooleanLiteral,
            StringLiteral,
            NullLiteral,
            UndefinedLiteral,
            RegExpLiteral,
        }

        /**
         * @Provides "type"
         */
        private LiteralTag() {
        }
    }

    /*** Unary operations ***/
    @Tag.Identifier("UnaryOperationTag")
    public static class UnaryOperationTag extends Tag {
        /**
         * @Provides "operator"
         */
        private UnaryOperationTag() {
        }
    }

    /*** Binary operations ***/
    @Tag.Identifier("BinaryOperationTag")
    public static class BinaryOperationTag extends Tag {
        /**
         * @Provides "operator"
         */
        private BinaryOperationTag() {
        }
    }

    /*** Conditional operations ***/
    @Tag.Identifier("ConditionalExpressionTag")
    public static class ConditionalExpressionTag extends Tag {
        private ConditionalExpressionTag() {
        }
    }

    /*** Local variables write ***/
    @Tag.Identifier("VariableWriteTag")
    public static class VariableWriteTag extends Tag {
        /**
         * @Provides "name"
         */
        private VariableWriteTag() {
        }
    }

    /*** Local variables read ***/
    @Tag.Identifier("VariableReadTag")
    public static class VariableReadTag extends Tag {
        /**
         * @Provides "name"
         */
        private VariableReadTag() {
        }
    }

    /*** Element read operations ***/
    @Tag.Identifier("ElementReadTag")
    public static class ElementReadTag extends Tag {
        private ElementReadTag() {
        }

    }

    /*** Element write operations ***/
    @Tag.Identifier("ElementWriteTag")
    public static class ElementWriteTag extends Tag {
        private ElementWriteTag() {
        }

    }

    /*** Property reads ***/
    @Tag.Identifier("PropertyReadTag")
    public static class PropertyReadTag extends Tag {
        /**
         * @Provides "key"
         */
        private PropertyReadTag() {
        }
    }

    /*** Property writes ***/
    @Tag.Identifier("PropertyWriteTag")
    public static class PropertyWriteTag extends Tag {

        /**
         * @Provides "key"
         */
        private PropertyWriteTag() {
        }
    }

    public static NodeObjectDescriptor createNodeObjectDescriptor() {
        return new NodeObjectDescriptor();
    }
}

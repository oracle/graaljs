package com.oracle.js.parser.ir;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class ClassElement extends Node {

    /**
     * Class element kinds types:
     * - method
     * - accessor (getter, setter)
     * - field
     */
    private static final int KIND_METHOD = 1 << 0;
    private static final int KIND_ACCESSOR = 1 << 1;
    private static final int KIND_FIELD = 1 << 2;

    /**
     * Class element placement types:
     * - static
     * - prototype
     * - own
     */
    private static final int PLACEMENT_STATIC = 1 << 0;
    private static final int PLACEMENT_PROTOTYPE = 1 << 1;
    private static final int PLACEMENT_OWN = 1 << 2;

    /**
     * PropertyDescriptor values:
     * - writable
     * - configurable
     * - enumerable
     */
    private static final int WRITABLE_TRUE = 1 << 0;
    private static final int WRITABLE_FALSE = 1 << 1;
    private static final int CONFIGURABLE_TRUE = 1 << 2;
    private static final int CONFIGURABLE_FALSE = 1 << 3;
    private static final int ENUMERABLE_TRUE = 1 << 4;
    private static final int ENUMERABLE_FALSE = 1 << 5;

    /** Class element kind. */
    private final int kind;

    /** Class element key. */
    private final Expression key;

    /** Class element property descriptor values. These values are handled in the Parser for coalescing of class elements. */
    private final int propertyDescriptorValues;

    /** Class element value. Value for method kind, Initialize for field kind.  */
    private final Expression value;

    /** Class element get. */
    private final FunctionNode get;

    /** Class element set. */
    private final FunctionNode set;

    /** Class element placement. Placements are handled in the Parser for coalescing of class elements. */
    private final int placement;

    /** Class element decorators. */
    private final List<Expression> decorators;

    private final boolean isPrivate;
    private final boolean hasComputedKey;
    private final boolean isAnonymousFunctionDefinition;

    private ClassElement(long token, int finish, int kind, Expression key, Expression value, FunctionNode get, FunctionNode set, int placement, int propertyDescriptorValues, List<Expression> decorators,
                         boolean isPrivate, boolean hasComputedKey, boolean isAnonymousFunctionDefinition) {
        super(token, finish);
        this.kind = kind;
        this.key = key;
        this.value = value;
        this.get = get;
        this.set = set;
        this.placement = placement;
        this.propertyDescriptorValues = propertyDescriptorValues;
        this.decorators = decorators;
        this.isPrivate = isPrivate;
        this.hasComputedKey = hasComputedKey;
        this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
    }

    private ClassElement(ClassElement element, int kind,  Expression key, Expression value, FunctionNode get, FunctionNode set, int placement, int propertyDescriptorValues, List<Expression> decorators,
                         boolean isPrivate, boolean hasComputedKey, boolean isAnonymousFunctionDefinition) {
        super(element);
        this.kind = kind;
        this.key = key;
        this.value = value;
        this.get = get;
        this.set = set;
        this.placement = placement;
        this.propertyDescriptorValues = propertyDescriptorValues;
        this.decorators = decorators;
        this.isPrivate = isPrivate;
        this.hasComputedKey = hasComputedKey;
        this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
    }

    public static ClassElement createMethod(long token, int finish, Expression key, Expression value, List<Expression> decorators,
                                            boolean isPrivate, boolean isStatic, boolean hasComputedKey) {
        int placement = isStatic ? PLACEMENT_STATIC : isPrivate ? PLACEMENT_OWN : PLACEMENT_PROTOTYPE;
        int propertyDescriptorValues = isPrivate ? ENUMERABLE_FALSE + CONFIGURABLE_FALSE + WRITABLE_FALSE : ENUMERABLE_TRUE + CONFIGURABLE_TRUE + WRITABLE_TRUE;
        return new ClassElement(token, finish, KIND_METHOD, key, value, null, null, placement, propertyDescriptorValues, decorators, isPrivate, hasComputedKey, false);
    }

    public static ClassElement createAccessor(long token, int finish, Expression key, FunctionNode get, FunctionNode set, List<Expression> decorators,
                                              boolean isPrivate, boolean isStatic, boolean hasComputedKey) {
        int placement = isStatic ? PLACEMENT_STATIC : isPrivate ? PLACEMENT_OWN : PLACEMENT_PROTOTYPE;
        int propertyDescriptorValues = isPrivate ? ENUMERABLE_FALSE + CONFIGURABLE_FALSE: ENUMERABLE_TRUE + CONFIGURABLE_TRUE;
        return new ClassElement(token, finish, KIND_ACCESSOR, key, null, get, set, placement, propertyDescriptorValues, decorators, isPrivate, hasComputedKey, false);
    }

    public static ClassElement createField(long token, int finish, Expression key, Expression initialize, List<Expression> decorators,
                                           boolean isPrivate, boolean isStatic, boolean hasComputedKey, boolean isAnonymousFunctionDefinition) {
        int placement = isStatic ? PLACEMENT_STATIC : PLACEMENT_OWN;
        int propertyDescriptorValues = isPrivate ? ENUMERABLE_FALSE + CONFIGURABLE_FALSE + WRITABLE_FALSE : ENUMERABLE_TRUE + CONFIGURABLE_TRUE + WRITABLE_TRUE;
        return new ClassElement(token, finish, KIND_FIELD, key, initialize, null, null, placement, propertyDescriptorValues, decorators, isPrivate, hasComputedKey, isAnonymousFunctionDefinition);
    }

    public static ClassElement createDefaultConstructor(long token, int finish, Expression key, Expression value) {
        return new ClassElement(token, finish, KIND_METHOD, key, value, null, null, 0, 0, null, false, false, false);
    }

    @Override
    public Node accept(NodeVisitor<? extends LexicalContext> visitor) {
        if(visitor.enterClassElement(this)) {
            ClassElement element =
                setKey((Expression) key.accept(visitor)).
                setValue(value == null ? null: (Expression) value.accept(visitor)).
                setGetter(get == null ? null : (FunctionNode) get.accept(visitor)).
                setSetter(set == null ? null : (FunctionNode) set.accept(visitor));
            if(decorators != null) {
                List<Expression> d = new ArrayList<>();
                for (Expression decorator : decorators) {
                    d.add((Expression) decorator.accept(visitor));
                }
                element = element.setDecorators(d);
            } else {
                element = element.setDecorators(null);
            }
            return element;
        }
        return this;
    }

    @Override
    public <R> R accept(TranslatorNodeVisitor<? extends LexicalContext, R> visitor) {
        return visitor.enterClassElement(this);
    }

    public List<Expression> getDecorators() {
        return decorators;
    }

    public ClassElement setDecorators(List<Expression> decorators) {
        if(this.decorators == decorators) {
            return this;
        }
        return new ClassElement(this, kind, key, value, get, set, placement, propertyDescriptorValues, decorators, isPrivate, hasComputedKey, isAnonymousFunctionDefinition);
    }

    public FunctionNode getGetter() {
        return get;
    }

    public ClassElement setGetter(FunctionNode get) {
        if(this.get == get) {
            return this;
        }
        return new ClassElement(this, kind, key, value, get, set, placement, propertyDescriptorValues, decorators, isPrivate, hasComputedKey, isAnonymousFunctionDefinition);
    }

    public Expression getKey() {
        return key;
    }

    public ClassElement setKey(Expression key) {
        if(this.key == key) {
            return this;
        }
        return new ClassElement(this, kind, key, value, get, set, placement, propertyDescriptorValues, decorators, isPrivate, hasComputedKey, isAnonymousFunctionDefinition);
    }

    public String getKeyName() {
        return key instanceof PropertyKey ? ((PropertyKey) key).getPropertyName() : null;
    }

    public int getKind() {
        return kind;
    }

    public int getPlacement() {
        return placement;
    }

    public String getPrivateName() {
        assert isPrivate;
        return ((IdentNode) key).getName();
    }

    public FunctionNode getSetter() {
        return set;
    }

    public ClassElement setSetter(FunctionNode set) {
        if(this.set == set) {
            return this;
        }
        return new ClassElement(this, kind, key, value, get, set, placement, propertyDescriptorValues, decorators, isPrivate, hasComputedKey, isAnonymousFunctionDefinition);
    }

    public Expression getValue() {
        return value;
    }

    public ClassElement setValue(Expression value) {
        if(this.value == value) {
            return this;
        }
        return new ClassElement(this, kind, key, value, get, set, placement, propertyDescriptorValues, decorators, isPrivate, hasComputedKey, isAnonymousFunctionDefinition);
    }

    public boolean hasComputedKey() {
        return hasComputedKey;
    }

    public boolean isAccessor() {
        return (kind & KIND_ACCESSOR) != 0;
    }

    public boolean isConfigurable() {
        return (propertyDescriptorValues & CONFIGURABLE_TRUE) != 0;
    }

    public boolean isField() {
        return (kind & KIND_FIELD) != 0;
    }

    public boolean isMethod() {
        return (kind & KIND_METHOD) != 0;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isStatic() {
        return (placement & PLACEMENT_STATIC) != 0;
    }

    @Override
    public void toString(StringBuilder sb, boolean printType) {
        if(decorators != null) {
            for (Expression decorator : decorators) {
                sb.append("@");
                decorator.toString(sb, printType);
                sb.append(" ");
            }
        }
        if(isStatic())
        {
            sb.append("static ");
        }
        if(isMethod()) {
            toStringKey(sb, printType);
            ((FunctionNode) value).toStringTail(sb, printType);
        }
        if(isAccessor()) {
            if(get != null) {
                sb.append("get ");
                toStringKey(sb, printType);
                get.toStringTail(sb, printType);
            }
            if(set != null) {
                sb.append("set ");
                toStringKey(sb, printType);
                set.toStringTail(sb, printType);
            }
        }
        if(isField()) {
            toStringKey(sb, printType);
            if(value != null) {
                value.toString(sb, printType);
            }
        }
    }

    private void toStringKey(final StringBuilder sb, final boolean printType) {
        if (hasComputedKey) {
            sb.append('[');
        }
        key.toString(sb, printType);
        if (hasComputedKey) {
            sb.append(']');
        }
    }
}

package com.oracle.js.parser.ir;

import com.oracle.js.parser.ir.visitor.NodeVisitor;
import com.oracle.js.parser.ir.visitor.TranslatorNodeVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * IR representation for class elements.
 */
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

    /** Class element kind. */
    private final int kind;

    /** Class element key. */
    private final Expression key;

    /** Class element value. Value for method kind, Initialize for field kind.  */
    private final Expression value;

    /** Class element get. */
    private final FunctionNode get;

    /** Class element set. */
    private final FunctionNode set;

    /** Class element decorators. */
    private final List<Expression> decorators;

    private final boolean hasComputedKey;
    private final boolean isAnonymousFunctionDefinition;
    private final boolean isPrivate;
    private final boolean isStatic;

    private ClassElement(long token, int finish, int kind, Expression key, Expression value, FunctionNode get, FunctionNode set, List<Expression> decorators,
                         boolean hasComputedKey, boolean isAnonymousFunctionDefinition, boolean isPrivate, boolean isStatic) {
        super(token, finish);
        this.kind = kind;
        this.key = key;
        this.value = value;
        this.get = get;
        this.set = set;
        this.decorators = decorators;
        this.hasComputedKey = hasComputedKey;
        this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
        this.isPrivate = isPrivate;
        this.isStatic = isStatic;
    }

    private ClassElement(ClassElement element, int kind,  Expression key, Expression value, FunctionNode get, FunctionNode set, List<Expression> decorators,
                         boolean hasComputedKey, boolean isAnonymousFunctionDefinition, boolean isPrivate, boolean isStatic) {
        super(element);
        this.kind = kind;
        this.key = key;
        this.value = value;
        this.get = get;
        this.set = set;
        this.decorators = decorators;
        this.hasComputedKey = hasComputedKey;
        this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
        this.isPrivate = isPrivate;
        this.isStatic = isStatic;
    }

    /**
     * @param token
     * @param finish
     * @param key The name of the method.
     * @param value The value of the method.
     * @param decorators The decorators of the method. Optional.
     * @param isPrivate
     * @param isStatic
     * @param hasComputedKey
     * @return A ClassElement node representing a method.
     */
    public static ClassElement createMethod(long token, int finish, Expression key, Expression value, List<Expression> decorators,
                                            boolean isPrivate, boolean isStatic, boolean hasComputedKey) {
        return new ClassElement(token, finish, KIND_METHOD, key, value, null, null, decorators, hasComputedKey, false, isPrivate, isStatic);
    }

    /**
     * @param token
     * @param finish
     * @param key The name of the accessor.
     * @param get The getter of the accessor. Optional.
     * @param set The setter of the accessor. Optional.
     * @param decorators The decorators of the accessor. Optional.
     * @param isPrivate
     * @param isStatic
     * @param hasComputedKey
     * @return A ClassElement node representing an accessor (getter, setter).
     */
    public static ClassElement createAccessor(long token, int finish, Expression key, FunctionNode get, FunctionNode set, List<Expression> decorators,
                                              boolean isPrivate, boolean isStatic, boolean hasComputedKey) {
        return new ClassElement(token, finish, KIND_ACCESSOR, key, null, get, set, decorators, hasComputedKey, false, isPrivate,isStatic);
    }

    /**
     * @param token
     * @param finish
     * @param key The name of the field.
     * @param initialize The initialization value of the field. Optional.
     * @param decorators The decorators of the field. Optional.
     * @param isPrivate
     * @param isStatic
     * @param hasComputedKey
     * @param isAnonymousFunctionDefinition
     * @return A ClassElement node representing a field.
     */
    public static ClassElement createField(long token, int finish, Expression key, Expression initialize, List<Expression> decorators,
                                           boolean isPrivate, boolean isStatic, boolean hasComputedKey, boolean isAnonymousFunctionDefinition) {
        return new ClassElement(token, finish, KIND_FIELD, key, initialize, null, null, decorators, hasComputedKey, isAnonymousFunctionDefinition, isPrivate, isStatic);
    }

    /**
     * @param token
     * @param finish
     * @param key
     * @param value
     * @return A ClassElement node representing a default constructor.
     */
    public static ClassElement createDefaultConstructor(long token, int finish, Expression key, Expression value) {
        return new ClassElement(token, finish, KIND_METHOD, key, value, null, null, null, false, false, false, false);
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

    public List<Expression> getDecorators() { return decorators; }

    public ClassElement setDecorators(List<Expression> decorators) {
        if(this.decorators == decorators) {
            return this;
        }
        return new ClassElement(this, kind, key, value, get, set, decorators, hasComputedKey, isAnonymousFunctionDefinition, isPrivate, isStatic);
    }

    public FunctionNode getGetter() { return get; }

    public ClassElement setGetter(FunctionNode get) {
        if(this.get == get) {
            return this;
        }
        return new ClassElement(this, kind, key, value, get, set, decorators, hasComputedKey, isAnonymousFunctionDefinition, isPrivate, isStatic);
    }

    public Expression getKey() { return key; }

    public ClassElement setKey(Expression key) {
        if(this.key == key) {
            return this;
        }
        return new ClassElement(this, kind, key, value, get, set, decorators, hasComputedKey, isAnonymousFunctionDefinition, isPrivate, isStatic);
    }

    public String getKeyName() { return key instanceof PropertyKey ? ((PropertyKey) key).getPropertyName() : null; }

    public String getPrivateName() {
        assert isPrivate;
        return ((IdentNode) key).getName();
    }

    public FunctionNode getSetter() { return set; }

    public ClassElement setSetter(FunctionNode set) {
        if(this.set == set) {
            return this;
        }
        return new ClassElement(this, kind, key, value, get, set, decorators, hasComputedKey, isAnonymousFunctionDefinition, isPrivate, isStatic);
    }

    public Expression getValue() { return value; }

    public ClassElement setValue(Expression value) {
        if(this.value == value) {
            return this;
        }
        return new ClassElement(this, kind, key, value, get, set, decorators, hasComputedKey, isAnonymousFunctionDefinition, isPrivate, isStatic);
    }

    public boolean hasComputedKey() { return hasComputedKey; }

    public boolean isAccessor() { return (kind & KIND_ACCESSOR) != 0; }

    public boolean isAnonymousFunctionDefinition() { return isAnonymousFunctionDefinition; }

    public boolean isField() { return (kind & KIND_FIELD) != 0; }

    public boolean isMethod() { return (kind & KIND_METHOD) != 0; }

    public boolean isPrivate() { return isPrivate; }

    public boolean isStatic() { return isStatic; }


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

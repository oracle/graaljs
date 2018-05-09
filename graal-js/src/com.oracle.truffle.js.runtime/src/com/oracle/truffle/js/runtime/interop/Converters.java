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
package com.oracle.truffle.js.runtime.interop;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class Converters {
    public abstract static class ConverterFactory {
        public abstract boolean accept(Class<?> destType);

        public abstract boolean accept(Class<?> destType, Object argument);

        public abstract Converter make(Class<?> destType);
    }

    public abstract static class Converter {

        public abstract Object convert(Object argument);

        public abstract boolean guard(Object argument);
    }

    public static final ConverterFactory IDENTITY_CONVERTER_FACTORY = new IdentityConverterFactory();
    public static final ConverterFactory PRIMITIVE_CONVERTER_FACTORY = new PrimitiveConverterFactory();
    public static final ConverterFactory SAMTYPE_CONVERTER_FACTORY = new SamTypeConverterFactory();
    public static final ConverterFactory LOSSY_PRIMITIVE_CONVERTER_FACTORY = new LossyPrimitiveConverterFactory();
    public static final ConverterFactory STRING_TO_PRIMITIVE_CONVERTER_FACTORY = new StringToPrimitiveConverterFactory();
    public static final ConverterFactory JSOBJECT_TO_STRING_CONVERTER_FACTORY = new JSObjectToStringConverterFactory();
    public static final ConverterFactory JSOBJECT_TO_BOOLEAN_CONVERTER_FACTORY = new JSObjectToBooleanConverterFactory();
    public static final ConverterFactory JSOBJECT_TO_NUMBER_CONVERTER_FACTORY = new JSObjectToNumberConverterFactory();

    public static final Converter JAVA_TO_JS_CONVERTER = new JavaToJSConverter();
    public static final Converter JS_TO_JAVA_CONVERTER;

    public static final ConverterFactory[] COERCIONLESS_CONVERTER_FACTORIES;
    public static final ConverterFactory[] DEFAULT_CONVERTER_FACTORIES;
    public static final ConverterFactory[] FORCING_CONVERTER_FACTORIES;

    static {
        COERCIONLESS_CONVERTER_FACTORIES = new ConverterFactory[]{new JSToJavaConverterFactory(IDENTITY_CONVERTER_FACTORY)};
        DEFAULT_CONVERTER_FACTORIES = new ConverterFactory[]{new JSToJavaConverterFactory(IDENTITY_CONVERTER_FACTORY), new JSToJavaConverterFactory(PRIMITIVE_CONVERTER_FACTORY),
                        new JSToJavaConverterFactory(SAMTYPE_CONVERTER_FACTORY), new TruffleJavaObjectConverterFactory(IDENTITY_CONVERTER_FACTORY)};
        FORCING_CONVERTER_FACTORIES = new ConverterFactory[]{new JSToJavaConverterFactory(IDENTITY_CONVERTER_FACTORY), new JSToJavaConverterFactory(PRIMITIVE_CONVERTER_FACTORY),
                        new JSToJavaConverterFactory(LOSSY_PRIMITIVE_CONVERTER_FACTORY), new JSToJavaConverterFactory(STRING_TO_PRIMITIVE_CONVERTER_FACTORY),
                        new JSToJavaConverterFactory(SAMTYPE_CONVERTER_FACTORY), new JSToJavaConverterFactory(JSOBJECT_TO_STRING_CONVERTER_FACTORY),
                        new JSToJavaConverterFactory(JSOBJECT_TO_BOOLEAN_CONVERTER_FACTORY), new JSToJavaConverterFactory(JSOBJECT_TO_NUMBER_CONVERTER_FACTORY)};
        JS_TO_JAVA_CONVERTER = SerialConverterAdapter.fromFactories(Object.class, COERCIONLESS_CONVERTER_FACTORIES);
    }

    public static class IdentityConverterFactory extends ConverterFactory {
        @Override
        public boolean accept(Class<?> destType) {
            return true;
        }

        @Override
        public boolean accept(Class<?> destType, Object argument) {
            return destType.isInstance(argument) || (!destType.isPrimitive() && argument == null) || (destType.isPrimitive() && JavaMethod.getBoxedType(destType).isInstance(argument));
        }

        @Override
        public Converter make(Class<?> destType) {
            return new IdentityConverter(destType);
        }
    }

    public static class IdentityConverter extends Converter {
        private final Class<?> destType;

        public IdentityConverter(Class<?> destType) {
            this.destType = destType;
        }

        @Override
        public Object convert(Object argument) {
            return argument;
        }

        @Override
        public boolean guard(Object argument) {
            return IDENTITY_CONVERTER_FACTORY.accept(destType, argument);
        }
    }

    public static class PrimitiveConverterFactory extends ConverterFactory {
        @Override
        public boolean accept(Class<?> destType) {
            return (destType == double.class || destType == Double.class) || //
                            (destType == int.class || destType == Integer.class) || //
                            (destType == float.class || destType == Float.class) || //
                            (destType == boolean.class || destType == Boolean.class) || //
                            (destType == long.class || destType == Long.class);
        }

        @Override
        public boolean accept(Class<?> destType, Object argument) {
            if (destType == double.class || destType == Double.class) {
                if (argument instanceof Integer) {
                    return true;
                } else if (argument == Undefined.instance) {
                    return true;
                }
            } else if (destType == int.class || destType == Integer.class) {
                if (argument instanceof Double && JSRuntime.doubleIsRepresentableAsInt((double) argument)) {
                    return true;
                } else if (argument == Undefined.instance) {
                    return true;
                }
            } else if (destType == float.class || destType == Float.class) {
                if (argument instanceof Integer) {
                    return true;
                } else if (argument instanceof Double) {
                    return true; // lossy conversion (GR-2050)
                } else if (argument == Undefined.instance) {
                    return true;
                }
            } else if (destType == boolean.class || destType == Boolean.class) {
                if (argument instanceof Integer) {
                    return true;
                } else if (argument == Undefined.instance) {
                    return true;
                }
            } else if (destType == long.class || destType == Long.class) {
                if (argument instanceof Integer) {
                    return true;
                } else if (argument instanceof Double && (long) (double) argument == (double) argument) {
                    return true;
                } else if (argument == Undefined.instance) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Converter make(Class<?> destType) {
            return new PrimitiveConverter(destType);
        }
    }

    public static class PrimitiveConverter extends Converter {
        private final Class<?> destType;

        public PrimitiveConverter(Class<?> destType) {
            this.destType = destType;
        }

        @Override
        public Object convert(Object value) {
            if (destType == double.class || destType == Double.class) {
                if (value instanceof Integer) {
                    return (double) (int) value;
                } else if (value == Undefined.instance) {
                    return Double.NaN;
                }
            } else if (destType == int.class || destType == Integer.class) {
                if (value instanceof Double && JSRuntime.doubleIsRepresentableAsInt((double) value)) {
                    return (int) (double) value;
                } else if (value == Undefined.instance) {
                    return 0;
                }
            } else if (destType == float.class || destType == Float.class) {
                if (value instanceof Integer) {
                    return (float) (int) value;
                } else if (value instanceof Double) {
                    return (float) (double) value; // lossy conversion (GR-2050)
                } else if (value == Undefined.instance) {
                    return Float.NaN;
                }
            } else if (destType == boolean.class || destType == Boolean.class) {
                if (value instanceof Integer) {
                    return (int) value != 0;
                } else if (value == Undefined.instance) {
                    return false;
                }
            } else if (destType == long.class || destType == Long.class) {
                if (value instanceof Integer) {
                    return (long) (int) value;
                } else if (value instanceof Double && (long) (double) value == (double) value) {
                    return (long) (double) value;
                } else if (value == Undefined.instance) {
                    return 0L;
                }
            }
            CompilerDirectives.transferToInterpreter();
            throw new IllegalArgumentException();
        }

        @Override
        public boolean guard(Object argument) {
            return PRIMITIVE_CONVERTER_FACTORY.accept(destType, argument);
        }
    }

    public static class LossyPrimitiveConverterFactory extends ConverterFactory {
        @Override
        public boolean accept(Class<?> destType) {
            Class<?> boxedType;
            if (!destType.isPrimitive()) {
                boxedType = destType;
            } else {
                assert destType != void.class;
                boxedType = JavaMethod.getBoxedType(destType);
            }
            return Number.class.isAssignableFrom(boxedType);
        }

        @Override
        public boolean accept(Class<?> destType, Object argument) {
            return accept(destType) && argument instanceof Number;
        }

        @Override
        public Converter make(Class<?> destType) {
            return new LossyPrimitiveConverter(destType);
        }
    }

    public static class LossyPrimitiveConverter extends Converter {
        private final Class<?> destType;

        public LossyPrimitiveConverter(Class<?> destType) {
            this.destType = destType;
        }

        @Override
        public Object convert(Object value) {
            Number n = (Number) value;
            if (destType == double.class || destType == Double.class) {
                return JSRuntime.doubleValue(n);
            } else if (destType == int.class || destType == Integer.class) {
                return JSRuntime.intValue(n);
            } else if (destType == float.class || destType == Float.class) {
                return JSRuntime.floatValue(n);
            } else if (destType == long.class || destType == Long.class) {
                return JSRuntime.longValue(n);
            }
            CompilerDirectives.transferToInterpreter();
            throw new IllegalArgumentException(Boundaries.stringValueOf(value));
        }

        @Override
        public boolean guard(Object argument) {
            return argument instanceof Number;
        }
    }

    public static class StringToPrimitiveConverterFactory extends ConverterFactory {
        @Override
        public boolean accept(Class<?> destType) {
            Class<?> boxedType;
            if (!destType.isPrimitive()) {
                boxedType = destType;
            } else {
                assert destType != void.class;
                boxedType = JavaMethod.getBoxedType(destType);
            }
            return Number.class.isAssignableFrom(boxedType) || boxedType == Boolean.class;
        }

        @Override
        public boolean accept(Class<?> destType, Object argument) {
            return accept(destType) && argument instanceof String;
        }

        @Override
        public Converter make(Class<?> destType) {
            return new StringToPrimitiveConverter(destType);
        }
    }

    public static class StringToPrimitiveConverter extends Converter {
        private final Class<?> destType;

        public StringToPrimitiveConverter(Class<?> destType) {
            this.destType = destType;
        }

        @Override
        public Object convert(Object value) {
            String s = (String) value;
            if (destType == double.class || destType == Double.class) {
                return Boundaries.doubleValueOf(s);
            } else if (destType == int.class || destType == Integer.class) {
                return Boundaries.integerValueOf(s);
            } else if (destType == float.class || destType == Float.class) {
                return Boundaries.floatValueOf(s);
            } else if (destType == long.class || destType == Long.class) {
                return Boundaries.longValueOf(s);
            } else if (destType == boolean.class || destType == Boolean.class) {
                return s.length() != 0;
            }
            CompilerDirectives.transferToInterpreter();
            throw new IllegalArgumentException(s);
        }

        @Override
        public boolean guard(Object argument) {
            return argument instanceof String;
        }
    }

    public static class SamTypeConverterFactory extends ConverterFactory {
        @Override
        public boolean accept(Class<?> destType) {
            return JavaClass.isAbstract(destType);
        }

        @Override
        public boolean accept(Class<?> destType, Object value) {
            if (accept(destType) && JSFunction.isJSFunction(value)) {
                JavaClass javaClass = JavaClass.forClass(destType);
                if (javaClass.isSamType()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Converter make(Class<?> destType) {
            return new SamTypeConverter(destType);
        }
    }

    public static class SamTypeConverter extends Converter {
        private final JavaClass destJavaClass;
        @CompilationFinal private MethodHandle constructor;

        public SamTypeConverter(Class<?> destType) {
            this.destJavaClass = JavaClass.forClass(destType);
        }

        @Override
        public Object convert(Object argument) {
            if (constructor == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                initialize(argument);
            }
            try {
                return constructor.invokeExact((DynamicObject) argument);
            } catch (ControlFlowException | GraalJSException e) {
                throw e;
            } catch (Throwable e) {
                CompilerDirectives.transferToInterpreter();
                throw UserScriptException.createJavaException(e);
            }
        }

        private void initialize(Object argument) {
            // TODO pass context to constructor and move this code there
            JavaClass extendedClass = destJavaClass.extend(null, null);
            this.constructor = extendedClass.getBestConstructor(new Object[]{argument}).getFirst().getMethodHandle().asType(MethodType.methodType(Object.class, DynamicObject.class));
        }

        @Override
        public boolean guard(Object argument) {
            return JSFunction.isJSFunction(argument);
        }
    }

    public static class JSObjectToStringConverterFactory extends ConverterFactory {
        @Override
        public boolean accept(Class<?> destType) {
            return destType == String.class;
        }

        @Override
        public boolean accept(Class<?> destType, Object value) {
            return accept(destType) && JSObject.isDynamicObject(value);
        }

        @Override
        public Converter make(Class<?> destType) {
            return new JSObjectToStringTypeConverter();
        }
    }

    public static class JSObjectToStringTypeConverter extends Converter {
        @Override
        public Object convert(Object argument) {
            return JSRuntime.toString(argument);
        }

        @Override
        public boolean guard(Object argument) {
            return JSObject.isDynamicObject(argument);
        }
    }

    public static class JSObjectToBooleanConverterFactory extends ConverterFactory {
        @Override
        public boolean accept(Class<?> destType) {
            return destType == boolean.class || destType == Boolean.class;
        }

        @Override
        public boolean accept(Class<?> destType, Object value) {
            return accept(destType) && (JSObject.isDynamicObject(value) || value == null);
        }

        @Override
        public Converter make(Class<?> destType) {
            return new JSObjectToBooleanTypeConverter();
        }
    }

    public static class JSObjectToBooleanTypeConverter extends Converter {
        @Override
        public Object convert(Object argument) {
            return argument != Undefined.instance && argument != Null.instance && argument != null;
        }

        @Override
        public boolean guard(Object argument) {
            return JSObject.isDynamicObject(argument);
        }
    }

    public static class JSObjectToNumberConverterFactory extends ConverterFactory {
        @Override
        public boolean accept(Class<?> destType) {
            return (destType == double.class || destType == Double.class) || //
                            (destType == int.class || destType == Integer.class) || //
                            (destType == float.class || destType == Float.class) || //
                            (destType == long.class || destType == Long.class);
        }

        @Override
        public boolean accept(Class<?> destType, Object value) {
            return accept(destType) && JSObject.isDynamicObject(value);
        }

        @Override
        public Converter make(Class<?> destType) {
            return new JSObjectToNumberTypeConverter(destType);
        }
    }

    public static class JSObjectToNumberTypeConverter extends Converter {
        private final Class<?> destType;

        public JSObjectToNumberTypeConverter(Class<?> destType) {
            this.destType = destType;
        }

        @Override
        public Object convert(Object argument) {
            DynamicObject jsObj = (DynamicObject) argument;
            Object value = JSRuntime.toPrimitive(jsObj, JSRuntime.HINT_NUMBER);

            if (destType == double.class || destType == Double.class) {
                if (value instanceof Integer) {
                    return (double) (int) value;
                } else if (value instanceof String) {
                    return Boundaries.doubleValueOf((String) value);
                }
            } else if (destType == int.class || destType == Integer.class) {
                if (value instanceof Double && JSRuntime.doubleIsRepresentableAsInt((double) value)) {
                    return (int) (double) value;
                } else if (value instanceof String) {
                    return Boundaries.integerValueOf((String) value);
                }
            } else if (destType == float.class || destType == Float.class) {
                if (value instanceof Integer) {
                    return (float) (int) value;
                } else if (value instanceof Double) {
                    return (float) (double) value; // lossy conversion (GR-2050)
                } else if (value instanceof String) {
                    return Boundaries.floatValueOf((String) value);
                }
            } else if (destType == long.class || destType == Long.class) {
                if (value instanceof Integer) {
                    return (long) (int) value;
                } else if (value instanceof Double && (long) (double) value == (double) value) {
                    return (long) (double) value;
                } else if (value instanceof String) {
                    return Boundaries.longValueOf((String) value);
                }
            }

            CompilerDirectives.transferToInterpreter();
            throw new IllegalArgumentException();
        }

        @Override
        public boolean guard(Object argument) {
            return JSObject.isDynamicObject(argument);
        }
    }

    public static class CombineConverter extends Converter {
        private final Converter one;
        private final Converter two;

        public CombineConverter(Converter one, Converter two) {
            this.one = one;
            this.two = two;
        }

        @Override
        public Object convert(Object argument) {
            return two.convert(one.convert(argument));
        }

        @Override
        public boolean guard(Object argument) {
            return one.guard(argument) && two.guard(one.convert(argument));
        }
    }

    public static class OrConverter extends Converter {
        private final Converter one;
        private final Converter two;

        public OrConverter(Converter one, Converter two) {
            this.one = one;
            this.two = two;
        }

        @Override
        public Object convert(Object argument) {
            return one.guard(argument) ? one.convert(argument) : two.convert(argument);
        }

        @Override
        public boolean guard(Object argument) {
            return one.guard(argument) || two.guard(argument);
        }
    }

    public static class OptionalConverter extends Converter {
        private final Converter one;
        private final Converter two;

        public OptionalConverter(Converter one, Converter two) {
            this.one = one;
            this.two = two;
        }

        @Override
        public Object convert(Object argument) {
            return two.convert(one.guard(argument) ? one.convert(argument) : argument);
        }

        @Override
        public boolean guard(Object argument) {
            return two.guard(one.guard(argument) ? one.convert(argument) : argument);
        }
    }

    public static class JSToJavaConverterFactory extends ConverterFactory {
        private final ConverterFactory nested;

        public JSToJavaConverterFactory(ConverterFactory nested) {
            this.nested = nested;
        }

        @Override
        public boolean accept(Class<?> destType) {
            return nested.accept(destType);
        }

        @Override
        public boolean accept(Class<?> destType, Object argument) {
            return nested.accept(destType, JSRuntime.isLazyString(argument) ? JSRuntime.javaToString(argument) : JSRuntime.toJavaNull(argument));
        }

        @Override
        public Converter make(Class<?> destType) {
            Converter converter = nested.make(destType);
            converter = destType == Object.class ? new OptionalConverter(new TruffleJavaObjectConverter(Object.class), converter) : converter;
            converter = destType.isAssignableFrom(String.class) || nested instanceof StringToPrimitiveConverterFactory ? new OptionalConverter(new JSCharSequenceConverter(), converter) : converter;
            converter = destType.isAssignableFrom(Number.class) && !(nested instanceof StringToPrimitiveConverterFactory) ? new OptionalConverter(new JSLargeIntegerConverter(), converter) : converter;
            converter = !destType.isPrimitive() ? new OptionalConverter(new JSNullToJavaNullConverter(), converter) : converter;
            converter = !destType.isPrimitive() ? new OptionalConverter(new UndefinedToJavaNullConverter(), converter) : converter;
            return converter;
        }
    }

    public static class JSCharSequenceConverter extends Converter {
        @Override
        public Object convert(Object argument) {
            assert JSRuntime.isLazyString(argument);
            return JSRuntime.javaToString(argument);
        }

        @Override
        public boolean guard(Object argument) {
            return JSRuntime.isLazyString(argument);
        }
    }

    public static class JSNullToJavaNullConverter extends Converter {
        @Override
        public Object convert(Object argument) {
            return null;
        }

        @Override
        public boolean guard(Object argument) {
            return argument == Null.instance;
        }
    }

    public static class UndefinedToJavaNullConverter extends Converter {
        @Override
        public Object convert(Object argument) {
            return null;
        }

        @Override
        public boolean guard(Object argument) {
            return argument == Undefined.instance;
        }
    }

    public static class JSLargeIntegerConverter extends Converter {
        @Override
        public Object convert(Object argument) {
            return ((LargeInteger) argument).doubleValue();
        }

        @Override
        public boolean guard(Object argument) {
            return argument instanceof LargeInteger;
        }
    }

    public static class JavaToJSConverter extends Converter {
        @Override
        public Object convert(Object argument) {
            return JSRuntime.toJSNull(argument);
        }

        @Override
        public boolean guard(Object argument) {
            return true;
        }
    }

    public static class NullConverter extends Converter {
        @Override
        public Object convert(Object argument) {
            return argument;
        }

        @Override
        public boolean guard(Object argument) {
            return true;
        }
    }

    public static class ObjectArrayConverter extends Converter {
        @CompilationFinal(dimensions = 1) private final Converter[] converters;

        public ObjectArrayConverter(Converter[] converters) {
            this.converters = converters;
        }

        @ExplodeLoop
        @Override
        public Object convert(Object argument) {
            Object[] arguments = (Object[]) argument;
            Object[] converted = new Object[converters.length];
            for (int i = 0; i < converters.length; i++) {
                converted[i] = converters[i].convert(arguments[i]);
            }
            return converted;
        }

        @ExplodeLoop
        @Override
        public boolean guard(Object argument) {
            Object[] arguments = (Object[]) argument;
            for (int i = 0; i < converters.length; i++) {
                if (!converters[i].guard(arguments[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class ArrayConverter extends Converter {
        private final Class<?> componentType;
        @CompilationFinal(dimensions = 1) private final Converter[] converters;

        public ArrayConverter(Class<?> componentType, Converter[] converters) {
            this.componentType = componentType;
            this.converters = converters;
        }

        @ExplodeLoop
        @Override
        public Object convert(Object argument) {
            Object[] arguments = (Object[]) argument;
            Object converted = Array.newInstance(componentType, converters.length);
            for (int i = 0; i < converters.length; i++) {
                Array.set(converted, i, converters[i].convert(arguments[i]));
            }
            return converted;
        }

        @ExplodeLoop
        @Override
        public boolean guard(Object argument) {
            Object[] arguments = (Object[]) argument;
            for (int i = 0; i < converters.length; i++) {
                if (!converters[i].guard(arguments[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class VarArgsConverter extends Converter {
        private final Class<?> componentType;
        @CompilationFinal(dimensions = 1) private final Converter[] converters;

        public VarArgsConverter(Class<?> componentType, Converter[] converters) {
            this.componentType = componentType;
            this.converters = converters;
        }

        @ExplodeLoop
        @Override
        public Object convert(Object argument) {
            Object[] arguments = (Object[]) argument;
            Object converted = Array.newInstance(componentType, converters.length);
            for (int i = 0; i < converters.length - 1; i++) {
                Array.set(converted, i, converters[i].convert(arguments[i]));
            }
            Array.set(converted, converters.length - 1, converters[converters.length - 1].convert(Arrays.copyOfRange(arguments, converters.length - 1, arguments.length)));
            return converted;
        }

        @ExplodeLoop
        @Override
        public boolean guard(Object argument) {
            Object[] arguments = (Object[]) argument;
            for (int i = 0; i < converters.length - 1; i++) {
                if (!converters[i].guard(arguments[i])) {
                    return false;
                }
            }
            return converters[converters.length - 1].guard(Arrays.copyOfRange(arguments, converters.length - 1, arguments.length));
        }
    }

    public static class ConverterAdapter extends Converter {
        private final Converter converter;

        public ConverterAdapter(Converter converter) {
            this.converter = converter;
        }

        @Override
        public Object convert(Object argument) {
            if (converter.guard(argument)) {
                return converter.convert(argument);
            }
            return argument;
        }

        @Override
        public boolean guard(Object argument) {
            throw new UnsupportedOperationException();
        }
    }

    public static class SerialConverterAdapter extends Converter {
        @CompilationFinal(dimensions = 1) private final Converter[] converters;

        public SerialConverterAdapter(Converter[] converters) {
            this.converters = converters;
        }

        @ExplodeLoop
        @Override
        public Object convert(Object argument) {
            for (int i = 0; i < converters.length; i++) {
                if (converters[i].guard(argument)) {
                    return converters[i].convert(argument);
                }
            }
            return argument;
        }

        @Override
        public boolean guard(Object argument) {
            throw new UnsupportedOperationException();
        }

        public static Converter fromFactories(Class<?> destType, ConverterFactory[] converterFactories) {
            assert destType != void.class;
            ArrayList<Converter> filtered = new ArrayList<>(converterFactories.length);
            for (ConverterFactory converterFactory : converterFactories) {
                if (converterFactory.accept(destType)) {
                    filtered.add(converterFactory.make(destType));
                }
            }
            if (filtered.size() == 1) {
                return new ConverterAdapter(filtered.get(0));
            }
            return new SerialConverterAdapter(filtered.toArray(new Converter[filtered.size()]));
        }
    }

    public static class LazySerialConverterAdapter extends Converter {
        private final Class<?> destType;
        @CompilationFinal(dimensions = 1) private final ConverterFactory[] converterFactories;
        @CompilationFinal(dimensions = 1) private final Converter[] converters;

        public LazySerialConverterAdapter(Class<?> destType, ConverterFactory[] converterFactories) {
            this.destType = destType;
            this.converterFactories = converterFactories;
            this.converters = new Converter[this.converterFactories.length];
            for (ConverterFactory converterFactory : converterFactories) {
                assert converterFactory.accept(destType);
            }
        }

        public static Converter fromFactories(Class<?> destType, ConverterFactory[] availableConverterFactories) {
            return new LazySerialConverterAdapter(destType, applicableConverterFactories(destType, availableConverterFactories));
        }

        private static ConverterFactory[] applicableConverterFactories(Class<?> destType, ConverterFactory[] converterFactories) {
            ArrayList<ConverterFactory> filtered = new ArrayList<>(converterFactories.length);
            for (ConverterFactory converterFactory : converterFactories) {
                if (converterFactory.accept(destType)) {
                    filtered.add(converterFactory);
                }
            }
            return filtered.toArray(new ConverterFactory[filtered.size()]);
        }

        @ExplodeLoop
        @Override
        public Object convert(Object argument) {
            for (int i = 0; i < converters.length; i++) {
                if (converters[i] == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    converters[i] = converterFactories[i].make(destType);
                }

                if (converters[i].guard(argument)) {
                    return converters[i].convert(argument);
                }
            }
            return argument;
        }

        @Override
        public boolean guard(Object argument) {
            throw new UnsupportedOperationException();
        }
    }

    public static class TruffleJavaObjectConverterFactory extends ConverterFactory {
        private final ConverterFactory nested;

        public TruffleJavaObjectConverterFactory(ConverterFactory nested) {
            this.nested = nested;
        }

        @Override
        public boolean accept(Class<?> destType) {
            return !destType.isPrimitive() && nested.accept(destType);
        }

        @Override
        public boolean accept(Class<?> destType, Object value) {
            return !destType.isPrimitive() && TruffleJavaObjectConverter.isJavaObject(destType, value) &&
                            nested.accept(destType, TruffleJavaObjectConverter.asJavaObject(value));
        }

        @Override
        public Converter make(Class<?> destType) {
            Converter converter = nested.make(destType);
            return new CombineConverter(new TruffleJavaObjectConverter(destType), converter);
        }
    }

    public static class TruffleJavaObjectConverter extends Converter {
        private final Class<?> destType;

        public TruffleJavaObjectConverter(Class<?> destType) {
            this.destType = destType;
        }

        @Override
        public Object convert(Object argument) {
            return asJavaObject(argument);
        }

        @Override
        public boolean guard(Object argument) {
            return isJavaObject(destType, argument);
        }

        static boolean isJavaObject(Class<?> destType, Object value) {
            if (JSRuntime.isForeignObject(value)) {
                TruffleLanguage.Env env = AbstractJavaScriptLanguage.getCurrentEnv();
                if (env.isHostObject(value)) {
                    return destType == Object.class || destType.isInstance(env.asHostObject(value));
                }
            }
            return false;
        }

        static Object asJavaObject(Object argument) {
            TruffleLanguage.Env env = AbstractJavaScriptLanguage.getCurrentEnv();
            return env.asHostObject(argument);
        }
    }
}

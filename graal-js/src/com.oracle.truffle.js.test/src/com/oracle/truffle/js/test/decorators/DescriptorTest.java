package com.oracle.truffle.js.test.decorators;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.junit.Assert;

public class DescriptorTest extends JSTest {
    protected static final String METHOD = "'method'";
    protected static final String HOOK = "'hook'";
    protected static final String FIELD = "'field'";
    protected static final String ACCESSOR = "'accessor'";
    protected static final String OWN = "'own'";
    protected static final String PROTOTYPE = "'prototype'";
    protected static final String STATIC = "'static'";
    protected static final String KEY = "'key'";
    protected static final String PRIVATE_KEY = "'#key'";
    protected static final String TRUE = "true";
    protected static final String FALSE = "false";

    protected static final String[] NON_CALLABLES = {"0","'test'","true","{}","null"};

    protected static final String EMPTY_METHOD = "() => {}";
    protected static final String EMPTY_GETTER = "() => {return 0}";
    protected static final String EMPTY_SETTER = "(v) => {}";

    protected static void testError(String sourceCode, String expectedMsg) {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceCode, "decorator-test").buildLiteral());
            Assert.fail("should have thrown");
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage().contains(expectedMsg));
        }
    }

    protected static String createDecoratorWithPropertyDescriptor(String kind, String key, String placement, String value, String writable, String get, String set, String body) {
        StringBuilder builder = new StringBuilder();
        builder.append("decorator(d) {");
        if (kind != null) {
            builder.append("d.kind = ").append(kind).append(";");
        }
        if (key != null) {
            builder.append("d.key = ").append(key).append(";");
        }
        if (placement != null) {
            builder.append("d.placement = ").append(placement).append(";");
        }
        builder.append("d.descriptor = {};");
        if(value != null) {
            builder.append("d.descriptor.method = ").append(value).append(";");
        }
        if(writable != null) {
            builder.append("d.descriptor.writable = ").append(writable).append(";");
        }
        if(get != null) {
            builder.append("d.descriptor.get").append(get).append(";");
        }
        if(set != null) {
            builder.append("d.descriptor.set").append(set).append(";");
        }
        if (body != null) {
            builder.append(body);
        }
        builder.append("return d;}");
        builder.append("class C { @decorated method() {} }");
        return builder.toString();
    }

    protected static String createDecorator(String kind, String key, String placement, String body) {
        return createDecoratorWithPropertyDescriptor(kind, key, placement, null, null, null, null, body);
    }

    protected static String createDecoratorWithDataDescriptor(String kind, String key, String placement, String value, String writable, String body) {
        return createDecoratorWithPropertyDescriptor(kind, key, placement, value, writable, null,null,body);
    }

    protected static String createDecoratorWithAccessorDescriptor(String kind, String key, String placement, String get, String set, String body) {
        return createDecoratorWithPropertyDescriptor(kind, key, placement, null, null, get, set, body);
    }
}

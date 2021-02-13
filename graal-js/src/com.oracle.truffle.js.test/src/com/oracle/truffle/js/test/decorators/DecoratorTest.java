package com.oracle.truffle.js.test.decorators;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.junit.Assert;

public class DecoratorTest extends JSTest {
    protected static final String METHOD = "'method'";
    protected static final String HOOK = "'hook'";
    protected static final String FIELD = "'field'";
    protected static final String ACCESSOR = "'accessor'";
    protected static final String CLASS = "'class'";

    protected static final String OWN = "'own'";
    protected static final String PROTOTYPE = "'prototype'";
    protected static final String STATIC = "'static'";
    protected static final String KEY = "'key'";
    protected static final String TRUE = "true";

    protected static final String EMPTY_METHOD = "() => {}";
    protected static final String EMPTY_GETTER = "() => {return 0}";
    protected static final String EMPTY_SETTER = "(v) => {}";

    protected static final String[] NON_CALLABLES = {"0","'test'","true","{}","2.7"};
    protected static final String[] NON_VALIDS = {"'invalid'","0", EMPTY_METHOD,TRUE,"null"};

    protected static void testError(String sourceCode, String expectedMsg) {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022").build()) {
            context.eval(JavaScriptLanguage.ID, sourceCode);
            Assert.fail("should have thrown");
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage().contains(expectedMsg));
        }
    }

    protected static void testSuccess(String sourceCode) {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022").build()) {
            context.eval(JavaScriptLanguage.ID, sourceCode);
        } catch (Exception ex) {
            Assert.fail("should not have thrown: " + ex.getMessage());
        }
    }

    protected static void createDecorator(StringBuilder builder,String kind, String key, String placement, String value, String writable, String get, String set, String body) {
        builder.append("function decorator(a) {");
        builder.append("d = {};");
        if (kind != null) {
            builder.append("d.kind = ").append(kind).append(";");
        }
        if (key != null) {
            builder.append("d.key = ").append(key).append(";");
        }
        if (placement != null) {
            builder.append("d.placement = ").append(placement).append(";");
        }
        if(value != null) {
            builder.append("d.method = ").append(value).append(";");
        }
        if(writable != null) {
            builder.append("d.writable = ").append(writable).append(";");
        }
        if(get != null) {
            builder.append("d.get = ").append(get).append(";");
        }
        if(set != null) {
            builder.append("d.set = ").append(set).append(";");
        }
        if (body != null) {
            builder.append(body);
        }
        builder.append("return d;}");
    }

    protected static void updateDecorator(StringBuilder builder,String kind, String key, String placement, String value, String writable, String get, String set, String body) {
        builder.append("function decorator(d) {");
        if (kind != null) {
            builder.append("d.kind = ").append(kind).append(";");
        }
        if (key != null) {
            builder.append("d.key = ").append(key).append(";");
        }
        if (placement != null) {
            builder.append("d.placement = ").append(placement).append(";");
        }
        if(value != null) {
            builder.append("d.method = ").append(value).append(";");
        }
        if(writable != null) {
            builder.append("d.writable = ").append(writable).append(";");
        }
        if(get != null) {
            builder.append("d.get = ").append(get).append(";");
        }
        if(set != null) {
            builder.append("d.set = ").append(set).append(";");
        }
        if (body != null) {
            builder.append(body);
        }
        builder.append("return d;}");
    }

    protected static String createElementDecoratorWithPropertyDescriptor(String kind, String key, String placement, String value, String writable, String get, String set, String body) {
        StringBuilder builder = new StringBuilder();
        createDecorator(builder, kind, key, placement, value, writable, get,set,body);
        builder.append("class C { @decorator method() {} }");
        return builder.toString();
    }

    protected static String createElementDecorator(String kind, String key, String placement, String body) {
        return createElementDecoratorWithPropertyDescriptor(kind, key, placement, null, null, null, null, body);
    }

    protected static String createElementDecoratorWithDataDescriptor(String kind, String key, String placement, String value, String writable, String body) {
        return createElementDecoratorWithPropertyDescriptor(kind, key, placement, value, writable, null,null,body);
    }

    protected static String createElementDecoratorWithAccessorDescriptor(String kind, String key, String placement, String get, String set, String body) {
        return createElementDecoratorWithPropertyDescriptor(kind, key, placement, null, null, get, set, body);
    }

    protected static String createClassDecorator(String kind, String key, String placement, String body) {
        StringBuilder builder = new StringBuilder();
        createDecorator(builder, kind, key, placement, null,null,null,null, body);
        builder.append("@decorator class C {}");
        return builder.toString();
    }

    protected static String createElementDecoratorWithPrivateMethod(String kind, String key, String placement, String body) {
        StringBuilder builder = new StringBuilder();
        updateDecorator(builder, kind, null, placement, null, null, null,null,body);
        builder.append("class C { @decorator ").append(key).append("() {} }");
        return builder.toString();
    }
}

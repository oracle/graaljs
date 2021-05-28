package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

public class ErrorTest {
    private static final String[] errorTypes = {
            "Error",
            "RangeError",
            "TypeError",
            "ReferenceError",
            "SyntaxError",
            "EvalError",
            "URIError",
            "AggregateError"
    };

    private static final String[] defaults = {
            "'message'",
            "'message'",
            "'message'",
            "'message'",
            "'message'",
            "'message'",
            "'message'",
            "[],'message'"
    };

    private static final String[] defaultChecks = {
            "e.message === 'message'",
            "e.message === 'message'",
            "e.message === 'message'",
            "e.message === 'message'",
            "e.message === 'message'",
            "e.message === 'message'",
            "e.message === 'message'",
            "e.message === 'message'"
    };

    private void runErrorTest(String[] errors) {
        for(String source: errors) {
            try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "13").build()) {
                Value value = context.eval(JavaScriptLanguage.ID, source);
                Assert.assertTrue(value.isBoolean());
                Assert.assertTrue(value.asBoolean());
            } catch (Exception e) {
                Assert.fail();
            }
        }
    }

    private String[] buildErrors(String cause, String additionalChecks) {
        String[] errors = new String[errorTypes.length];
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < errorTypes.length; i++) {
            sb.setLength(0);
            sb.append("let e = new ").append(errorTypes[i]).append('(').append(defaults[i]);
            if(!cause.isEmpty())
            {
                sb.append(',').append(cause);
            }
            sb.append(");").append(defaultChecks[i]);
            if(!additionalChecks.isEmpty())
            {
                sb.append(" && ").append(additionalChecks);
            }
            sb.append(';');
            errors[i] = sb.toString();
        }
        return errors;
    }

    @Test
    public void testNoCause(){
        runErrorTest(buildErrors("","e.cause === undefined"));
    }

    @Test
    public void testEmptyCause() {
        runErrorTest(buildErrors("{}", "e.cause === undefined"));
    }

    @Test
    public void testStringCause() {
        runErrorTest(buildErrors("{ cause: 'test'}","e.cause === 'test'"));
    }

    @Test
    public void testNumberCause() {
        runErrorTest(buildErrors("{ cause: 0 }", "e.cause === 0"));
    }

    @Test
    public void testBooleanCause() {
        runErrorTest(buildErrors("{ cause: false }", "e.cause === false"));
    }

    @Test
    public void testObjectCause() {
        runErrorTest(buildErrors("{ cause: { a: 0, b: false}}", "e.cause.a === 0 && e.cause.b === false"));
    }

    @Test
    public void testNullCause() {
        runErrorTest(buildErrors("{ cause: null }" ,"e.cause === null"));
    }

    @Test
    public void testUndefinedCause() {
        runErrorTest(buildErrors("{ cause: undefined }", "e.cause === undefined"));
    }

    @Test
    public void testOtherProperties() {
        runErrorTest(buildErrors("{ a: 0, b: false}", "e.cause === undefined && e.a === undefined && e.b === undefined"));
        runErrorTest(buildErrors("{ cause: 0, a: 0, b: false}", "e.cause === 0 && e.a === undefined && e.b === undefined"));
    }
}

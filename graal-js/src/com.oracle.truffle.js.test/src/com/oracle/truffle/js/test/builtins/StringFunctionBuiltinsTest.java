package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.builtins.StringFunctionBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

// use js files instead of snippets
@RunWith(JUnit4.class)
public class StringFunctionBuiltinsTest {
    private static final String BUILDER_NAME = "string-builtins-test";

    private Context createContext() {
        return JSTest.newContextBuilder().option("js.ecmascript-version", JSContextOptions.ECMASCRIPT_VERSION_STAGING).build();
    }

    @Test
    public void testThrowErrorIfNoOpeningLine() {
        String sourceText = "String.dedent`value`";
        Exception exception = null;
        try (Context context = createContext()) {
            context.eval(
                    Source.newBuilder(JavaScriptLanguage.ID, sourceText, BUILDER_NAME).buildLiteral()
            );
        } catch (Exception e) {
            exception = e;
        }
        assert exception != null;
        assertEquals(
                exception.getMessage(),
                "TypeError: " + StringFunctionBuiltins.StringDedentNode.MISSING_START_NEWLINE_MESSAGE
        );
    }

    @Test
    public void testThrowErrorIfNoClosingLine() {
        String sourceText = "String.dedent`\nvalue`";
        Exception exception = null;
        try (Context context = createContext()) {
            context.eval(
                    Source.newBuilder(JavaScriptLanguage.ID, sourceText, BUILDER_NAME).buildLiteral()
            );
        } catch (Exception e) {
            exception = e;
        }
        assert exception != null;
        assertEquals(
                exception.getMessage(),
                "TypeError: " + StringFunctionBuiltins.StringDedentNode.MISSING_END_NEWLINE_MESSAGE
        );
    }

    @Test
    public void testDedentSingleLineWithTab() {
        String sourceText = "String.dedent`\n\tvalue\n`;";
        try (Context context = createContext()) {
            Value result = context.eval(
                    Source.newBuilder(JavaScriptLanguage.ID, sourceText, BUILDER_NAME).buildLiteral()
            );
            assertEquals(result.asString(), "value");
        }
    }

    @Test
    public void testWithSubstitutions() {
        String sourceText =
                "String.dedent`\n" +
                        "                            create table student(\n" +
                        "                              key: \\t${1+2},\\r\n" +
                        "                              name: ${\"John\"}\\r\n" +
                        "                            )\n" +
                        "\n" +
                        "                            create table student(\n" +
                        "                              key: ${8},\n" +
                        "                              name: ${\"Doe\"}\n" +
                        "                            )" +
                        "\n`";
        String expected =
                "create table student(\n" +
                        "  key: \t3,\r\n" +
                        "  name: John\r\n" +
                        ")\n" +
                        "\n" +
                        "create table student(\n" +
                        "  key: 8,\n" +
                        "  name: Doe\n" +
                        ")";
        try (Context context = createContext()) {
            Value result = context.eval(
                    Source.newBuilder(JavaScriptLanguage.ID, sourceText, BUILDER_NAME).buildLiteral()
            );
            assertEquals(result.asString(), expected);
        }
    }
}

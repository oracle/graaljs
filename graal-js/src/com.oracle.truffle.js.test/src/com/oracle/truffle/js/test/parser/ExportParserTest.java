/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.Parser;
import com.oracle.js.parser.ParserException;
import com.oracle.js.parser.ScriptEnvironment;
import com.oracle.js.parser.Source;
import com.oracle.js.parser.ir.BlockExpression;
import com.oracle.js.parser.ir.ClassNode;
import com.oracle.js.parser.ir.ExportNode;
import com.oracle.js.parser.ir.ExportSpecifierNode;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.ExpressionStatement;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.LiteralNode;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ExportEntry;
import com.oracle.js.parser.ir.ObjectNode;
import com.oracle.js.parser.ir.VarNode;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Strings;

public class ExportParserTest {

    private static FunctionNode parseModule(String code) {
        ScriptEnvironment env = ScriptEnvironment.builder().strict(true).ecmaScriptVersion(ScriptEnvironment.ES_2020).importAttributes(true).build();
        Parser parser = new Parser(env, Source.sourceFor("name", code), new ErrorManager.ThrowErrorManager());
        return parser.parseModule("moduleName");
    }

    @Test
    public void testConstNumber() {
        String code = "export const foo = 1;";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        VarNode var = export.getVar();
        assertTrue(var.isConst());
        assertEquals(ImportParserTest.FOO, var.getName().getNameTS());
        assertNotNull(var.getInit());
    }

    private static Expression testHelper(String code, boolean isDefault) {
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertEquals(isDefault, export.isDefault());
        Expression expression = export.getExpression();
        assertNotNull(expression);
        return expression;
    }

    @Test
    public void testDefaultArray() {
        Expression expression = testHelper("export default [];", true);
        assertTrue(expression instanceof LiteralNode.ArrayLiteralNode);
    }

    @Test
    public void testDefaultClass() {
        Expression expression = testHelper("export default class {}", true);
        assertTrue(expression instanceof ClassNode ||
                        (expression instanceof BlockExpression && ((BlockExpression) expression).getBlock().getLastStatement() instanceof ExpressionStatement &&
                                        ((ExpressionStatement) ((BlockExpression) expression).getBlock().getLastStatement()).getExpression() instanceof ClassNode));
    }

    @Test
    public void testDefaultExpression() {
        testHelper("export default (1 + 2);", true);
    }

    @Test
    public void testDefaultFunction() {
        Expression expression = testHelper("export default function () {}", true);
        assertTrue(expression instanceof FunctionNode);
    }

    @Test
    public void testDefaultNamedFunction() {
        Expression expression = testHelper("export default function foo() {}", true);
        assertTrue(expression instanceof FunctionNode);
        FunctionNode fn = (FunctionNode) expression;
        assertEquals(ImportParserTest.FOO, fn.getIdent().getNameTS());
    }

    @Test
    public void testDefaultNumber() {
        Expression expression = testHelper("export default 42;", true);
        assertTrue(expression instanceof LiteralNode.PrimitiveLiteralNode);
    }

    @Test
    public void testDefaultObject() {
        Expression expression = testHelper("export default { foo: 1 };", true);
        assertTrue(expression instanceof ObjectNode);
    }

    @Test
    public void testDefaultValue() {
        Expression expression = testHelper("export default foo;", true);
        assertTrue(expression instanceof IdentNode);
        IdentNode ident = (IdentNode) expression;
        assertEquals(ImportParserTest.FOO, ident.getNameTS());
    }

    private static ExportSpecifierNode testFromHelper(String code) {
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        assertEquals(ImportParserTest.FOO, export.getModuleSpecifier().getValue());
        List<ExportSpecifierNode> specifiers = export.getNamedExports().getExportSpecifiers();
        assertEquals(1, specifiers.size());
        return specifiers.get(0);
    }

    @Test
    public void testFromBatch() {
        String code = "export * from \"foo\";";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        assertEquals(ImportParserTest.FOO, export.getModuleSpecifier().getValue());
    }

    @Test
    public void testFromDefault() {
        String code = "export {default} from \"foo\";";
        ExportSpecifierNode specifier = testFromHelper(code);
        assertEquals(ImportParserTest.DEFAULT, specifier.getIdentifier().getPropertyNameTS());
    }

    @Test
    public void testFromNamedAsDefault() {
        String code = "export {foo as default} from \"foo\";";
        ExportSpecifierNode specifier = testFromHelper(code);
        assertEquals(ImportParserTest.FOO, specifier.getIdentifier().getPropertyNameTS());
        assertEquals(ImportParserTest.DEFAULT, specifier.getExportIdentifier().getPropertyNameTS());
    }

    @Test
    public void testFromDefaultAsDefault() {
        String code = "export {default as default} from \"foo\";";
        ExportSpecifierNode specifier = testFromHelper(code);
        assertEquals(ImportParserTest.DEFAULT, specifier.getIdentifier().getPropertyNameTS());
        assertEquals(ImportParserTest.DEFAULT, specifier.getExportIdentifier().getPropertyNameTS());
    }

    @Test
    public void testFromNamedAsSpecifier() {
        String code = "export {foo as bar} from \"foo\";";
        ExportSpecifierNode specifier = testFromHelper(code);
        assertEquals(ImportParserTest.FOO, specifier.getIdentifier().getPropertyNameTS());
        assertEquals(ImportParserTest.BAR, specifier.getExportIdentifier().getPropertyNameTS());
    }

    @Test
    public void testFromNamedAsSpecifiers() {
        String code = "export {foo as default, bar} from \"foo\";";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        assertEquals(ImportParserTest.FOO, export.getModuleSpecifier().getValue());
        List<ExportSpecifierNode> specifiers = export.getNamedExports().getExportSpecifiers();
        assertEquals(2, specifiers.size());

        ExportSpecifierNode specifier = specifiers.get(0);
        assertEquals(ImportParserTest.FOO, specifier.getIdentifier().getPropertyNameTS());
        assertEquals(ImportParserTest.DEFAULT, specifier.getExportIdentifier().getPropertyNameTS());

        specifier = specifiers.get(1);
        assertEquals(ImportParserTest.BAR, specifier.getIdentifier().getPropertyNameTS());
    }

    @Test
    public void testFromSpecifier() {
        String code = "export {foo} from \"foo\";";
        ExportSpecifierNode specifier = testFromHelper(code);
        assertEquals(ImportParserTest.FOO, specifier.getIdentifier().getPropertyNameTS());
    }

    @Test
    public void testFromSpecifiers() {
        String code = "export {foo, bar} from \"foo\";";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        assertEquals(ImportParserTest.FOO, export.getModuleSpecifier().getValue());
        List<ExportSpecifierNode> specifiers = export.getNamedExports().getExportSpecifiers();
        assertEquals(2, specifiers.size());

        ExportSpecifierNode specifier = specifiers.get(0);
        assertEquals(ImportParserTest.FOO, specifier.getIdentifier().getPropertyNameTS());

        specifier = specifiers.get(1);
        assertEquals(ImportParserTest.BAR, specifier.getIdentifier().getPropertyNameTS());
    }

    @Test
    public void testExportNamedFromWithAttributes() {
        String code = """
                        export {bar} from "foo" with {type: "json"};
                        """;
        Module module = parseModule(code).getModule();
        Map<TruffleString, TruffleString> expectedAttributes = Map.of(Strings.fromJavaString("type"), Strings.fromJavaString("json"));

        List<ExportNode> exports = module.getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        assertEquals(ImportParserTest.FOO, export.getModuleSpecifier().getValue());
        List<ExportSpecifierNode> specifiers = export.getNamedExports().getExportSpecifiers();
        assertEquals(1, specifiers.size());
        assertEquals(ImportParserTest.BAR, specifiers.get(0).getIdentifier().getPropertyNameTS());

        assertEquals(export.toString(), expectedAttributes.size(), export.getAttributes().size());
        assertEquals(export.toString(), expectedAttributes, export.getAttributes());

        assertEquals(1, module.getIndirectExportEntries().size());
        ExportEntry exportEntry = module.getIndirectExportEntries().get(0);
        assertEquals(exportEntry.toString(), expectedAttributes.size(), exportEntry.getModuleRequest().attributes().size());
        assertEquals(exportEntry.toString(), expectedAttributes, exportEntry.getModuleRequest().attributes());
    }

    @Test
    public void testExportStarFromWithAttributes() {
        String code = """
                        export * from "foo" with {type: "json"};
                        """;
        Module module = parseModule(code).getModule();
        Map<TruffleString, TruffleString> expectedAttributes = Map.of(Strings.fromJavaString("type"), Strings.fromJavaString("json"));

        List<ExportNode> exports = module.getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        assertEquals(ImportParserTest.FOO, export.getModuleSpecifier().getValue());

        assertEquals(export.toString(), expectedAttributes.size(), export.getAttributes().size());
        assertEquals(export.toString(), expectedAttributes, export.getAttributes());

        assertEquals(1, module.getStarExportEntries().size());
        ExportEntry exportEntry = module.getStarExportEntries().get(0);
        assertEquals(exportEntry.toString(), expectedAttributes.size(), exportEntry.getModuleRequest().attributes().size());
        assertEquals(exportEntry.toString(), expectedAttributes, exportEntry.getModuleRequest().attributes());
    }

    @Test
    public void testExportNamespaceFromWithAttributes() {
        String code = """
                        export * as bar from "foo" with {type: "json"};
                        """;
        Module module = parseModule(code).getModule();
        Map<TruffleString, TruffleString> expectedAttributes = Map.of(Strings.fromJavaString("type"), Strings.fromJavaString("json"));

        List<ExportNode> exports = module.getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        assertEquals(ImportParserTest.FOO, export.getModuleSpecifier().getValue());
        assertNull(export.toString(), export.getNamedExports());
        assertEquals(export.toString(), ImportParserTest.BAR, export.getExportIdentifier().getPropertyNameTS());

        assertEquals(export.toString(), expectedAttributes.size(), export.getAttributes().size());
        assertEquals(export.toString(), expectedAttributes, export.getAttributes());

        assertEquals(1, module.getIndirectExportEntries().size());
        ExportEntry exportEntry = module.getIndirectExportEntries().get(0);
        assertEquals(exportEntry.toString(), expectedAttributes.size(), exportEntry.getModuleRequest().attributes().size());
        assertEquals(exportEntry.toString(), expectedAttributes, exportEntry.getModuleRequest().attributes());
    }

    @Test
    public void testFunction() {
        Expression expression = testHelper("export function foo () {}", false);
        assertTrue(expression instanceof FunctionNode);
        FunctionNode fn = (FunctionNode) expression;
        assertEquals(ImportParserTest.FOO, fn.getIdent().getNameTS());
    }

    @Test
    public void testLetNumber() {
        String code = "export let foo = 1;";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        VarNode var = export.getVar();
        assertTrue(var.isLet());
        assertEquals(ImportParserTest.FOO, var.getName().getNameTS());
        assertNotNull(var.getInit());
    }

    private static ExportSpecifierNode testNamedHelper(String code) {
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        List<ExportSpecifierNode> specifiers = export.getNamedExports().getExportSpecifiers();
        assertEquals(1, specifiers.size());
        return specifiers.get(0);
    }

    @Test
    public void testNamedAsDefault() {
        String code = "var foo; export {foo as default};";
        ExportSpecifierNode specifier = testNamedHelper(code);
        assertEquals(ImportParserTest.FOO, specifier.getIdentifier().getPropertyNameTS());
        assertEquals(ImportParserTest.DEFAULT, specifier.getExportIdentifier().getPropertyNameTS());
    }

    @Test
    public void testNamedAsSpecifier() {
        String code = "var foo; export {foo as bar};";
        ExportSpecifierNode specifier = testNamedHelper(code);
        assertEquals(ImportParserTest.FOO, specifier.getIdentifier().getPropertyNameTS());
        assertEquals(ImportParserTest.BAR, specifier.getExportIdentifier().getPropertyNameTS());
    }

    @Test
    public void testNamedAsSpecifiers() {
        String code = "var foo, bar; export {foo as default, bar};";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        List<ExportSpecifierNode> specifiers = export.getNamedExports().getExportSpecifiers();
        assertEquals(2, specifiers.size());

        ExportSpecifierNode specifier = specifiers.get(0);
        assertEquals(ImportParserTest.FOO, specifier.getIdentifier().getPropertyNameTS());
        assertEquals(ImportParserTest.DEFAULT, specifier.getExportIdentifier().getPropertyNameTS());

        specifier = specifiers.get(1);
        assertEquals(ImportParserTest.BAR, specifier.getIdentifier().getPropertyNameTS());
    }

    @Test
    public void testNamedEmpty() {
        String code = "export {};";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        List<ExportSpecifierNode> specifiers = export.getNamedExports().getExportSpecifiers();
        assertEquals(0, specifiers.size());
    }

    @Test
    public void testNamedSpecifier() {
        String code = "var foo; export {foo};";
        ExportSpecifierNode specifier = testNamedHelper(code);
        assertEquals(ImportParserTest.FOO, specifier.getIdentifier().getPropertyNameTS());
    }

    @Test
    public void testNamedSpecifiers() {
        String code = "var foo, bar; export {foo, bar};";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        List<ExportSpecifierNode> specifiers = export.getNamedExports().getExportSpecifiers();
        assertEquals(2, specifiers.size());

        ExportSpecifierNode specifier = specifiers.get(0);
        assertEquals(ImportParserTest.FOO, specifier.getIdentifier().getPropertyNameTS());

        specifier = specifiers.get(1);
        assertEquals(ImportParserTest.BAR, specifier.getIdentifier().getPropertyNameTS());
    }

    @Test
    public void testVar() {
        String code = "export var bar;";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        VarNode var = export.getVar();
        assertEquals(ImportParserTest.BAR, var.getName().getNameTS());
    }

    @Test
    public void testVarAnonymousFunction() {
        String code = "export var foo = function () {};";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        VarNode var = export.getVar();
        assertEquals(ImportParserTest.FOO, var.getName().getNameTS());
        assertNotNull(var.getInit());
    }

    @Test
    public void testVarNumber() {
        String code = "export var foo = 1;";
        List<ExportNode> exports = parseModule(code).getModule().getExports();
        assertEquals(1, exports.size());
        ExportNode export = exports.get(0);
        assertFalse(export.isDefault());
        VarNode var = export.getVar();
        assertEquals(ImportParserTest.FOO, var.getName().getNameTS());
        assertNotNull(var.getInit());
    }

    @Test(expected = ParserException.class)
    public void testInvalidBatchMissingFromClause() {
        String code = "export *";
        parseModule(code).getModule().getExports();
    }

    @Test(expected = ParserException.class)
    public void testInvalidBatchToken() {
        String code = "export * +";
        parseModule(code).getModule().getExports();
    }

    @Test(expected = ParserException.class)
    public void testInvalidExportDefault() {
        String code = "export default from \"foo\"";
        parseModule(code).getModule().getExports();
    }

    @Test(expected = ParserException.class)
    public void testInvalidExportDefaultEquals() {
        String code = "export default = 42";
        parseModule(code).getModule().getExports();
    }

    @Test(expected = ParserException.class)
    public void testInvalidExportDefaultToken() {
        String code = "export {default} +";
        parseModule(code).getModule().getExports();
    }

    @Test(expected = ParserException.class)
    public void testInvalidExportNamedDefault() {
        String code = "export {default}";
        parseModule(code).getModule().getExports();
    }

    @Test(expected = ParserException.class)
    public void testInvalidExportYield() {
        String code = "export {yield}";
        parseModule(code).getModule().getExports();
    }

    @Test
    public void testInvalidExportYieldFrom() {
        String code = "export {yield} from \"foo\"";
        parseModule(code).getModule().getExports();
    }
}

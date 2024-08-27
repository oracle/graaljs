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

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.Parser;
import com.oracle.js.parser.ScriptEnvironment;
import com.oracle.js.parser.Source;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.ImportClauseNode;
import com.oracle.js.parser.ir.ImportNode;
import com.oracle.js.parser.ir.ImportSpecifierNode;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ImportEntry;
import com.oracle.js.parser.ir.NameSpaceImportNode;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Strings;

public class ImportParserTest {

    public static final TruffleString FOO = Strings.constant("foo");
    public static final TruffleString BAR = Strings.constant("bar");
    public static final TruffleString BAZ = Strings.constant("baz");
    public static final TruffleString XYZ = Strings.constant("xyz");
    public static final TruffleString DEFAULT = Strings.constant("default");

    private static FunctionNode parseModule(String code) {
        ScriptEnvironment env = ScriptEnvironment.builder().strict(true).ecmaScriptVersion(ScriptEnvironment.ES_2020).importAttributes(true).build();
        Parser parser = new Parser(env, Source.sourceFor("name", code), new ErrorManager.ThrowErrorManager());
        return parser.parseModule("moduleName");
    }

    @Test
    public void testDefault() {
        String code = "import bar from \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        IdentNode ident = importNode.getImportClause().getDefaultBinding();
        assertEquals(BAR, ident.getNameTS());
        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testDefaultAndNamedSpecifiers() {
        String code = "import foo, {bar} from \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        ImportClauseNode clause = importNode.getImportClause();
        IdentNode ident = clause.getDefaultBinding();
        assertEquals(FOO, ident.getNameTS());
        List<ImportSpecifierNode> specifiers = clause.getNamedImports().getImportSpecifiers();
        assertEquals(1, specifiers.size());
        ImportSpecifierNode specifier = specifiers.get(0);
        assertEquals(BAR, specifier.getBindingIdentifier().getNameTS());
        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testDefaultAndNamespaceSpecifiers() {
        String code = "import foo, * as bar from \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        ImportClauseNode clause = importNode.getImportClause();
        IdentNode ident = clause.getDefaultBinding();
        assertEquals(FOO, ident.getNameTS());
        NameSpaceImportNode namespace = clause.getNameSpaceImport();
        assertEquals(BAR, namespace.getBindingIdentifier().getNameTS());
        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testDefaultAs() {
        String code = "import {default as foo} from \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        ImportClauseNode clause = importNode.getImportClause();
        List<ImportSpecifierNode> specifiers = clause.getNamedImports().getImportSpecifiers();
        assertEquals(1, specifiers.size());
        ImportSpecifierNode specifier = specifiers.get(0);
        assertEquals(FOO, specifier.getBindingIdentifier().getNameTS());
        assertEquals(DEFAULT, specifier.getIdentifier().getPropertyNameTS());
        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testModule() {
        String code = "import \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testNamedAsSpecifier() {
        String code = "import {bar as baz} from \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        ImportClauseNode clause = importNode.getImportClause();
        List<ImportSpecifierNode> specifiers = clause.getNamedImports().getImportSpecifiers();
        assertEquals(1, specifiers.size());
        ImportSpecifierNode specifier = specifiers.get(0);
        assertEquals(BAZ, specifier.getBindingIdentifier().getNameTS());
        assertEquals(BAR, specifier.getIdentifier().getPropertyNameTS());
        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testNamedAsSpecifiers() {
        String code = "import {bar as baz, xyz} from \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        ImportClauseNode clause = importNode.getImportClause();
        List<ImportSpecifierNode> specifiers = clause.getNamedImports().getImportSpecifiers();
        assertEquals(2, specifiers.size());

        ImportSpecifierNode specifier = specifiers.get(0);
        assertEquals(BAZ, specifier.getBindingIdentifier().getNameTS());
        assertEquals(BAR, specifier.getIdentifier().getPropertyNameTS());

        specifier = specifiers.get(1);
        assertEquals(XYZ, specifier.getBindingIdentifier().getNameTS());

        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testNamedEmpty() {
        String code = "import {} from \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        ImportClauseNode clause = importNode.getImportClause();
        List<ImportSpecifierNode> specifiers = clause.getNamedImports().getImportSpecifiers();
        assertEquals(0, specifiers.size());
        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testNamedSpecifier() {
        String code = "import {bar} from \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        ImportClauseNode clause = importNode.getImportClause();
        List<ImportSpecifierNode> specifiers = clause.getNamedImports().getImportSpecifiers();
        assertEquals(1, specifiers.size());
        ImportSpecifierNode specifier = specifiers.get(0);
        assertEquals(BAR, specifier.getBindingIdentifier().getNameTS());
        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testNamedSpecifiers() {
        String code = "import {bar, baz} from \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        ImportClauseNode clause = importNode.getImportClause();
        List<ImportSpecifierNode> specifiers = clause.getNamedImports().getImportSpecifiers();
        assertEquals(2, specifiers.size());

        ImportSpecifierNode specifier = specifiers.get(0);
        assertEquals(BAR, specifier.getBindingIdentifier().getNameTS());

        specifier = specifiers.get(1);
        assertEquals(BAZ, specifier.getBindingIdentifier().getNameTS());

        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testNamespaceSpecifier() {
        String code = "import * as foo from \"foo\";";
        List<ImportNode> imports = parseModule(code).getModule().getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);
        NameSpaceImportNode namespace = importNode.getImportClause().getNameSpaceImport();
        assertEquals(FOO, namespace.getBindingIdentifier().getNameTS());
        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
    }

    @Test
    public void testImportNamedWithAttributes() {
        String code = """
                        import {bar} from "foo" with {type: "json"};
                        """;
        Module module = parseModule(code).getModule();
        Map<TruffleString, TruffleString> expectedAttributes = Map.of(Strings.fromJavaString("type"), Strings.fromJavaString("json"));

        List<ImportNode> imports = module.getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);

        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
        ImportClauseNode clause = importNode.getImportClause();
        List<ImportSpecifierNode> specifiers = clause.getNamedImports().getImportSpecifiers();
        assertEquals(1, specifiers.size());
        ImportSpecifierNode specifier = specifiers.get(0);
        assertEquals(BAR, specifier.getBindingIdentifier().getNameTS());

        assertEquals(importNode.toString(), expectedAttributes.size(), importNode.getAttributes().size());
        assertEquals(importNode.toString(), expectedAttributes, importNode.getAttributes());

        assertEquals(1, module.getImportEntries().size());
        ImportEntry importEntry = module.getImportEntries().get(0);
        assertEquals(importEntry.toString(), expectedAttributes.size(), importEntry.getModuleRequest().attributes().size());
        assertEquals(importEntry.toString(), expectedAttributes, importEntry.getModuleRequest().attributes());
    }

    @Test
    public void testImportNamespaceWithAttributes() {
        String code = """
                        import * as bar from "foo" with {type: "json"};
                        """;
        Module module = parseModule(code).getModule();
        Map<TruffleString, TruffleString> expectedAttributes = Map.of(Strings.fromJavaString("type"), Strings.fromJavaString("json"));

        List<ImportNode> imports = module.getImports();
        assertEquals(1, imports.size());
        ImportNode importNode = imports.get(0);

        assertEquals(FOO, importNode.getModuleSpecifier().getValue());
        ImportClauseNode clause = importNode.getImportClause();
        NameSpaceImportNode nameSpaceImport = clause.getNameSpaceImport();
        assertEquals(BAR, nameSpaceImport.getBindingIdentifier().getNameTS());

        assertEquals(importNode.toString(), expectedAttributes.size(), importNode.getAttributes().size());
        assertEquals(importNode.toString(), expectedAttributes, importNode.getAttributes());

        assertEquals(1, module.getImportEntries().size());
        ImportEntry importEntry = module.getImportEntries().get(0);
        assertEquals(importEntry.toString(), expectedAttributes.size(), importEntry.getModuleRequest().attributes().size());
        assertEquals(importEntry.toString(), expectedAttributes, importEntry.getModuleRequest().attributes());
    }
}

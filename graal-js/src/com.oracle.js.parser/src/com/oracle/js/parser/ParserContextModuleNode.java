/*
 * Copyright (c) 2015, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.js.parser;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.strings.TruffleString;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;

import com.oracle.js.parser.ir.ExportNode;
import com.oracle.js.parser.ir.ExportSpecifierNode;
import com.oracle.js.parser.ir.ImportNode;
import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ExportEntry;
import com.oracle.js.parser.ir.Module.ImportEntry;
import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.js.parser.ir.Scope;

/**
 * ParserContextNode that represents a module.
 */
class ParserContextModuleNode extends ParserContextBaseNode {

    private static final String MSG_DUPLICATE_EXPORT = "duplicate.export";
    private static final String MSG_EXPORT_NOT_DEFINED = "export.not.defined";

    /** Module name. */
    private final String name;
    private final Scope moduleScope;
    private final AbstractParser parser;

    private List<ModuleRequest> requestedModules = new ArrayList<>();
    private List<ImportEntry> importEntries = new ArrayList<>();
    private List<ExportEntry> localExportEntries = new ArrayList<>();
    private List<ExportEntry> indirectExportEntries = new ArrayList<>();
    private List<ExportEntry> starExportEntries = new ArrayList<>();

    private List<ImportNode> imports = new ArrayList<>();
    private List<ExportNode> exports = new ArrayList<>();

    private EconomicMap<String, ImportEntry> importedLocalNames = EconomicMap.create();
    private EconomicSet<String> exportedNames = EconomicSet.create();

    /**
     * Constructor.
     *
     * @param name name of the module
     */
    ParserContextModuleNode(final String name, Scope moduleScope, AbstractParser parser) {
        this.name = name;
        this.moduleScope = moduleScope;
        this.parser = parser;
    }

    /**
     * Returns the name of the module.
     *
     * @return name of the module
     */
    public String getModuleName() {
        return name;
    }

    public void addImport(ImportNode importNode) {
        imports.add(importNode);
    }

    public void addExport(ExportNode exportNode) {
        exports.add(exportNode);
    }

    public void addModuleRequest(ModuleRequest moduleRequest) {
        requestedModules.add(moduleRequest);
    }

    public void addImportEntry(ImportEntry importEntry) {
        importEntries.add(importEntry);
        importedLocalNames.put(importEntry.getLocalName().toJavaStringUncached(), importEntry);
    }

    public void addLocalExportEntry(long exportToken, ExportEntry exportEntry) {
        localExportEntries.add(exportEntry);
        addExportedName(exportToken, exportEntry);
        if (!moduleScope.hasSymbol(exportEntry.getLocalName().toJavaStringUncached())) {
            throw parser.error(AbstractParser.message(MSG_EXPORT_NOT_DEFINED, exportEntry.getLocalName().toJavaStringUncached()), exportToken);
        }
    }

    public void addIndirectExportEntry(long exportToken, ExportEntry exportEntry) {
        indirectExportEntries.add(exportEntry);
        addExportedName(exportToken, exportEntry);
    }

    public void addStarExportEntry(ExportEntry exportEntry) {
        starExportEntries.add(exportEntry);
    }

    private void addExportedName(long exportToken, ExportEntry exportEntry) {
        if (!exportedNames.add(exportEntry.getExportName().toJavaStringUncached())) {
            throw parser.error(AbstractParser.message(MSG_DUPLICATE_EXPORT, exportEntry.getExportName().toJavaStringUncached()), exportToken);
        }
    }

    private void resolveExports() {
        for (ExportNode export : exports) {
            long exportToken = export.getToken();
            if (export.getNamedExports() != null) {
                assert export.getExportIdentifier() == null;
                for (ExportSpecifierNode s : export.getNamedExports().getExportSpecifiers()) {
                    TruffleString localName = s.getIdentifier().getPropertyNameTS();
                    ExportEntry ee;
                    if (s.getExportIdentifier() != null) {
                        ee = ExportEntry.exportSpecifier(s.getExportIdentifier().getPropertyNameTS(), localName);
                    } else {
                        ee = ExportEntry.exportSpecifier(localName);
                    }
                    if (export.getModuleSpecifier() == null) {
                        ImportEntry ie = importedLocalNames.get(localName.toJavaStringUncached());
                        if (ie == null) {
                            addLocalExportEntry(exportToken, ee);
                        } else if (ie.getImportName().equals(Module.STAR_NAME)) {
                            // This is a re-export of an imported module namespace object.
                            addLocalExportEntry(exportToken, ee);
                        } else {
                            // This is a re-export of a single name.
                            addIndirectExportEntry(exportToken, ExportEntry.exportIndirect(ee.getExportName(), ie.getModuleRequest(), ie.getImportName()));
                        }
                    } else {
                        addIndirectExportEntry(exportToken, ee.withFrom(ModuleRequest.create(export.getModuleSpecifier().getValue(), export.getAttributes())));
                    }
                }
            } else if (export.getModuleSpecifier() != null) {
                TruffleString specifier = export.getModuleSpecifier().getValue();
                ModuleRequest moduleRequest = ModuleRequest.create(specifier, export.getAttributes());
                if (export.getExportIdentifier() == null) {
                    addStarExportEntry(ExportEntry.exportStarFrom(moduleRequest));
                } else {
                    addIndirectExportEntry(exportToken, ExportEntry.exportStarAsNamespaceFrom(export.getExportIdentifier().getPropertyNameTS(), moduleRequest));
                }
            } else if (export.isDefault()) {
                addLocalExportEntry(exportToken, ExportEntry.exportDefault(export.getExportIdentifier().getPropertyNameTS()));
            } else {
                addLocalExportEntry(exportToken, ExportEntry.exportSpecifier(export.getExportIdentifier().getPropertyNameTS()));
            }
        }
    }

    public Module createModule() {
        resolveExports();
        return new Module(requestedModules, importEntries, localExportEntries, indirectExportEntries, starExportEntries, imports, exports);
    }
}

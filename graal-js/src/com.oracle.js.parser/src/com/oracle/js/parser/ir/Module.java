/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.js.parser.ir;

import com.oracle.js.parser.ParserStrings;
import com.oracle.truffle.api.strings.TruffleString;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Module information.
 */
public final class Module {

    /**
     * The synthetic binding name assigned to export default declarations with unnamed expressions.
     */
    public static final TruffleString DEFAULT_EXPORT_BINDING_NAME = ParserStrings.constant("*default*");
    public static final TruffleString DEFAULT_NAME = ParserStrings.constant("default");
    public static final TruffleString STAR_NAME = ParserStrings.constant("*");
    public static final TruffleString NAMESPACE_EXPORT_BINDING_NAME = ParserStrings.constant("*namespace*");

    public static final class ExportEntry {
        private final TruffleString exportName;
        private final ModuleRequest moduleRequest;
        private final TruffleString importName;
        private final TruffleString localName;

        private ExportEntry(TruffleString exportName, ModuleRequest moduleRequest, TruffleString importName, TruffleString localName) {
            this.exportName = exportName;
            this.moduleRequest = moduleRequest;
            this.importName = importName;
            this.localName = localName;
        }

        public static ExportEntry exportStarFrom(ModuleRequest moduleRequest) {
            return new ExportEntry(null, moduleRequest, STAR_NAME, null);
        }

        public static ExportEntry exportStarAsNamespaceFrom(TruffleString exportName, ModuleRequest moduleRequest) {
            return new ExportEntry(exportName, moduleRequest, STAR_NAME, null);
        }

        public static ExportEntry exportDefault(TruffleString localName) {
            return new ExportEntry(DEFAULT_NAME, null, null, localName);
        }

        public static ExportEntry exportSpecifier(TruffleString exportName, TruffleString localName) {
            return new ExportEntry(exportName, null, null, localName);
        }

        public static ExportEntry exportSpecifier(TruffleString exportName) {
            return exportSpecifier(exportName, exportName);
        }

        public static ExportEntry exportIndirect(TruffleString exportName, ModuleRequest moduleRequest, TruffleString importName) {
            return new ExportEntry(exportName, moduleRequest, importName, null);
        }

        public ExportEntry withFrom(@SuppressWarnings("hiding") ModuleRequest moduleRequest) {
            return new ExportEntry(exportName, moduleRequest, localName, null);
        }

        public TruffleString getExportName() {
            return exportName;
        }

        public ModuleRequest getModuleRequest() {
            return moduleRequest;
        }

        public TruffleString getImportName() {
            return importName;
        }

        public TruffleString getLocalName() {
            return localName;
        }

        @Override
        public String toString() {
            return "ExportEntry [exportName=" + exportName + ", moduleRequest=" + moduleRequest + ", importName=" + importName + ", localName=" + localName + "]";
        }
    }

    public static final class ImportEntry {
        private final ModuleRequest moduleRequest;
        private final TruffleString importName;
        private final TruffleString localName;

        private ImportEntry(ModuleRequest moduleRequest, TruffleString importName, TruffleString localName) {
            this.moduleRequest = moduleRequest;
            this.importName = importName;
            this.localName = localName;
        }

        public static ImportEntry importDefault(TruffleString localName) {
            return new ImportEntry(null, DEFAULT_NAME, localName);
        }

        public static ImportEntry importStarAsNameSpaceFrom(TruffleString localNameSpace) {
            return new ImportEntry(null, STAR_NAME, localNameSpace);
        }

        public static ImportEntry importSpecifier(TruffleString importName, TruffleString localName) {
            return new ImportEntry(null, importName, localName);
        }

        public static ImportEntry importSpecifier(TruffleString importName) {
            return importSpecifier(importName, importName);
        }

        public ImportEntry withFrom(@SuppressWarnings("hiding") ModuleRequest moduleRequest) {
            return new ImportEntry(moduleRequest, importName, localName);
        }

        public ModuleRequest getModuleRequest() {
            return moduleRequest;
        }

        public TruffleString getImportName() {
            return importName;
        }

        public TruffleString getLocalName() {
            return localName;
        }

        @Override
        public String toString() {
            return "ImportEntry [moduleRequest=" + moduleRequest + ", importName=" + importName + ", localName=" + localName + "]";
        }
    }

    public static final class ModuleRequest {
        private final TruffleString specifier;
        private Map<TruffleString, TruffleString> assertions;

        private ModuleRequest(TruffleString specifier, Map<TruffleString, TruffleString> assertions) {
            this.specifier = specifier;
            this.assertions = assertions;
        }

        public static ModuleRequest create(TruffleString specifier) {
            return new ModuleRequest(specifier, Collections.emptyMap());
        }

        public static ModuleRequest create(TruffleString specifier, Map<TruffleString, TruffleString> assertions) {
            return new ModuleRequest(specifier, Map.copyOf(assertions));
        }

        public static ModuleRequest create(TruffleString specifier, Map.Entry<TruffleString, TruffleString>[] assertions) {
            return new ModuleRequest(specifier, Map.ofEntries(assertions));
        }

        public TruffleString getSpecifier() {
            return specifier;
        }

        public Map<TruffleString, TruffleString> getAssertions() {
            return assertions;
        }

        public void setAssertions(Map<TruffleString, TruffleString> assertions) {
            this.assertions = assertions;
        }
    }

    private final List<ModuleRequest> requestedModules;
    private final List<ImportEntry> importEntries;
    private final List<ExportEntry> localExportEntries;
    private final List<ExportEntry> indirectExportEntries;
    private final List<ExportEntry> starExportEntries;
    private final List<ImportNode> imports;
    private final List<ExportNode> exports;

    public Module(List<ModuleRequest> requestedModules, List<ImportEntry> importEntries, List<ExportEntry> localExportEntries, List<ExportEntry> indirectExportEntries,
                    List<ExportEntry> starExportEntries, List<ImportNode> imports, List<ExportNode> exports) {
        this.requestedModules = List.copyOf(requestedModules);
        this.importEntries = List.copyOf(importEntries);
        this.localExportEntries = List.copyOf(localExportEntries);
        this.indirectExportEntries = List.copyOf(indirectExportEntries);
        this.starExportEntries = List.copyOf(starExportEntries);
        this.imports = imports == null ? null : List.copyOf(imports);
        this.exports = exports == null ? null : List.copyOf(exports);
    }

    public List<ModuleRequest> getRequestedModules() {
        return requestedModules;
    }

    public List<ImportEntry> getImportEntries() {
        return importEntries;
    }

    public List<ExportEntry> getLocalExportEntries() {
        return localExportEntries;
    }

    public List<ExportEntry> getIndirectExportEntries() {
        return indirectExportEntries;
    }

    public List<ExportEntry> getStarExportEntries() {
        return starExportEntries;
    }

    public List<ImportNode> getImports() {
        return imports;
    }

    public List<ExportNode> getExports() {
        return exports;
    }

    @Override
    public String toString() {
        return "Module [requestedModules=" + requestedModules + ", importEntries=" + importEntries + ", localExportEntries=" + localExportEntries + ", indirectExportEntries=" +
                        indirectExportEntries + ", starExportEntries=" + starExportEntries + ", imports=" + imports + ", exports=" + exports + "]";
    }
}

/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.js.parser.ir;

import java.util.List;

/**
 * Module information.
 */
public final class Module {
    /**
     * The synthetic binding name assigned to export default declarations with unnamed expressions.
     */
    public static final String DEFAULT_EXPORT_BINDING_NAME = "*default*";
    public static final String DEFAULT_NAME = "default";
    public static final String STAR_NAME = "*";

    public static final class ExportEntry {
        private final String exportName;
        private final String moduleRequest;
        private final String importName;
        private final String localName;

        private ExportEntry(String exportName, String moduleRequest, String importName, String localName) {
            this.exportName = exportName;
            this.moduleRequest = moduleRequest;
            this.importName = importName;
            this.localName = localName;
        }

        public static ExportEntry exportStarFrom(String moduleRequest) {
            return new ExportEntry(null, moduleRequest, STAR_NAME, null);
        }

        public static ExportEntry exportDefault() {
            return exportDefault(DEFAULT_EXPORT_BINDING_NAME);
        }

        public static ExportEntry exportDefault(String localName) {
            return new ExportEntry(DEFAULT_NAME, null, null, localName);
        }

        public static ExportEntry exportSpecifier(String exportName, String localName) {
            return new ExportEntry(exportName, null, null, localName);
        }

        public static ExportEntry exportSpecifier(String exportName) {
            return exportSpecifier(exportName, exportName);
        }

        public static ExportEntry exportIndirect(String exportName, String moduleRequest, String importName) {
            return new ExportEntry(exportName, moduleRequest, importName, null);
        }

        public ExportEntry withFrom(@SuppressWarnings("hiding") String moduleRequest) {
            return new ExportEntry(exportName, moduleRequest, localName, null);
        }

        public String getExportName() {
            return exportName;
        }

        public String getModuleRequest() {
            return moduleRequest;
        }

        public String getImportName() {
            return importName;
        }

        public String getLocalName() {
            return localName;
        }

        @Override
        public String toString() {
            return "ExportEntry [exportName=" + exportName + ", moduleRequest=" + moduleRequest + ", importName=" + importName + ", localName=" + localName + "]";
        }
    }

    public static final class ImportEntry {
        private final String moduleRequest;
        private final String importName;
        private final String localName;

        private ImportEntry(String moduleRequest, String importName, String localName) {
            this.moduleRequest = moduleRequest;
            this.importName = importName;
            this.localName = localName;
        }

        public static ImportEntry importDefault(String localName) {
            return new ImportEntry(null, DEFAULT_NAME, localName);
        }

        public static ImportEntry importStarAsNameSpaceFrom(String localNameSpace) {
            return new ImportEntry(null, STAR_NAME, localNameSpace);
        }

        public static ImportEntry importSpecifier(String importName, String localName) {
            return new ImportEntry(null, importName, localName);
        }

        public static ImportEntry importSpecifier(String importName) {
            return importSpecifier(importName, importName);
        }

        public ImportEntry withFrom(@SuppressWarnings("hiding") String moduleRequest) {
            return new ImportEntry(moduleRequest, importName, localName);
        }

        public String getModuleRequest() {
            return moduleRequest;
        }

        public String getImportName() {
            return importName;
        }

        public String getLocalName() {
            return localName;
        }

        @Override
        public String toString() {
            return "ImportEntry [moduleRequest=" + moduleRequest + ", importName=" + importName + ", localName=" + localName + "]";
        }
    }

    private final List<String> requestedModules;
    private final List<ImportEntry> importEntries;
    private final List<ExportEntry> localExportEntries;
    private final List<ExportEntry> indirectExportEntries;
    private final List<ExportEntry> starExportEntries;
    private final List<ImportNode> imports;
    private final List<ExportNode> exports;

    public Module(List<String> requestedModules, List<ImportEntry> importEntries, List<ExportEntry> localExportEntries, List<ExportEntry> indirectExportEntries,
                    List<ExportEntry> starExportEntries, List<ImportNode> imports, List<ExportNode> exports) {
        this.requestedModules = requestedModules;
        this.importEntries = importEntries;
        this.localExportEntries = localExportEntries;
        this.indirectExportEntries = indirectExportEntries;
        this.starExportEntries = starExportEntries;
        this.imports = imports;
        this.exports = exports;
    }

    public List<String> getRequestedModules() {
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

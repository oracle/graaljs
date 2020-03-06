package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DefaultEsModuleLoader implements JSModuleLoader {

    protected final JSRealm realm;
    private final Map<String, JSModuleRecord> moduleMap = new HashMap<>();

    public static DefaultEsModuleLoader create(JSRealm realm) {
        return new DefaultEsModuleLoader(realm);
    }

    protected DefaultEsModuleLoader(JSRealm realm) {
        this.realm = realm;
    }

    @Override
    public JSModuleRecord resolveImportedModule(ScriptOrModule referrer, String specifier) {
        String refPath = referrer == null ? null : referrer.getSource().getPath();
        try {
            TruffleFile moduleFile;
            if (refPath == null) {
                // Importing module source does not originate from a file.
                moduleFile = realm.getEnv().getPublicTruffleFile(specifier).getCanonicalFile();
            } else {
                TruffleFile refFile = realm.getEnv().getPublicTruffleFile(refPath);
                moduleFile = refFile.resolveSibling(specifier).getCanonicalFile();
            }
            String canonicalPath = moduleFile.getPath();
            return loadModuleFromUrl(specifier, moduleFile, canonicalPath);
        } catch (IOException | SecurityException e) {
            throw Errors.createErrorFromException(e);
        }
    }

    protected JSModuleRecord loadModuleFromUrl(String specifier, TruffleFile moduleFile, String canonicalPath) throws IOException {
        JSModuleRecord existingModule = moduleMap.get(canonicalPath);
        if (existingModule != null) {
            return existingModule;
        }
        Source source = Source.newBuilder(JavaScriptLanguage.ID, moduleFile).name(specifier).build();
        JSModuleRecord newModule = realm.getContext().getEvaluator().parseModule(realm.getContext(), source, this);
        moduleMap.put(canonicalPath, newModule);
        return newModule;
    }

    @Override
    public JSModuleRecord loadModule(Source source) {
        String path = source.getPath();
        String canonicalPath;
        if (path == null) {
            // Source does not originate from a file.
            canonicalPath = source.getName();
        } else {
            try {
                TruffleFile moduleFile = realm.getEnv().getPublicTruffleFile(path);
                canonicalPath = moduleFile.getCanonicalFile().getPath();
            } catch (IOException | SecurityException e) {
                throw Errors.createErrorFromException(e);
            }
        }
        return moduleMap.computeIfAbsent(canonicalPath, (key) -> realm.getContext().getEvaluator().parseModule(realm.getContext(), source, this));
    }
}

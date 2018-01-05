/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.objects;

import java.util.Objects;

/**
 * Result of the {@code ResolveExport} method of module records.
 */
public abstract class ExportResolution {
    private static final ExportResolution NULL = new Null();
    private static final ExportResolution AMBIGUOUS = new Ambiguous();

    private ExportResolution() {
    }

    /**
     * Definition not found or circular request.
     */
    public boolean isNull() {
        return false;
    }

    public boolean isAmbiguous() {
        return false;
    }

    public JSModuleRecord getModule() {
        throw new UnsupportedOperationException();
    }

    public String getBindingName() {
        throw new UnsupportedOperationException();
    }

    public static ExportResolution resolved(JSModuleRecord module, String bindingName) {
        return new Resolved(module, bindingName);
    }

    /**
     * Definition not found or circular request.
     */
    public static ExportResolution notFound() {
        return NULL;
    }

    public static ExportResolution ambiguous() {
        return AMBIGUOUS;
    }

    private static class Resolved extends ExportResolution {
        private final JSModuleRecord module;
        private final String bindingName;

        Resolved(JSModuleRecord module, String bindingName) {
            this.module = module;
            this.bindingName = bindingName;
        }

        @Override
        public JSModuleRecord getModule() {
            return module;
        }

        @Override
        public String getBindingName() {
            return bindingName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((bindingName == null) ? 0 : bindingName.hashCode());
            result = prime * result + ((module == null) ? 0 : module.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Resolved other = (Resolved) obj;
            return Objects.equals(this.module, other.module) && Objects.equals(this.bindingName, other.bindingName);
        }
    }

    private static class Null extends ExportResolution {
        @Override
        public boolean isNull() {
            return true;
        }
    }

    private static class Ambiguous extends ExportResolution {
        @Override
        public boolean isAmbiguous() {
            return true;
        }
    }
}

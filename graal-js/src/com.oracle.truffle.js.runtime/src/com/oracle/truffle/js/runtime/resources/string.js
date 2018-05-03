/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

"use strict";

(function(){

// String methods

Internal.CreateMethodProperty(String, "raw",
    function raw(template, ...substitutions) {
        var cooked = Internal.ToObject(template);
        var raw = Internal.ToObject(cooked.raw);
        var literalSegments = Internal.ToLength(raw.length);
        if (literalSegments <= 0) {
            return "";
        }
        var numberOfSubstitutions = substitutions.length;
        var stringElements = "";
        for (var i = 0;; i++) {
            stringElements += Internal.ToString(raw[i]);
            if (i + 1 == literalSegments) {
                break;
            }
            if (i < numberOfSubstitutions) {
                stringElements += Internal.ToString(substitutions[i]);
            }
        }
        return stringElements;
    }
);

})();

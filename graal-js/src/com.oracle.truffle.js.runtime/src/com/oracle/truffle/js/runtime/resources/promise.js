/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

"use strict";

(function(){

if (Internal.V8CompatibilityMode) {
    var defer = function() {
        var deferred = {};
        deferred.promise = new this(function(resolve, reject) {
            deferred.resolve = resolve;
            deferred.reject = reject;
        });
        return deferred;
    };

    Internal.CreateMethodProperty(Promise.prototype, "chain", Promise.prototype.then);
    Internal.CreateMethodProperty(Promise, "accept", Promise.resolve);
    Internal.CreateMethodProperty(Promise, "defer", defer);
}

})();

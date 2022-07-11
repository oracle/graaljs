/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * [GR-39719] Active module resolution is confused by block scopes.
 */

{
    const answer = 42;
    Promise.resolve(function() {
        return answer;
    });
    import('./GR-38391import.mjs');
}

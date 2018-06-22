/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Parse function returns a JSON object representing ECMAScript code passed.
 * name is optional name for the code source and location param tells whether to
 * include location information for AST nodes or not.
 *
 * Example:
 *
 *    load("nashorn:parser.js");
 *    try {
 *        var json = parse("print('hello')");
 *        print(JSON.stringify(json));
 *    } catch (e) {
 *        print(e);
 *    }
 */
function parse(code, name, location) {
    var jsonStr = parseToJSON(code, name, location);
    return JSON.parse(jsonStr,
        function (prop, value) {
            if (typeof value === "string" && prop === "value") {
                // handle regexps and strings - both are encoded as strings but strings
                // do not start with '/'. If regexp, then eval it to make RegExp object
                return value.startsWith('/') ? eval(value) : value.substring(1);
            } else {
                // anything else is returned "as is""
                return value;
            }
        });
}

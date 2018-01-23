/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
    var jsonStr = TestNashorn.parseToJSON(code, name, location);
    return JSON.parse(jsonStr,
        function (prop, value) {
            if (typeof value === "string" && prop === "value") {
                // handle regexps and strings - both are encoded as strings but strings
                // do not start with '/'. If regexp, then eval it to make RegExp object
                return value.startsWith('/') ? eval(value) : value.substring(1);
            } else {
                // anythin else is returned "as is""
                return value;
            }
        });
}

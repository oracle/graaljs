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


if (Internal.AnnexB) {
// HTML generation methods of String.prototype

function CreateHTML(string, tag, attribute, value) {
    Internal.RequireObjectCoercible(string);
    var S = Internal.ToString(string);
    var p1 = "<" + tag;
    if (attribute !== "") {
        p1 += " " + attribute + '="' + Internal.StringReplace(Internal.ToString(value), '"', "&quot;") + '"';
    }
    return p1 + ">" + S + "</" + tag + ">";
}

Internal.CreateMethodProperty(String.prototype, "anchor", function anchor(name) { return CreateHTML(this, "a", "name", name); });
Internal.CreateMethodProperty(String.prototype, "big", function big() { return CreateHTML(this, "big", "", ""); });
Internal.CreateMethodProperty(String.prototype, "blink", function blink() { return CreateHTML(this, "blink", "", ""); });
Internal.CreateMethodProperty(String.prototype, "bold", function bold() { return CreateHTML(this, "b", "", ""); });
Internal.CreateMethodProperty(String.prototype, "fixed", function fixed() { return CreateHTML(this, "tt", "", ""); });
Internal.CreateMethodProperty(String.prototype, "fontcolor", function fontcolor(clr) { return CreateHTML(this, "font", "color", clr); });
Internal.CreateMethodProperty(String.prototype, "fontsize", function fontsize(size) { return CreateHTML(this, "font", "size", size); });
Internal.CreateMethodProperty(String.prototype, "italics", function italics() { return CreateHTML(this, "i", "", ""); });
Internal.CreateMethodProperty(String.prototype, "link", function link(url) { return CreateHTML(this, "a", "href", url); });
Internal.CreateMethodProperty(String.prototype, "small", function small() { return CreateHTML(this, "small", "", ""); });
Internal.CreateMethodProperty(String.prototype, "strike", function strike() { return CreateHTML(this, "strike", "", ""); });
Internal.CreateMethodProperty(String.prototype, "sub", function sub() { return CreateHTML(this, "sub", "", ""); });
Internal.CreateMethodProperty(String.prototype, "sup", function sup() { return CreateHTML(this, "sup", "", ""); });
}

})();

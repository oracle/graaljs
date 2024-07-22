---
layout: docs
toc_group: js
link_title: Migration Guide from Rhino to GraalJS
permalink: /reference-manual/js/RhinoMigrationGuide/
---

# Migration Guide from Rhino to GraalJS

This document serves as a migration guide for code previously targeted to the Rhino engine.
See the [Java Interoperability guide](JavaInteroperability.md) for an overview of supported features.

Both Rhino and GraalJS support a similar set of syntax and semantics for Java interoperability.
The most important differences relevant for migrations are listed here.

### `Java.type(typename)` instead of `java.a.b.c.typename`

GraalJS does not put available Java classes in the JavaScript scope.
You have to explicitly load the classes using `Java.type(typename)`.

GraalJS supports the `Packages` global object, but loading the classes explicitly is still encouraged.
The following Java package globals are available in the Nashorn compatibility mode (`js.nashorn-compat` option): `java`, `javafx`, `javax`, `com`, `org`, `edu`.

### Console Output of Java Classes and Java Objects

GraalJS provides the `print` builtin function.
It tries to special-case its behavior on Java classes and Java objects to provide the most useful output.

Note that GraalJS also provides a `console.log` function.
This is an alias for `print` in pure JavaScript mode, but uses an implementation provided by Node.js when in Node mode.
The behavior around interop objects differs for `console.log` in Node mode as it does not implement special treatment for such objects.

### JavaScript vs Java Strings

GraalJS uses Java strings internally to represent JavaScript strings.
This makes it impossible to differentiate whether a specific string was created by JavaScript or by Java code.
In GraalJS, the JavaScript properties take precedence over Java fields or methods.
For instance, you can query the `length` property (of JavaScript) but you cannot call the `length` function (of Java) on JavaScript strings - `length` behaves like a data property, not like a function.

### JavaImporter

The `JavaImporter` feature is available only in the Nashorn compatibility mode (`js.nashorn-compat`).

### Related Documentation

* [Migration Guide from Nashorn to GraalJS](NashornMigrationGuide.md)
* [Java Interoperability](JavaInteroperability.md)
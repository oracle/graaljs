GraalVM includes an ECMAScript compliant JavaScript engine. It is designed to be
fully standard compliant, execute applications with high performance, and
provide all the benefits from the GraalVM stack, including language
interoperability and common tooling. With that engine, GraalVM can execute plain
JavaScript code or run unmodified Node.js applications. Applications can import
npm modules, including native ones.

This user documentation provides information on available GraalVM JavaScript
engine configuration, Node.JS runtime, the `javax.script.ScriptEngine`
implementation, multithreading support details, GraalVM JavaScript execution on
a stock JVM like OpenJDK, possible embedding scenarios and other. To
migrate the code previously targeted to the Nashorn or Rhino engines, migration
guides are available.  

enablePlugins(ScalaJSPlugin)
//enablePlugins(ScalaJSBundlerPlugin)

name := "tessel-display"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "org.scala-js" %%% "scalajs-dom"    % "0.9.0"
libraryDependencies += "com.nativelibs4java" %% "scalaxy-streams" % "0.3.4" % "provided"
libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.5" % "provided"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.0"
//libraryDependencies += "fr.hmil" %%% "roshttp" % "2.0.0-RC1"

//npmDependencies in Compile += "jpeg-js" -> "0.2.0"
//npmDependencies in Compile += "node-fetch" -> "1.6.3"
//
//scalaJSModuleKind := ModuleKind.CommonJSModule

scalaJSOutputWrapper := (
  """ |var addGlobalProps = function(obj) {
      |  obj.require = require;
      |}
      |if((typeof __ScalaJSEnv === "object") && typeof __ScalaJSEnv.global === "object") {
      |  addGlobalProps(__ScalaJSEnv.global);
      |} else if(typeof  global === "object") {
      |  addGlobalProps(global);
      |} else if(typeof __ScalaJSEnv === "object") {
      |  __ScalaJSEnv.global = {};
      |  addGlobalProps(__ScalaJSEnv.global);
      |} else {
      |  var __ScalaJSEnv = { global: {} };
      |  addGlobalProps(__ScalaJSEnv.global)
      |}
  """.stripMargin,
  "org.bruchez.tessel.Demo().main();"
)
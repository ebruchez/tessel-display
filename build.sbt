enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

name := "tessel-display"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "org.scala-js"           %%% "scalajs-dom"    % "0.9.0"

libraryDependencies += "com.nativelibs4java"    %% "scalaxy-streams" % "0.3.4" % "provided"
libraryDependencies += "org.scala-lang.modules" %% "scala-async"     % "0.9.5" % "provided"

npmDependencies in Compile += "jpeg-js"    → "0.2.0"
npmDependencies in Compile += "node-fetch" → "1.6.3"

scalaJSModuleKind := ModuleKind.CommonJSModule
webpackConfigFile := Some(baseDirectory.value / "tessel.webpack.config.js")

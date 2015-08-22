# cpptool-lib-metrics

A java project using the [CppTool-Lib](https://github.com/search-rug/cpptool-lib) library for calculating software metrics based on the data files produced by [CppTool](https://github.com/search-rug/cpptool)

This project uses Java 8 and contains public implementation.
Users should only use the methods contained in `nl.rug.search.cpptool.metrics.Metrics.java`.
See [the example TestMain.java](src/main/java/nl/rug/search/cpptool/metrics/TestMain.java) for an example on how to use the funtions for calculation of metrics.

#### Building
* Clone this project and run `git submodule update --init`.
* Execute the command `./gradlew idea` to produce the intelliJ project

This project is MIT licensed.
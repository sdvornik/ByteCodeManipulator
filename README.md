# JDBC bytecode manipulator

## Running

* Import project to IDE.
* Create folders "input" and "output" in root folder.
* Put in folder "input" any JDBC driver implementation and file driver.conf in accordance with
example driver.h2.conf and driver.mysql.conf (in root folder)
* For IntelliJI: Select menu option Run -> Edit Configuration, in dialog menu in field Program arguments
  point name of JDBC driver. 
* Run program with right mouse click on main class Main.
* The modified driver is in the "output" folder.

## Built With

* [SBT](http://www.scala-sbt.org/) - Scala build tool


# akka-streams-experiments


## get scala-cli

```bash
brew install Virtuslab/scala-cli/scala-cli
```

## then run it
````bash
$ scala-cli run main.scala

Compiling project (Scala 2.13.10, JVM)
Compiled project (Scala 2.13.10, JVM)
[hint] ./main.scala:2:16
[hint] "akka-stream is outdated, update to 2.8.0"
[hint]      akka-stream 2.6.20 -> com.typesafe.akka::akka-stream:2.8.0
[hint] //> using dep "com.typesafe.akka::akka-stream:2.6.20"
[hint]                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
retry item:[1]
so sad we give up on item:[Failure(1,java.lang.RuntimeException: it failed)]
stop retring item:[2]
result - item:[number: 2]
retry item:[3]
so sad we give up on item:[Failure(3,java.lang.RuntimeException: it failed)]
stop retring item:[4]
result - item:[number: 4]
retry item:[5]
so sad we give up on item:[Failure(5,java.lang.RuntimeException: it failed)]
```

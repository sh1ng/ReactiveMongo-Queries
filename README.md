# ReactiveMongo-Queries easier to write queries with ReactiveMongo

ReactiveMongo-Queries is available on sonatype.org.

If you use ReactiveMongo 0.10.0, you just have to edit build.sbt and add the following:

```scala
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.github.ReactiveMongo-Queries" %% "reactivemongo-queries" % "0.10.0.a-SNAPSHOT"
)
```

There's a version for 0.10.5.akka23-SNAPSHOT

```scala
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.github.ReactiveMongo-Queries" %% "reactivemongo-queries" % "0.10.5.a-SNAPSHOT"
)
```


## Strongly typed queries to sophisticated objects

Suppose we query employees with below structure
```scala
case class Contacts(email: String, phone: String)
case class Employee(name: String, contacts: Contacts, salary: Int)
```

### Getting Employee(s) by name 
without Queries

```scala
collection.find(BSONDocument("name" -> "john"))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.find(on[Employee].eq(_.name, "john"))
```

### Getting Employee(s) by email
without Queries

```scala
collection.find(BSONDocument("contacts.email" -> "john@company.com"))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.find(on[Employee].eq(_.contacts.email, "john@company.com"))
```

### Find employees with salary greater than 100k
without Queries

```scala
collection.update.find(BSONDocument("salary" -> BSONDocument("$gt" -> 100000)))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.find(on[Employee].gt(_.salary, 100000))
```


### Increase salary for all employees earning less than 50k on 10%
without Queries

```scala
collection.update.find(BSONDocument("salary" -> BSONDocument("$lt" -> 50000)), 
BSONDocument("$mul" -> BSONDocument("salary" -> 1.1)))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.update(on[Employee].lt(_.salary, 50000), 
on[Employee].update(_.mul(_.salary, 1.1)))
```

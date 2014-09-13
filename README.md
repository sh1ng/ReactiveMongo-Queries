# ReactiveMongo-Queries easier to write queries with ReactiveMongo

## Strongly typed queries to sophisticated objects

Suppose we quiry employees with below structure
```scala
case class Contacts(email: String, phone: String)
case class Employee(name: String, contacts: Contacts, sellary: Int)
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

### Find employees with sellary greater that 100k
without Queries

```scala
collection.update.find(BSONDocument("sellary" -> BSONDocument("$gt" -> 100000)))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.update(on[Employee].gt(_.sellary, 100000))
```


### Increase sellary for all employees eaning less that 50k on 10%
without Queries

```scala
collection.update.find(BSONDocument("sellary" -> BSONDocument("$lt" -> 50000)), 
BSONDocument("$mul" -> BSONDocument("sellary" -> 1.1)))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.update(on[Employee].lt(_.sellary, 50000), 
on[Employee].update(_.mul(_.sellary, 1.1)))
```

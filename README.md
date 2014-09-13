# ReactiveMongo-Queries easier to write queries with ReactiveMongo

## Strongly typed queries to sophisticated objects

Suppose we quiry employees with below structure
```scala
case class Contacts(email: String, phone: String)
case class Employee(name: String, contacts: Contacts, sallary: Int)
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

### Find employees with sallary greater that 100k
without Queries

```scala
collection.update.find(BSONDocument("sallary" -> BSONDocument("$gt" -> 100000)))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.update(on[Employee].gt(_.sallary, 100000))
```


### Increase sallary for all employees eaning less that 50k on 10%
without Queries

```scala
collection.update.find(BSONDocument("sallary" -> BSONDocument("$lt" -> 50000)), 
BSONDocument("$mul" -> BSONDocument("sallary" -> 1.1)))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.update(on[Employee].lt(_.sallary, 50000), 
on[Employee].update(_.mul(_.sallary, 1.1)))
```

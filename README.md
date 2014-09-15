# ReactiveMongo-Queries easier to write queries with ReactiveMongo

## The project'll be published as soon as I get access to nexus repository.

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

### Find employees with salary greater that 100k
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

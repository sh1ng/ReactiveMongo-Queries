# ReactiveMongo-Queries easier to write queries with ReactiveMongo

## Strongly typed queries to sophisticated objects

Suppose we quiry employees with below structure
```scala
case class Contacts(email: String, phone: String)
case class Employee(name: String, contacts: Contacts)
```

### Getting Employee by name 
without Queries

```scala
collection.find(BSONDocument("name" -> "john"))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.find(on[Employee].eq(_.name, "john"))
```

### Getting Employee by email
without Queries

```scala
collection.find(BSONDocument("contacts.email" -> "john@company.com"))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.find(on[Employee].eq(_.contacts.email, "john@company.com"))
```

### Updating employee's email
without Queries

```scala
collection.update.find(BSONDocument("name" -> "john"), 
BSONDocument("$set" -> BSONDocument("contacts.email" -> "john@company.com")))
```
with Queries

```scala
import reactivemongo.queries.Query._

collection.update(on[Employee].eq(_.name, "john"), 
on[Employee].update(_.set(_.contacts.email, "john@company.com")))
```






import reactivemongo.bson._
import reactivemongo.queries._
import org.specs2.mutable._
import reactivemongo.bson.exceptions.DocumentKeyNotFound
import reactivemongo.bson.Macros.Annotations.Key
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

object PetType extends Enumeration {
    type PetType = Value
    val Dog, Cat, Rabbit = Value
    
   implicit object PetTypeBSONHandler extends BSONHandler[BSONString, PetType] {
      def read(v: BSONString) = PetType.withName(v.value)
      def write(v: PetType) = BSONString(v.toString())
    }
  }

import PetType._


case class Account(_id: BSONObjectID, login: String, age: Option[Int])
case class Person(firstName: String, lastName: String)
case class Pet(nick: String, age: Int, typ: PetType, favoriteDishes: List[String])
case class Contacts(email: String, phone: String)
case class Employee(name: String, contacts: Contacts)

@RunWith(classOf[JUnitRunner])
class QueryMacroSpec extends Specification {
  import Query._
  
  "and" in {
    on[Pet].and(List()) mustEqual BSONDocument()
    on[Pet].and(List(BSONDocument("age" -> 2))) mustEqual BSONDocument("$and" -> BSONArray(BSONDocument("age" -> 2)))
    on[Pet].and(List(BSONDocument("age" -> 2), BSONDocument("nick" -> "kitty"))) mustEqual BSONDocument("$and" -> BSONArray(List(BSONDocument("age" -> 2), BSONDocument("nick" -> "kitty"))))
    
    on[Pet].and() mustEqual BSONDocument()
    on[Pet].and(_.eq(_.age, 2)) mustEqual BSONDocument("$and" -> BSONArray(BSONDocument("age" -> 2)))
    on[Pet].and(_.eq(_.age, 2), _.eq(_.nick, "kitty")) mustEqual BSONDocument("$and" -> BSONArray(List(BSONDocument("age" -> 2), BSONDocument("nick" -> "kitty"))))
  }
  
  "eq" in {
    val id = BSONObjectID.generate
    val findById = on[Account].eq(_._id, id)
    findById mustEqual BSONDocument("_id" -> id)
  }
  
  "eq Traversable" in {
    val pet = Pet("dog", 5, PetType.Dog, List("beaf", "meal", "fish"))
    val q = on[Pet].eq(_.favoriteDishes, "fish")
    q mustEqual BSONDocument("favoriteDishes" -> "fish")
  }
  
  "eq Option" in {
    val q = on[Account].eq(_.age, 18)
    q mustEqual BSONDocument("age" -> 18)
  }
  
  "eq multiple access" in {
    val q = on[Employee].eq(_.contacts.email, "john@doe.com")
    println(q.elements(0)._1)
    q mustEqual BSONDocument("contacts.email" -> "john@doe.com")
  }
  
  
  
    "in" in {
    val q = on[Person].in(_.firstName, List("abc", "cde"))
    q.elements must have size(1)
    
    val in = q.getAs[BSONDocument]("firstName").get
    
    in.elements must have size(1)
    
    val arr = in.getAs[BSONArray]("$in").get
    
    arr.length mustEqual 2
        
    arr.getAs[BSONString](0).get.value mustEqual "abc"
    arr.getAs[BSONString](1).get.value mustEqual "cde"
  	}
  
  "gt,gte,lt,lte,ne" in {
    val query = on[Person]
    val value = "abc"
    val operations = List[Pair[String, (Queryable[Person], String)  => BSONDocument]](
        ("$gt", (q, v) => q.gt(_.firstName, v)),
        ("$gte",(q, v) => q.gte(_.firstName, v)),
        ("$lt",(q, v) => q.lt(_.firstName, v)),
        ("$lte",(q, v) => q.lte(_.firstName, v)),
        ("$ne",(q, v) => q.ne(_.firstName, v))
        )
        operations.forall(p=> p._2(query, value) mustEqual BSONDocument("firstName" -> BSONDocument(p._1 -> value)))
  }
  
  "gt option" in {
    val q = on[Account].gt(_.age, 18)
    q mustEqual BSONDocument("age" -> BSONDocument("$gt" -> 18))
  }
  
  "set" in {
   val query = on[Person]
   val updateQuery = query.update(_.set[String](_.firstName, "john"), _.set(_.lastName, "doe"))
   
   updateQuery.getAs[BSONDocument]("$set") must beSome(BSONDocument("firstName" -> "john", "lastName" -> "doe"))
  }
  
  "setOpt" in {
    val setQuery = on[Account].update(_.setOpt(_.age, Some(42)))
    setQuery mustEqual BSONDocument("$set" -> BSONDocument("age" -> 42))
    
    val unsetQuery = on[Account].update(_.setOpt(_.age, None))
    unsetQuery mustEqual BSONDocument("$unset" -> BSONDocument("age" -> ""))
  }
  
  "exists" in {
    val query = on[Account].exists(_.age, true)
    query mustEqual BSONDocument("age" -> BSONDocument("$exists" -> true))
  }
  
  "set, inc & handler" in {
    import PetTypeBSONHandler._
    val update = on[Pet].update(_.set(_.typ, PetType.Cat), _.set(_.nick, "Manul"), _.inc(_.age, 1))
    update.getAs[BSONDocument]("$inc") must beSome(BSONDocument("age" -> 1))
    update.getAs[BSONDocument]("$set") must beSome(BSONDocument("typ" -> "Cat", "nick" -> "Manul"))
    update.elements must be size(2)
  }
  
  "mul" in {
    import PetTypeBSONHandler._

    val update = on[Pet].update(_.mul(_.age, 2))
    update.getAs[BSONDocument]("$mul") must beSome(BSONDocument("age" -> 2))
    update.elements must be size(1)
  }
  
  "push" in {
    import PetTypeBSONHandler._
    
    val update = on[Pet].update(_.push(_.favoriteDishes, List("Milk")))
    update.getAs[BSONDocument]("$push") must beSome(BSONDocument("favoriteDishes" -> "Milk"))
    update.elements must be size(1)
    
    val updateEach = on[Pet].update(_.push(_.favoriteDishes, List("Milk", "Fish")))
    updateEach.getAs[BSONDocument]("$push") must beSome(BSONDocument("favoriteDishes" -> BSONDocument("$each" -> BSONArray("Milk", "Fish"))))
    updateEach.elements must be size(1)
  }
  
  "addToSet" in {
    import PetTypeBSONHandler._
    val update = on[Pet].update(_.addToSet(_.favoriteDishes, List("Milk")))
    update.getAs[BSONDocument]("$addToSet") must beSome(BSONDocument("favoriteDishes" -> "Milk"))
    update.elements must be size(1)
    
    val updateEach = on[Pet].update(_.addToSet(_.favoriteDishes, List("Milk", "Fish")))
    updateEach.getAs[BSONDocument]("$addToSet") must beSome(BSONDocument("favoriteDishes" -> BSONDocument("$each" -> BSONArray("Milk", "Fish"))))
    updateEach.elements must be size(1)
  }
  
  "multiple updates" in {
    import PetTypeBSONHandler._
    
    val update = on[Pet].update(_.set(_.nick, "kitty"), _.addToSet(_.favoriteDishes, List("milk")))
    update.elements must be size(2)
    update.getAs[BSONDocument]("$set") must beSome(BSONDocument("nick" -> "kitty"))
    update.getAs[BSONDocument]("$addToSet") must beSome(BSONDocument("favoriteDishes" -> "milk"))
  }
  
  "sort" in {
    val q = on[Account].orderBy(_.sortAsc(_.age), _.sortDesc(_.login))
    
    q mustEqual BSONDocument("age" -> 1, "login" -> -1)
  }
}

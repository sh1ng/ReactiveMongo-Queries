package reactivemongo.queries

import reactivemongo.bson._

trait BSONQueryWriter[C, T, B <: BSONValue] {
  def write: BSONWriter[T, B]
}

abstract class PlainBSONQueryWriter[C, T, B <: BSONValue] extends BSONQueryWriter[C, T, B]

case class ValueBSONQueryWriter[T, B <: BSONValue](implicit writer: BSONWriter[T, B]) extends PlainBSONQueryWriter[T, T, B] {
  def write = writer
}

case class OptionBSONQueryWriter[C <: Option[T], T , B <: BSONValue](implicit writer: BSONWriter[T, B]) extends PlainBSONQueryWriter[Option[T], T, B] {
  def write = writer
}

case class TraversableBSONQueryWriter[C <: Traversable[T], T , B <: BSONValue](implicit writer: BSONWriter[T, B]) extends BSONQueryWriter[C, T,  B] {
  def write = writer
}

object Query{
  def on[T] = new Queryable[T]
  
  implicit def valueQueryWriter[T, B <: BSONValue](implicit writer: BSONWriter[T, B]) : PlainBSONQueryWriter[T, T, B] = ValueBSONQueryWriter[T, B]()
  
  implicit def optionQueryWriter[C <: Option[T], T , B <: BSONValue](implicit writer: BSONWriter[T, B]): PlainBSONQueryWriter[Option[T], T,  B] = OptionBSONQueryWriter[C, T, B]()
  
  implicit def traversableQueryWriter[C <: Traversable[T], T , B <: BSONValue](implicit writer: BSONWriter[T, B]): BSONQueryWriter[C, T,  B] = TraversableBSONQueryWriter[C, T, B]() 
  
}

trait UpdateOperator {
  val operator: String
  val field: String
  val value: BSONValue
}

case class SetOperator(val field: String, val value: BSONValue) extends UpdateOperator {
  val operator = "$set"
}
case class UnsetOperator(val field: String) extends UpdateOperator {
  val operator = "$unset"
  val value = BSONString("")
}
case class IncOperator(val field: String, val value: BSONValue) extends UpdateOperator {
  val operator = "$inc"
}
case class MulOperator(val field: String, val value: BSONValue) extends UpdateOperator {
  val operator = "$mul"
}
case class MinOperator(val field: String, val value: BSONValue) extends UpdateOperator {
  val operator = "$min"
}
case class MaxOperator(val field: String, val value: BSONValue) extends UpdateOperator {
  val operator = "$max"
}
case class AddToSetOperator(val field: String, val value: BSONValue) extends UpdateOperator {
  val operator = "$addToSet"
}
case class PopOperator(val field: String, val value: BSONValue) extends UpdateOperator {
  val operator = "$pop"
}
case class PullAllOperator(val field: String, val value: BSONValue) extends UpdateOperator {
  val operator = "$pullAll"
}
case class PushOperator(val field: String, val value: BSONValue) extends UpdateOperator {
  val operator = "$push"
}



class Queryable[T] {
  import language.experimental.macros
  import Query._
  
  private def aggr(exps: Traversable[BSONDocument], tag: String) = if(exps.isEmpty) BSONDocument() else BSONDocument(tag -> BSONArray(exps))
  
	def eq[C, A](p: T => C, value: A)(implicit queryWriter: BSONQueryWriter[C, A, _ <: BSONValue]) : BSONDocument = macro QueryMacroImpl.eq[T, A, C]
	def gt[A, C](p: T => C, value: A)(implicit queryWriter: PlainBSONQueryWriter[C, A, _ <: BSONValue]) : BSONDocument = macro QueryMacroImpl.gt[T, A, C]
	def gte[A, C](p: T => C, value: A)(implicit queryWriter: PlainBSONQueryWriter[C, A, _ <: BSONValue]) : BSONDocument = macro QueryMacroImpl.gte[T, A, C]
	def in[C, A](p: T => C, values: Traversable[A])(implicit queryWriter: BSONQueryWriter[C, A, _ <: BSONValue]) : BSONDocument = macro QueryMacroImpl.in[T, A, C]
	def lt[A, C](p: T => C, value: A)(implicit queryWriter: PlainBSONQueryWriter[C, A, _ <: BSONValue]) : BSONDocument = macro QueryMacroImpl.lt[T, A, C]
	def lte[A, C](p: T => C, value: A)(implicit queryWriter: PlainBSONQueryWriter[C, A, _ <: BSONValue]) : BSONDocument = macro QueryMacroImpl.lte[T, A, C]
	def ne[C, A](p: T => C, value: A)(implicit queryWriter: BSONQueryWriter[C, A, _ <: BSONValue]) : BSONDocument = macro QueryMacroImpl.ne[T, A, C]
	def nin[C, A](p: T => C, values: Traversable[A])(implicit queryWriter: BSONQueryWriter[C, A, _ <: BSONValue]) : BSONDocument = macro QueryMacroImpl.nin[T, A, C]
  def exists[A](p: T => Option[A], exists: Boolean): BSONDocument = macro QueryMacroImpl.exists[T, A]
	
	def sortAsc[A](p: T => A): BSONDocument = macro QueryMacroImpl.sortAsc[T, A]
	def sortDesc[A](p: T => A): BSONDocument = macro QueryMacroImpl.sortDesc[T, A]
	
	def orderBy(exps: Queryable[T] => BSONDocument *) : BSONDocument = orderBy(exps.map(p => p(on[T])))
	def orderBy(exps: Traversable[BSONDocument]) : BSONDocument = BSONDocument(exps.flatMap(_.elements))
  
	def set[A](p: T => A, value: A)(implicit handler: BSONWriter[A, _ <: BSONValue]) : SetOperator = macro QueryMacroImpl.set[T, A]
  def setOpt[A](p: T => Option[A], value: Option[A])(implicit handler: BSONWriter[A, _ <: BSONValue]) : UpdateOperator = macro QueryMacroImpl.setOpt[T, A]
  def unset[A](p: T => A) : UnsetOperator = macro QueryMacroImpl.unset[T, A]
  def inc[A](p: T => A, value: A)(implicit handler: BSONWriter[A, _ <: BSONValue]) : IncOperator = macro QueryMacroImpl.inc[T, A]
  def mul[A](p: T => A, value: A)(implicit handler: BSONWriter[A, _ <: BSONValue]) : MulOperator = macro QueryMacroImpl.mul[T, A]
  def min[A](p: T => A, value: A)(implicit handler: BSONWriter[A, _ <: BSONValue]) : MinOperator = macro QueryMacroImpl.min[T, A]
  def max[A](p: T => A, value: A)(implicit handler: BSONWriter[A, _ <: BSONValue]) : MaxOperator = macro QueryMacroImpl.max[T, A]
  def addToSet[A](p: T => Traversable[A], values: Traversable[A])(implicit handler: BSONWriter[A, _ <: BSONValue]) : AddToSetOperator = macro QueryMacroImpl.addToSet[T, A]
  
  def pullAll[A](p: T => Traversable[A], value: Traversable[A])(implicit handler: BSONWriter[A, _ <: BSONValue]) : PullAllOperator = macro QueryMacroImpl.pullAll[T, A]
	def push[A](p: T => Traversable[A], values: Traversable[A])(implicit handler: BSONWriter[A, _ <: BSONValue]) : PushOperator = macro QueryMacroImpl.push[T, A]
  
  
  
  def update(updateOperators: Queryable[T] => UpdateOperator *) = {
    val operators = updateOperators.map(_ apply on[T]).groupBy(_.operator)
         .map(p => (p._1, BSONDocument(p._2.map(x => (x.field, x.value)))))
    BSONDocument(operators)
  }
	def and(exps: Queryable[T] => BSONDocument *): BSONDocument =  and(exps.map(_(on[T])).toSeq)
	def and(exps: Traversable[BSONDocument]): BSONDocument = aggr(exps, "$and")
	def or(exps: Queryable[T] => BSONDocument *) = BSONDocument("$or" -> BSONArray(exps.map(_(on[T]))))
	def or(exps: Traversable[BSONDocument]) = aggr(exps, "$or")
	def not(exp: Queryable[T] => BSONDocument) = BSONDocument("$not" -> exp(on[T]))
	def nor(exps: Queryable[T] => BSONDocument *) = BSONDocument("$nor" -> BSONArray(exps.map(_(on[T]))))
	def nor(exps: Traversable[BSONDocument]) = aggr(exps, "$nor")
}
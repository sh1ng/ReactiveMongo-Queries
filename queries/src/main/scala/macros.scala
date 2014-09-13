package reactivemongo.queries

import collection.mutable.ListBuffer
import scala.reflect.macros.Context
import reactivemongo.bson._

private object QueryMacroImpl{
  
  private def path[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => A]) = {
    import c.universe._
    object propertyTraverser extends Traverser {
      var applies = List[String]()
       override def traverse(tree: c.universe.Tree): Unit = tree match {
       case Select(a, b) => {
         applies = b.decoded :: applies
         this.traverse(a)
       }
       case _ => super.traverse(tree)   
      } 
    }
    propertyTraverser.traverse(p.tree.children(1))
    val literal = Literal(Constant(propertyTraverser.applies.mkString(".")))
    c.Expr[String](literal)
  }
  
  
  private def builder[T: c.WeakTypeTag, A: c.WeakTypeTag, C: c.WeakTypeTag](c: Context)(p : c.Expr[T => C], value: c.Expr[A])
 (handler: c.Expr[BSONQueryWriter[C, A, _ <: BSONValue]])
 (tag: String): c.Expr[BSONDocument] = {
    import c.universe._
    
    val tagRepTree = Literal(Constant(tag))
    reify {
      val tagName = c.Expr[String](tagRepTree).splice
      val n = path(c)(p).splice
      val v = handler.splice.write.write(value.splice)
      val v2 = BSONDocument(List((tagName, v)))
	    BSONDocument(List((n, v2)))
    } 
  }
  
 def eq[T: c.WeakTypeTag, A: c.WeakTypeTag, C: c.WeakTypeTag](c: Context)(p : c.Expr[T => C], value: c.Expr[A])
 (queryWriter: c.Expr[BSONQueryWriter[C, A, _ <: BSONValue]]): c.Expr[BSONDocument] = {
   import c.universe._
     c.universe.reify {
       val n = path(c)(p).splice
       val v = queryWriter.splice.write.write(value.splice)
	     BSONDocument(List((n, v)))
	    }
    
  }
 
 def gt[T: c.WeakTypeTag, A: c.WeakTypeTag, C: c.WeakTypeTag](c: Context)(p : c.Expr[T => C], value: c.Expr[A])
 (queryWriter: c.Expr[PlainBSONQueryWriter[C, A, _ <: BSONValue]]): c.Expr[BSONDocument] = 
   builder[T, A, C](c)(p, value)(queryWriter)("$gt")
   
 def gte[T: c.WeakTypeTag, A: c.WeakTypeTag, C: c.WeakTypeTag](c: Context)(p : c.Expr[T => C], value: c.Expr[A])
 (queryWriter: c.Expr[PlainBSONQueryWriter[C, A, _ <: BSONValue]]): c.Expr[BSONDocument] = 
   builder[T, A, C](c)(p, value)(queryWriter)("$gte")
   
 def lt[T: c.WeakTypeTag, A: c.WeakTypeTag, C: c.WeakTypeTag](c: Context)(p : c.Expr[T => C], value: c.Expr[A])
 (queryWriter: c.Expr[PlainBSONQueryWriter[C, A, _ <: BSONValue]]): c.Expr[BSONDocument] = 
   builder[T, A, C](c)(p, value)(queryWriter)("$lt")
   
 def lte[T: c.WeakTypeTag, A: c.WeakTypeTag, C: c.WeakTypeTag](c: Context)(p : c.Expr[T => C], value: c.Expr[A])
 (queryWriter: c.Expr[PlainBSONQueryWriter[C, A, _ <: BSONValue]]): c.Expr[BSONDocument] = 
   builder[T, A, C](c)(p, value)(queryWriter)("$lte")
   
 def ne[T: c.WeakTypeTag, A: c.WeakTypeTag, C: c.WeakTypeTag](c: Context)(p : c.Expr[T => C], value: c.Expr[A])
 (queryWriter: c.Expr[BSONQueryWriter[C, A, _ <: BSONValue]]): c.Expr[BSONDocument] = 
   builder[T, A, C](c)(p, value)(queryWriter)("$ne")
   
    
 def in[T: c.WeakTypeTag, A: c.WeakTypeTag, C: c.WeakTypeTag](c: Context)(p : c.Expr[T => C], values: c.Expr[Traversable[A]])
 (queryWriter: c.Expr[BSONQueryWriter[C, A, _ <: BSONValue]]): c.Expr[BSONDocument] = {
   import c.universe._
   
   reify {
    val n = path(c)(p).splice
	  val writer = queryWriter.splice.write
	  val items = values.splice.map(writer.write(_))
	  val v = BSONDocument(List(("$in", BSONArray(items))))
	  BSONDocument(List((n, v))) 
   }
 }

 def nin[T: c.WeakTypeTag, A: c.WeakTypeTag, C: c.WeakTypeTag](c: Context)(p : c.Expr[T => A], values: c.Expr[Traversable[A]])
 (queryWriter: c.Expr[BSONQueryWriter[C, A, _ <: BSONValue]]): c.Expr[BSONDocument] = {
   import c.universe._
   
   reify {
     val n = path(c)(p).splice
     val writer = queryWriter.splice.write
	   val items = values.splice.map(writer.write(_))
	   val v = BSONDocument(List(("$in", BSONArray(items))))
	   BSONDocument(List((n, v)))
   }
 }
   
   
 def sortAsc[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => A]): c.Expr[BSONDocument] = {
   import c.universe._
   c.universe.reify {
     BSONDocument(path(c)(p).splice -> 1)
   }
 }
 
 def sortDesc[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => A]): c.Expr[BSONDocument] = {
   import c.universe._
   c.universe.reify {
     BSONDocument(path(c)(p).splice -> -1)
   }
 }
   
def exists[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => Option[A]], exists: c.Expr[Boolean]): c.Expr[BSONDocument] = {
   import c.universe._
   
   reify {
     val param = path(c)(p).splice
     BSONDocument(param -> BSONDocument("$exists" -> BSONBoolean(exists.splice)))
   }
 }
   
 def set[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => A], value: c.Expr[A])
 (handler: c.Expr[BSONWriter[A, _ <: BSONValue]]): c.Expr[SetOperator] = {
    import c.universe._
    
    reify {
     val param = path(c)(p).splice
     SetOperator(param, handler.splice.write(value.splice)) 
    } 
  }
 
 def setOpt[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => Option[A]], value: c.Expr[Option[A]])
 (handler: c.Expr[BSONWriter[A, _ <: BSONValue]]): c.Expr[UpdateOperator] = {
   import c.universe._
   
   reify {
     val param = path(c)(p).splice
     val opt = value.splice
     opt.map(p => SetOperator(param, handler.splice.write(p))).getOrElse(UnsetOperator(param))
   }
  }
 
 
 def unset[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => A]): c.Expr[UnsetOperator] = {
    import c.universe._
    
    reify {
      val param = path(c)(p).splice
      UnsetOperator(param)
    } 
  }
 
 def inc[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => A], value: c.Expr[A])
 (handler: c.Expr[BSONWriter[A, _ <: BSONValue]]): c.Expr[IncOperator] = {
    import c.universe._
    
    reify {
      val param = path(c)(p).splice
      IncOperator(param, handler.splice.write(value.splice))
    }
  }
 
 def mul[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => A], value: c.Expr[A])
 (handler: c.Expr[BSONWriter[A, _ <: BSONValue]]): c.Expr[MulOperator] = {
    import c.universe._
    
    reify {
      val param = path(c)(p).splice
      MulOperator(param, handler.splice.write(value.splice))
    }
  }

 def min[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => A], value: c.Expr[A])
 (handler: c.Expr[BSONWriter[A, _ <: BSONValue]]): c.Expr[MinOperator] = {
    import c.universe._
    
    reify {
      val param = path(c)(p).splice
      MinOperator(param, handler.splice.write(value.splice))
    }
  }
 
 def max[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => A], value: c.Expr[A])
 (handler: c.Expr[BSONWriter[A, _ <: BSONValue]]): c.Expr[MaxOperator] = {
    import c.universe._
    
    reify {
      val param = path(c)(p).splice
      MaxOperator(param, handler.splice.write(value.splice))
    } 
  }
 
  def addToSet[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => Traversable[A]], values: c.Expr[Traversable[A]])
 (handler: c.Expr[BSONWriter[A, _ <: BSONValue]]): c.Expr[AddToSetOperator] = {
    import c.universe._
    
    reify {
      var param = path(c)(p).splice
  	  val items = values.splice.map(handler.splice.write(_))
  	  items match {
  	       case head :: Nil => AddToSetOperator(param, head)
  	       case _ => AddToSetOperator(param, BSONDocument("$each" -> BSONArray(items)))
  	  } 
    } 
  }
  
  def pullAll[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => Traversable[A]], value: c.Expr[Traversable[A]])
 (handler: c.Expr[BSONWriter[A, _ <: BSONValue]]): c.Expr[PullAllOperator] = {
    import c.universe._
    
    reify {
      val param = path(c)(p).splice
      val items = value.splice.map(handler.splice.write(_))
      PullAllOperator(param, BSONArray(items))
    } 
  }
 
  def push[T: c.WeakTypeTag, A: c.WeakTypeTag](c: Context)(p : c.Expr[T => Traversable[A]], values: c.Expr[Traversable[A]])
 (handler: c.Expr[BSONWriter[A, _ <: BSONValue]]): c.Expr[PushOperator] = {
    import c.universe._
    
    reify {
      val param = path(c)(p).splice
  	  val items = values.splice.map(handler.splice.write(_))
  	  items match {
  	       case head :: Nil => PushOperator(param, head)
  	       case _ => PushOperator(param, BSONDocument("$each" -> BSONArray(items)))
  	     }
    } 
  }
 
}
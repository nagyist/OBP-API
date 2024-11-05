package code.bankconnectors.generator

import code.bankconnectors.generator.ConnectorBuilderUtil._

import scala.reflect.runtime.{universe => ru}

object CommonsCaseClassGenerator extends App {
  
  val returnModels: Iterable[ru.Type] = connectorDeclsMethodsReturnOBPRequiredType
    .map(it => it.asMethod.returnType)
    .filter(it => {
      val symbol = it.typeSymbol
      val isAbstract = symbol.isAbstract
      isAbstract //Depends here, maybe no need this guard for some commons classes.
    })
    .toSet
  
  returnModels.map(_.typeSymbol.fullName).foreach(it => println(s"import $it"))

  def mkClass(tp: ru.Type) = {
    val varibles = tp.decls.map(it => s"${it.name} :${it.typeSignature.typeSymbol.name}").mkString(", \n    ")

      s"""
         |case class ${tp.typeSymbol.name}Commons(
         |    $varibles) extends ${tp.typeSymbol.name}
         |object ${tp.typeSymbol.name}Commons extends Converter[${tp.typeSymbol.name}, ${tp.typeSymbol.name}Commons]    
       """.stripMargin
  }
 // private val str: String = ru.typeOf[Bank].decls.map(it => s"${it.name} :${it.typeSignature.typeSymbol.name}").mkString(", \n")
  returnModels.map(mkClass).foreach(println)
  println()

}

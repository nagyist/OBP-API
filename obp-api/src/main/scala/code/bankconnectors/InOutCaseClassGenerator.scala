package code.bankconnectors

import code.api.util.{APIUtil, NewStyle}
import org.apache.commons.io.FileUtils

import java.io.File
import java.util.Date
import scala.concurrent.Future
import scala.reflect.runtime.{universe => ru}

object InOutCaseClassGenerator extends App {

  def extractReturnModel(tp: ru.Type): ru.Type = {
    if (tp.typeArgs.isEmpty) {
      tp
    } else {
      extractReturnModel(tp.typeArgs(0))
    }
  }
  
  private val mirror: ru.Mirror = ru.runtimeMirror(this.getClass.getClassLoader)
  private val clazz: ru.ClassSymbol = mirror.typeOf[Connector].typeSymbol.asClass
  private val connectorDecls= mirror.typeOf[Connector].decls
  private val connectorDeclsMethods= connectorDecls.filter(symbol => {
      val isMethod = symbol.isMethod && !symbol.asMethod.isVal && !symbol.asMethod.isVar && !symbol.asMethod.isConstructor && !symbol.isProtected
      isMethod})
  private val connectorDeclsMethodsReturnOBPRequiredType = connectorDeclsMethods
    .map(it => it.asMethod)
    .filter(it => {
      extractReturnModel(it.returnType).typeSymbol.fullName.matches("((code\\.|com.openbankproject\\.).+)|(scala\\.Boolean)") //to make sure, it returned the OBP class and Boolean.
    })

  val code = connectorDeclsMethodsReturnOBPRequiredType.map(it => {
    val returnType = it.returnType
    val tp = extractReturnModel(returnType)
    val isCaseClass = tp.typeSymbol.asClass.isCaseClass
    var payload = returnType.toString
      .replaceAll("([\\w\\.]+\\.)", "")
      .replaceFirst("OBPReturnType\\[Box\\[(.*)\\]\\]$", "$1")
      .replaceFirst("Future\\[Box\\[\\((.*), Option\\[CallContext\\]\\)\\]\\]$", "$1")
    if(!isCaseClass) {
      val name = tp.typeSymbol.name.toString
      println(name)
      payload = payload.replace(name, name+"Commons")
    }
    var parameters = it.asMethod.typeSignature.toString.replaceAll("([\\w\\.]+\\.)", "")
    if(parameters.startsWith("(callContext: Option[CallContext])")) {
      parameters = ""
    } else {
      parameters = parameters.replaceFirst("^\\(", ", ").replaceFirst(", callContext: Option.*$", "").replace(",", ",\n")
    }
    s"""
       |case class OutBound${it.name.toString.capitalize} (outboundAdapterCallContext: OutboundAdapterCallContext$parameters) extends TopicTrait
       |case class InBound${it.name.toString.capitalize} (inboundAdapterCallContext: InboundAdapterCallContext, status: Status, data: $payload) extends InBoundTrait[$payload]
     """.stripMargin
  })
  code.foreach(println)
  
  println("#################################Finished########################################################################")
  println("Please copy and compair the result to obp-commons/src/main/scala/com/openbankproject/commons/model/CommonModel.scala")

}

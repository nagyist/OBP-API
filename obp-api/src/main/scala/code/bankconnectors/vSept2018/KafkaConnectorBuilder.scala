package code.bankconnectors.vSept2018

import code.bankconnectors.generator.ConnectorBuilderUtil._

import scala.collection.immutable.List
import scala.language.postfixOps

object KafkaConnectorBuilder extends App {


  generateMethods(commonMethodNames,
    "src/main/scala/code/bankconnectors/vSept2018/KafkaMappedConnector_vSept2018.scala",
     "processRequest[InBound](req)", true)
}






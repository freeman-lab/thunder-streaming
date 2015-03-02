package org.project.thunder.streaming.outputs

import org.apache.spark.streaming.Time
import org.project.thunder.streaming.rdds.StreamingData

import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

/**
 * An AnalysisOutput class is responsible for handling the result (a collected DStream generated by a StreamingData
 * instance) of an Analysis (which wraps operations of StreamingData objects).
 *
 * An AnalysisOutput instance must always be completely defined by its class and its input parameters (don't do any
 * additional configuration outside of handleResult) unless the base serialization implementation is overridden. To make
 * things simpler, just do all initialization/configuration inside of handleResult.
 *
 * @tparam T the type of Analysis result (a List of (K, List[V]) objects) that this output can handle.
 */
abstract class Output[T <: List[_]](val params: Map[String, String]) extends Serializable {
  def handleResult(data: T, time: Time): Unit
}

object Output {
  /*

  An AnalysisOutput is specified with the following schema. Each <output> tag must only have one <type> tag, and
  may have zero or more <param> tags.

  Schema:
  <output>
    <name>{type}</name>
    <param name={name_1} value={value_1} />
    ...
    <param name={name_n} value={value_n} />
  </output>;
  */

  class BadOutputConfigException(msg: String) extends RuntimeException(msg)

  /** Extracts one or more AnalysisOutputs from the XML subtree under an <analysis> element */
  def instantiateFromConf(nodes: NodeSeq): List[Try[Output[List[_]]]] = {
    // Try to find a class with the given type name
    def extractAndFindClass(nodes: NodeSeq): Try[Class[_ <: Output[List[_]]]] = {
      val nameNodes = nodes \ "name"
      if (nameNodes.length != 1) {
        Failure(new BadOutputConfigException("The AnalysisOutput name was not correctly specified in a single <name> element"))
      } else {
        nameNodes(0) match {
          case <name>{ name @ _* }</name> => Success(Class.forName(name(0).text)
            .asSubclass(classOf[Output[List[_]]]))
          case _ => Failure(new BadOutputConfigException("The AnalysisOutput name was not correctly specified"))
        }
      }
    }
    // Extract all parameters necessary to instantiate an instance of the above output type
    def extractParameters(nodes: NodeSeq): Try[Map[String,String]] =  {
      val paramNodes = nodes \ "param"
      val paramList = ((paramNodes \\ "@name").map(_.text)).zip((paramNodes \\ "@value").map(_.text)).toList
      Success(Map(paramList: _*))
    }
    // Attempt to invoke the (maybe) AnalysisOutput class' constructor with paramMap as an argument
    // Not using for..yield because I want Failures to propagate out of this method
    val maybeOutputs = (nodes \ "output") map { node =>
      extractAndFindClass(node) match {
        case Success(clazz) => {
          extractParameters(node) match {
            case Success(parameters) => Try(instantiateAnalysisOutput(clazz)(parameters))
            case Failure(f) => Failure(f)
          }
        }
        case Failure(f) => Failure(f)
      }
    }
    // Convert it to a List for ease of use
    maybeOutputs.toList
  }

  /**
   *
   * @param clazz
   * @param args
   * @tparam T
   * @return
   */
  def instantiateAnalysisOutput[T <: Output[List[_]]](clazz: java.lang.Class[T])(args:AnyRef*): T = {
    val constructor = clazz.getConstructors()(0)
    return constructor.newInstance(args:_*).asInstanceOf[T]
  }
}

/*
/**
 * An AnalysisOutput can be mixed into an StreamingData object to give that class some specific output capability
 * (i.e. sending results to Lightning, or writing them to disk)
 */
trait AnalysisOutput {
  def handleResults(data: StreamingData, params: Map[String,String]): Unit
}

trait FileSystemOutput extends AnalysisOutput {
  override def handleResults(data: StreamingData, params: Map[String, String]) = {

    writeToPath(data, params("path"))
  }
  def writeToPath(data: StreamingData, path: String): Unit
}

trait LightingOutput extends AnalysisOutput {
  override def handleResults(data: StreamingData, params: Map[String, String]) = {
    sendToLightning(data, params("host"), params("port"))
  }
  def sendToLightning(data: )
}
*/
package skinny.test

import scalikejdbc._
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import skinny.util.JavaReflectAPI
import org.slf4j.LoggerFactory
import skinny.orm.ParameterBinderOps
import skinny.orm.feature.CRUDFeatureWithId

/**
 * Test data generator highly inspired by thoughtbot/factory_girl
 *
 * @see "https://github.com/thoughtbot/factory_girl"
 */
case class LightFactoryGirl[Id, Entity](mapper: CRUDFeatureWithId[Id, Entity], name: Symbol = null) {

  private[this] val logger = LoggerFactory.getLogger(classOf[LightFactoryGirl[Id, Entity]])

  private[this] val c = mapper.column

  val autoSession = AutoSession

  private[this] val valuesToReplaceVariablesInConfig = new scala.collection.concurrent.TrieMap[Symbol, Any]()
  private[this] val additionalNamedValues = new scala.collection.concurrent.TrieMap[Symbol, Any]()

  /**
   * Set named values to bind #{name} in "src/test/resources/factories.conf".
   *
   * @param namedValues named values
   * @return self
   */
  def withVariables(namedValues: (Symbol, Any)*): LightFactoryGirl[Id, Entity] = {
    namedValues.foreach { case (key, value) => valuesToReplaceVariablesInConfig.put(key, value) }
    this
  }

  /**
   * Returns the prefix of factory settings.
   *
   * @return prefix
   */
  def factoryName: Symbol = {
    val n = Option(name).map(_.name).getOrElse(JavaReflectAPI.classSimpleName(mapper))
    Symbol((n.head.toLower + n.tail).replaceFirst("\\$$", ""))
  }

  /**
   * Loads attributes from "src/test/resources/factories.conf".
   *
   * @return attributes in conf file
   */
  def loadedAttributes(): Map[SQLSyntax, Any] = {
    // TODO directory scan and work with factories/*.conf
    val config = ConfigFactory.load(getClass.getClassLoader, "factories.conf").getConfig(factoryName.name)
    config.root().unwrapped().asScala.map { case (k, v) => c.field(k) -> v.toString }.toMap
  }

  /**
   * Appends additional named values.
   * @param attributes attributes
   * @return self
   */
  def withAttributes(attributes: (Symbol, Any)*): LightFactoryGirl[Id, Entity] = {
    attributes.foreach { case (key, value) => additionalNamedValues.put(key, value) }
    this
  }

  /**
   * Creates a record with factories.conf & some replaced attributes.
   *
   * @param attributes attributes
   * @param s session
   * @return created entity
   */
  def create(attributes: (Symbol, Any)*)(implicit s: DBSession = autoSession): Entity = {
    val mergedAttributes = (additionalNamedValues ++ attributes).foldLeft(loadedAttributes()) {
      case (xs, (Symbol(key), value)) =>
        if (xs.exists(_._1 == mapper.column.field(key))) {
          xs.map {
            case (k, _) if k == mapper.column.field(key) => (k, value)
            case (k, v) => (k, v)
          }
        } else xs.updated(c.field(key), value)

    }.map(ParameterBinderOps.extractValueFromParameterBinder).map {
      case (key, value) => {
        // will replace only value which starts with #{ ... } because '#' might be used for test data in some case
        if (value.toString.startsWith("#")) {
          val variableKey = value.toString.trim.replaceAll("[#{}]", "")
          val replacedValue = valuesToReplaceVariablesInConfig.get(Symbol(variableKey)).orNull[Any]
          (key, replacedValue)
        } else {
          (key, value)
        }
      }
    }.toSeq

    val id = try mapper.createWithNamedValues(mergedAttributes: _*)
    catch {
      case e: Exception =>
        val message = s"Failed to create an entity because ${e.getMessage}"
        logger.error(message, e)
        throw new Exception(message, e)
    }
    try mapper.findById(id).get
    catch {
      // id might be a raw value because of type erasure
      case e: ClassCastException =>
        try mapper.findById(mapper.rawValueToId(id)).get
        catch {
          case e: Exception =>
            val message = s"Failed to find created entity because ${e.getMessage}"
            logger.error(message, e)
            throw new Exception(message, e)
        }
    }
  }

}

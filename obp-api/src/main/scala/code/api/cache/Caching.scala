package code.api.cache

import code.api.Constant._
import code.api.JedisMethod
import code.api.cache.Redis.use
import code.util.Helper.MdcLoggable

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.language.postfixOps
object Caching extends MdcLoggable {

  def memoizeSyncWithProvider[A](cacheKey: Option[String])(ttl: Duration)(f: => A)(implicit m: Manifest[A]): A = {
    (cacheKey, ttl) match {
      case (_, t) if t == Duration.Zero  => // Just forwarding a call
        f
      case (Some(_), _) => // Caching a call
        Redis.memoizeSyncWithRedis(cacheKey)(ttl)(f)
      case _  => // Just forwarding a call
        f
    }

  }

  def memoizeWithProvider[A](cacheKey: Option[String])(ttl: Duration)(f: => Future[A])(implicit m: Manifest[A]): Future[A] = {
    (cacheKey, ttl) match {
      case (_, t) if t == Duration.Zero  => // Just forwarding a call
        f
      case (Some(_), _) => // Caching a call
        Redis.memoizeWithRedis(cacheKey)(ttl)(f)
      case _  => // Just forwarding a call
        f
    }

  }

  def getLocalisedResourceDocCache(key: String) = {
    use(JedisMethod.GET, (LOCALISED_RESOURCE_DOC_PREFIX + key).intern(), Some(CREATE_LOCALISED_RESOURCE_DOC_JSON_TTL)) 
  }
    
  def setLocalisedResourceDocCache(key:String, value: String)=  {
    use(JedisMethod.SET, (LOCALISED_RESOURCE_DOC_PREFIX+key).intern(), Some(CREATE_LOCALISED_RESOURCE_DOC_JSON_TTL), Some(value))
  }

  def getDynamicResourceDocCache(key: String) = {
    use(JedisMethod.GET, (DYNAMIC_RESOURCE_DOC_CACHE_KEY_PREFIX + key).intern(), Some(GET_DYNAMIC_RESOURCE_DOCS_TTL))
  }
  
  def setDynamicResourceDocCache(key:String, value: String)= {
    use(JedisMethod.SET, (DYNAMIC_RESOURCE_DOC_CACHE_KEY_PREFIX+key).intern(), Some(GET_DYNAMIC_RESOURCE_DOCS_TTL), Some(value))
  }

  def getStaticResourceDocCache(key: String) = {
    use(JedisMethod.GET, (STATIC_RESOURCE_DOC_CACHE_KEY_PREFIX + key).intern(), Some(GET_STATIC_RESOURCE_DOCS_TTL))
  }
  
  def setStaticResourceDocCache(key:String, value: String)= {
    use(JedisMethod.SET, (STATIC_RESOURCE_DOC_CACHE_KEY_PREFIX+key).intern(), Some(GET_STATIC_RESOURCE_DOCS_TTL), Some(value))
  }

  def getAllResourceDocCache(key: String) = {
    use(JedisMethod.GET, (ALL_RESOURCE_DOC_CACHE_KEY_PREFIX + key).intern(), Some(GET_DYNAMIC_RESOURCE_DOCS_TTL))
  }
  
  def setAllResourceDocCache(key:String, value: String)= {
    use(JedisMethod.SET, (ALL_RESOURCE_DOC_CACHE_KEY_PREFIX+key).intern(), Some(GET_DYNAMIC_RESOURCE_DOCS_TTL), Some(value))
  }

  def getStaticSwaggerDocCache(key: String) = {
    use(JedisMethod.GET, (STATIC_SWAGGER_DOC_CACHE_KEY_PREFIX + key).intern(), Some(GET_STATIC_RESOURCE_DOCS_TTL))
  }
  
  def setStaticSwaggerDocCache(key:String, value: String)= {
    use(JedisMethod.SET, (STATIC_SWAGGER_DOC_CACHE_KEY_PREFIX+key).intern(), Some(GET_STATIC_RESOURCE_DOCS_TTL), Some(value))
  }
  
}

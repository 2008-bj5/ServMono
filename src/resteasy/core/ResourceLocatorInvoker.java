package resteasy.core;



import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.ws.rs.NotFoundException;

import resteasy.logging.Logger;
import resteasy.specimpl.BuiltResponse;
import resteasy.spi.ApplicationException;
import resteasy.spi.Failure;
import resteasy.spi.HttpRequest;
import resteasy.spi.HttpResponse;
import resteasy.spi.InjectorFactory;
import resteasy.spi.InternalServerErrorException;
import resteasy.spi.MethodInjector;
import resteasy.spi.Registry;
import resteasy.spi.ResourceFactory;
import resteasy.spi.ResteasyProviderFactory;
import resteasy.spi.ResteasyUriInfo;
import resteasy.spi.metadata.ResourceBuilder;
import resteasy.spi.metadata.ResourceClass;
import resteasy.spi.metadata.ResourceLocator;
import resteasy.util.FindAnnotation;
import resteasy.util.GetRestful;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("unchecked")
public class ResourceLocatorInvoker implements ResourceInvoker
{

   final static Logger logger = Logger.getLogger(ResourceLocatorInvoker.class);

   protected InjectorFactory injector;
   protected MethodInjector methodInjector;
   protected ResourceFactory resource;
   protected ResteasyProviderFactory providerFactory;
   protected ResourceLocator method;
   protected ConcurrentHashMap<Class, LocatorRegistry> cachedSubresources = new ConcurrentHashMap<Class, LocatorRegistry>();

   public ResourceLocatorInvoker(ResourceFactory resource, InjectorFactory injector, ResteasyProviderFactory providerFactory, ResourceLocator locator)
   {
      this.resource = resource;
      this.injector = injector;
      this.providerFactory = providerFactory;
      this.method = locator;
      this.methodInjector = injector.createMethodInjector(locator, providerFactory);
   }

   protected Object createResource(HttpRequest request, HttpResponse response)
   {
      Object resource = this.resource.createResource(request, response, providerFactory);
      return createResource(request, response, resource);

   }

   protected Object createResource(HttpRequest request, HttpResponse response, Object locator)
   {
      ResteasyUriInfo uriInfo = request.getUri();
      Object[] args = new Object[0];
      RuntimeException lastException = (RuntimeException)request.getAttribute(ResourceMethodRegistry.REGISTRY_MATCHING_EXCEPTION);
      try
      {
         args = methodInjector.injectArguments(request, response);
      }
      catch (NotFoundException failure)
      {
         if (lastException != null) throw lastException;
         throw failure;
      }
      try
      {
         uriInfo.pushCurrentResource(locator);
         Object subResource = method.getMethod().invoke(locator, args);
         return subResource;

      }
      catch (IllegalAccessException e)
      {
         throw new InternalServerErrorException(e);
      }
      catch (InvocationTargetException e)
      {
         throw new ApplicationException(e.getCause());
      }
   }

   public Method getMethod()
   {
      return method.getMethod();
   }

   public BuiltResponse invoke(HttpRequest request, HttpResponse response)
   {
      Object target = createResource(request, response);
      return invokeOnTargetObject(request, response, target);
   }

   public BuiltResponse invoke(HttpRequest request, HttpResponse response, Object locator)
   {
      Object target = createResource(request, response, locator);
      return invokeOnTargetObject(request, response, target);
   }

   protected BuiltResponse invokeOnTargetObject(HttpRequest request, HttpResponse response, Object target)
   {
      if (target == null)
      {
         NotFoundException notFound = new NotFoundException("Null subresource for path: " + request.getUri().getAbsolutePath());
         throw notFound;
      }
      Class<? extends Object> clazz = target.getClass();
      LocatorRegistry registry = cachedSubresources.get(clazz);
      if (registry == null)
      {
         if (!GetRestful.isSubResourceClass(clazz))
         {
            String msg = "Subresource for target class has no jax-rs annotations.: " + clazz.getName();
            throw new InternalServerErrorException(msg);
         }
         registry = new LocatorRegistry(clazz, providerFactory);
         cachedSubresources.putIfAbsent(clazz, registry);
      }
      ResourceInvoker invoker = registry.getResourceInvoker(request);
      if (invoker instanceof ResourceLocatorInvoker)
      {
         ResourceLocatorInvoker locator = (ResourceLocatorInvoker) invoker;
         return locator.invoke(request, response, target);
      }
      else
      {
         ResourceMethodInvoker method = (ResourceMethodInvoker) invoker;
         return method.invoke(request, response, target);
      }
   }
}

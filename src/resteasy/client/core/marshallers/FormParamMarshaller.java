package resteasy.client.core.marshallers;


import java.util.Collection;

import resteasy.client.ClientRequest;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FormParamMarshaller implements Marshaller
{
   private String paramName;

   public FormParamMarshaller(String paramName)
   {
      this.paramName = paramName;
   }

   public void build(ClientRequest request, Object object)
   {
      if (object == null) return;

      if (object instanceof Collection)
      {
         for (Object obj : (Collection) object)
         {
            request.formParameter(paramName, obj);
         }
      }
      else if (object.getClass().isArray())
      {
         if (object.getClass().getComponentType().isPrimitive())
         {
            Class componentType = object.getClass().getComponentType();
            if (componentType.equals(boolean.class))
            {
               for (Boolean bool : (boolean[]) object) request.formParameter(paramName, bool.toString());
            }
            else if (componentType.equals(byte.class))
            {
               for (Byte val : (byte[]) object) request.formParameter(paramName, val.toString());
            }
            else if (componentType.equals(short.class))
            {
               for (Short val : (short[]) object) request.formParameter(paramName, val.toString());
            }
            else if (componentType.equals(int.class))
            {
               for (Integer val : (int[]) object) request.formParameter(paramName, val.toString());
            }
            else if (componentType.equals(long.class))
            {
               for (Long val : (long[]) object) request.formParameter(paramName, val.toString());
            }
            else if (componentType.equals(float.class))
            {
               for (Float val : (float[]) object) request.formParameter(paramName, val.toString());
            }
            else if (componentType.equals(double.class))
            {
               for (Double val : (double[]) object) request.formParameter(paramName, val.toString());
            }
         }
         else
         {
            Object[] objs = (Object[]) object;
            for (Object obj : objs)
            {
               request.formParameter(paramName, obj);

            }
         }
      }
      else
      {
         request.formParameter(paramName, object);
      }
   }
}
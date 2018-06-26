package com.innopals.edge.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.val;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author bestmike007
 */
public final class JacksonTypeHelper {
  public static JavaType fromGenericType(Type type) {
    if (!ParameterizedType.class.isInstance(type)) {
      return TypeFactory.defaultInstance().constructType(type);
    }
    ParameterizedType parameterizedType = (ParameterizedType) type;
    val typeArgs = parameterizedType.getActualTypeArguments();
    val javaTypes = new JavaType[typeArgs.length];
    for (int i = 0; i < typeArgs.length; i++) {
      javaTypes[i] = fromGenericType(typeArgs[i]);
    }
    return TypeFactory.defaultInstance().constructParametricType(
      TypeFactory.rawClass(type),
      javaTypes
    );
  }

  public static JavaType fromMethodReturnType(Method method) {
    return fromGenericType(method.getGenericReturnType());
  }

}

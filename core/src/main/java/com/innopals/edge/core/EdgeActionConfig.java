package com.innopals.edge.core;

import com.innopals.edge.EdgeContext;
import com.innopals.edge.annotations.EdgeAction;
import com.innopals.edge.annotations.EdgeController;
import com.innopals.edge.annotations.EnableEdge;
import lombok.Getter;
import lombok.val;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

/**
 * @author bestmike007
 */
public class EdgeActionConfig {

  private final static WeakHashMap<Class<?>, EdgeController> CONTROLLER_CONFIGS = new WeakHashMap<>();
  private final static WeakHashMap<Method, EdgeAction> ACTION_CONFIGS = new WeakHashMap<>();

  private static EdgeController getControllerConfig(Class<?> controller) {
    if (CONTROLLER_CONFIGS.containsKey(controller)) {
      return CONTROLLER_CONFIGS.get(controller);
    }
    val config = controller.getAnnotation(EdgeController.class);
    CONTROLLER_CONFIGS.put(controller, config);
    return config;
  }

  private static EdgeAction getActionConfig(Method action) {
    if (ACTION_CONFIGS.containsKey(action)) {
      return ACTION_CONFIGS.get(action);
    }
    val config = action.getAnnotation(EdgeAction.class);
    ACTION_CONFIGS.put(action, config);
    return config;
  }

  @Getter
  private final MethodSignature methodSignature;
  @Getter
  private final EnableEdge applicationConfig;
  @Getter
  private final EdgeController controllerConfig;
  @Getter
  private final EdgeAction actionConfig;

  public EdgeActionConfig(EnableEdge applicationConfig, MethodSignature signature) {
    this.methodSignature = signature;
    this.applicationConfig = applicationConfig;
    this.controllerConfig = getControllerConfig(signature.getDeclaringType());
    this.actionConfig = getActionConfig(signature.getMethod());
  }

  public int getContextArgIndex() {
    val parameterTypes = methodSignature.getMethod().getParameterTypes();
    for(int i = 0; i< parameterTypes.length;i++) {
      if (parameterTypes[i].equals(EdgeContext.class)) {
        return i;
      }
    }
    return -1;
  }
}

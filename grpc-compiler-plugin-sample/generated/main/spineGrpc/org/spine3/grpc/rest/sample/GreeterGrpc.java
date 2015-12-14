package org.spine3.grpc.rest.sample;

import org.spine3.grpc.rest.AbstractRpcService;
import org.spine3.grpc.rest.RpcCallHandler;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;

import org.spine3.grpc.rest.sample.handlers.*;

@Generated("by Spine gRPC proto compiler")
public class GreeterGrpc extends AbstractRpcService {

  private static final Map<String, RpcCallHandler> handlers = new HashMap<>();

  @Override
  protected RpcCallHandler getRpcCallHandler(String method) {
    final RpcCallHandler rpcCallHandler = handlers.get(method);
    if (rpcCallHandler == null) {
      throw new IllegalStateException("No handler registered for method: " + method);
    }
    return rpcCallHandler;
  }

  public static void registerSayHelloHandler(AbstractSayHelloHandler handler) {
    handlers.put("SayHello", handler);
  }

  public static void registerSayByeHandler(AbstractSayByeHandler handler) {
    handlers.put("SayBye", handler);
  }

}

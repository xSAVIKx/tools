package org.spine3.grpc.rest.sample;

import org.spine3.grpc.rest.sample.handlers.SayHelloHandler;

public class ConfiguredGreeterGrpc extends GreeterGrpc {
    static {
        registerSayHelloHandler(new SayHelloHandler());
    }
}

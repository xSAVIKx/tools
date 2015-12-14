package org.spine3.grpc.rest.sample.handlers;

import org.spine3.grpc.rest.sample.ServiceRequest;
import org.spine3.grpc.rest.sample.ServiceResponse;

public class SayHelloHandler extends AbstractSayHelloHandler {
    @Override
    public ServiceResponse handle(ServiceRequest serviceRequest) {
        return ServiceResponse.newBuilder().setMessage("Hello, " + serviceRequest.getName() + "!").build();
    }

    // TODO:2015-12-14:mikhail.mikhaylov: Generate this.
    @Override
    public Class<ServiceRequest> getParameterClass() {
        return ServiceRequest.class;
    }
}

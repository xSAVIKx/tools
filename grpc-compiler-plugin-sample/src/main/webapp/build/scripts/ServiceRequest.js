define(['protobuf'], function (ProtoBuf) {

  var ServiceRequest = ProtoBuf.loadProtoFile("/build/res/grpc/rest/sample/service.proto").build("spine3.grpc.rest.sample.ServiceRequest");

  return ServiceRequest;
});

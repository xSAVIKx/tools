define(['protobuf'], function (ProtoBuf) {

  var ServiceResponse = ProtoBuf.loadProtoFile("/build/res/grpc/rest/sample/service.proto").build("spine3.grpc.rest.sample.ServiceResponse");

  return ServiceResponse;
});

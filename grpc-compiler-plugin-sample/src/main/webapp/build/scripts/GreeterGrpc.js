define(['protobuf', 'constants', 'serviceRequest', 'serviceResponse'], function(protobuf, constants, serviceRequest, serviceResponse) {
  var GreeterGrpc = function() {};

  GreeterGrpc.prototype.SayHello = function(requestArgument) {

    return new Promise(function (resolve, reject) {
      if (!requestArgument instanceof serviceRequest) {
        reject(new Error("Invalid argument."));
      } else {
        var value = requestArgument.toBase64();

        $.ajax({
          type: 'POST',
          url: Constants.GreeterGrpcPath,
          data: 'rpc_method_type=SayHello&rpc_method_argument=' + value
        }).done(function (data) {
          var convertedResult = serviceResponse.decode(data);
          resolve(convertedResult);
        }).fail(function (error) {
          reject(error);
        });
      }
    });
  };

  GreeterGrpc.prototype.SayBye = function(requestArgument) {

    return new Promise(function (resolve, reject) {
      if (!requestArgument instanceof serviceRequest) {
        reject(new Error("Invalid argument."));
      } else {
        var value = requestArgument.toBase64();

        $.ajax({
          type: 'POST',
          url: Constants.GreeterGrpcPath,
          data: 'rpc_method_type=SayBye&rpc_method_argument=' + value
        }).done(function (data) {
          var convertedResult = serviceResponse.decode(data);
          resolve(convertedResult);
        }).fail(function (error) {
          reject(error);
        });
      }
    });
  };

  return GreeterGrpc;
});

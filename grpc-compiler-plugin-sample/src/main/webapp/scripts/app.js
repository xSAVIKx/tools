requirejs.config({
    baseUrl: 'scripts',
    paths: {
        long: 'long',
        bytebuffer: 'bytebuffer',
        jquery: 'jquery-2.1.4',
        protobuf: 'protobuf',
        constants: 'Constants',
        serviceRequest: '../build/scripts/ServiceRequest',
        serviceResponse: '../build/scripts/ServiceResponse',
        greeterService: '../build/scripts/GreeterGrpc'
    }
});

requirejs([
    'jquery',
    'long',
    'bytebuffer',
    'protobuf',
    'constants',
    'serviceRequest',
    'serviceResponse',
    'greeterService'], function ($, long, byteBuffer, protoBuf, constants,
                                 serviceRequest, serviceResponse, greeterService) {
    console.info('Module loaded');

    $("#testBtn").bind('click', function (e) {
        console.log('Testing Spine RPC calls...');

        var service = new greeterService();

        var request = new serviceRequest('testName');
        var promise = service.SayHello(request);

        promise.then(function (result) {
            console.log("RPC call was successfull, {}", result);
        }, function (reason) {
            console.log('RPC call failed {}', reason);
        });

        e.stopPropagation();
        e.preventDefault();

        return false;
    });
});



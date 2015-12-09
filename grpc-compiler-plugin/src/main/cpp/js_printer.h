#ifndef PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H
#define PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H

#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/descriptor.h>

#include <string>

using namespace std;

namespace js_printer {
    // Generates JS part for service
    void GenerateJs(const google::protobuf::ServiceDescriptor *service,
                    string js_path,
                    google::protobuf::compiler::GeneratorContext *context);
}


#endif //PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H
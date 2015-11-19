#ifndef PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H
#define PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H

#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/descriptor.h>

#include <string>

using namespace std;

namespace handler_printer {
    // Writes the generated method interface into the given ZeroCopyOutputStream
    void GenerateHandler(const google::protobuf::MethodDescriptor *method,
                         google::protobuf::io::ZeroCopyOutputStream *out,
                         bool generate_nano, string package_name);

    string HandlerClassName(const google::protobuf::MethodDescriptor *method);
}


#endif //PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H

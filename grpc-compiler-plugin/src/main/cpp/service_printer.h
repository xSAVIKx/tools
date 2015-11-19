#ifndef PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H
#define PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H

#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/descriptor.h>

#include <string>

using namespace std;

namespace service_printer {
    // Writes the generated service interface into the given ZeroCopyOutputStream
    void GenerateService(const google::protobuf::ServiceDescriptor *service,
                         google::protobuf::io::ZeroCopyOutputStream *out,
                         bool generate_nano, string class_name,
                         compiler::GeneratorContext *context);

    string ServiceJavaPackage(const google::protobuf::FileDescriptor *file, bool nano);

    string ServiceClassName(const google::protobuf::ServiceDescriptor *service);
}


#endif //PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H

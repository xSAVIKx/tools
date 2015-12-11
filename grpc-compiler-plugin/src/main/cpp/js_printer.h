#ifndef PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H
#define PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H

#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/descriptor.h>

#include <string>

using namespace std;

namespace js_printer {
    // Generates JS part for service
    void GenerateJsService(const google::protobuf::ServiceDescriptor *service,
                           string webapp_path,
                           google::protobuf::compiler::GeneratorContext *context);

    void GenerateJsMessage(const google::protobuf::Descriptor *descriptor,
                           string webapp_path, string proto_file_name,
                           google::protobuf::compiler::GeneratorContext *context);

    void GenerateConstantsSample(string webapp_path, set<string> service_names,
                                 google::protobuf::compiler::GeneratorContext *context);

    void CopyProtoFile(const google::protobuf::FileDescriptor *descriptor, string webapp_path, string file_name,
                       google::protobuf::compiler::GeneratorContext *context);
}


#endif //PROTOCOMPILERPLUGIN_SERVICE_PRINTER_H
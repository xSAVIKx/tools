#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/descriptor.pb.h>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/compiler/code_generator.h>

#include "js_printer.h"
#include "service_printer.h"

using namespace std;
using namespace google::protobuf;
using namespace google::protobuf::io;

namespace js_printer {

    void PrintImports(Printer *p, set<string> imports) {
        p->Print("define([\'protobuf\'");
        for (set<string>::const_iterator iter = imports.begin(); iter != imports.end(); ++iter) {
            string import = *iter;
            p->Print(", \'$import$\'", "import", import);
        }
    }

    void GenerateJs(const google::protobuf::ServiceDescriptor *service, string js_path,
                    google::protobuf::compiler::GeneratorContext *context) {
        const FileDescriptor *f = service->file();

        map<string, string> args;

        const string &service_name = service_printer::ServiceClassName(service);
        string service_filename = js_path + service_name;

        std::unique_ptr<google::protobuf::io::ZeroCopyOutputStream> output(
                context->Open(service_filename));

        int method_count = service->method_count();
        set<string> imports;

        for (int i = 0; i < method_count; ++i) {
            const MethodDescriptor *method_descriptor = service->method(i);
            const Descriptor *input_type_descriptor = method_descriptor->input_type();
            const Descriptor *output_type_descriptor = method_descriptor->output_type();
            string input_type_name = input_type_descriptor->name();
            string output_type_name = output_type_descriptor->name();
            input_type_name[0] = tolower(input_type_name[0]);
            output_type_name[0] = tolower(output_type_name[0]);

            imports.insert(input_type_name);
            imports.insert(output_type_name);
        }

        Printer p(output.get(), '$');

        PrintImports(&p, imports);
//        PrintConstructor();
//        PrintFunctions();
//        PrintEnding();
    }
}
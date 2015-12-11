#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/descriptor.pb.h>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/compiler/code_generator.h>

#include <iostream>
#include <stdio.h>
#include <fstream>

#include "js_printer.h"
#include "service_printer.h"

using namespace std;
using namespace google::protobuf;
using namespace google::protobuf::io;

namespace js_printer {

    void PrintImports(Printer *p, set<string> imports) {
        p->Print("define([\'protobuf\', \'constants\'");
        for (set<string>::const_iterator iter = imports.begin(); iter != imports.end(); ++iter) {
            string import = *iter;
            p->Print(", \'$import$\'", "import", import);
        }
        p->Print("], function(protobuf, constants");
        for (set<string>::const_iterator iter = imports.begin(); iter != imports.end(); ++iter) {
            string import = *iter;
            p->Print(", $import$", "import", import);
        }
        p->Print(") {\n");
        p->Indent();

    }

    void PrintConstructor(Printer *p, string service_name) {
        p->Print("var $service_name$ = function() {};\n\n", "service_name", service_name);
    }

    void PrintMethods(Printer *p, string service_name, const ServiceDescriptor *service) {
        int method_count = service->method_count();
        for (int i = 0; i < method_count; ++i) {
            p->Print("$service_name$.prototype.", "service_name", service_name);
            const MethodDescriptor *method = service->method(i);
            string method_name = method->name();
            string arg_type = method->input_type()->name();
            arg_type[0] = (char) tolower(arg_type[0]);
            p->Print("$method_name$ = function(requestArgument) {\n\n", "method_name", method_name);
            p->Indent();
            p->Print("return new Promise(function (resolve, reject) {\n");
            p->Indent();
            p->Print("if (!requestArgument instanceof $arg_type$) {\n", "arg_type", arg_type);
            p->Indent();
            p->Print("reject(new Error(\"Invalid argument.\"));\n");
            p->Outdent();
            p->Print("} else {\n");
            p->Indent();
            p->Print("var value = requestArgument.toBase64();\n\n");
            p->Print("$$.ajax({\n");
            p->Indent();
            p->Print("type: \'POST\',\n");
            p->Print("url: Constants.$service_name$Path,\n", "service_name", service_name);
            p->Print("data: \'rpc_method_type=$method_name$&rpc_method_argument=\' + value\n",
                     "method_name", method_name);
            p->Outdent();
            p->Print("}).done(function (data) {\n");
            p->Indent();
            p->Print("var convertedResult = serviceResponse.decode(data);\n"
                             "resolve(convertedResult);\n");
            p->Outdent();
            p->Print("}).fail(function (error) {\n");
            p->Indent();
            p->Print("reject(error);\n");
            p->Outdent();
            p->Print("});\n");
            p->Outdent();
            p->Print("}\n");
            p->Outdent();
            p->Print("});\n");
            p->Outdent();
            p->Print("};\n\n");
        }
    }

    void PrintEnding(Printer *p, string service_name) {
        p->Print("return $service_name$;\n", "service_name", service_name);
        p->Outdent();
        p->Print("});\n");
    }

    void GenerateJsMessage(const google::protobuf::Descriptor *descriptor,
                           string webapp_path, string proto_file_name,
                           google::protobuf::compiler::GeneratorContext *context) {
        const string message_name = descriptor->name();
        const string file_name = "../../../" + webapp_path + "build/scripts/" + message_name + ".js";

        std::unique_ptr<google::protobuf::io::ZeroCopyOutputStream> output(
                context->Open(file_name));

        string proto_full_name = descriptor->full_name();

        Printer p(output.get(), '$');

        p.Print("define(['protobuf'], function (ProtoBuf) {\n\n");
        p.Indent();
        p.Print("var $message_name$ = ProtoBuf.loadProtoFile(\"/build/res/", "message_name", message_name);
        p.Print("$proto_file$\").build(", "proto_file", proto_file_name);
        p.Print("\"$proto_full_name$\");\n\n", "proto_full_name", proto_full_name);
        p.Print("return $message_name$;\n", "message_name", message_name);
        p.Outdent();
        p.Print("});\n");
    }

    void GenerateJsService(const google::protobuf::ServiceDescriptor *service, string webapp_path,
                           google::protobuf::compiler::GeneratorContext *context) {
        const string &service_name = service_printer::ServiceClassName(service);
        string service_filename = "../../../" + webapp_path + "build/scripts/" + service_name + ".js";

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
            input_type_name[0] = (char) tolower(input_type_name[0]);
            output_type_name[0] = (char) tolower(output_type_name[0]);

            imports.insert(input_type_name);
            imports.insert(output_type_name);
        }

        Printer p(output.get(), '$');

        PrintImports(&p, imports);
        PrintConstructor(&p, service_name);
        PrintMethods(&p, service_name, service);
        PrintEnding(&p, service_name);
    }

    void GenerateConstantsSample(string webapp_path, set<string> service_names,
                                 google::protobuf::compiler::GeneratorContext *context) {
        const string file_name = "../../../" + webapp_path + "build/scripts/Constants.js.sample";

        std::unique_ptr<google::protobuf::io::ZeroCopyOutputStream> output(
                context->Open(file_name));

        Printer p(output.get(), '$');

        p.Print("var Constants = {\n");
        p.Indent();
        for (set<string>::const_iterator iter = service_names.begin(); iter != service_names.end(); ++iter) {
            string service = *iter;
            p.Print("'$service$Path': '',\n", "service", service);
        }
        p.Outdent();
        p.Print("};\n");
    }

    void CopyProtoFile(const google::protobuf::FileDescriptor *descriptor, string webapp_path, string file_name,
                       google::protobuf::compiler::GeneratorContext *context) {
        string proto_path_prefix = "src/main/proto/";

        string destination = "../../../" + webapp_path + "build/res/" + file_name;
        string descriptor_path = proto_path_prefix + descriptor->name();

        std::unique_ptr<google::protobuf::io::ZeroCopyOutputStream> output(
                context->Open(destination));

        Printer p(output.get(), '$');
        FILE *src = fopen(descriptor_path.c_str(), "rb");
        char buf[32];
        while (fread(buf, 1, 31, src)) {
            p.PrintRaw(buf);
            std::fill_n(buf, 32, 0);
        }

        fclose(src);
    }
}
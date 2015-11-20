#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/descriptor.pb.h>
#include <google/protobuf/compiler/java/java_names.h>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/compiler/code_generator.h>
#include <algorithm>

#include "handler_printer.h"
#include "service_printer.h"

using namespace std;
using namespace google::protobuf;
using namespace google::protobuf::io;

namespace service_printer {

    string ServiceJavaPackage(const FileDescriptor *file, bool nano) {
        string result = compiler::java::ClassName(file);
        size_t last_dot_pos = result.find_last_of('.');
        if (last_dot_pos != string::npos) {
            result.resize(last_dot_pos);
        } else {
            result = "";
        }
        if (nano && !file->options().javanano_use_deprecated_package()) {
            if (!result.empty()) {
                result += ".";
            }
            result += "nano";
        }
        return result;
    }

    void PrintPackage(Printer *p, const FileDescriptor *f, bool generate_nano, map<string, string> args) {
        string package_name = args["package_name"];
        if (!package_name.empty()) {
            p->Print(
                    "package $package_name$;\n\n",
                    "package_name", package_name);
        }
    }

    void PrintImports(Printer *p, string package_name, string class_name, bool generate_nano) {
        p->Print(
                "import org.spine.grpc.rest.AbstractRpcService;\n"
                        "import org.spine.grpc.rest.RpcCallHandler;\n"
                        "\n"
                        "import java.util.HashMap;\n"
                        "import java.util.Map;\n"
                        "import javax.annotation.Generated;\n"
                        "\n"
        );

        p->Print(
                "import $package_name$.handlers.*;"
                        "\n\n", "package_name", package_name
        );

        //todo:2015-11-13:mikhail.mikhaylov: Review this later.
        if (generate_nano) {
            p->Print("import java.io.IOException;\n\n");
        }
    }

    void PrintClassName(Printer *p, map<string, string> args) {
        p->Print(
                args,
                "@Generated(\"by Spine gRPC proto compiler\")\n"
                        "public class $class_name$ extends AbstractRpcService {\n\n");
        p->Indent();
    }

    void PrintHandlersMap(Printer *p, map<string, string> args, const ServiceDescriptor *service) {
        p->Print(
                args,
                "private final Map<String, RpcCallHandler> handlers = new HashMap<>();\n"
                        "\n"
        );
    }

    void PrintGetHandler(Printer *p) {
        p->Print(
                "@Override\n"
                        "protected RpcCallHandler getRpcCallHandler(String method) {\n"
        );
        p->Indent();
        p->Print("final RpcCallHandler rpcCallHandler = handlers.get(method);\n"
            "if (rpcCallHandler == null) {\n");
        p->Indent();
        p->Print("throw new IllegalStateException(\"No handler registered for method: \" + method);\n");
        p->Outdent();
        p->Print("}\n");
        p->Print("return rpcCallHandler;\n");
        p->Outdent();
        p->Print("}\n\n");
    }

    void PrintClassEnd(Printer *p) {
        p->Outdent();
        p->Print("}\n");
    }

    void PrintRequiredHandlersArray(Printer *p, const ServiceDescriptor *service) {
        p->Print("private static final String[] requiredMethodHandlers = {\n");
        p->Indent();
        int method_count = service->method_count();
        for (int i = 0; i < method_count; ++i) {
            const MethodDescriptor *methodDescriptor = service->method(i);
            string handlerMethod = methodDescriptor->name();
            p->Print("\"$handlerMethod$\"", "handlerMethod", handlerMethod);
            if (i < method_count - 1) {
                p->Print(",");
            }
            p->Print("\n");
        }
        p->Outdent();
        p->Print("};\n\n");
    }

    void PrintHandlerFiles(const ServiceDescriptor *service,
                           bool generate_nano,
                           map<string, string> args,
                           compiler::GeneratorContext *context) {
        int method_count = service->method_count();
        for (int i = 0; i < method_count; ++i) {
            const MethodDescriptor *method = service->method(i);
            const string &class_name = handler_printer::HandlerClassName(method);
            string folder = args["package_name"] + ".handlers.";
            std::replace(folder.begin(), folder.end(), '.', '/');
            string filename = folder + class_name + ".java";

            std::unique_ptr<google::protobuf::io::ZeroCopyOutputStream> output(
                    context->Open(filename));

            handler_printer::GenerateHandler(method, output.get(), generate_nano, args["package_name"]);
        }
    }

    void PrintRegisterers(Printer *p, string class_name, const ServiceDescriptor *service) {
        int method_count = service->method_count();
        for (int i = 0; i < method_count; ++i) {
            const MethodDescriptor *methodDescriptor = service->method(i);
            string handlerClassName = handler_printer::HandlerClassName(methodDescriptor);
            p->Print("public void register$handler$", "handler", methodDescriptor->name() + "Handler");
            p->Print("($handler$ handler) {\n", "handler", handlerClassName);
            p->Indent();
            p->Print("handlers.put(\"$method$\", handler);\n", "method", methodDescriptor->name());
            p->Outdent();
            p->Print("}\n\n");
        }

    }

    void GenerateService(const ServiceDescriptor *service,
                         ZeroCopyOutputStream *out,
                         bool generate_nano, string class_name,
                         compiler::GeneratorContext *context) {

        const FileDescriptor *f = service->file();

        map<string, string> args;
        args["class_name"] = class_name;
        args["package_name"] = ServiceJavaPackage(f, generate_nano);

        Printer p(out, '$');

        PrintPackage(&p, f, generate_nano, args);
        PrintImports(&p, args["package_name"], class_name, generate_nano);

        PrintClassName(&p, args);
        PrintRequiredHandlersArray(&p, service);
        PrintHandlersMap(&p, args, service);
        PrintGetHandler(&p);
        PrintRegisterers(&p, class_name, service);
        PrintClassEnd(&p);

        PrintHandlerFiles(service, generate_nano, args, context);
    }

    string ServiceClassName(const ServiceDescriptor *service) {
        return service->name() + "Grpc";
    }
}
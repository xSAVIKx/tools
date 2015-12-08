#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/descriptor.pb.h>
#include <google/protobuf/compiler/java/java_names.h>
#include <google/protobuf/io/printer.h>

#include <google/protobuf/compiler/java/java_name_resolver.h>

#include "handler_printer.h"

using namespace std;
using namespace google::protobuf;
using namespace google::protobuf::io;

namespace handler_printer {

    void PrintPackage(Printer *p, map<string, string> args) {
        string package_name = args["package_name"];
        if (!package_name.empty()) {
            package_name += ".";
        }
        package_name += "handlers";
        p->Print(
                "package $package_name$;\n\n",
                "package_name", package_name);

    }

    void PrintImports(Printer *p, bool generate_nano, const MethodDescriptor *method) {
        google::protobuf::compiler::java::ClassNameResolver *classNameResolver =
                new google::protobuf::compiler::java::ClassNameResolver();

        p->Print(
                "import org.spine3.grpc.rest.RpcCallHandler;\n"
                        "\n"
        );

        const Descriptor *input_type_descriptor = method->input_type();
        const Descriptor *output_type_descriptor = method->output_type();

        const string &input_type = classNameResolver->GetClassName(input_type_descriptor, true);
        const string &output_type = classNameResolver->GetClassName(output_type_descriptor, true);

        p->Print("import $type$;\n", "type", input_type);
        if (input_type.compare(output_type) != 0) {
            p->Print("import $type$;\n", "type", output_type);
        }
        p->Print("\n");
        p->Print("import javax.annotation.Generated;\n\n");

        delete classNameResolver;
    }

    void PrintClassName(Printer *p, map<string, string> args) {
        p->Print(
                args,
                "@Generated(\"by Spine gRPC proto compiler\")\n"
                        "public abstract class $class_name$ implements RpcCallHandler"
                        "<$handler_argument_name$, $handler_result_name$> {\n"
                        "\n");
        p->Indent();
    }

    void PrintClassImplementation(Printer *p, map<string, string> args) {
        p->Print(
                args,
                "public Class<$handler_argument_name$> getParameterClass() {\n"
        );
        p->Indent();
        p->Print(
                args,
                "return $handler_argument_name$.class;\n"
        )
        p->Outdent();
        p->Print("}\n")
    }

    void PrintClassEnd(Printer *p) {
        p->Outdent();
        p->Print("}\n");
    }

    string HandlerClassName(const MethodDescriptor *method) {
        return "Abstract" + method->name() + "Handler";
    }

    void GenerateHandler(const MethodDescriptor *method,
                         ZeroCopyOutputStream *out,
                         bool generate_nano, string package_name) {

        string class_name = HandlerClassName(method);

        map<string, string> args;
        args["class_name"] = class_name;
        args["package_name"] = package_name;
        args["handler_argument_name"] = method->input_type()->name();
        args["handler_result_name"] = method->output_type()->name();

        Printer p(out, '$');

        PrintPackage(&p, args);
        PrintImports(&p, generate_nano, method);

        PrintClassName(&p, args);
        PrintClassEnd(&p);
    }
}
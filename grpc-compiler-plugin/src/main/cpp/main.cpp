#include <iostream>
#include <google/protobuf/compiler/code_generator.h>
#include <google/protobuf/compiler/plugin.h>
#include "codegen.h"
#include "service_printer.h"
#include "js_printer.h"
#include <google/protobuf/compiler/cpp/cpp_generator.h>
#include <memory>

static string JavaPackageToDir(const string &package_name) {
    string package_dir = package_name;
    for (size_t i = 0; i < package_dir.size(); ++i) {
        if (package_dir[i] == '.') {
            package_dir[i] = '/';
        }
    }
    if (!package_dir.empty()) package_dir += "/";
    return package_dir;
}

static string DefaultJsPath() {
    return "src/main/webapp/scripts/";
}

using namespace std;

namespace org {
    namespace spine {
        namespace plugins {

            Codegen::Codegen() { }

            Codegen::~Codegen() { }

            bool Codegen::Generate(const FileDescriptor *file, const string &parameter,
                                   compiler::GeneratorContext *context, string *error) const {
                vector<pair<string, string> > options;
                google::protobuf::compiler::ParseGeneratorParameter(parameter, &options);

                string js_path = DefaultJsPath();
                bool generate_nano = false;
                for (int i = 0; i < options.size(); i++) {
                    if (options[i].first == "nano" && options[i].second == "true") {
                        generate_nano = true;
                    } else if (options[i].first == "js_path") {
                        js_path = options[i].second;
                    }
                }

                //TODO:2015-12-09:mikhail.mikhaylov: end path with slash if it does not.

                string package_name = service_printer::ServiceJavaPackage(file, generate_nano);
                string package_filename = JavaPackageToDir(package_name);

                for (int i = 0; i < file->service_count(); ++i) {
                    const google::protobuf::ServiceDescriptor *service = file->service(i);

                    const string &class_name = service_printer::ServiceClassName(service);
                    string filename = package_filename
                                      + class_name + ".java";

                    std::unique_ptr<google::protobuf::io::ZeroCopyOutputStream> output(
                            context->Open(filename));

                    service_printer::GenerateService(service, output.get(), generate_nano, class_name, context);
                    js_printer::GenerateJs(service, js_path, context);
                }

                return true;
            }
        }
    }
}

int main(int argc, char *argv[]) {
    org::spine::plugins::Codegen generator;
    return PluginMain(argc, argv, &generator);
}
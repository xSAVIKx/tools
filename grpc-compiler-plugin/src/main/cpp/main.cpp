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

static string DefaultWebappPath() {
    return "src/main/webapp";
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
                set<string> service_names;

                string webapp_path = DefaultWebappPath();
                bool generate_nano = false;
                for (int i = 0; i < options.size(); i++) {
                    if (options[i].first == "nano" && options[i].second == "true") {
                        generate_nano = true;
                    } else if (options[i].first == "webapp_path") {
                        webapp_path = options[i].second;
                    }
                }

                if (webapp_path[webapp_path.size() - 1] != '/') {
                    webapp_path += '/';
                }
                //TODO:2015-12-09:mikhail.mikhaylov: end path with slash if it does not.

                string package_name = service_printer::ServiceJavaPackage(file, generate_nano);
                string package_filename = JavaPackageToDir(package_name);

                for (int i = 0; i < file->service_count(); ++i) {
                    const google::protobuf::ServiceDescriptor *service = file->service(i);

                    const string &class_name = service_printer::ServiceClassName(service);
                    service_names.insert(class_name);

                    string filename = package_filename
                                      + class_name + ".java";

                    std::unique_ptr<google::protobuf::io::ZeroCopyOutputStream> output(
                            context->Open(filename));

                    service_printer::GenerateService(service, output.get(), generate_nano, class_name, context);
                    js_printer::GenerateJsService(service, webapp_path, context);
                }

                string file_name = file->name();
                size_t last_path_delim_pos = file_name.find('/');
                if (last_path_delim_pos != string::npos) {
                    file_name = file_name.substr(last_path_delim_pos + 1);
                }

                int message_count = file->message_type_count();
                for (int i = 0; i < message_count; ++i) {
                    const Descriptor *message_descriptor = file->message_type(i);
                    js_printer::GenerateJsMessage(message_descriptor, webapp_path, file_name, context);
                }

                js_printer::GenerateConstantsSample(webapp_path, service_names, context);
                js_printer::CopyProtoFile(file, webapp_path, file_name, context);

                return true;
            }
        }
    }
}

int main(int argc, char *argv[]) {
    org::spine::plugins::Codegen generator;
    return PluginMain(argc, argv, &generator);
}
#ifndef SPINEPROTOCPLUGIN_CODEGEN_H_H
#define SPINEPROTOCPLUGIN_CODEGEN_H_H

#include <google/protobuf/compiler/plugin.h>
#include <google/protobuf/compiler/code_generator.h>

using namespace google::protobuf;

namespace org {
    namespace spine {
        namespace plugins {
            class LIBPROTOC_EXPORT Codegen : public compiler::CodeGenerator {
            public:
                Codegen();
                ~Codegen();
                bool Generate(const FileDescriptor *file,
                        const string &parameter,
                        compiler::GeneratorContext *context,
                        string *error) const;
            };
        }
    }
}

#endif //SPINEPROTOCPLUGIN_CODEGEN_H_H

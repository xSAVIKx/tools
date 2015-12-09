cmake_minimum_required(VERSION 3.3)
project(ProtoCompilerPlugin)

set(CMAKE_EXE_LINKER_FLAGS "-lprotoc -lprotobuf -static-libgcc -static-libstdc++ -s")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES src/main/cpp/main.cpp src/main/cpp/codegen.h src/main/cpp/handler_printer.h src/main/cpp/js_printer.h src/main/cpp/handler_printer.cpp src/main/cpp/service_printer.cpp src/main/cpp/service_printer.h src/main/cpp/js_printer.cpp)
add_executable(ProtoCompilerPlugin ${SOURCE_FILES})

INCLUDE_DIRECTORIES(src/protobuf/src)
set(SEARCH_DIR usr/local/lib)
LINK_DIRECTORIES(src/protobuf/src usr/lib usr/lib/bin usr/local/lib)

target_link_libraries(ProtoCompilerPlugin protoc.a protobuf.a)
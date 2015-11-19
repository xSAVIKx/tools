cmake_minimum_required(VERSION 3.3)
project(ProtoCompilerPlugin)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(SOURCE_FILES src/main/cpp/main.cpp src/main/cpp/codegen.h src/main/cpp/handler_printer.h src/main/cpp/handler_printer.cpp src/main/cpp/service_printer.cpp src/main/cpp/service_printer.h)
add_executable(ProtoCompilerPlugin ${SOURCE_FILES})

INCLUDE_DIRECTORIES(src/protobuf/src)
LINK_DIRECTORIES(src/protobuf/src usr/lib usr/lib/bin)

target_link_libraries(ProtoCompilerPlugin protobuf protoc)
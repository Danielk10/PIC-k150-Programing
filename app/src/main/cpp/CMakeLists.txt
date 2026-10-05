cmake_minimum_required(VERSION 3.22.1)

project("fdsan_bypass")

add_library(fdsan_bypass SHARED fdsan_bypass.cpp)
target_link_libraries(fdsan_bypass log)

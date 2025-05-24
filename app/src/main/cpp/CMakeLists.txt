cmake_minimum_required(VERSION 3.22.1)

project("runner")

add_executable(starter_bash
        starter_bash.c)

add_executable(starter_sh
        starter_sh.c)

set_target_properties(starter_bash PROPERTIES PREFIX "lib")
set_target_properties(starter_bash PROPERTIES SUFFIX ".so")

set_target_properties(starter_sh PROPERTIES PREFIX "lib")
set_target_properties(starter_sh PROPERTIES SUFFIX ".so")

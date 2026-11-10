cmake_minimum_required(VERSION 3.22.1)
project(resteam C CXX)
include(FetchContent)
FetchContent_Declare(imgui GIT_REPOSITORY https://github.com/ocornut/imgui.git GIT_TAG v1.91.9 GIT_SHALLOW TRUE)
FetchContent_MakeAvailable(imgui)

set(GLUE ${CMAKE_ANDROID_NDK}/sources/android/native_app_glue)
add_library(glue STATIC ${GLUE}/android_native_app_glue.c)
target_include_directories(glue PUBLIC ${GLUE})

add_library(resteam SHARED resteam.cpp
    ${imgui_SOURCE_DIR}/imgui.cpp ${imgui_SOURCE_DIR}/imgui_draw.cpp
    ${imgui_SOURCE_DIR}/imgui_tables.cpp ${imgui_SOURCE_DIR}/imgui_widgets.cpp
    ${imgui_SOURCE_DIR}/backends/imgui_impl_opengl3.cpp)
target_include_directories(resteam PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${imgui_SOURCE_DIR} ${imgui_SOURCE_DIR}/backends)
if(NOT GITHUB_REPO)
    set(GITHUB_REPO "udyneO2/Resteam")
endif()
target_compile_definitions(resteam PRIVATE IMGUI_IMPL_OPENGL_ES3 "IMGUI_USER_CONFIG=\"imconfig_resteam.h\"" "GITHUB_REPO=\"${GITHUB_REPO}\"")
target_link_libraries(resteam glue android log EGL GLESv3)
target_link_options(resteam PRIVATE -u ANativeActivity_onCreate -Wl,-z,max-page-size=16384)

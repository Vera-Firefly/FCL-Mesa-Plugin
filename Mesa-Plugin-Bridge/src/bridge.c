//
// Created by Vera-Firefly on 07.02.2025.
//
#include <stdio.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <stdbool.h>
#include "bridge.h"
#include <GL/osmesa.h>
#include <GL/gl.h>

#define EXPORT __attribute__((visibility("default"), used))
#define FILE_PATH "/sdcard/Mesa/env.txt"
#define MAX_LINE 256

static bool logOutPut = false;
static void* dl_handle = NULL;
static void* self_handle = NULL;

static OSMESAproc (*real_OSMesaGetProcAddress)(const char *);
static GLboolean (*real_OSMesaMakeCurrent)(OSMesaContext, void*, GLenum, GLsizei, GLsizei);
static OSMesaContext (*real_OSMesaGetCurrentContext)(void);
static OSMesaContext (*real_OSMesaCreateContext)(GLenum, OSMesaContext);
static void (*real_OSMesaDestroyContext)(OSMesaContext);
static void (*real_OSMesaFlushFrontbuffer)(void);
static void (*real_OSMesaPixelStore)(GLint, GLint);
static const GLubyte* (*real_glGetString)(GLenum);
static void (*real_glFinish)(void);
static void (*real_glClearColor)(GLclampf, GLclampf, GLclampf, GLclampf);
static void (*real_glClear)(GLbitfield);
static void (*real_glReadPixels)(GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, void*);
static void (*real_glReadBuffer)(GLenum);

bool checkHandle() {
    if (!logOutPut)
    {
        char* log_env = getenv("OSM_PLUGIN_LOGE");
        if (log_env && !strcmp(log_env, "true"))
            logOutPut = true;
    }
    if (!dl_handle) {
        char* mesa_library = getenv("MESA_LIBRARY");
        if (!mesa_library)
        {
            if (logOutPut) fprintf(stderr, "Error[OSM Plugin Bridge]: MESA_LIBRARY environment variable is not set\n");
            return false;
        }

        dlerror();
        dl_handle = dlopen(mesa_library, RTLD_LAZY);
        char* error = dlerror();
        if (!dl_handle)
        {
            if (logOutPut) fprintf(stderr, "Error[OSM Plugin Bridge]: Failed to load %s: %s\n", mesa_library, error);
            return false;
        }
    }
    return true;
}

void set_env_from_file(const char *file_path) {
    FILE *file = fopen(file_path, "r");
    if (!file) return;

    char line[MAX_LINE];
    while (fgets(line, sizeof(line), file))
    {
        line[strcspn(line, "\r\n")] = '\0';
        char *delimiter = strchr(line, '=');
        if (delimiter)
        {
            *delimiter = '\0';
            char *key = line;
            char *value = delimiter + 1;

            if (setenv(key, value, 1) != 0) {
                if (logOutPut) fprintf(stderr, "Warning[OSM Plugin Bridge]: Failed to set environment variable %s=%s\n", key, value);
            } else {
                printf("[OSM Plugin Bridge]: Put Env %s=%s\n", key, value);
            }
        }
    }

    if (fclose(file) != 0) {
        if (logOutPut) fprintf(stderr, "Warning[OSM Plugin Bridge]: Failed to close file %s\n", file_path);
    }
}

__attribute__((constructor))
static void init() {
    set_env_from_file(FILE_PATH);

    Dl_info info;
    if (dladdr((void*)init, &info))
    {
        self_handle = dlopen(info.dli_fname, RTLD_NOW | RTLD_NOLOAD);
        if (!self_handle)
        {
            self_handle = dlopen(info.dli_fname, RTLD_NOW);
        }
    }

    if (!self_handle) {
        if (logOutPut) fprintf(stderr, "Error[OSM Plugin Bridge]: Failed to get self_handle: %s\n", dlerror());
    }

    if (checkHandle()) {
        #define LOAD_SYMBOL(name) \
            real_##name = dlsym(dl_handle, #name); \
            if (!real_##name && logOutPut) fprintf(stderr, "Error[OSM Plugin Bridge]: Failed to find symbol '%s' in Mesa Library: %s\n", #name, dlerror());

        LOAD_SYMBOL(OSMesaGetProcAddress);
        LOAD_SYMBOL(OSMesaMakeCurrent);
        LOAD_SYMBOL(OSMesaGetCurrentContext);
        LOAD_SYMBOL(OSMesaCreateContext);
        LOAD_SYMBOL(OSMesaDestroyContext);
        LOAD_SYMBOL(OSMesaFlushFrontbuffer);
        LOAD_SYMBOL(OSMesaPixelStore);
        LOAD_SYMBOL(glGetString);
        LOAD_SYMBOL(glFinish);
        LOAD_SYMBOL(glClearColor);
        LOAD_SYMBOL(glClear);
        LOAD_SYMBOL(glReadPixels);
        LOAD_SYMBOL(glReadBuffer);
    }
}

void* GetProcAddress(const char *funcName) {
    char* onlyUseGetProcAddress = getenv("ONLY_GET_PROC_ADDRESS");

    if (!checkHandle() && !strcmp(onlyUseGetProcAddress, "false")) return NULL;

    dlerror();
    void* symbol = dlsym(dl_handle, funcName);
    char* error = dlerror();
    if (error)
    {
        if (logOutPut) fprintf(stderr, "Error[OSM Plugin Bridge]: Failed to find symbol '%s' in Mesa Library: %s\n", funcName, error);
        return NULL;
    }

    return symbol;
}

EXPORT
OSMESAproc OSMesaGetProcAddress(const char *funcName) {
    if (!real_OSMesaGetProcAddress) return GetProcAddress(funcName);
    return real_OSMesaGetProcAddress(funcName);
}

EXPORT
GLboolean OSMesaMakeCurrent(OSMesaContext ctx, void *buffer, GLenum type, GLsizei width, GLsizei height) {
    if (!real_OSMesaMakeCurrent) return GL_FALSE;
    return real_OSMesaMakeCurrent(ctx, buffer, type, width, height);
}

EXPORT
OSMesaContext OSMesaGetCurrentContext(void) {
    if (!real_OSMesaGetCurrentContext) return NULL;
    return real_OSMesaGetCurrentContext();
}

EXPORT
OSMesaContext OSMesaCreateContext(GLenum format, OSMesaContext sharelist) {
    if (!real_OSMesaCreateContext) return NULL;
    return real_OSMesaCreateContext(format, sharelist);
}

EXPORT
void OSMesaDestroyContext(OSMesaContext ctx) {
    if (real_OSMesaDestroyContext) real_OSMesaDestroyContext(ctx);
}

EXPORT
void OSMesaFlushFrontbuffer(void) {
    if (real_OSMesaFlushFrontbuffer) real_OSMesaFlushFrontbuffer();
}

EXPORT
void OSMesaPixelStore(GLint pname, GLint value) {
    if (real_OSMesaPixelStore) real_OSMesaPixelStore(pname, value);
}

EXPORT
const GLubyte* glGetString(GLenum name) {
    if (real_glGetString) return real_glGetString(name);
    return NULL;
}

EXPORT
void glFinish(void) {
    if (real_glFinish) real_glFinish();
}

EXPORT
void glClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha) {
    if (real_glClearColor) real_glClearColor(red, green, blue, alpha);
}

EXPORT
void glClear(GLbitfield mask) {
    if (real_glClear) real_glClear(mask);
}

EXPORT
void glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, void* data) {
    if (real_glReadPixels) real_glReadPixels(x, y, width, height, format, type, data);
}

EXPORT
void glReadBuffer(GLenum mode) {
    if (real_glReadBuffer) real_glReadBuffer(mode);
}

__attribute__((destructor))
static void cleanup() {
    if (dl_handle) {
        dlclose(dl_handle);
        dl_handle = NULL;
    }
}
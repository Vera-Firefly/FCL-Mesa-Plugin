//
// Created by Vera-Firefly on 07.02.2025.
//
#include <stdio.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "bridge.h"

#define FILE_PATH "/sdcard/Mesa/env.txt"
#define MAX_LINE 256

void set_env_from_file(const char *file_path) {
    FILE *file = fopen(file_path, "r");
    if (!file) {
        return;
    }

    char line[MAX_LINE];
    while (fgets(line, sizeof(line), file)) {
        line[strcspn(line, "\r\n")] = '\0';
        char *delimiter = strchr(line, '=');
        if (delimiter) {
            *delimiter = '\0';
            char *key = line;
            char *value = delimiter + 1;

            if (setenv(key, value, 1) != 0) {
                // Nothing to do here
            } else {
                // Nothing to do here
            }
        }
    }

    fclose(file);
}

__attribute__((constructor)) 
static void init_env() {
    set_env_from_file(FILE_PATH);
}

__attribute__((visibility("default"), used))
void* OSMesaGetProcAddress(const char* funcName) {
    char* mesa_library = getenv("MESA_LIBRARY");
    void* dl_handle = dlopen(mesa_library, RTLD_LAZY);
    if (!dl_handle) {
        fprintf(stderr, "Error[OSM Plugin Bridge]: Failed to load %s: %s\n", mesa_library, dlerror());
        return NULL;
    }

    void* symbol = dlsym(dl_handle, funcName);
    if (!symbol) {
        char* log_env = getenv("OSM_PLUGIN_LOGE");
        if (log_env && !strcmp(log_env, "true"))
            fprintf(stderr, "Error[OSM Plugin Bridge]: Failed to find symbol '%s' in %s: %s\n", funcName, mesa_library, dlerror());

        dlclose(dl_handle);
        return NULL;
    }

    dlclose(dl_handle);
    return symbol;
}
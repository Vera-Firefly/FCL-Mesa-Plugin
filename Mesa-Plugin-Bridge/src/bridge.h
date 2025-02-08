//
// Created by Vera-Firefly on 07.02.2025.
//
#ifndef BRIDGE_H
#define BRIDGE_H

#ifdef __cplusplus
extern "C" {
#endif

#define EXPORT __attribute__((visibility("default"), used))

EXPORT void* OSMesaGetProcAddress(const char* funcName);
__attribute__((constructor)) static void init_env();

#ifdef __cplusplus
}
#endif

#endif // BRIDGE_H
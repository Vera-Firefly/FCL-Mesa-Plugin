//
// Created by Vera-Firefly on 07.02.2025.
//
#ifndef BRIDGE_H
#define BRIDGE_H

#ifdef __cplusplus
extern "C" {
#endif

#include <GL/osmesa.h>
#include <GL/gl.h>

#define EXPORT __attribute__((visibility("default"), used))

EXPORT OSMESAproc OSMesaGetProcAddress(const char *funcName);
EXPORT GLboolean OSMesaMakeCurrent(OSMesaContext ctx, void *buffer, GLenum type, GLsizei width, GLsizei height);
EXPORT OSMesaContext OSMesaGetCurrentContext(void);
EXPORT OSMesaContext OSMesaCreateContext(GLenum format, OSMesaContext sharelist);
EXPORT void OSMesaDestroyContext(OSMesaContext ctx);
EXPORT void OSMesaFlushFrontbuffer(void);
EXPORT void OSMesaPixelStore(GLint pname, GLint value);
EXPORT const GLubyte* glGetString(GLenum name);
EXPORT void glFinish(void);
EXPORT void glClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
EXPORT void glClear(GLbitfield mask);
EXPORT void glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, void* data);
EXPORT void glReadBuffer(GLenum mode);

#ifdef __cplusplus
}
#endif

#endif // BRIDGE_H
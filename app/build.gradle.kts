plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.mio.plugin.renderer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mio.plugin.renderer"
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        configureEach {
            //应用名
            //app name
            resValue("string","app_name","Mesa 25.0.0")
            //包名后缀
            //package name Suffix
            applicationIdSuffix = ".mesa2500"

            //渲染器在启动器内显示的名称
            //The name displayed by the renderer in the launcher
            manifestPlaceholders["des"] = "Mesa 25.0.0"
            //渲染器的具体定义 格式为 名称:渲染器库名:EGL库名 例如 LTW:libltw.so:libltw.so
            //The specific definition format of a renderer is ${name}:${renderer library name}:${EGL library name}, for example:   LTW:libltw.so:libltw.so
            manifestPlaceholders["renderer"] = "Mesa24.3.4:libOSMBridge.so:libEGL.so"

            //特殊Env
            //Special Env
            //DLOPEN=libxxx.so 用于加载额外库文件
            //DLOPEN=libxxx.so used to load external library
            //如果有多个库,可以使用","隔开,例如  DLOPEN=libxxx.so,libyyy.so
            //If there are multiple libraries, you can use "," to separate them, for example  DLOPEN=libxxx.so,libyyy.so
            manifestPlaceholders["boatEnv"] = mutableMapOf<String,String>().apply {

            }.run {
                var env = "LIBGL_STRING=custom_gallium:LIBGL_NAME=libOSMesa.so:LIB_MESA_NAME=libOSMesa.so:MESA_LIBRARY=libOSMesa.so:DLOPEN=libOSMBridge.so"
                forEach { (key, value) ->
                    env += "$key=$value:"
                }
                env.dropLast(1)
            }

            manifestPlaceholders["pojavEnv"] = mutableMapOf<String,String>().apply {

            }.run {
                var env = "POJAV_RENDERER=custom_gallium:LIB_MESA_NAME=libOSMBridge.so:MESA_LIBRARY=libOSMesa.so:DLOPEN=libOSMesa.so"
                forEach { (key, value) ->
                    env += "$key=$value:"
                }
                env.dropLast(1)
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
}
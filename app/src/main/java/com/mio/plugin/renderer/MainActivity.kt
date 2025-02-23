package com.mio.plugin.renderer

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.lang.NumberFormatException

import com.mio.plugin.renderer.CustomDialog

class MainActivity : Activity() {
    private val REQUEST_CODE = 12
    private val REQUEST_CODE_PERMISSION = 0x00099

    private var hasAllFilesPermission = false
    private var isNoticedAllFilesPermissionMissing = false
    private val envFile = File(Environment.getExternalStorageDirectory(), "Mesa/env.txt")

    private lateinit var logSwitch: Switch
    private lateinit var ogpaSwitch: Switch
    private lateinit var galliumSettings: Button
    private lateinit var glVersionSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
        checkAndCreateEnvFile()

        val scrollView = ScrollView(this)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(16, 16, 16, 16)
        }

        val rendererNameTextView = TextView(this).apply {
            text = "Mesa Plugin"
            textSize = 28f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        val releaseTextView = TextView(this).apply {
            text = "Release v1.1"
            textSize = 18f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        val authorTextView = TextView(this).apply {
            text = "By Vera-Firefly"
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            setBackgroundColor(Color.GRAY)
        }

        val mainButton = Button(this).apply {
            text = "修改渲染器设置"
            setOnClickListener {
                if (hasAllFilesPermission) {
                    checkAndCreateEnvFile()
                    visibility = Button.GONE
                    logSwitch.visibility = Switch.VISIBLE
                    ogpaSwitch.visibility = Switch.VISIBLE
                    galliumSettings.visibility = Button.VISIBLE
                    glVersionSettings.visibility = Button.VISIBLE
                } else {
                    checkPermission()
                }
            }
        }

        logSwitch = Switch(this).apply {
            text = "插件日志输出"
            isChecked = readLogStatus()
            setOnCheckedChangeListener { _, isChecked ->
                updateLogStatus(isChecked)
            }
        }

        ogpaSwitch = Switch(this).apply {
            text = "仅使用getProcAddress(不建议使用)"
            isChecked = readOGPAStatus()
            setOnCheckedChangeListener { _, isChecked ->
                updateOGPAStatus(isChecked)
            }
        }

        galliumSettings = Button(this).apply {
            text = "Gallium驱动设置"
            setOnClickListener {
                showGalliumDriverDialog()
            }
        }

        glVersionSettings = Button(this).apply {
            text = "GL/GLSL版本设置"
            setOnClickListener {
                showSetGLVersionDialog()
            }
        }

        logSwitch.visibility = Switch.GONE
        ogpaSwitch.visibility = Switch.GONE
        galliumSettings.visibility = Button.GONE
        glVersionSettings.visibility = Button.GONE

        mainLayout.addView(rendererNameTextView)
        mainLayout.addView(releaseTextView)
        mainLayout.addView(authorTextView)
        mainLayout.addView(divider)

        mainLayout.addView(mainButton)

        mainLayout.addView(logSwitch)
        mainLayout.addView(ogpaSwitch)
        mainLayout.addView(galliumSettings)
        mainLayout.addView(glVersionSettings)

        scrollView.addView(mainLayout)
        setContentView(scrollView)
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                hasAllFilesPermission = true
                checkAndCreateEnvFile()
            } else {
                showPermissionDialog()
            }
        } else {
            requestLegacyPermissions()
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限请求")
            .setMessage("程序需要获取访问所有文件权限才能正常使用 Mesa 设置功能。是否授予？")
            .setPositiveButton("是") { _: DialogInterface?, _: Int ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE)
                isNoticedAllFilesPermissionMissing = false
            }
            .setNegativeButton("否") { _: DialogInterface?, _: Int ->
                isNoticedAllFilesPermissionMissing = true
                Toast.makeText(
                    this,
                    "拒绝授权将导致设置功能无法正常工作",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setOnKeyListener { _, keyCode, _ -> keyCode == KeyEvent.KEYCODE_BACK }
            .setCancelable(false)
            .show()
    }

    private fun requestLegacyPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSION
            )
        } else {
            hasAllFilesPermission = true
            checkAndCreateEnvFile()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasAllFilesPermission = Environment.isExternalStorageManager()
            if (!hasAllFilesPermission && !isNoticedAllFilesPermissionMissing) {
                Toast.makeText(this, "拒绝授权将导致设置功能无法正常工作", Toast.LENGTH_SHORT).show()
                isNoticedAllFilesPermissionMissing = true
            }
        }
    }

    private fun checkAndCreateEnvFile() {
        if (!hasAllFilesPermission) return
        if (!envFile.exists()) {
            envFile.parentFile?.mkdirs()
            envFile.writeText(
                """
                GALLIUM_DRIVER=zink
                MESA_GL_VERSION_OVERRIDE=4.6
                MESA_GLSL_VERSION_OVERRIDE=460
                OSM_PLUGIN_LOGE=false
                ONLY_GET_PROC_ADDRESS=false
                """.trimIndent()
            )
        }
    }

    // 插件日志输出
    private fun readLogStatus(): Boolean {
        if (!envFile.exists() || !hasAllFilesPermission) return false
        return envFile.readLines().any { it.trim() == "OSM_PLUGIN_LOGE=true" }
    }

    private fun updateLogStatus(enabled: Boolean) {
        checkAndCreateEnvFile()
        if (!envFile.exists()) return

        val lines = envFile.readLines().toMutableList()
        var found = false

        for (i in lines.indices) {
            if (lines[i].startsWith("OSM_PLUGIN_LOGE=")) {
                lines[i] = "OSM_PLUGIN_LOGE=$enabled"
                found = true
                break
            }
        }

        if (!found)
            lines.add("OSM_PLUGIN_LOGE=$enabled")

        envFile.writeText(lines.joinToString("\n"))
    }

    // 仅使用 getProcAddress
    private fun readOGPAStatus(): Boolean {
        if (!envFile.exists() || !hasAllFilesPermission) return false
        return envFile.readLines().any { it.trim() == "ONLY_GET_PROC_ADDRESS=true" }
    }

    private fun updateOGPAStatus(enabled: Boolean) {
        checkAndCreateEnvFile()
        if (!envFile.exists()) return

        val lines = envFile.readLines().toMutableList()
        var found = false

        for (i in lines.indices) {
            if (lines[i].startsWith("ONLY_GET_PROC_ADDRESS=")) {
                lines[i] = "ONLY_GET_PROC_ADDRESS=$enabled"
                found = true
                break
            }
        }

        if (!found)
            lines.add("ONLY_GET_PROC_ADDRESS=$enabled")

        envFile.writeText(lines.joinToString("\n"))
    }

    // 选择 gallium 驱动
    private fun showGalliumDriverDialog() {
        val drivers = arrayOf("zink", "freedreno", "panfrost"/*, "softpipe", "llvmpipe"*/)
        val currentDriver = readCurrentGalliumDriver()
        val selectedIndex = drivers.indexOf(currentDriver)

        AlertDialog.Builder(this)
            .setTitle("选择 Gallium 驱动")
            .setSingleChoiceItems(drivers, selectedIndex) { dialog, which ->
                val selectedDriver = drivers[which]
                updateGalliumDriver(selectedDriver)
                Toast.makeText(this, "已选择: $selectedDriver", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun readCurrentGalliumDriver(): String {
        if (!envFile.exists() || !hasAllFilesPermission) return "zink"
        return envFile.readLines()
            .firstOrNull { it.startsWith("GALLIUM_DRIVER=") }
            ?.substringAfter("=")
            ?.trim() ?: "zink"
    }

    private fun updateGalliumDriver(newDriver: String) {
        checkAndCreateEnvFile()
        if (!hasAllFilesPermission || !envFile.exists()) return

        val lines = envFile.readLines().toMutableList()
        var found = false

        for (i in lines.indices) {
            if (lines[i].startsWith("GALLIUM_DRIVER=")) {
                lines[i] = "GALLIUM_DRIVER=$newDriver"
                found = true
                break
            }
        }

        if (!found)
            lines.add("GALLIUM_DRIVER=$newDriver")

        envFile.writeText(lines.joinToString("\n"))
    }

    private fun showSetGLVersionDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val glVersionInput = EditText(this).apply {
            hint = "MESA_GL_VERSION_OVERRIDE"
            setText(readGLVersion()) // 读取当前 GL 版本
        }

        val glslVersionInput = EditText(this).apply {
            hint = "MESA_GLSL_VERSION_OVERRIDE"
            setText(readGLSLVersion()) // 读取当前 GLSL 版本
        }

        layout.addView(glVersionInput)
        layout.addView(glslVersionInput)

        val dialog = CustomDialog.Builder(this)
            .setTitle("设置 GL/GLSL 版本")
            .setMessage("请输入你需要的 GL/GLSL 版本")
            .setView(layout)
            .setConfirmListener { 
                val newGLVersion = glVersionInput.text.toString().trim()
                val newGLSLVersion = glslVersionInput.text.toString().trim()

                val validGLVersion = isValidVersion(newGLVersion, "2.8", "4.6") && newGLVersion.matches(Regex("[234]\\.(\\d)"))
                val validGLSLVersion = isValidVersion(newGLSLVersion, "280", "460") && newGLSLVersion.matches(Regex("[234](\\d)0"))

                if (!validGLVersion || !validGLSLVersion) {
                    if (!validGLVersion) {
                        glVersionInput.error = "输入值不合法"
                        glVersionInput.requestFocus()
                    }
                    if (!validGLSLVersion) {
                        glslVersionInput.error = "输入值不合法"
                        glslVersionInput.requestFocus()
                    }
                    false
                } else {
                    updateGLVersions(newGLVersion, newGLSLVersion)
                    Toast.makeText(this, "GL/GLSL 版本已更新", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            .build()
        dialog.show()
    }

    private fun isValidVersion(version: String, minVersion: String, maxVersion: String): Boolean {
        return try {
            val versionNumber = version.toFloat()
            val minVersionNumber = minVersion.toFloat()
            val maxVersionNumber = maxVersion.toFloat()

            versionNumber in minVersionNumber..maxVersionNumber
        } catch (e: NumberFormatException) {
            false
        }
    }

    // 读取当前 GL 版本
    private fun readGLVersion(): String {
        if (!envFile.exists() || !hasAllFilesPermission) return "4.6"
        return envFile.readLines().firstOrNull { it.startsWith("MESA_GL_VERSION_OVERRIDE=") }
            ?.substringAfter("=")?.trim() ?: "4.6"
    }

    // 读取当前 GLSL 版本
    private fun readGLSLVersion(): String {
        if (!envFile.exists() || !hasAllFilesPermission) return "460"
        return envFile.readLines().firstOrNull { it.startsWith("MESA_GLSL_VERSION_OVERRIDE=") }
            ?.substringAfter("=")?.trim() ?: "460"
    }

    // 更新 GL/GLSL 版本
    private fun updateGLVersions(newGL: String, newGLSL: String) {
        checkAndCreateEnvFile()
        if (!hasAllFilesPermission || !envFile.exists()) return

        val lines = envFile.readLines().toMutableList()
    
        var glFound = false
        var glslFound = false

        for (i in lines.indices) {
            if (lines[i].startsWith("MESA_GL_VERSION_OVERRIDE=")) {
                lines[i] = "MESA_GL_VERSION_OVERRIDE=$newGL"
                glFound = true
            }
            if (lines[i].startsWith("MESA_GLSL_VERSION_OVERRIDE=")) {
                lines[i] = "MESA_GLSL_VERSION_OVERRIDE=$newGLSL"
                glslFound = true
            }
        }

        if (!glFound) lines.add("MESA_GL_VERSION_OVERRIDE=$newGL")
        if (!glslFound) lines.add("MESA_GLSL_VERSION_OVERRIDE=$newGLSL")

        envFile.writeText(lines.joinToString("\n"))
    }

}
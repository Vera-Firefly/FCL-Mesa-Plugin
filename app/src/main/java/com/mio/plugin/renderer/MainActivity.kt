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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : Activity() {
    private val REQUEST_CODE = 12
    private val REQUEST_CODE_PERMISSION = 0x00099

    private var hasAllFilesPermission = false
    private var isNoticedAllFilesPermissionMissing = false
    private val envFile = File(Environment.getExternalStorageDirectory(), "Mesa/env.txt")

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
            text = "Mesa 25.0.0-RC1"
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
            text = "Debug RC 1"
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

        val logSwitch = Switch(this).apply {
            text = "插件日志输出"
            isChecked = readLogStatus()
            setOnCheckedChangeListener { _, isChecked ->
                updateLogStatus(isChecked)
            }
        }

        val ogpaSwitch = Switch(this).apply {
            text = "仅使用getProcAddress(不建议使用)"
            isChecked = readOGPAStatus()
            setOnCheckedChangeListener { _, isChecked ->
                updateOGPAStatus(isChecked)
            }
        }

        val settingsButton = Button(this).apply {
            text = "Gallium驱动设置"
            setOnClickListener {
                showGalliumDriverDialog()
            }
        }

        mainLayout.addView(rendererNameTextView)
        mainLayout.addView(releaseTextView)
        mainLayout.addView(authorTextView)
        mainLayout.addView(logSwitch)
        mainLayout.addView(ogpaSwitch)
        mainLayout.addView(settingsButton)

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
        if (!hasAllFilesPermission || !envFile.exists()) return

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
        if (!hasAllFilesPermission || !envFile.exists()) return

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
        if (!hasAllFilesPermission) return
        val drivers = arrayOf("zink", "freedreno", "panfrost", "softpipe", "llvmpipe")
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

}
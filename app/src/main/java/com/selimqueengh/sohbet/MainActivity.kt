package com.selimqueengh.sohbet

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.selimqueengh.sohbet.fragments.ChatsFragment
import com.selimqueengh.sohbet.services.FirebaseService
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : AppCompatActivity() {
    
    private lateinit var firebaseService: FirebaseService
    private lateinit var sharedPreferences: SharedPreferences
    
    private var currentUserId: String = ""
    private var currentUsername: String = ""
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val DOWNLOAD_CHUNK_SIZE = 8192
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentUsername = intent.getStringExtra("username") ?: "Kullanıcı"
        
        sharedPreferences = getSharedPreferences("SnickersChatv4", MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("user_id", "") ?: ""
        
        firebaseService = FirebaseService()
        
        initViews()
        setupClickListeners()
        
        // Check for updates first
        checkForUpdates()
        
        // Default to chats fragment
        showChatsFragment()
    }

    private fun initViews() {
        // Toolbar'ı ayarla
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun setupClickListeners() {
        val bottomNavigation = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_chats -> {
                    showChatsFragment()
                    true
                }
                R.id.nav_friends -> {
                    val intent = Intent(this, FriendsActivity::class.java)
                    startActivityForResult(intent, 1001)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, TestUsersActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun showChatsFragment() {
        val fragment = ChatsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
    }
    
    private fun checkForUpdates() {
        firebaseService.checkForUpdates { hasUpdate, updateUrl, forceUpdate ->
            runOnUiThread {
                if (hasUpdate) {
                    showUpdateDialog(updateUrl, forceUpdate)
                }
            }
        }
    }
    
    private fun showUpdateDialog(updateUrl: String, forceUpdate: Boolean) {
        val dialogBuilder = AlertDialog.Builder(this)
        
        if (forceUpdate) {
            dialogBuilder.setTitle("Güncelleme Gerekli")
                .setMessage("Uygulamanın yeni bir sürümü mevcut. Devam etmek için güncelleme yapmanız gerekiyor.")
                .setCancelable(false)
                .setPositiveButton("Güncelle") { _, _ ->
                    downloadAndInstallUpdate(updateUrl)
                }
                .setNegativeButton("Çıkış") { _, _ ->
                    finish()
                }
        } else {
            dialogBuilder.setTitle("Güncelleme Mevcut")
                .setMessage("Uygulamanın yeni bir sürümü mevcut. Güncellemek ister misiniz?")
                .setCancelable(true)
                .setPositiveButton("Güncelle") { _, _ ->
                    downloadAndInstallUpdate(updateUrl)
                }
                .setNegativeButton("Daha Sonra") { dialog, _ ->
                    dialog.dismiss()
                }
        }
        
        dialogBuilder.show()
    }
    
    private fun downloadAndInstallUpdate(updateUrl: String) {
        // Check permissions first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 
                PERMISSION_REQUEST_CODE)
            return
        }
        
        // Show download progress dialog
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Güncelleme İndiriliyor")
            .setMessage("Lütfen bekleyin...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        // Download in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apkFile = downloadApk(updateUrl)
                
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    installApk(apkFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Güncelleme indirilemedi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private suspend fun downloadApk(updateUrl: String): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadsDir, "sohbet-update.apk")
        
        val connection = URL(updateUrl).openConnection()
        val inputStream = connection.getInputStream()
        val outputStream = FileOutputStream(apkFile)
        
        val buffer = ByteArray(DOWNLOAD_CHUNK_SIZE)
        var bytesRead: Int
        
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        
        inputStream.close()
        outputStream.close()
        
        return apkFile
    }
    
    private fun installApk(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                apkFile
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "APK yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry download
                firebaseService.checkForUpdates { hasUpdate, updateUrl, forceUpdate ->
                    runOnUiThread {
                        if (hasUpdate) {
                            downloadAndInstallUpdate(updateUrl)
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Depolama izni gerekli", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            // Refresh chats when returning from friend requests
            showChatsFragment()
        }
    }
}
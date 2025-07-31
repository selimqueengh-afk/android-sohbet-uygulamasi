package com.selimqueengh.sohbet.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.selimqueengh.sohbet.LoginActivity
import com.selimqueengh.sohbet.R

class SettingsFragment : Fragment() {

    private lateinit var darkModeSwitch: Switch
    private lateinit var logoutLayout: LinearLayout
    private lateinit var aboutLayout: LinearLayout
    private lateinit var privacyLayout: LinearLayout
    private lateinit var notificationLayout: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        loadSettings()
    }

    private fun initViews(view: View) {
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch)
        logoutLayout = view.findViewById(R.id.logoutLayout)
        aboutLayout = view.findViewById(R.id.aboutLayout)
        privacyLayout = view.findViewById(R.id.privacyLayout)
        notificationLayout = view.findViewById(R.id.notificationLayout)
    }

    private fun setupListeners() {
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            setDarkMode(isChecked)
            saveDarkModeSetting(isChecked)
        }

        logoutLayout.setOnClickListener {
            showLogoutDialog()
        }

        aboutLayout.setOnClickListener {
            showAboutDialog()
        }

        privacyLayout.setOnClickListener {
            showPrivacyDialog()
        }

        notificationLayout.setOnClickListener {
            showNotificationSettings()
        }
    }

    private fun loadSettings() {
        val sharedPref = requireActivity().getSharedPreferences("ChatApp", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)
        darkModeSwitch.isChecked = isDarkMode
    }

    private fun setDarkMode(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun saveDarkModeSetting(isDarkMode: Boolean) {
        val sharedPref = requireActivity().getSharedPreferences("ChatApp", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("dark_mode", isDarkMode)
            apply()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Çıkış Yap")
            .setMessage("Uygulamadan çıkmak istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                logout()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun logout() {
        val sharedPref = requireActivity().getSharedPreferences("ChatApp", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("username")
            apply()
        }
        
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("SuperChat Hakkında")
            .setMessage("""
                📱 SuperChat v2.0
                
                ✨ WhatsApp, Telegram ve Instagram'dan daha iyi modern chat uygulaması!
                
                🎨 Özellikler:
                • Modern Material Design
                • Fragment tabanlı mimari  
                • Bottom Navigation
                • Dark/Light tema
                • Gerçek zamanlı mesajlaşma
                
                👨‍💻 Geliştirici: SelimQueenGH
                📅 2024
            """.trimIndent())
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun showPrivacyDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Gizlilik Ayarları")
            .setMessage("""
                🔒 Gizlilik Politikası
                
                • Verileriniz cihazınızda güvenle saklanır
                • Üçüncü taraflarla paylaşılmaz
                • İstediğiniz zaman hesabınızı silebilirsiniz
                • End-to-end şifreleme (gelecek sürümde)
                
                📞 İletişim: privacy@superchat.com
            """.trimIndent())
            .setPositiveButton("Anladım", null)
            .show()
    }

    private fun showNotificationSettings() {
        AlertDialog.Builder(requireContext())
            .setTitle("Bildirim Ayarları")
            .setMessage("Bildirim ayarları gelecek sürümde eklenecek!")
            .setPositiveButton("Tamam", null)
            .show()
    }
}

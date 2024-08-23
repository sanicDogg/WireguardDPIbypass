package com.example.wireguarddpibypass

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var editTextAddress: EditText
    private lateinit var editTextExternalPort: EditText
    private lateinit var editTextInternalPort: EditText
    private lateinit var logTextView: TextView
    private lateinit var logScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        editTextAddress = findViewById(R.id.editTextAddress)
        editTextExternalPort = findViewById(R.id.editTextExternalPort)
        editTextInternalPort = findViewById(R.id.editTextInternalPort)
        logTextView = findViewById(R.id.logTextView)
        logScrollView = findViewById(R.id.logScrollView)

        renderWelcome()

        val buttonSend: Button = findViewById(R.id.buttonSend)
        val buttonClearLogs: Button = findViewById(R.id.buttonClearLogs)

        loadSavedData()

        buttonSend.setOnClickListener {
            sendUdpPackets()
        }
        buttonClearLogs.setOnClickListener {
            clearLogs()
        }

        logMessage("Приложение запущено")
    }

    override fun onPause() {
        super.onPause()
        // Сохранение данных при закрытии или сворачивании приложения
        saveData()
    }

    private fun renderWelcome() {
        val welcomeTextView: TextView = findViewById(R.id.welcomeTextView)
        welcomeTextView.append("1. Указать данные соединения\n")
        welcomeTextView.append("2. Нажать кнопку Отправить\n")
        welcomeTextView.append("3. Получить сообщение об успешной отправке\n")
        welcomeTextView.append("4. Подключиться к VPN Wireguard\n\n")
        welcomeTextView.append("В случае неудачи изменить порт в настройках WireGuard и попровать сначала")
    }

    private fun loadSavedData() {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        editTextAddress.setText(sharedPreferences.getString("address", ""))
        editTextExternalPort.setText(sharedPreferences.getString("externalPort", ""))
        editTextInternalPort.setText(sharedPreferences.getString("internalPort", ""))
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("address", editTextAddress.text.toString())
            putString("externalPort", editTextExternalPort.text.toString())
            putString("internalPort", editTextInternalPort.text.toString())
            apply()
        }
        logMessage("Данные сохранены.")
    }

    private fun sendUdpPackets() {
        val address = editTextAddress.text.toString()
        val externalPort = editTextExternalPort.text.toString().toIntOrNull()
        val internalPort = editTextInternalPort.text.toString().toIntOrNull()

        if (address.isNotEmpty() && externalPort != null && internalPort != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val socket = DatagramSocket(internalPort)
                    val ipAddress = InetAddress.getByName(address)
                    val data = ByteArray(16) // 16 нулевых байтов

                    val packet1 = DatagramPacket(data, data.size, ipAddress, externalPort)
                    socket.send(packet1)

                    val packet2 = DatagramPacket(data, data.size, ipAddress, externalPort)
                    socket.send(packet2)

                    socket.close()

                    runOnUiThread {
                        logMessage("Пакеты отправлены на $address:$externalPort с исходящего порта $internalPort")
                        logMessage("Попробуйте подключиться к WireGuard")
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        logMessage("Ошибка при отправке пакетов: ${e.message}")
                    }
                }
            }
        } else {
            logMessage("Пожалуйста, заполните все поля корректными данными.")
        }
    }

    private fun logMessage(message: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())

        val logMessage = "$currentDateTime: $message"

        val spannableString = SpannableString(logMessage)
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            currentDateTime.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        logTextView.append(spannableString)
        logTextView.append("\n")

        logScrollView.post {
            logScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun clearLogs() {
        logTextView.text = "";
    }
}
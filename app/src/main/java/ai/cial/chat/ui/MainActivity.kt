package ai.cial.chat.ui

import ai.cial.chat.CialAccessibilityService
import ai.cial.chat.R
import ai.cial.chat.polish.OutputMode
import ai.cial.chat.polish.PolishClient
import ai.cial.chat.polish.Tone
import ai.cial.chat.settings.CialSettings
import ai.cial.chat.settings.InputLanguage
import ai.cial.chat.settings.Provider
import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Onboarding and settings screen for Cial.ai. */
class MainActivity : AppCompatActivity() {

    private lateinit var settings: CialSettings
    private lateinit var accessibilityStatus: TextView
    private lateinit var micStatus: TextView
    private lateinit var apiKeyInput: EditText
    private lateinit var modelInput: EditText
    private lateinit var testResult: TextView
    private lateinit var testButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = CialSettings(this)

        accessibilityStatus = findViewById(R.id.accessibility_status)
        micStatus = findViewById(R.id.mic_status)
        apiKeyInput = findViewById(R.id.input_api_key)
        modelInput = findViewById(R.id.input_model)
        testResult = findViewById(R.id.test_result)
        testButton = findViewById(R.id.btn_test)

        findViewById<Button>(R.id.btn_accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btn_mic).setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MIC
            )
        }

        setupProviderSpinner()
        setupDefaultsSpinners()

        apiKeyInput.setText(settings.apiKey)
        modelInput.setText(settings.model)

        testButton.setOnClickListener { runConnectionTest() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
    }

    override fun onPause() {
        super.onPause()
        persistApiSettings()
    }

    private fun persistApiSettings() {
        settings.apiKey = apiKeyInput.text.toString()
        settings.model = modelInput.text.toString()
    }

    private fun refreshStatuses() {
        val a11yOn = isAccessibilityServiceEnabled()
        accessibilityStatus.text = getString(
            if (a11yOn) R.string.status_enabled else R.string.status_disabled
        )
        accessibilityStatus.setTextColor(
            ContextCompat.getColor(this, if (a11yOn) R.color.success_text else R.color.error_text)
        )
        val micOk = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        micStatus.text = getString(if (micOk) R.string.mic_granted else R.string.mic_missing)
        micStatus.setTextColor(
            ContextCompat.getColor(this, if (micOk) R.color.success_text else R.color.error_text)
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = getSystemService(AccessibilityManager::class.java) ?: return false
        val enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabled.any { info ->
            val serviceInfo = info.resolveInfo.serviceInfo
            serviceInfo.packageName == packageName &&
                serviceInfo.name == CialAccessibilityService::class.java.name
        }
    }

    private fun setupProviderSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinner_provider)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Provider.entries.map { it.label }
        )
        spinner.adapter = adapter
        spinner.setSelection(Provider.entries.indexOf(settings.provider))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val provider = Provider.entries[position]
                if (provider != settings.provider) {
                    settings.provider = provider
                    modelInput.setText(provider.defaultModel)
                    settings.model = provider.defaultModel
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupDefaultsSpinners() {
        val languageSpinner = findViewById<Spinner>(R.id.spinner_language)
        languageSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            InputLanguage.entries.map { it.label }
        )
        languageSpinner.setSelection(InputLanguage.entries.indexOf(settings.inputLanguage))
        languageSpinner.onItemSelectedListener = simpleListener { position ->
            settings.inputLanguage = InputLanguage.entries[position]
        }

        val modeSpinner = findViewById<Spinner>(R.id.spinner_mode)
        modeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            OutputMode.entries.map { it.label }
        )
        modeSpinner.setSelection(OutputMode.entries.indexOf(settings.outputMode))
        modeSpinner.onItemSelectedListener = simpleListener { position ->
            settings.outputMode = OutputMode.entries[position]
        }

        val toneSpinner = findViewById<Spinner>(R.id.spinner_tone)
        toneSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Tone.entries.map { it.label }
        )
        toneSpinner.setSelection(Tone.entries.indexOf(settings.tone))
        toneSpinner.onItemSelectedListener = simpleListener { position ->
            settings.tone = Tone.entries[position]
        }
    }

    private fun simpleListener(onSelected: (Int) -> Unit): AdapterView.OnItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSelected(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

    private fun runConnectionTest() {
        persistApiSettings()
        if (!settings.hasApiKey) {
            showTestResult("Add your API key first, then test.", isError = true)
            return
        }
        testButton.isEnabled = false
        testButton.text = getString(R.string.testing)
        lifecycleScope.launch {
            val result = PolishClient(settings).testConnection()
            testButton.isEnabled = true
            testButton.text = getString(R.string.test_connection)
            result.fold(
                onSuccess = { reply ->
                    showTestResult("Connection works. The model replied: $reply", isError = false)
                },
                onFailure = { error ->
                    showTestResult("Connection failed: ${error.message}", isError = true)
                }
            )
        }
    }

    private fun showTestResult(message: String, isError: Boolean) {
        testResult.visibility = View.VISIBLE
        testResult.text = message
        testResult.setTextColor(
            ContextCompat.getColor(this, if (isError) R.color.error_text else R.color.success_text)
        )
    }

    companion object {
        private const val REQUEST_MIC = 1001
    }
}

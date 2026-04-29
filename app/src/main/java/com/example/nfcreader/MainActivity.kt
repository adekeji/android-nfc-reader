package com.example.nfcreader

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nfcreader.databinding.ActivityMainBinding
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            binding.tvStatus.text = getString(R.string.nfc_not_supported)
            binding.tvTagData.text = ""
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            binding.tvStatus.text = getString(R.string.nfc_disabled)
            Toast.makeText(this, R.string.nfc_enable_prompt, Toast.LENGTH_LONG).show()
        } else {
            binding.tvStatus.text = getString(R.string.ready_to_scan)
        }

        // PendingIntent used for foreground dispatch — routes all NFC intents to this activity
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // If the activity was started by an NFC intent, handle it immediately
        handleIntent(getIntent())
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    /** Called when a new Intent arrives while the activity is running (foreground dispatch). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    // -------------------------------------------------------------------------
    // NFC processing
    // -------------------------------------------------------------------------

    private fun handleIntent(intent: Intent) {
        val action = intent.action ?: return

        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED
        ) return

        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return

        val sb = StringBuilder()

        // --- Tag ID ---
        sb.appendLine("Tag ID: ${tag.id.toHex()}")

        // --- Technologies ---
        sb.appendLine("Technologies: ${tag.techList.joinToString { it.substringAfterLast('.') }}")
        sb.appendLine()

        // --- NDEF payload (if available) ---
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                val ndefMessage: NdefMessage? = ndef.ndefMessage
                if (ndefMessage != null) {
                    sb.appendLine("NDEF Records (${ndefMessage.records.size}):")
                    ndefMessage.records.forEachIndexed { index, record ->
                        sb.appendLine("  Record ${index + 1}:")
                        sb.appendLine("    TNF: ${record.tnf}")
                        sb.appendLine("    Type: ${String(record.type, Charset.forName("US-ASCII"))}")
                        val payload = record.payload
                        // For well-known text records, skip the language-code prefix
                        val text = if (record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                            record.type.contentEquals(android.nfc.NdefRecord.RTD_TEXT) &&
                            payload.isNotEmpty()
                        ) {
                            val languageCodeLength = payload[0].toInt() and 0x3F
                            val textStart = languageCodeLength + 1
                            if (textStart <= payload.size) {
                                String(payload, textStart, payload.size - textStart, Charsets.UTF_8)
                            } else {
                                String(payload, Charsets.UTF_8)
                            }
                        } else {
                            String(payload, Charsets.UTF_8)
                        }
                        sb.appendLine("    Payload: $text")
                    }
                } else {
                    sb.appendLine("NDEF: empty message")
                }
            } catch (e: Exception) {
                sb.appendLine("NDEF read error: ${e.message}")
            } finally {
                try { ndef.close() } catch (_: Exception) {}
            }
        } else {
            sb.appendLine("Tag is not NDEF formatted.")
        }

        // --- Additional tech info ---
        sb.appendLine()
        sb.appendLine("--- Tag Details ---")

        NfcA.get(tag)?.let { tech ->
            try {
                tech.connect()
                sb.appendLine("NFC-A  ATQA: ${tech.atqa.toHex()}  SAK: ${tech.sak}")
            } catch (_: Exception) {} finally { try { tech.close() } catch (_: Exception) {} }
        }

        NfcB.get(tag)?.let { tech ->
            try {
                tech.connect()
                sb.appendLine("NFC-B  App Data: ${tech.applicationData.toHex()}")
            } catch (_: Exception) {} finally { try { tech.close() } catch (_: Exception) {} }
        }

        NfcF.get(tag)?.let { tech ->
            try {
                tech.connect()
                sb.appendLine("NFC-F  Manufacturer: ${tech.manufacturer.toHex()}")
            } catch (_: Exception) {} finally { try { tech.close() } catch (_: Exception) {} }
        }

        NfcV.get(tag)?.let { tech ->
            try {
                tech.connect()
                sb.appendLine("NFC-V  DSF ID: ${tech.dsfId}  Response flags: ${tech.responseFlags}")
            } catch (_: Exception) {} finally { try { tech.close() } catch (_: Exception) {} }
        }

        MifareClassic.get(tag)?.let { tech ->
            try {
                tech.connect()
                sb.appendLine("MIFARE Classic  Size: ${tech.size} bytes  Sectors: ${tech.sectorCount}")
            } catch (_: Exception) {} finally { try { tech.close() } catch (_: Exception) {} }
        }

        MifareUltralight.get(tag)?.let { tech ->
            try {
                tech.connect()
                sb.appendLine("MIFARE Ultralight  Type: ${tech.type}")
            } catch (_: Exception) {} finally { try { tech.close() } catch (_: Exception) {} }
        }

        IsoDep.get(tag)?.let { tech ->
            try {
                tech.connect()
                sb.appendLine("ISO-DEP  Hi Layer Response: ${tech.hiLayerResponse?.toHex() ?: "N/A"}")
            } catch (_: Exception) {} finally { try { tech.close() } catch (_: Exception) {} }
        }

        NdefFormatable.get(tag)?.let {
            sb.appendLine("Tag is NDEF-formatable (currently unformatted).")
        }

        binding.tvTagData.text = sb.toString()
        binding.tvStatus.text = getString(R.string.tag_detected)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02X".format(byte) }
}

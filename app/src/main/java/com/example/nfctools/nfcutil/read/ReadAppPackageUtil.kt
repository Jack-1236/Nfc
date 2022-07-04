package com.example.nfcreadorwrite.utils.nfc_read

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.util.Log
import java.nio.charset.Charset

/**
 *   created by sunLook
 *   time       2022/6/1
 */
object ReadAppPackageUtil {

    fun ReadTag(intent: Intent): String? {

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action
        ) {

            var rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            var msgs: Array<NdefMessage?>? = null
            var contentSize = 0
            if (rawMsgs != null) {
                msgs = arrayOfNulls<NdefMessage>(rawMsgs.size)
                for ((i, item) in rawMsgs.withIndex()) {
                    msgs[i] = rawMsgs[i] as NdefMessage
                    contentSize += msgs[i]!!.toByteArray().size
                }
            }

            val record = msgs!![0]!!.records[0]
            return ParseTextRecord(record)!!


        }else{return null}


    }

    fun ParseTextRecord(ndefRecord: NdefRecord): String? {

        try {
            //获得字节数组，解析
            val payload: ByteArray = ndefRecord.payload
            return String(payload,
              Charset.forName("UTF-8"))

        } catch (e: Exception) {
            Log.e("tag", e.message.toString())
            return null
        }


    }
}
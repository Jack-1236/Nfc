package com.example.nfctools.nfcutil

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.util.Log

/**
 *   created by sunLook
 *   time       2022/6/1
 */
object IsNdefDataTypeUtil {


    fun IS(intent: Intent): ByteArray? {

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action
        ) {

            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            var msgs: Array<NdefMessage?>? = null

            if (rawMsgs != null) {
                msgs = arrayOfNulls(rawMsgs.size)
                for ((i, item) in rawMsgs.withIndex()) {
                    msgs[i] = item as NdefMessage

                }


            }


            var record: NdefRecord? = null
            msgs!!.let {
                record = it[0]!!.records[0]
            }


            Log.e("tag","${record!!.type}")
            return  record!!.type


        } else {
            return null

        }
    }
}
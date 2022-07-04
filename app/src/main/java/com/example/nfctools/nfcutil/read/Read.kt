package com.example.nfctools.nfcutil.read

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import com.example.nfcreadorwrite.utils.nfc_read.ReadAppPackageUtil
import com.example.nfcreadorwrite.utils.nfc_read.ReadTextUtil
import com.example.nfcreadorwrite.utils.nfc_read.ReadUriUtil
import com.example.nfctools.nfcutil.IsNdefDataTypeUtil
import java.util.*

/**
 *   created by sunLook
 *   time       2022/6/8
 */
object Read {

    fun ReadNdef(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)!!
        val ndef = Ndef.get(tag) ?: return
        val type: String = ndef.type
        val maxSize = "${ndef.maxSize} byte"
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == action
        ) {
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            var msgs: Array<NdefMessage?>? = null
            var contentSize = 0
            if (rawMsgs != null) {
                msgs = arrayOfNulls(rawMsgs.size)
                for ((i, item) in rawMsgs.withIndex()) {
                    msgs[i] = item as NdefMessage
                    contentSize += (msgs[i]?.toByteArray()?.size!!)
                }

                //todo 读取文本数据
                if (Arrays.equals(IsNdefDataTypeUtil.IS(intent),
                        NdefRecord.RTD_TEXT)
                ) {
                    val text = ReadTextUtil.ReadTag(intent)
                }
                //todo 读取URL数据
                else if (Arrays.equals(IsNdefDataTypeUtil.IS(intent),
                        NdefRecord.RTD_URI)
                ) {
                    val url = ReadUriUtil.ReadNfcTag(intent)

                }//todo 读取其他数据
                else {
                    val other = ReadAppPackageUtil.ReadTag(intent)

                }

            }

        }
    }

}
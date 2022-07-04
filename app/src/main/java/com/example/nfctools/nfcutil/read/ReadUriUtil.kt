package com.example.nfcreadorwrite.utils.nfc_read

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter

import com.example.nfctools.nfcutil.UriPrefix
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and

/**
 *   created by sunLook
 *   time       2022/6/1
 */
object ReadUriUtil {


    fun ReadNfcTag(intent: Intent): String? {


        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action
        )  {
            val rawgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            var ndefMessage: Array<NdefMessage?>?=null
            var contentsize = 0
            if (rawgs != null) {
                if (rawgs.isNotEmpty()) {
                    ndefMessage = arrayOfNulls(rawgs.size)
                    for ((i ,item) in rawgs.withIndex()){
                        ndefMessage[i] = rawgs[i] as NdefMessage
                    }
                   // ndefMessage = rawgs[0] as NdefMessage
                    contentsize = ndefMessage[0]!!.toByteArray().size
                } else {
                    return null
                }

            }
            var ndefRecord = ndefMessage!![0]!!.records[0]
            val uri: Uri = parse(ndefRecord)!!

            return uri.toString()


        }else{
            return  null
        }
    }

    /**
     * 解析NdefRecord中Uri数据
     *
     * @param record
     * @return
     */
    fun parse(ndefRecord: NdefRecord): Uri? {
        var tnf: Short = ndefRecord.tnf
        var uri: Uri? = null
        if (tnf == NdefRecord.TNF_WELL_KNOWN) {
            uri = parseWellKnown(ndefRecord)

        } else if (tnf == NdefRecord.TNF_ABSOLUTE_URI) {
            uri = parseAbsolute(ndefRecord)
        }
        return uri
    }


    /**
     * 处理已知类型的Uri
     *
     * @param ndefRecord
     * @return
     */
    fun parseWellKnown(ndefRecord: NdefRecord): Uri? {
        if (!Arrays.equals(ndefRecord.type, NdefRecord.RTD_URI))
            return null


        //获取所有字节
        val payload: ByteArray = ndefRecord.payload
        val prefix = UriPrefix.URI_PREFIX_MAP[payload[0]]
        val prefixBytes = prefix!!.toByteArray(Charset.forName(if ((payload[0] and 0x80.toByte()).toInt() == 0) "UTF-8" else "UTF-16"))
        val fullUri = ByteArray(prefixBytes.size + payload.size - 1)
        System.arraycopy(prefixBytes, 0, fullUri, 0, prefixBytes.size)
        System.arraycopy(payload, 1, fullUri, prefixBytes.size, payload.size - 1)
        return Uri.parse(String(fullUri, Charset.forName(if ((payload[0] and 0x80.toByte()).toInt()== 0) "UTF-8" else "UTF-16")))
    }

    /**
     * 处理绝对的Uri
     * 没有Uri识别码，也就是没有Uri前缀，存储的全部是字符串
     *
     * @param ndefRecord 描述NDEF信息的一个信息段，一个NdefMessage可能包含一个或者多个NdefRecord
     * @return
     */
    fun parseAbsolute(ndefRecord: NdefRecord): Uri {

        //获取所有的字节数据
        val payload = ndefRecord.payload
        val uri = Uri.parse(String(payload, Charset.forName("UTF-8")))
        return uri
    }

}
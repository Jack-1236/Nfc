package com.example.nfcreadorwrite.utils.nfc_read

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.util.Log
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and

/**
 *   created by sunLook
 *   time       2022/6/1
 */
object ReadTextUtil {


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
        //判断数据是否为NDEF格式
        if (ndefRecord.tnf != NdefRecord.TNF_WELL_KNOWN) return null
        if (!Arrays.equals(ndefRecord.type, NdefRecord.RTD_TEXT)) return null
        try {
            //获得字节数组，解析
            val payload: ByteArray = ndefRecord.payload
            //下面开始NDEF文本数据第一个字节，状态字节
            //判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            //其他位都是0，所以进行"位与"运算后就会保留最高位
            //下面开始NDEF文本数据第一个字节，状态字节
            //判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            //其他位都是0，所以进行"位与"运算后就会保留最高位
            val textEncoding =
                (if ((payload[0] and 0x80.toByte()).toInt() == 0) "UTF-8" else "UTF-16").toString()
            //3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            //3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            val languageCodeLength: Int = (payload[0] and 0x3f).toInt()
            //下面开始NDEF文本数据第二个字节，语言编码
            //获得语言编码
            val languageCode = String(payload, 1, languageCodeLength, Charset.forName("US-ASCII"))
            //下面开始NDEF文本数据后面的字节，解析出文本
            return String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1,
                Charset.forName(textEncoding))

        } catch (e: Exception) {
            Log.e("tag", e.message.toString())
            return null
        }


    }


}
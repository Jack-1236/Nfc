package com.example.nfctools.nfcutil

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

/**
 *   created by sunLook
 *   time       2022/6/17
 */
class NfcWriteTools(private var coroutineScope: CoroutineScope, private var activity: Activity) :
    NfcWrite {
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var teachLis: Array<Array<String>>


    override fun ResetCard(intent: Intent, content: Context): Boolean {
        var isresult = false
        var blockIndex: Int = 0
        val lastSectorTrailer = byteArrayOf(
            -1, -1, -1, -1, -1, -1,
            -1, 7, -128, -68, -1, -1, -1, -1, -1, -1
        )
        val emptyBlock =
            byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val normalSectorTrailer = byteArrayOf(
            -1, -1, -1, -1, -1, -1,
            -1, 7, -128, 105, -1, -1, -1, -1, -1, -1
        )
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val mifare = MifareClassic.get(tag)
        try {
            mifare.connect()
            if (mifare.isConnected) {
                if (mifare.size != MifareClassic.SIZE_4K) {
                    for (i in 0 until mifare.sectorCount) {
                        var isAuthenticated = false
                        if (mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT)) {
                            isAuthenticated = true
                            Log.e("验证成功", "KEY_DEFAULT $i")
                        } else if (mifare.authenticateSectorWithKeyA(i,
                                MifareClassic.KEY_NFC_FORUM)
                        ) {
                            isAuthenticated = true
                            Log.e("验证成功", "KEY_NFC_FORUM $i")
                        } else if (mifare.authenticateSectorWithKeyA(
                                i,
                                MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY
                            )
                        ) {
                            isAuthenticated = true
                            Log.e("验证成功", "KEY_MIFARE_APPLICATION_DIRECTORY $i")
                        } else {
                            Log.e("验证失败", "")
                        }
                        if (mifare.authenticateSectorWithKeyB(i, MifareClassic.KEY_DEFAULT)) {

                            isAuthenticated = true
                            Log.e("验证成功", "KEY_DEFAULT $i")
                        } else if (mifare.authenticateSectorWithKeyB(i,
                                MifareClassic.KEY_NFC_FORUM)
                        ) {
                            isAuthenticated = true
                            Log.e("验证成功", "KEY_NFC_FORUM $i")
                        } else if (mifare.authenticateSectorWithKeyB(
                                i,
                                MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY
                            )
                        ) {
                            isAuthenticated = true
                            Log.e("验证成功", "KEY_MIFARE_APPLICATION_DIRECTORY $i")
                        } else {
                            Log.e("验证失败", "")
                        }

                        if (isAuthenticated) {
                            //  var blockIndex = mifare.sectorToBlock(i)
                            for (j in 0 until mifare.getBlockCountInSector(i)) {
                                //第0扇区第0块装的卡片的uid和厂商信息，不需要重写
                                if (i == 0) {
                                    if (j == 0) {
                                    } else if (j == 1 || j == 2) {
                                        mifare.writeBlock(blockIndex, emptyBlock)

                                    } else {
                                        mifare.writeBlock(blockIndex, normalSectorTrailer)
                                    }
                                    blockIndex++
                                } else {
                                    if (j == mifare.getBlockCountInSector(i) - 1) {
                                        mifare.writeBlock(blockIndex, normalSectorTrailer)
                                    } else {
                                        mifare.writeBlock(blockIndex, emptyBlock)
                                    }
                                    blockIndex++
                                }


                            }
                            Log.e("结束", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")


                        } else {
                            coroutineScope.launch(Dispatchers.Main) {
                                Toast.makeText(content,
                                    "卡已被加密，无法格式化",
                                    Toast.LENGTH_LONG).show()
                            }

                        }
                    }
                    mifare.close()
                    isresult = true
                } else {
                    coroutineScope.launch(Dispatchers.Main) {
                        Toast.makeText(content,
                            "当前卡片内存无法进行格式化,当前卡内存:${MifareClassic.SIZE_4K}KB",
                            Toast.LENGTH_LONG).show()
                    }


                }
            } else {
                coroutineScope.launch(Dispatchers.Main) {
                    Toast.makeText(content,
                        "当前卡片为只读，无法格式化",
                        Toast.LENGTH_LONG).show()
                }


            }
        } catch (e: Exception) {
            mifare.close()
            Log.e(javaClass.name, e.message.toString())

            return false
        }
        return isresult


    }

    override fun WriteCard(tag: Tag, ndefRecord: NdefRecord): Boolean {
        try {

            val ndefRecoard: Array<NdefRecord> = arrayOf(ndefRecord)
            val ndefMessage = NdefMessage(ndefRecoard)
            val ndef = Ndef.get(tag)
            val ndefFormat = NdefFormatable.get(tag)


            if (ndef != null) {
                if (!ndef.isWritable) {
                    coroutineScope.launch(Dispatchers.Main) {
                        Toast.makeText(activity, "当前NFC卡为只读卡", Toast.LENGTH_LONG).show()
                    }
                    return false
                } else {
                    return if (ndef.maxSize > ndefMessage.toByteArray().size) {

                        ndef.connect()//连接
                        EraseNdef(ndef)
                        ndef.writeNdefMessage(ndefMessage)
                        ndef.close()
                        true
                    } else {
                        coroutineScope.launch(Dispatchers.Main) {
                            Toast.makeText(activity, "数据过大无法写入", Toast.LENGTH_LONG).show()
                        }

                        false
                    }
                }
            } else if (ndefFormat != null) {
                ndefFormat.connect()
                //WriteFormat.FormatCard(tag, ndefFormat)
                ndefFormat.format(ndefMessage)
                ndefFormat.close()
                return true

            }

        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            return false

        }
        return false
    }

    override fun TypeUrl(uri: String): NdefRecord {
        var uriStr: String? = null
        var prefix: Byte? = null
        for ((key, item) in UriPrefix.URI_PREFIX_MAP) {
            var prefistr: String? = UriPrefix.URI_PREFIX_MAP[key]?.toLowerCase()
            if ("" == prefistr) {
                continue
            }
            if (uri.toLowerCase().startsWith(prefistr!!)) {
                prefix = key
                uriStr = uri.substring(prefistr.length)

                break
            }
        }
        Log.e("tag", "${uriStr?.length} ${uriStr}")
        val date: ByteArray = ByteArray(1 + uriStr!!.length)
        date[0] = prefix!!
        System.arraycopy(uriStr.toByteArray(), 0, date, 1, uriStr.length)
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, ByteArray(0), date)

    }

    override fun TypeText(text: String): NdefRecord {

        val langBytes = Locale.CHINA.language.toByteArray(Charset.forName("US-ASCII"))
        val utfEncoding = Charset.forName("UTF-8")
        //将文本转换为UTF-8格式
        //将文本转换为UTF-8格式
        val textBytes = text.toByteArray(utfEncoding)
        //设置状态字节编码最高位数为0
        //设置状态字节编码最高位数为0
        val utfBit = 0
        //定义状态字节
        //定义状态字节
        val status = (utfBit + langBytes.size).toChar()
        val data = ByteArray(1 + langBytes.size + textBytes.size)
        //设置第一个状态字节，先将状态码转换成字节
        //设置第一个状态字节，先将状态码转换成字节
        data[0] = status.code.toByte()
        //设置语言编码，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1到langBytes.length的位置
        //设置语言编码，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1到langBytes.length的位置
        System.arraycopy(langBytes, 0, data, 1, langBytes.size)
        //设置文本字节，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1 + langBytes.length
        //到textBytes.length的位置
        //设置文本字节，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1 + langBytes.length
        //到textBytes.length的位置
        System.arraycopy(textBytes, 0, data, 1 + langBytes.size, textBytes.size)
        //通过字节传入NdefRecord对象
        //NdefRecord.RTD_TEXT：传入类型 读写

        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), data)

    }

    override fun TypeVcard(
        name: String,
        phoneNumber: String,
        email: String,
        post: String,
        company: String,
        uri: String,
        uri2: String,
    ): NdefRecord {
        val msg = "BEGIN:VCARD\n" +
                "VERSION:3.0\n" +
                "FN:${name}\n" +
                "TEL;CELL:${phoneNumber}\n" +
                "EMAIL;WORK:${email}\n" +
                "ORG:${company}\n" +
                "TITLE:${post}\n" +
                "URL:${uri}\n" +
                "URL:${uri2}\n" +
                "END:VCARD"

        val vCardDataBytes = msg.toByteArray(Charset.forName("UTF-8"))
        val vCardPlayLoad = ByteArray(vCardDataBytes.size + 1)
        System.arraycopy(vCardDataBytes, 0, vCardPlayLoad, 1, vCardDataBytes.size)
        return NdefRecord(NdefRecord.TNF_MIME_MEDIA,
            "text/vcard".toByteArray(), ByteArray(0), vCardPlayLoad)
    }

    override fun TypeOpenApp(packName: String): NdefRecord {
        return NdefRecord.createApplicationRecord(packName)
    }

    @SuppressLint("InlinedApi")
    override fun init(activity: Activity) {

        pendingIntent = PendingIntent.getActivity(activity,
            0,
            Intent(activity, activity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE)
        val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val tag = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        tag.addCategory(Intent.CATEGORY_DEFAULT)
        intentFilters = arrayOf(tech, ndef, tag)
        teachLis = arrayOf(arrayOf(MifareClassic::class.java.name),
            arrayOf(NfcA::class.java.name),
            arrayOf(IsoDep::class.java.name),
            arrayOf(NfcB::class.java.name),
            arrayOf(NfcF::class.java.name),
            arrayOf(NfcV::class.java.name),
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name),
            arrayOf(MifareUltralight::class.java.name))// 允许扫描的标签类型


    }

    override fun getPendingIntent(): PendingIntent {
        return pendingIntent
    }

    override fun setPeningIntent(pendingIntent: PendingIntent) {
        this.pendingIntent = pendingIntent
    }

    override fun getIntentFilters(): Array<IntentFilter> {
        return intentFilters
    }

    override fun setIntentFilters(intentFilters: Array<IntentFilter>) {
        this.intentFilters = intentFilters
    }

    override fun getTeachList(): Array<Array<String>> {
        return teachLis
    }

    override fun setTeachList(teachs: Array<Array<String>>) {
        this.teachLis = teachs
    }

    override fun ReadCardUID(intent: Intent): String? {

        val formatter = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")
        val curdate = Date(System.currentTimeMillis())


        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val bytes = tag?.id
        val stringBuilder = StringBuilder()
        if (bytes == null || bytes.size <= 0) {
            return null
        }
        val buffer: CharArray = CharArray(2)
        for ((i, item) in bytes.withIndex()) {
            buffer[0] = Character.forDigit(bytes.get(i).toInt() ushr 4 and 0x0F, 16)

            buffer[1] = Character.forDigit(bytes.get(i).toInt() and 0x0F, 16)
            stringBuilder.append(buffer)
        }

        return stringBuilder.toString()
    }

    override fun EraseNdef(ndef: Ndef) {
        val byte = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val emptyrecord = NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)
        val emptyArray = arrayOf(emptyrecord)
        val emptyMsg = NdefMessage(emptyArray)
        ndef.writeNdefMessage(emptyMsg)
    }
}
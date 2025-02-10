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
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

/**
 *   created by sunLook
 *   time       2022/6/17
 */
class NfcWriteTools : NfcWrite {
    companion object {
        private const val TAG = "NFCJar"
    }

    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var teachLis: Array<Array<String>>


    override fun resetCard(intent: Intent): Boolean {
        var isresult = false
        var blockIndex = 0
        val lastSectorTrailer = byteArrayOf(
            -1, -1, -1, -1, -1, -1,
            -1, 7, -128, -68, -1, -1, -1, -1, -1, -1
        )
        val emptyBlock = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
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
                            Log.e(TAG, "KEY_DEFAULT $i")
                        } else if (mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_NFC_FORUM)) {
                            isAuthenticated = true
                            Log.e(TAG, "KEY_NFC_FORUM $i")
                        } else if (mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                            isAuthenticated = true
                            Log.e(TAG, "KEY_MIFARE_APPLICATION_DIRECTORY $i")
                        } else {
                            Log.e(TAG, "")
                        }
                        if (mifare.authenticateSectorWithKeyB(i, MifareClassic.KEY_DEFAULT)) {
                            isAuthenticated = true
                            Log.e(TAG, "KEY_DEFAULT $i")
                        } else if (mifare.authenticateSectorWithKeyB(i, MifareClassic.KEY_NFC_FORUM)) {
                            isAuthenticated = true
                            Log.e(TAG, "KEY_NFC_FORUM $i")
                        } else if (mifare.authenticateSectorWithKeyB(i, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                            isAuthenticated = true
                            Log.e(TAG, "KEY_MIFARE_APPLICATION_DIRECTORY $i")
                        } else {
                            Log.e(TAG, "")
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
                            Log.e(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")

                        } else {
                            Log.d(TAG, "卡已被加密，无法格式化")
                        }
                    }
                    mifare.close()
                    isresult = true
                } else {
                    Log.d(TAG, "当前卡片内存无法进行格式化,当前卡内存:${MifareClassic.SIZE_4K}KB")
                }
            } else {
                Log.d(TAG, "当前卡片为只读，无法格式化")
            }
        } catch (e: Exception) {
            mifare.close()
            Log.e(TAG, e.message.toString())

            return false
        }
        return isresult


    }

    override fun writeCard(tag: Tag, ndefRecord: NdefRecord): Boolean {
        try {

            val ndefRecoard: Array<NdefRecord> = arrayOf(ndefRecord)
            val ndefMessage = NdefMessage(ndefRecoard)
            val ndef = Ndef.get(tag)
            val ndefFormat = NdefFormatable.get(tag)


            if (ndef != null) {
                if (!ndef.isWritable) {
                    Log.d(TAG, "当前NFC卡为只读卡")
                    return false
                } else {
                    return if (ndef.maxSize > ndefMessage.toByteArray().size) {

                        ndef.connect()//连接
                        eraseNdef(ndef)
                        ndef.writeNdefMessage(ndefMessage)
                        ndef.close()
                        true
                    } else {
                        Log.d(TAG, "数据过大无法写入")
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

    override fun typeUrl(uri: String): NdefRecord {
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

    override fun typeText(text: String): NdefRecord {

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

    override fun typeVcard(
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
        return NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            "text/vcard".toByteArray(), ByteArray(0), vCardPlayLoad
        )
    }

    override fun typeOpenApp(packName: String): NdefRecord {
        return NdefRecord.createApplicationRecord(packName)
    }

    @SuppressLint("InlinedApi")
    override fun init(context: Context) {
        pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, context::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
        val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val tag = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        tag.addCategory(Intent.CATEGORY_DEFAULT)
        intentFilters = arrayOf(tech, ndef, tag)
        teachLis = arrayOf(
            arrayOf(MifareClassic::class.java.name),
            arrayOf(NfcA::class.java.name),
            arrayOf(IsoDep::class.java.name),
            arrayOf(NfcB::class.java.name),
            arrayOf(NfcF::class.java.name),
            arrayOf(NfcV::class.java.name),
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name),
            arrayOf(MifareUltralight::class.java.name)
        )// 允许扫描的标签类型


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

    override fun setTeachList(teaches: Array<Array<String>>) {
        this.teachLis = teaches
    }

    override fun readCardUID(intent: Intent): String? {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val bytes = tag?.id
        val stringBuilder = StringBuilder()
        if (bytes == null || bytes.isEmpty()) {
            return null
        }
        val buffer = CharArray(2)
        for ((i, item) in bytes.withIndex()) {
            buffer[0] = Character.forDigit(bytes[i].toInt() ushr 4 and 0x0F, 16)
            buffer[1] = Character.forDigit(bytes[i].toInt() and 0x0F, 16)
            stringBuilder.append(buffer)
        }

        return stringBuilder.toString().uppercase(Locale.ROOT)
    }


    override fun readCardContent(intent: Intent): String? {
        intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.let { tag ->
            val techList = tag.techList
            Log.d(TAG, "检测到 NFC 设备，支持的协议：${techList.joinToString()}")
            return readNdef(tag)
        }
        return null
    }

    override fun eraseNdef(ndef: Ndef) {
        val byte = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val emptyrecord = NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)
        val emptyArray = arrayOf(emptyrecord)
        val emptyMsg = NdefMessage(emptyArray)
        ndef.writeNdefMessage(emptyMsg)
    }


    private fun readNfcA(tag: Tag) {
        val nfcA = NfcA.get(tag)
        nfcA.use {
            it.connect()
            val atqa = it.atqa.joinToString("") { byte -> "%02X".format(byte) }
            val sak = it.sak
            Log.d(TAG, "NFC-A 读取成功，ATQA: $atqa, SAK: $sak")
        }
    }

    private fun readNfcB(tag: Tag) {
        val nfcB = NfcB.get(tag)
        nfcB.use {
            it.connect()
            val applicationData = it.applicationData?.joinToString("") { byte -> "%02X".format(byte) }
            val protocolInfo = it.protocolInfo?.joinToString("") { byte -> "%02X".format(byte) }
            Log.d(TAG, "NFC-B 读取成功，ApplicationData: $applicationData, ProtocolInfo: $protocolInfo")
        }
    }

    private fun readNfcF(tag: Tag) {
        val nfcF = NfcF.get(tag)
        nfcF.use {
            it.connect()
            val manufacturer = it.manufacturer?.joinToString("") { byte -> "%02X".format(byte) }
            Log.d("NFC", "NFC-F 读取成功，Manufacturer: $manufacturer")
        }
    }

    private fun readNfcV(tag: Tag) {
        val nfcV = NfcV.get(tag)
        nfcV.use {
            it.connect()
            val dsfid = nfcV.dsfId.toInt() // 数据存储格式标识符
            val responseFlags = nfcV.responseFlags.toInt() // 响应标志位
            val uid = nfcV.tag.id.joinToString("") { byte -> "%02X".format(byte) }

            Log.d(TAG, "NfcV 读取成功")
            Log.d(TAG, "UID: $uid")
            Log.d(TAG, "DSFID: $dsfid")
            Log.d(TAG, "Response Flags: $responseFlags")
        }
    }

    private fun readIsoDep(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        isoDep.use {
            it.connect()
            val selectCommand = byteArrayOf(
                0x00.toByte(),
                0xA4.toByte(),
                0x04.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0xA0.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x86.toByte(),
                0x98.toByte(),
                0x07.toByte()
            )
            val response = it.transceive(selectCommand)
            Log.d(TAG, "ISO-DEP 读取成功，返回数据: ${response.joinToString("") { byte -> "%02X".format(byte) }}")
        }
    }

    private fun readNdef(tag: Tag): String {
        val ndef = Ndef.get(tag)
        val builder = StringBuilder()
        ndef?.use {
            it.connect()
            val ndefMessage = it.cachedNdefMessage
            ndefMessage?.records?.let { records ->
                records.forEach { record ->
                    // 读取文本记录 (RTD_TEXT)
                    if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                        val payload = record.payload
                        val languageCodeLength = payload[0].toInt() // 第一个字节表示语言代码长度
                        val languageCode = String(payload, 1, languageCodeLength, Charsets.US_ASCII) // 获取语言代码
                        val text = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charsets.UTF_8) // 获取文本内容
                        Log.d(TAG, "NDEF Text记录，语言代码: $languageCode, 内容: $text")
                        builder.append(text)
                    }

                    // 读取 URL 记录 (RTD_URI)
                    else if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI)) {
                        val payload = record.payload
                        val url = String(payload, Charsets.UTF_8)
                        Log.d(TAG, "NDEF URL记录，内容: $url")
                        builder.append(url)
                    }

                    // 读取 MIME 类型记录
                    else if (record.tnf == NdefRecord.TNF_MIME_MEDIA && record.type.contentEquals("application/json".toByteArray())) {
                        val payload = record.payload
                        val jsonString = String(payload, Charsets.UTF_8)
                        Log.d(TAG, "NDEF MIME记录，内容: $jsonString")
                        builder.append(jsonString)
                    }

                    // 读取其他自定义数据记录
                    else {
                        val payload = record.payload
                        val customData = String(payload, Charsets.UTF_8)
                        Log.d(TAG, "自定义数据记录，内容: $customData")
                        builder.append(customData)
                    }
                }
            }
        } ?: Log.e(TAG, "NDEF 读取失败")
        return builder.toString()
    }

    private fun readMifareClassic(tag: Tag) {
        val mifare = MifareClassic.get(tag)
        mifare.use {
            it.connect()
            if (it.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT)) {
                val data = it.readBlock(0)
                val cardInfo = String(data, Charsets.UTF_8)
                Log.d(TAG, "Mifare Classic 读取成功，数据: $cardInfo")
            }
        }
    }

    private fun readMifareUltralight(tag: Tag) {
        val mifareUl = MifareUltralight.get(tag)
        mifareUl.use {
            it.connect()
            val page0 = it.readPages(0)
            Log.d(TAG, "Mifare Ultralight 读取成功，Page 0: ${page0.joinToString("") { byte -> "%02X".format(byte) }}")
        }
    }


}
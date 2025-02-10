package com.example.nfctools.nfcutil

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef

/**
 *   created by sunLook
 *   time       2022/6/17
 */
interface NfcWrite {

    //重置nfc卡片，恢复出厂格式
    fun resetCard(intent: Intent ): Boolean

    //写入数据
    fun writeCard(tag: Tag, ndefRecord: NdefRecord): Boolean


    //写入uri类型数据
    fun typeUrl(uri: String): NdefRecord

    //写入文本类型数据
    fun typeText(text: String): NdefRecord

    //写入电子名片
    fun typeVcard(name: String, phoneNumber: String, email: String, post: String, company: String, uri: String, uri2: String): NdefRecord


    //读取nfc卡片的uid
    fun readCardUID(intent: Intent): String?

    fun readCardContent(intent: Intent): String?


    //擦除数据格式为ndef类型卡的数据
    fun eraseNdef(ndef: Ndef)

    //写入app包名打开app
    fun typeOpenApp(packName: String): NdefRecord

    //初始化
    fun init(context: Context)


    fun getPendingIntent(): PendingIntent
    fun setPeningIntent(pendingIntent: PendingIntent)

    fun getIntentFilters(): Array<IntentFilter>
    fun setIntentFilters(intentFilters: Array<IntentFilter>)

    fun getTeachList(): Array<Array<String>>
    fun setTeachList(teaches: Array<Array<String>>)


}
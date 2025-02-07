# AndroidManifest.xml
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example.app">
    <uses-sdk android:minSdkVersion="10" />
    <uses-feature android:name="android.hardware.nfc" android:required="true" />
    <uses-permission android:name="android.permission.NFC" />

    <activity android:launchMode="singleTop">
        <intent-filter>
            <action android:name="android.nfc.action.NDEF_DISCOVERED" />
            <data android:mimeType="text/plain" />
            <data android:scheme="http" />
            <data android:scheme="https" />
        </intent-filter>

        <intent-filter>
            <action android:name="android.nfc.action.TECH_DISCOVERED" />
        </intent-filter>

        <meta-data android:name="android.nfc.action.TECH_DISCOVERED"
            android:resource="@xml/nfc_tech_filter" />

        <intent-filter>
            <action android:name="android.nfc.action.TAG_DISCOVERED" />
            <category android:name="android.intent.category.DEFAULT" />
        </intent-filter>
    </activity>
</manifest>
```

# Activity.ktx
```kotlin
   private var nfcTools: NfcWriteTools? = null

   private var nfcAdapter: NfcAdapter? = null

   fun onCreate(savedInstanceState:Bundle) {
      super.onCreate(savedInstanceState)
      nfcTools?.ReadCardUID(intent)
            ?.let { sendReadNfcMessageToWeb(it)
                MLog . d (this, "Read NFC Message is $it.(onCreate)") }
            ?: MLog.d(this, "Not found read nfc message.(onCreate)")
    }

   fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcTools?.ReadCardUID(intent)
            ?.let {
              sendReadNfcMessageToWeb(it)
             MLog.d(this, "Read NFC Message is $it.(onNewIntent)")
         }
         ?: MLog.d(this, "Not found read nfc message.(onNewIntent)")
   }

  fun onResume() {
        super.onResume()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter?.let {
            it.apply {
                //todo 检查设备是否支持安全的NFC功能
                if (!isSecureNfcSupported()) {
                    if (isEnabled()) {
                        MLog.i(this, "该设备支持NFC功能 ${isEnabled()}")
                        nfcTools =
                            NfcWriteTools(CoroutineScope(EmptyCoroutineContext), this@MainActivity)
                        nfcTools!!.init(this@MainActivity)
                        enableForegroundDispatch(
                            this@MainActivity,
                            nfcTools!!.getPendingIntent(),
                            nfcTools!!.getIntentFilters(),
                            nfcTools!!.getTeachList()
                        )
                        MLog.i(this, "该设备的NFC功能已准备完成")
                    } else {
                        MLog.e(this, "该设备未打开NFC功能")
                        startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                    }
                } else {
                    MLog.e(this, "该设备不支持NFC功能")
                }
            }
        }
  }

  fun onPause() {
    super.onPause()
    nfcAdapter?.disableForegroundDispatch(this) 
  }
```

# xml Folders(create new xml file)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
    <!--s处理所有android NFC支持的模型-->
    <tech-list>
        <tech>android.nfc.tech.IsoDep</tech>
        <tech>android.nfc.tech.NfcA</tech>
        <tech>android.nfc.tech.NfcB</tech>
        <tech>android.nfc.tech.NfcF</tech>
        <tech>android.nfc.tech.NfcV</tech>
        <tech>android.nfc.tech.Ndef</tech>
        <tech>android.nfc.tech.NdefFormatable</tech>
        <tech>android.nfc.tech.MifareClassic</tech>
        <tech>android.nfc.tech.MifareUltralight</tech>
    </tech-list>
</resources>
```
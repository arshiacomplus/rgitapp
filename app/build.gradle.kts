// * DataStore for saving proxy settings
implementation("androidx.datastore:datastore-preferences:1.0.0")
// * OkHttp for network and proxy routing
implementation("com.squareup.okhttp3:okhttp:4.12.0")
// * Zip4j for split zip extraction
implementation("net.lingala.zip4j:zip4j:2.11.5")

android {
    // ... other configs ...

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}
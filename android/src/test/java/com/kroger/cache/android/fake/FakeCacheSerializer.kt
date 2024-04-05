package com.kroger.cache.android.fake

import com.kroger.cache.CacheSerializer

internal class FakeCacheSerializer : CacheSerializer<String> {
    var readCalled = false
    var writeCalled = false

    override fun decodeFromString(bytes: ByteArray?): String? {
        readCalled = true
        return if (bytes == null || bytes.isEmpty()) {
            null
        } else {
            bytes.decodeToString()
        }
    }

    override fun toByteArray(data: String?): ByteArray {
        writeCalled = true
        return data.orEmpty().encodeToByteArray()
    }
}

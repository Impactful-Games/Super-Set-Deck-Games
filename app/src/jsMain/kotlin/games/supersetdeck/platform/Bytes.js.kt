package games.supersetdeck.platform

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

// On JS, a ByteArray is an Int8Array: a view on the buffer is enough.
actual fun JsAny.arrayBufferToByteArray(): ByteArray =
    Int8Array(unsafeCast<ArrayBuffer>()).unsafeCast<ByteArray>()

package games.supersetdeck.platform

import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.wasmMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator

private fun byteLength(buffer: JsAny): Int = js("buffer.byteLength")

// Copies the whole buffer into Wasm linear memory in one call, instead of one interop call per byte.
private fun copyToWasmMemory(buffer: JsAny, memory: JsAny, address: Int): Unit = js("""
    new Int8Array(memory.buffer, address, buffer.byteLength).set(new Int8Array(buffer))
""")

@OptIn(UnsafeWasmMemoryApi::class)
actual fun JsAny.arrayBufferToByteArray(): ByteArray {
    val size = byteLength(this)
    if (size == 0) return ByteArray(0)
    return withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(size)
        copyToWasmMemory(this, wasmMemory, pointer.address.toInt())
        ByteArray(size) { (pointer + it).loadByte() }
    }
}

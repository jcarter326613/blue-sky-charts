package com.blueskycharts.app.utility

import android.util.JsonReader
import android.util.JsonWriter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Serializable

/**
 * Keeps recently touched items at the front of the queue and allows popping of items not touched for a while
 */
class HashQueue<T: JsonSerializable> {
    private val keys = mutableMapOf<Int, Item<T>>()
    private var head: Item<T>? = null
    private var tail: Item<T>? = null
    private val movementMutex = Mutex()

    suspend fun touch(item: T) {
        val existingItem = find(item)
        if (existingItem != null) {
            bringToFront(existingItem)
        } else {
            add(item)
        }
    }

    suspend fun write(jsonWriter: JsonWriter) {
        movementMutex.withLock {
            jsonWriter.beginArray()
            var currentItem = head
            while ( currentItem != null ) {
                currentItem.value.write(jsonWriter)
                currentItem = currentItem.next
            }
            jsonWriter.endArray()
        }
    }

    suspend fun reverseIterator(): Iterator<T>{
        movementMutex.lock()
        return ReverseIterator<T>(tail)
    }

    fun removeItem(it: Iterator<T>) {
        it as ReverseIterator<T>
        val toRemove = it.previous ?: return
        removeItem(toRemove)
    }

    private fun removeItem(it: Item<T>) {
        keys.remove(it.hashCode())
        it.previous?.next = it.next
        it.next?.previous = it.previous

        if ( it == head ) {
            head = it.next
        }
        if ( it == tail ) {
            tail = it.previous
        }
    }

    fun freeIterator() {
        movementMutex.unlock()
    }

    private fun find(item: T): Item<T>? {
        val code = item.hashCode()
        return keys[code]
    }

    private suspend fun bringToFront(item: Item<T>) {
        if (item == head) {
            return
        }
        movementMutex.withLock {
            if (tail == item) {
                tail = item.previous
            }
            item.previous?.next = item.next
            item.next?.previous = item.previous

            item.next = head
            item.next?.previous = item
            head = item
        }
    }

    private suspend fun add(item: T) {
        movementMutex.withLock {
            addNoSuspend(item)
        }
    }

    private fun addNoSuspend(item: T) {
        val newItem = Item(item)
        newItem.next = head
        head?.previous = newItem
        head = newItem
        if ( tail == null ) {
            tail = newItem
        }
        keys[item.hashCode()] = newItem
    }

    private class ReverseIterator<T: JsonSerializable>(private var item: Item<T>?): Iterator<T> {
        var previous: Item<T>? = null

        override fun hasNext(): Boolean = item != null

        override fun next(): T {
            val toReturn = item
            item = item?.previous
            previous = toReturn
            return toReturn?.value ?: throw IndexOutOfBoundsException("Iterated past the end of the list")
        }
    }

    private data class Item<T>(val value: T) {
        var previous: Item<T>? = null
        var next: Item<T>? = null
    }

    companion object {
        fun <T: JsonSerializable> read(reader: JsonReader, createCall: ((reader: JsonReader)->T?)): HashQueue<T> {
            val retVal = HashQueue<T>()

            reader.beginArray()
            while ( reader.hasNext() ) {
                val newObj = createCall(reader)
                if ( newObj != null ) {
                    retVal.addNoSuspend(newObj)
                }
            }
            reader.endArray()

            return retVal
        }
    }
}
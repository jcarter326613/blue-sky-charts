package com.blueskycharts.app.map.utility

import java.util.*
import kotlin.collections.ArrayList

class PriorityArray<T> {
    private var content: ArrayList<LinkedList<T>> = ArrayList()
    private var size: Int = 0
    private var lowestPriority: Int = 0

    fun clear() {
        this.content = ArrayList();
        this.size = 0;
    }

    fun add(obj: T, priority: Int) {
        // Setup the priority and set the size of this object
        if ( this.size == 0 || this.lowestPriority > priority ) {
            this.lowestPriority = priority;
        }
        this.size++;
        while ( this.content.size <= priority ) {
            this.content.add(LinkedList<T>());
        }

        // Insert the object
        this.content[priority].add(obj);
    }

    fun pop(): T? {
        if ( this.size == 0 ) {
            return null;
        }
        val retVal = this.content[this.lowestPriority].removeFirst() ?: return null;

        // Adjust the lowest priority if we've used this one up
        while ( this.content[this.lowestPriority].size == 0 ) {
            this.lowestPriority++;
        }

        this.size--;
        return retVal;
    }

    fun size(): Int {
        return this.size;
    }

    fun getPriorityList(priority: Int): Collection<T> {
        return this.content[priority];
    }
}
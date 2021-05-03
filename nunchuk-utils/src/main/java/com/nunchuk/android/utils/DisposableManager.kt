package com.nunchuk.android.utils

interface Disposable {
    fun dispose()
}

class DisposableManager {

    private val tasks = HashSet<Disposable>()

    fun dispose() {
        tasks.forEach(Disposable::dispose)
        tasks.clear()
    }

    fun add(task: Disposable) {
        tasks.add(task)
    }

    companion object {
        val instance = InstanceHolder.instance
    }

    private object InstanceHolder {
        var instance = DisposableManager()
    }
}
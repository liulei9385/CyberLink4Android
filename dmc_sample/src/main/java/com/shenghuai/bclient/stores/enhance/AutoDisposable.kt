package com.shenghuai.bclient.stores.enhance

import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Created by liulei
 * 2020/1/8 13:47
 */
class AutoDisposable(owner: LifecycleOwner) {

    private var d: CompositeDisposable? = null

    init {
        d = CompositeDisposable()
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                d?.dispose()
                d = null
                owner.lifecycle.removeObserver(this)
            }
        })
    }

    fun addDisposable(disposable: Disposable) {
        d?.add(disposable)
    }

    fun dispAllDisposable() {
        d?.dispose()
    }
}

object DisposableUtils {
    private val map: ArrayMap<LifecycleOwner, AutoDisposable> = arrayMapOf()
    fun addDisposable(owner: LifecycleOwner, disposable: Disposable) {
        if (map.containsKey(owner)) {
            map[owner]?.addDisposable(disposable)
        } else {
            val v = AutoDisposable(owner)
            v.addDisposable(disposable)
            map[owner] = v
        }
    }

    fun dispose(disposable: Disposable?) {
        try {
            if (disposable != null && !disposable.isDisposed)
                disposable.dispose()
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

}
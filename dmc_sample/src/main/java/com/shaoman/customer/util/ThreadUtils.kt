package com.shaoman.customer.util

import androidx.lifecycle.Lifecycle
import com.shenghuai.bclient.stores.common.OnDestroyLifeObserver
import com.shenghuai.bclient.stores.enhance.DisposableUtils
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.internal.functions.Functions
import io.reactivex.schedulers.Schedulers

/**
 * Created by liulei
 * 2020/1/16 13:43
 * {描述这个类功能}
 */
object ThreadUtils {
    fun runMain(runnable: (() -> Unit)?) {
        val subscribe = Flowable.just(1)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                try {
                    runnable?.invoke()
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
            }, { ex -> ex.printStackTrace() })
    }

    fun runOnBackground(lifecycle: Lifecycle, runnable: (() -> Unit)?) {
        var disposable: Disposable? = null
        val onDestroyLifeObserver = OnDestroyLifeObserver {
            DisposableUtils.dispose(disposable)
        }
        disposable = Flowable.just(lifecycle)
            .map {
                runnable?.invoke()
                it
            }
            .doOnCancel {
                runMain { lifecycle.removeObserver(onDestroyLifeObserver) }
            }
            .doOnEach {
                if (it.value != null && it.value?.currentState != Lifecycle.State.DESTROYED) {
                    runMain { it.value?.removeObserver(onDestroyLifeObserver) }
                }
            }
            .compose {
                it.takeUntil { t -> t.currentState == Lifecycle.State.DESTROYED }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Functions.emptyConsumer(), Functions.emptyConsumer())
        lifecycle.addObserver(onDestroyLifeObserver)
    }

    fun runOnBackground(runnable: (() -> Unit)?): Disposable {
        return Flowable.just(1)
            .map {
                try {
                    runnable?.invoke()
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
                it
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Functions.emptyConsumer(), Functions.emptyConsumer())
    }

}
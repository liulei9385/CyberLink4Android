package com.shenghuai.bclient.stores.common

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

open class OnDestroyLifeObserver constructor(val destroyUnit: () -> Unit) : DefaultLifecycleObserver {

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        destroyUnit()
        owner.lifecycle.removeObserver(this)
    }

}
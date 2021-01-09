package com.shaoman.customer.model.entity.res

object UrlReplace {

    private const val keyObsEast = "obsSouthEastHostName"
    private const val keyObsCn = "obsCnHostName"
    private const val obsSouthEastHostName = "https://shaomantest.obs.ap-southeast-2.myhuaweicloud.com"
    private const val obsCnHostName = "https://shaoman.obs.cn-north-4.myhuaweicloud.com"

    fun urlReplace(url: String): String {
        var newUrl: String = url.replace(keyObsEast, obsSouthEastHostName)
        newUrl = newUrl.replace(keyObsCn, obsCnHostName)
        newUrl = forkVideoUrl(newUrl)
        return newUrl
    }

    fun urlSample(url: String): String {
        var newUrl: String = url.replace(obsSouthEastHostName, keyObsEast)
        newUrl = newUrl.replace(obsCnHostName, keyObsCn)
        return newUrl
    }

    private fun forkVideoUrl(url: String): String {
        if (url.endsWith("IMG_4322F807B661-6119-4BD3-911F-F3E041F318F2.mp4"))
            return "$obsCnHostName/videoExt/IMG_4322F807B661-6119-4BD3-911F-F3E041F318F2~1.mp4"
        if (url.endsWith("IMG_306710C34175-1B83-47AD-811B-D7B74EFDB702.mp4"))
            return "$obsCnHostName/videoExt/IMG_306710C34175-1B83-47AD-811B-D7B74EFDB702.mp4"
        return url
    }
}
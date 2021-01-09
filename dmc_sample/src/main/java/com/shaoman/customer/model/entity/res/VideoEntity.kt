package com.shaoman.customer.model.entity.res


class VideoEntity {

    var img: String? = null
    var url: String? = null
    var id = -1
    var vid: Int = -1

    fun getActualUrl(): String {
        val url = UrlReplace.urlReplace(url ?: "")
        this.url = url
        return url
    }

    fun getActualImgUrl(): String {
        val img = UrlReplace.urlReplace(img ?: "")
        this.img = img
        return img
    }

    override fun toString(): String {
        return "VideoEntity(" +
            "img=$img, " +
            "url=$url, " +
            "id=$id, " +
            "vid=$vid" +
            ")"
    }

}
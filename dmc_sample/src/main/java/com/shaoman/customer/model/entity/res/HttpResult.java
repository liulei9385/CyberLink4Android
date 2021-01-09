package com.shaoman.customer.model.entity.res;

/**
 * @author Richard Jia
 * @date 2017/9/27
 * @description Api base response data
 */

public class HttpResult<T> {

    private Integer status;//Status
    private String message;//Message
    private T data;//data

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "HttpResult{" +
            "status=" + status +
            ", message='" + message + '\'' +
            ", data=" + data +
            '}';
    }
}

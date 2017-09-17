package com.zero.common.enums;

/**
 * 状态码
 *
 * @author yezhaoxing
 * @date 2017/4/29
 */
public enum CodeEnum {

    /**
     * 用户未登录
     */
    NOT_LOGIN("403"),

    PARAM_NOT_MATCH("400"),

    PAGE_NOT_FOUND("404"),

    INTERNAL_SERVER_ERROR("500"),

    SUCCESS("000000");
    private String CodeEnum;

    private CodeEnum(String value) {
        this.CodeEnum = value;
    }

    public String getCodeEnum() {
        return CodeEnum;
    }

}

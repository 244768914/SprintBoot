package com.zero.common.vo;


import com.zero.common.enums.CodeEnum;
import com.zero.common.enums.StringEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 基础的返回类
 * 
 * @author yezhaoxing
 * @date 2017/4/29
 */
@ApiModel("基础返回vo对象")
public class BaseReturnVo {

    final static String SUCCESS_DEFAULT_DESC = "success";
    @ApiModelProperty("返回码")
    private String resCode;
    @ApiModelProperty("返回说明")
    private String resDes;

    public BaseReturnVo() {
    }

    public BaseReturnVo(StringEnum codeEnum, String msg) {
        this.resCode = codeEnum.getCodeEnum();
        this.resDes = msg;
    }

    public static BaseReturnVo success() {
        return new BaseReturnVo(CodeEnum.SUCCESS, SUCCESS_DEFAULT_DESC);
    }

    public String getResCode() {
        return resCode;
    }

    public String getResDes() {
        return resDes;
    }
}

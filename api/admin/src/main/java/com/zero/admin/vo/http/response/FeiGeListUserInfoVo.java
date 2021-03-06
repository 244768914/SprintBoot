package com.zero.admin.vo.http.response;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author yezhaoxing
 * @date 2017/11/02
 */
@Data
@ApiModel("飞鸽返回单个用户信息vo对象")
public class FeiGeListUserInfoVo {

    private Integer id;

    private String name;

    private String remark;
}

package com.test.show;

import lombok.Getter;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/13 11:21
 */
@Getter
public enum OperateType {

    ADDSHOW("addShow", "创建直播");

    private String type;

    private String description;

    OperateType(String type, String description) {
        this.type = type;
        this.description = description;
    }

}

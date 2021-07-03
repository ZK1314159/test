package com.test.entity;

import java.util.List;

import lombok.Data;

/**
 * 前端测试dto
 *
 * @author zengkai
 * Date: 2021/7/3 14:14
 */
@Data
public class FrontTestDTO {

    private Byte testByte;

    private Short testShort;

    private Integer integer;

    private Long testLong;

    private Float testFloat;

    private Double testDouble;

    private Boolean testBoolean;

    private Character character;

    private String string;

    private List<String> stringList;

}

package com.test.entity;

import lombok.Data;

/**
 * 源对象
 *
 * @author zengkai
 * Date: 2020/12/2 23:20
 */
@Data
public class SrcObject {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 姓名
     */
    private String name;

    /**
     * 爱好
     */
    private String hobby;

    /**
     * 家庭情况
     */
    private SrcFamily family;

    @Data
    public static class SrcFamily {

        /**
         * 父亲姓名
         */
        private String father;

        /**
         * 母亲姓名
         */
        private String mother;

        /**
         * 父亲年龄
         */
        private Integer fatherAge;
    }

}

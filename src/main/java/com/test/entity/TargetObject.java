package com.test.entity;

import lombok.Data;

/**
 * Description
 *
 * @author zengkai
 * Date: 2020/12/2 23:24
 */
@Data
public class TargetObject {

    /**
     * 主键
     */
    private Integer id;


    /**
     * 姓名
     */
    private String name;

    /**
     * 理想
     */
    private String dream;

    /**
     * 家庭情况
     */
    private Family family;

    @Data
    public static class Family {

        /**
         * 父亲姓名
         */
        private String father;

        /**
         * 母亲姓名
         */
        private String mother;

        /**
         * 家庭资产
         */
        private Integer money;
    }

}

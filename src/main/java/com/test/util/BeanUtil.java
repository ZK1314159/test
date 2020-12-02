package com.test.util;

import com.alibaba.fastjson.JSONObject;

/**
 * Bean操作工具类, 深度复制属性，可以复制内部类
 *
 * @author zengkai
 * Date: 2020/12/2 23:05
 */
public class BeanUtil {

    @SuppressWarnings("unchecked")
    public static <T> T copyProperties(Object source, T target) {
        String src = JSONObject.toJSONString(source);
        return (T) JSONObject.parseObject(src, target.getClass());
    }

    /**
     * 推荐使用
     */
    public static <T> T copyProperties(Object source, Class<T> target) {
        String src = JSONObject.toJSONString(source);
        return JSONObject.parseObject(src, target);
    }

}

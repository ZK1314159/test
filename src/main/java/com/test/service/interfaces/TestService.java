package com.test.service.interfaces;

/**
 * @Description
 * @Author zengkai
 * @Date 2020/10/7 17:34
 */
public interface TestService {

    void ymlPropertyTest();

    /**
     * 测试nacos
     */
    void nacosTest();


    public void retryTest(Integer count);
}
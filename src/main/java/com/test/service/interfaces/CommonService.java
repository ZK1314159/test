package com.test.service.interfaces;


/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/24 10:06 <br>
 */
public interface CommonService {

    String greet(String clientName);

    void serve() throws Exception;

    void walk();

    String print();

    void testProperty();
}

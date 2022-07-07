package com.test.show.chain;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/7/6 21:42
 */
@Component
@Order(3)
public class PushChannel extends AbstractChain {

    @Override
    void filter(ServletRequest request, ServletResponse response) {
        response.setContentType("false");
        System.out.println("SendToChannel");
    }

    @Override
    String getReason() {
        return "发送channel失败";
    }

}

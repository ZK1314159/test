package com.test.other.show.chain;

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
@Order(2)
public class PushWencai extends AbstractChain {

    @Override
    void filter(ServletRequest request, ServletResponse response) {
        System.out.println("SendToChannel");
    }

    @Override
    String getReason() {
        return "发送wencai失败";
    }

}

package com.test.config.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/5/5 15:21
 */
@Component("myFilter")
@Slf4j
@Order(1)
public class MyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//        log.info("start!!!");
        chain.doFilter(request, response);
//        log.info("end!!!");
    }

}

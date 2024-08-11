package com.test.other.show.chain;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/7/6 21:42
 */
public abstract class AbstractChain {

    abstract void filter(ServletRequest request, ServletResponse response);

    abstract String getReason();

}

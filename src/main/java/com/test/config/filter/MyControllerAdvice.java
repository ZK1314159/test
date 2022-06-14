package com.test.config.filter;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/5/23 22:23
 */
//@ControllerAdvice
public class MyControllerAdvice {

    @ExceptionHandler(RuntimeException.class)
    public ModelAndView runtimeException(RuntimeException e) {
        e.printStackTrace();
        return new ModelAndView("error");
    }

}

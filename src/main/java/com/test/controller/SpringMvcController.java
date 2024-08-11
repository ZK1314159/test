package com.test.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import com.test.service.interfaces.CommonService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/3/27 17:30 <br>
 */
@RequestMapping("/springMvc")
public class SpringMvcController {

    @Autowired
    private CommonService commonService;

}

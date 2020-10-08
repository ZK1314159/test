package com.test.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.test.service.SpelService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/4/24 17:28 <br>
 */
@RestController
@RequestMapping("/spel")
public class SpelController {

    @Autowired
    private SpelService spelService;

    @RequestMapping(value = "modify", method = RequestMethod.GET)
    void spelTest() {
        spelService.spelTest();
    }
}

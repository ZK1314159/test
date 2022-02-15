package com.test.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.test.service.TransactionService;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/4/25 11:17 <br>
 */
@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @RequestMapping(value = "/annotation", method = RequestMethod.GET)
    void transactionTest() throws Exception {
        transactionService.transactionTest();
    }

    @RequestMapping(value = "/distribute", method = RequestMethod.GET)
    void distributeTest() throws Exception {
        transactionService.distribute();
    }

}

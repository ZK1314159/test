package com.test.service;

/**
 * Description：<br>
 *
 * @author zeng.kai <br>
 * CreateDate：2020/4/25 11:15 <br>
 */
public interface TransactionService {

    void transactionTest() throws Exception;

    void distribute() throws Exception;

    void propagate(boolean exception) throws Exception;

}

package com.test.service;

import com.test.entity.Show;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/11 22:39
 */
public interface OperationFlowServiceNew {

    void operate(String type, Show show);

    public void pushChannel(Show show);

    public void pushWencai(Show show);

    public void deleteCache(Show show);
}

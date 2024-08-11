package com.test.other.show.decorator;

import com.test.entity.Show;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/13 17:14
 */
public class PushWencai extends BackOperateFlow {

    private BackOperateFlow backOperateFlow;

    @Override
    void setBackOperateFlow(BackOperateFlow backOperateFlow) {
        this.backOperateFlow = backOperateFlow;
    }

    @Override
    void operate(Show show) {
        backOperateFlow.operate(show);
        myOperate(show);
    }

    private void myOperate(Show show) {
        System.out.println("PushWencai");
    }

}

package com.test.show.decorator;

import com.test.entity.Show;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/13 17:16
 */
public class DeleteCache extends BackOperateFlow {

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
        System.out.println("DeleteCache");
    }

}

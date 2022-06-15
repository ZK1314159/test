package com.test.impl.thread_pool;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.test.entity.thread_pool.TaskB;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/6/15 16:23
 */
public class TaskBThread implements Runnable {

    List<Integer> idList;

    private ConcurrentHashMap<Integer, TaskB> taskBMap;

    CountDownLatch countDownLatch2;

    TaskBHandleBean taskBHandleBean;

    public TaskBThread(List<Integer> idList, ConcurrentHashMap<Integer, TaskB> taskBMap,
            CountDownLatch countDownLatch2, TaskBHandleBean taskBHandleBean) {
        this.countDownLatch2 = countDownLatch2;
        this.idList = idList;
        this.taskBMap = taskBMap;
        this.taskBHandleBean = taskBHandleBean;
    }

    @Override
    public void run() {
        List<TaskB> taskBList = taskBHandleBean.doWork(idList);
        for (TaskB taskB : taskBList) {
            taskBMap.put(taskB.getId(), taskB);
        }
        countDownLatch2.countDown();
    }

}

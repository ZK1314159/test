package com.test.impl.thread_pool;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.test.entity.thread_pool.TaskC;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/6/15 16:23
 */
@Component
public class TaskCThread {
    
    @Autowired
    private TaskCHandleBean taskCHandleBean;

    @Async("taskExecutor")
    public void run(List<Integer> idList, ConcurrentHashMap<Integer, TaskC> taskCMap, CountDownLatch countDownLatch2) {
        List<TaskC> taskBList = taskCHandleBean.doWork(idList);
        for (TaskC taskC : taskBList) {
            taskCMap.put(taskC.getId(), taskC);
        }
        countDownLatch2.countDown();
    }

}

package com.test.impl.thread_pool;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.test.entity.thread_pool.TaskB;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/6/15 16:23
 */
@Component
public class TaskBHandleBean {

    public List<TaskB> doWork(List<Integer> idList) {
        List<TaskB> taskBList = new ArrayList<>();
        for (Integer integer : idList) {
            TaskB taskB = new TaskB();
            taskB.setId(integer);
            taskB.setHobby(integer + "_" + UUID.randomUUID().toString());
            taskBList.add(taskB);
        }
        return taskBList;
    }

}

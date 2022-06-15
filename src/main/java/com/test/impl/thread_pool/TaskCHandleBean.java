package com.test.impl.thread_pool;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.test.entity.thread_pool.TaskC;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/6/15 16:23
 */
@Component
public class TaskCHandleBean {

    public List<TaskC> doWork(List<Integer> idList) {
        List<TaskC> taskBList = new ArrayList<>();
        for (Integer integer : idList) {
            TaskC taskC = new TaskC();
            taskC.setId(integer);
            taskC.setGender(integer + "_" + UUID.randomUUID().toString());
            taskBList.add(taskC);
        }
        return taskBList;
    }

}

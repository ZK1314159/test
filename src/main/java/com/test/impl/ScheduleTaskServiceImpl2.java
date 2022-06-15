package com.test.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.test.entity.thread_pool.TaskA;
import com.test.entity.thread_pool.TaskB;
import com.test.entity.thread_pool.TaskC;
import com.test.entity.thread_pool.TaskD;
import com.test.impl.thread_pool.TaskBHandleBean;
import com.test.impl.thread_pool.TaskBThread;
import com.test.impl.thread_pool.TaskCThread;
import com.test.service.ScheduleTaskService;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/6/15 14:57
 */
@Service("scheduleTaskServiceImpl2")
public class ScheduleTaskServiceImpl2 implements ScheduleTaskService {

    @Autowired
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor threadPool;

    private ConcurrentHashMap<Integer, TaskA> taskAMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Integer, TaskB> taskBMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Integer, TaskC> taskCMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Integer, TaskD> taskDMap = new ConcurrentHashMap<>();

    private List<TaskD> task = new ArrayList<>();

    @Autowired
    private TaskBHandleBean taskBHandleBean;

    @Autowired
    private TaskCThread taskCThread;

    @Override
    public void doTask() {
        List<TaskA> taskAList = new ArrayList<>(100);
        for (int i = 1; i <= 100; i++) {
            TaskA taskA = new TaskA();
            taskA.setId(i);
            taskA.setName(i + "_" + UUID.randomUUID().toString());
            taskAList.add(taskA);
        }

        List<List<TaskA>> taskListList = ListUtils.partition(taskAList, 10);
        CountDownLatch countDownLatch = new CountDownLatch(taskListList.size());
        for (List<TaskA> taskAList1 : taskListList) {
            for (TaskA taskA : taskAList1) {
                taskAMap.put(taskA.getId(), taskA);
            }
            List<Integer> idList = taskAList1.stream().map(TaskA::getId).collect(Collectors.toList());
            CountDownLatch countDownLatch2 = new CountDownLatch(2);
            threadPool.submit(new TaskBThread(idList, taskBMap, countDownLatch2, taskBHandleBean));
            taskCThread.run(idList, taskCMap, countDownLatch2);
            threadPool.submit(() -> {
                try {
                    countDownLatch2.await();
                } catch (Exception e) {}
                for (Integer integer : idList) {
                    TaskA taskA = taskAMap.get(integer);
                    TaskB taskB = taskBMap.get(integer);
                    TaskC taskC = taskCMap.get(integer);
                    TaskD taskD = new TaskD();
                    taskD.setId(integer);
                    taskD.setName(taskA.getName());
                    taskD.setHobby(taskB.getHobby());
                    taskD.setGender(taskC.getGender());
                    taskDMap.put(integer, taskD);
                }
                countDownLatch.countDown();
            });
        }
        threadPool.submit(() -> {
            try {
                countDownLatch.await();
            } catch (Exception e) {}
            List<TaskD> taskDS = new ArrayList<>(taskDMap.values());
            task = taskDS.stream().sorted(Comparator.comparingInt(TaskD::getId)).collect(Collectors.toList());
        });

        try {
            Thread.sleep(5000);
        } catch (Exception e) {}
    }

}

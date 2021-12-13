package com.test.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.test.config.ShowConfigNew;
import com.test.entity.Show;
import com.test.service.OperationFlowServiceNew;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/13 22:00
 */
@Service
@Slf4j
public class OperationFlowServiceNewImpl implements OperationFlowServiceNew {

    @Autowired
    private ShowConfigNew showConfigNew;

    @Override
    public void operate(String type, Show show) {
        Class<ShowConfigNew> clazz = ShowConfigNew.class;
        try {
            Field field = clazz.getDeclaredField(type);
            field.setAccessible(true);
            ShowConfigNew.OperationFlowNew operationFlowNew = (ShowConfigNew.OperationFlowNew) field.get(showConfigNew);
            Class<OperationFlowServiceNew> operationFlowClass = OperationFlowServiceNew.class;
            Class<ShowConfigNew.OperationFlowNew> operationFlowNewClass = ShowConfigNew.OperationFlowNew.class;
            Field[] fields = operationFlowNewClass.getDeclaredFields();
            if (ObjectUtils.isNotEmpty(fields)) {
                List<Field> fieldList = Arrays.asList(fields);
                fieldList = new ArrayList<>(fieldList);
                Map<Integer, String> fieldMap = new TreeMap<>();
                for (Field ConfigField : fieldList) {
                    String fieldName = ConfigField.getName();
                    ConfigField.setAccessible(true);
                    Integer operateOrder = (Integer) ConfigField.get(operationFlowNew);
                    if (operateOrder != null) {
                        fieldMap.put(operateOrder, fieldName);
                    }
                }
                if (! fieldMap.isEmpty()) {
                    for (Map.Entry<Integer, String> entry : fieldMap.entrySet()) {
                        String fieldName = entry.getValue();
                        Method method = operationFlowClass.getMethod(fieldName, Show.class);
                        method.invoke(this, show);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pushChannel(Show show) {
        log.info("pushChannel");
    }

    @Override
    public void pushWencai(Show show) {
        log.info("pushWencai");
    }

    @Override
    public void deleteCache(Show show) {
        log.info("deleteCache");
    }

}

package com.test.service.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.test.config.ShowConfig;
import com.test.config.annotatin.FieldOrder;
import com.test.entity.Show;
import com.test.service.interfaces.OperationFlowService;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/11 22:40
 */
@Slf4j
@Service
public class OperationFlowServiceImpl implements OperationFlowService {

    @Override
    public void operate(ShowConfig.OperationFlow operationFlow, Show show) {
        Class<ShowConfig.OperationFlow> clazz = ShowConfig.OperationFlow.class;
        Field[] fields = clazz.getDeclaredFields();
        Class<OperationFlowService> operationFlowClass = OperationFlowService.class;
        if (ObjectUtils.isNotEmpty(fields)) {
            List<Field> fieldListTmp = Arrays.asList(fields);
            List<Field> fieldList = new ArrayList<>(fieldListTmp);
            fieldList.sort(Comparator.comparingInt((v) -> v.getAnnotation(FieldOrder.class).value()));
            for (Field field : fieldList) {
                String fieldName = field.getName();
                field.setAccessible(true);
                try {
                    Boolean needOperate = (Boolean) field.get(operationFlow);
                    if (needOperate != null && needOperate) {
                        Method method = operationFlowClass.getMethod(fieldName, Show.class);
                        method.invoke(this, show);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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

}

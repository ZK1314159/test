package com.test.show.strategy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.test.config.ShowConfig;
import com.test.config.annotatin.FieldOrder;
import com.test.entity.Show;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/13 10:51
 */
public class AbstractFlow {

    @Autowired
    private ShowConfig showConfig;

    void operate(String type, Show show) {
        Class<ShowConfig> showConfigClass = ShowConfig.class;
        try {
            Field field = showConfigClass.getDeclaredField(type);
            field.setAccessible(true);
            ShowConfig.OperationFlow operationFlow = (ShowConfig.OperationFlow) field.get(showConfig);
            Class<ShowConfig.OperationFlow> operationFlowClass = ShowConfig.OperationFlow.class;
            Field[] fields = operationFlowClass.getDeclaredFields();
            if (ObjectUtils.isNotEmpty(fields)) {
                List<Field> fieldList = Arrays.asList(fields);
                fieldList = new ArrayList<>(fieldList);
                fieldList = fieldList.stream()
                        .sorted(Comparator.comparingInt((v) -> v.getAnnotation(FieldOrder.class).value()))
                        .collect(Collectors.toList());
                Class<AbstractFlow> abstractFlowClass = AbstractFlow.class;
                for (Field configField : fieldList) {
                    String operateName = configField.getName();
                    configField.setAccessible(true);
                    Boolean needOperate = (Boolean) configField.get(operationFlow);
                    if (needOperate != null && needOperate) {
                        Method method = abstractFlowClass.getDeclaredMethod(operateName, Show.class);
                        method.invoke(this, show);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void pushChannel(Show show) {
    }

    void pushWencai(Show show) {
    }

    void deleteCache(Show show) {
        System.out.println("deleteCache");
    }

}

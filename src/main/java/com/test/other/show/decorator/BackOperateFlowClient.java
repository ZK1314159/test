package com.test.other.show.decorator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.test.other.show.strategy.OperateType;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.test.config.config_bean.ShowConfig;
import com.test.entity.Show;

/**
 * Description
 *
 * @author zengkai
 * Date: 2021/12/13 17:12
 */
@Component
public class BackOperateFlowClient {

    @Autowired
    private ShowConfig showConfig;

    public void operate(OperateType type, Show show) {
        String realType = type.getType();
        Class<ShowConfig> showConfigClass = ShowConfig.class;
        try {
            Field field = showConfigClass.getDeclaredField(realType);
            field.setAccessible(true);
            ShowConfig.OperationFlow operationFlow = (ShowConfig.OperationFlow) field.get(showConfig);
            Class<ShowConfig.OperationFlow> operationFlowClass = ShowConfig.OperationFlow.class;
            Field[] fields = operationFlowClass.getDeclaredFields();
            if (ObjectUtils.isNotEmpty(fields)) {
                List<Field> fieldList = Arrays.asList(fields);
                fieldList = new ArrayList<>(fieldList);
                BackOperateFlow flowStart = new FlowStart();
                for (Field configField : fieldList) {
                    configField.setAccessible(true);
                    Boolean needOperate = (Boolean) configField.get(operationFlow);
                    if (needOperate != null && needOperate) {
                        String name = configField.getName();
                        // 暂不考虑name长度问题
                        String firstChar = String.valueOf(name.charAt(0)).toUpperCase();
                        String subString = name.substring(1);
                        String prefix = "com.test.show.decorator";
                        name = prefix + "." + firstChar + subString;
                        @SuppressWarnings("unchecked")
                        Class<BackOperateFlow> backOperateFlowClass = (Class<BackOperateFlow>) Class.forName(name);
                        BackOperateFlow backOperateFlow = backOperateFlowClass.newInstance();
                        backOperateFlow.setBackOperateFlow(flowStart);
                        flowStart = backOperateFlow;
                    }
                }
                flowStart.operate(show);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

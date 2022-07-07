package com.test.show.chain;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.test.config.ShowConfig;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/7/6 21:46
 */
@Component
public class ChainClient {

    @Autowired
    private List<AbstractChain> abstractChains;

    @Autowired
    private ShowConfig showConfig;

    public FilterResponse filter(ServletRequest request, ServletResponse response, String type) throws Exception {
        FilterResponse filterResponse = new FilterResponse();
        filterResponse.setPass(true);
        if (ObjectUtils.isNotEmpty(abstractChains)) {
            abstractChains.sort(Comparator.comparingInt(v -> v.getClass().getAnnotation(Order.class).value()));
            Class<ShowConfig> showConfigClass = ShowConfig.class;
            Field field = showConfigClass.getDeclaredField(type);
            field.setAccessible(true);
            ShowConfig.OperationFlow operationFlow = (ShowConfig.OperationFlow) field.get(showConfig);

            Class<ShowConfig.OperationFlow> operationFlowClass = ShowConfig.OperationFlow.class;
            Field[] fields = operationFlowClass.getDeclaredFields();
            List<Field> fieldList = new ArrayList<>(Arrays.asList(fields));
            Map<String, Boolean> map = fieldList.stream().collect(Collectors.toMap(v -> v.getName().toLowerCase(),
                v -> {
                    try {
                        v.setAccessible(true);
                        return (Boolean) v.get(operationFlow);
                    } catch (Exception e) {
                        return false;
                    }
                }));
            for (AbstractChain abstractChain : abstractChains) {
                String key = abstractChain.getClass().getSimpleName().toLowerCase();
                Boolean needFilter = map.get(key);
                if (needFilter == null || !needFilter) {
                    continue;
                }
                abstractChain.filter(request, response);
                if ("false".equals(response.getContentType())) {
                    filterResponse.setPass(false);
                    filterResponse.setReason(abstractChain.getReason());
                    return filterResponse;
                }
            }
        }
        return filterResponse;
    }

    public static void main(String[] args) {
        System.out.println(ChainClient.class.getSimpleName());
    }

}

package com.test.config.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Description
 *
 * @author zengkai
 * Date: 2022/5/12 21:04
 */
@Configuration
public class MyWebMvcConfiguration implements WebMvcConfigurer {

//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("/resources/**").addResourceLocations("file:D:/Software/Work/Data/Idea/Projects" +
//                "/Springboot/test/src/main/resources/");
//    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MyInterceptor()).
                excludePathPatterns("/user/**")  // 放行
                .addPathPatterns("/**");
    }

}

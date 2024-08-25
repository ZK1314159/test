package com.test.config.factorybean;

import com.test.repository.mysql.CourseMapper;
import com.test.service.direct.CacheService;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MyFactoryBean implements FactoryBean<CacheService> {

  @Autowired
  private CourseMapper otherBean;

  @Override
  public CacheService getObject() throws Exception {
    return new CacheService(otherBean);
  }

  @Override
  public Class<?> getObjectType() {
    return CacheService.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}


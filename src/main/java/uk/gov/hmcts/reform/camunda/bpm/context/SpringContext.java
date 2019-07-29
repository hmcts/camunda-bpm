package uk.gov.hmcts.reform.camunda.bpm.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * SpringContext allows static access to Spring ApplicationContext. This is used to inject
 * Spring components in non-spring managed classes.
 */
@Component
public class SpringContext implements ApplicationContextAware {

  @Autowired
  static ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    context = applicationContext;
  }

  public static ApplicationContext getAppContext() {
    return context;
  }
}
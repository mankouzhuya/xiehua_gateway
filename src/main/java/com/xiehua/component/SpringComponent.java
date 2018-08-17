package com.xiehua.component;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringComponent implements ApplicationContextAware {
	  
    private ApplicationContext applicationContext;
  
    public ApplicationContext getApplicationContext() {
        return applicationContext;  
    }  
  
    public void setApplicationContext(ApplicationContext applicationContext)throws BeansException {
        this.applicationContext = applicationContext;  
    } 
    
    public <T> T getBean(Class<T> claxx) {
    	return applicationContext.getBean(claxx);
    }
    
    public <T> T getBean(String id,Class<T> claxx) {
    	return applicationContext.getBean(id, claxx);
    }
} 

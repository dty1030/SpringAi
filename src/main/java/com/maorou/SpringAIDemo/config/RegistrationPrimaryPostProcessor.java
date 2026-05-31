package com.maorou.SpringAIDemo.config;


import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class RegistrationPrimaryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory){
        beanFactory.getBeanDefinition("nacosAutoServiceRegistration").setPrimary(true);
    }
}

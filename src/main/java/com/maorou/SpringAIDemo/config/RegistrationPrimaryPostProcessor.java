package com.maorou.SpringAIDemo.config;

import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
@Profile("!ai-local")
public class RegistrationPrimaryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory){
        beanFactory.getBeanDefinition("nacosAutoServiceRegistration").setPrimary(true);
        beanFactory.getBeanDefinition("nacosRegistration").setPrimary(true);   // ← 新增
        beanFactory.getBeanDefinition("nacosServiceRegistry").setPrimary(true);   // ← 新增这行
    }
}

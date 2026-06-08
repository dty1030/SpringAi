package com.maorou.SpringAIDemo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "spring.cloud.nacos.discovery.enabled",
        havingValue = "true"
)
public class RegistrationPrimaryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        markPrimaryIfPresent(beanFactory, "nacosAutoServiceRegistration");
        markPrimaryIfPresent(beanFactory, "nacosRegistration");
        markPrimaryIfPresent(beanFactory, "nacosServiceRegistry");
    }

    private void markPrimaryIfPresent(
            ConfigurableListableBeanFactory beanFactory,
            String beanName) {
        if (beanFactory.containsBeanDefinition(beanName)) {
            beanFactory.getBeanDefinition(beanName).setPrimary(true);
        }
    }
}
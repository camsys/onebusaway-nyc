package org.onebusaway.api.actions.bindings;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class StrutsSetterNullHandlingAspect {

//    @Around("execution(* *.*(..))")
//    @Around("execution(* *(..))")
    public Object adviceForResourceSetters(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if(true){
            Object returnValue = joinPoint.proceed();
            return returnValue;
        } else{
            return null;
        }

    }

}

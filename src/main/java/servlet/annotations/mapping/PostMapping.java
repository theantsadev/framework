package servlet.annotations.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import servlet.enums.RequestMethod;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@RequestMapping(method = RequestMethod.POST)
public @interface PostMapping {
    String value() default "";
}

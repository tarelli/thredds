package thredds.mock.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface MockWebApplication {
    /**
     * MAVEN default locartion for webapp directory   
     */
	String webapp() default "src/main/webapp";
	
	/**
	 * Servlet Name as defined in the web.xml
	 */
	String name();
}

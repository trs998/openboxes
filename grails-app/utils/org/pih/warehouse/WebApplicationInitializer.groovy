import org.springframework.web.WebApplicationInitializer

import javax.servlet.ServletContainerInitializer
import javax.servlet.ServletContext
import javax.servlet.ServletException
import javax.servlet.annotation.HandlesTypes

@HandlesTypes(WebApplicationInitializer.class)
public class FooServletContainerInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        for (Class<?> clazz : c) {
            System.out.println(clazz);
            System.out.println(clazz.getResource('/' + clazz.getName().replace('.', '/') + ".class"));
            System.out.println("----------------");
        }
    }
}

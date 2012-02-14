package thredds.mock.web;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import thredds.servlet.ServletUtil;
import thredds.servlet.ThreddsConfig;
import ucar.nc2.util.cache.FileCache;


public class MockWebApplicationContextLoader extends AbstractContextLoader {
	
	private MockWebApplication configuration;
	
	@Override
	public ApplicationContext loadContext(String... locations) throws Exception {

		final MockServletContext servletContext = new MockServletContext( configuration.webapp() , new  FileSystemResourceLoader());
		final MockServletConfig servletConfig  = new MockServletConfig( servletContext, configuration.name() );	

		
		final XmlWebApplicationContext  webApplicationContext= new XmlWebApplicationContext();
		
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);
		webApplicationContext.setServletConfig(servletConfig);
		webApplicationContext.setConfigLocations(locations);
		
	
		//Mock file cache... needed for running init method in thredds.server.views.FileView
		//CdmInit will set again the file cache through the init method
	    int min = ThreddsConfig.getInt("HTTPFileCache.minFiles", 10);
	    int max = ThreddsConfig.getInt("HTTPFileCache.maxFiles", 20);
	    int secs = ThreddsConfig.getSeconds("HTTPFileCache.scour", 17 * 60);
	    if (max > 0) {
	      ServletUtil.setFileCache( new FileCache("HTTP File Cache", min, max, -1, secs));

	    }

		final DispatcherServlet dispatcherServlet = new DispatcherServlet(){
			@Override
			protected WebApplicationContext createWebApplicationContext(ApplicationContext parent){
				return webApplicationContext;
			}
		};
		
		webApplicationContext.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor(){
			@Override
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory){
				beanFactory.registerResolvableDependency(DispatcherServlet.class, dispatcherServlet );
				
				// ----> if using JSP a ViewResolver must be registered here too (as well any other beans)
				
				//TODO 
				//A different WebApplicationContextLoader is needed for OpendapServlet!!
				//Registering for OpendapServlet
				beanFactory.registerResolvableDependency(MockServletContext.class, servletContext );
				beanFactory.registerResolvableDependency(MockServletConfig.class, servletConfig );
				
			}
		});
		
		webApplicationContext.addApplicationListener(new SourceFilteringListener(webApplicationContext, new ApplicationListener<ContextRefreshedEvent>() {
              @Override
              public void onApplicationEvent(ContextRefreshedEvent event) {
                      dispatcherServlet.onApplicationEvent(event);
              }
         }));
		
		//Prepare the context
		webApplicationContext.refresh();
		webApplicationContext.registerShutdownHook();
		
		//Initialize the servlet
		dispatcherServlet.setContextConfigLocation("");
		dispatcherServlet.init(servletConfig);
		
		return webApplicationContext;
	}

	@Override
	protected String getResourceSuffix() {
		// TODO Auto-generated method stub
		return null;
	}	
	
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz){
		extractConfiguration(clazz);
		return super.generateDefaultLocations(clazz);
	}
	
	@Override
	protected String[] modifyLocations(Class<?> claszz, String... locations){
		extractConfiguration(claszz);
		return super.modifyLocations(claszz, locations);
	}
		
	private void extractConfiguration(Class<?> clazz){	
		configuration = AnnotationUtils.findAnnotation(clazz,  MockWebApplication.class);
		if(configuration == null){
			throw new IllegalArgumentException("Test class "+clazz.getName() + " must be annotated @MockWebApplication" );	
		}
	}
}

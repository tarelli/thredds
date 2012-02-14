package thredds.server.config;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

import thredds.mock.web.MockWebApplication;
import thredds.mock.web.MockWebApplicationContextLoader;
import thredds.server.root.RootController;
import thredds.servlet.DataRootHandler;
import thredds.servlet.HtmlWriter;
import thredds.servlet.RestrictedAccessConfigListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/applicationContext-tdsConfig.xml","/root-servlet.xml"},loader=MockWebApplicationContextLoader.class)
@MockWebApplication(name="thredds")
//@ContextConfiguration(locations={"/applicationContext-tdsConfig.xml","/root-servlet.xml"})
public class RootControllerTest {
	
	@Autowired
	private DispatcherServlet servlet;
	
	@Autowired
	private TdsContext tdsContext;
	
	@Autowired
	private CdmInit cdmInit;	
	
	@Autowired
	private DataRootHandler tdsDRH;
	
	@Autowired
	private HtmlConfig htmlConfig;
	
	@Autowired
	HtmlWriter htmlWriter;  
	
	@Autowired
	RootController rootController;
	
	private MockServletContext servletContext;
	
	
	@Before
	public void setUp() throws Exception {
		
		// --> same startup that in thredds.server.config.TdsConfigContextListener
		
		servletContext =  new MockServletContext();
		//Initialize the TDS context.
		tdsContext.init(servletContext);
		
		//CdmInit method is package visible so we moved the test into this package
		//It would be better if it was in thredds.server.opendap...make the CdmInit.init() method public?
		// Initialize the CDM, now that tdsContext is ready
		cdmInit.init(tdsContext);
		
		// Initialize the DataRootHandler.
		tdsDRH.registerConfigListener( new RestrictedAccessConfigListener() );
		tdsDRH.init();		
		DataRootHandler.setInstance(tdsDRH);

		// YUCK! This is done so that not-yet-Spring-ified servlets can access the singleton HtmlWriter.
	    // LOOK! ToDo This should be removed once the catalog service controllers uses JSP.
		htmlWriter.setSingleton( htmlWriter );
				
	}
	
	@Test
	public void getastModifiedTest() throws ServletException, IOException{
		
		rootController.setTdsContext(tdsContext);
		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/catalog.html");
		request.setContextPath("/thredds");
        //request.addParameter("id", "0");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
		servlet.service(request, response);
		String results = response.getContentAsString().trim();

        //long kk = rootController.getLastModified(request);
		
        
		fail("Not yet implemented");
	}


}

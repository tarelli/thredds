package thredds.server.catalogservice;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml","/WEB-INF/catalogService-servlet.xml" }, loader=MockTdsContextLoader.class)
@TdsContentRootPath(path="/share/testcatalogs/content")
public class RemoteCatalogControllerTest {
	
	@Autowired
	private RemoteCatalogServiceController remoteCatalogController;
	
	@Test
	public void TestRemoteCatalogRequest() throws Exception{
			
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/remoteCatalogService/catalog.xml");
		request.setServletPath("/catalog.xml");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        /*
         * default configuratuion has allowremote= false so mv will be null 
         */
        ModelAndView mv =remoteCatalogController.handleRequest(request, response);        		
		
        fail("Not implemented yet");
	}
	
	@Test
	public void TestRemoteCatalogServices() throws Exception{
			
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalogServices/catalog.xml");
		request.setServletPath("/catalog.xml");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        /*
         * default configuratuion has allowremote= false so mv will be null 
         */
        ModelAndView mv =remoteCatalogController.handleRequest(request, response);        		
		
        fail("Not implemented yet");
	}	
	
	
	@Test
	public void TestRemoteCatalogValidation() throws Exception{
			
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/remoteCatalogValidation.html");
		request.setServletPath("/remoteCatalogValidation.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        /*
         * default configuratuion has allowremote= false so mv will be null 
         */        
        ModelAndView mv =remoteCatalogController.handleRequest(request, response);        		
		
        fail("Not implemented yet");
	}	

}

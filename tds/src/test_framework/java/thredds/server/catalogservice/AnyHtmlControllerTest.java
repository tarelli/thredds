package thredds.server.catalogservice;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import thredds.mock.web.MockTdsContextLoader;
import thredds.server.config.TdsContext;
import thredds.servlet.HtmlWriter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml","/WEB-INF/catalogService-servlet.xml" }, loader=MockTdsContextLoader.class)
public class AnyHtmlControllerTest {
	
	@Autowired
	private TdsContext tdsContext;
	
	@Autowired
	private HtmlWriter htmlWriter;
	
	
	@Autowired
	private LocalCatalogServiceController anyHtmlController;
	
	
	
	@Before
	public void setUp(){		
		anyHtmlController.setTdsContext(tdsContext);
		anyHtmlController.setHtmlWriter(htmlWriter);
	}
	
	@Test
	public void testAnyXmlController() throws Exception{
		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/testAll/catalog.html");
		request.setServletPath("/testAll/catalog.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =anyHtmlController.handleRequest(request, response);        
        assertNull( mv);        
        assertEquals(200, response.getStatus() ); 
	}	

}

package thredds.server.catalogservice;

import static org.springframework.test.web.ModelAndViewAssert.assertAndReturnModelAttributeOfType;
import static org.springframework.test.web.ModelAndViewAssert.assertViewName;

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
import thredds.server.config.TdsServerInfo;
import thredds.servlet.HtmlWriter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext-tdsConfig.xml","/WEB-INF/catalogService-servlet.xml" }, loader=MockTdsContextLoader.class)
public class AnyXmlControllerTest{

	@Autowired
	private TdsContext tdsContext;
	
	@Autowired
	private HtmlWriter htmlWriter;
	
	
	@Autowired
	private LocalCatalogServiceController anyXmlController;
	
	
	
	@Before
	public void setUp(){		
		anyXmlController.setTdsContext(tdsContext);
		anyXmlController.setHtmlWriter(htmlWriter);
	}
	
	@Test
	public void testAnyXmlController() throws Exception{
		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/thredds/catalog/testAll/catalog.xml");
		request.setServletPath("/testAll/catalog.xml");
        MockHttpServletResponse response = new MockHttpServletResponse();
		
        ModelAndView mv =anyXmlController.handleRequest(request, response);        
        
        assertViewName(mv, "threddsInvCatXmlView");
        assertAndReturnModelAttributeOfType(mv,"catalog" , thredds.catalog.InvCatalogImpl.class);
        
		
	}
	


}

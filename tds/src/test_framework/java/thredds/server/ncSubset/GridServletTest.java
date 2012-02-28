package thredds.server.ncSubset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import thredds.mock.web.MockTdsContextLoader;
import thredds.mock.web.TdsContentRootPath;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/WEB-INF/applicationContext-tdsConfig.xml" }, loader = MockTdsContextLoader.class)
@TdsContentRootPath(path = "/share/testcatalogs/content")
public class GridServletTest {

	@Autowired
	private ServletConfig servletConfig;

	private GridServlet gridServlet;
	
	private String tempFileName ="test_response.nc"; 

	@Before
	public void setUp() throws ServletException {

		gridServlet = new GridServlet();
		gridServlet.init(servletConfig);

	}
	
	@Test
	public void testNCSSPointRequest() throws ServletException, IOException {

		String mockURI = "/thredds/ncss/grid/NCOF/POLCOMS/IRISH_SEA/files/20060926_0000.nc";
		String mockQueryString = "var=Relative_humidity_height_above_ground,Temperature_height_above_ground&latitude=40.0&longitude=-62.0&time_start=2006-09-26T03%3A00%3A00.000Z&time_end=2006-09-26T06%3A00%3A00.000Z&temporal=point&time=2006-09-26T03%3A00%3A00.000Z&vertCoord=&accept=xml&point=true";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", mockURI);
		request.setContextPath("/thredds");
		request.setPathInfo("/NCOF/POLCOMS/IRISH_SEA/files/20060926_0000.nc");
		request.setQueryString(mockQueryString);

		request.setParameter("point", "true");
		request.setParameter("accept", "xml");
		request.setParameter("temporal", "point");
		request.setParameter("time", "2006-09-26T06%3A00%3A00.000Z");
		request.setParameter("time_start", "2006-09-26T06%3A00%3A00.000Z");
		request.setParameter("time_end", "2006-09-26T06%3A00%3A00.000Z");
		request.setParameter("var", "Relative_humidity_height_above_ground,Temperature_height_above_ground");
		request.setParameter("latitude", "40.0");
		request.setParameter("longitude", "-62.0");

		MockHttpServletResponse response = new MockHttpServletResponse();

		gridServlet.doGet(request, response);
		assertEquals(200, response.getStatus());
		//Expected response, lat and lon 0, and values NaN????
		assertEquals(
				"<?xml version='1.0' encoding='UTF-8'?>\n"+
						"<grid dataset='/share/testdata/cdmUnitTest/ncml/agg/20060926_0000.nc'>\n"+
						"  <point>\n"+
						"    <data name='date'>2006-09-26T03:00:00.000Z</data>\n"+
						"    <data name='lat' units='degrees_north'>0.0</data>\n"+
						"    <data name='lon' units='degrees_east'>0.0</data>\n"+
						"    <data name='vertCoord' units='m'>0.0</data>\n"+
						"    <data name='Relative_humidity_height_above_ground' units='percent'>NaN</data>\n"+
						"    <data name='Temperature_height_above_ground' units='K'>NaN</data>\n"+
						"  </point>\n"+
						"</grid>\n",
				response.getContentAsString());

	}
	
	@Test
	public void testNCSSGridRequest() throws ServletException, IOException {

		String mockURI = "/thredds/ncss/grid/hioos/model/wav/swan/oahu/runs/SWAN_Oahu_Regional_Wave_Model_(500m)_RUN_2011-07-12T00:00:00.000Z";
		String mockQueryString = "var=salt,temp&north=21.9823&south=19.0184&east=-154.5193&west=-161.8306&time=2011-07-12T00:00:00.000Z";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", mockURI);
		request.setContextPath("/thredds");
		request.setPathInfo("/hioos/model/wav/swan/oahu/runs/SWAN_Oahu_Regional_Wave_Model_(500m)_RUN_2011-07-12T00:00:00.000Z");
		request.setQueryString(mockQueryString);
		
		request.setParameter("var", "temp,salt");
		request.setParameter("north", "21.9823");
		request.setParameter("south", "19.0184");
		request.setParameter("east", "-154.5193");
		request.setParameter("west", "-161.8306");
		request.setParameter("time", "2011-07-12T00:00:00.000Z");
		request.setParameter("time_start", "2011-07-12T00:00:00.000Z");
		request.setParameter("time_end", "2011-07-12T00:00:00.000Z");

		MockHttpServletResponse response = new MockHttpServletResponse();
		
		gridServlet.doGet(request, response);

		assertEquals(200, response.getStatus());		
		assertEquals("application/x-netcdf", response.getContentType());
		
		byte[] content = response.getContentAsByteArray();		
		NetcdfFile nf = null;
		
		//Open an in memory NetcdfFile and transform it into a NetcdfDataset  
		try{
			nf = NetcdfFile.openInMemory(tempFileName, content);
			GridDataset gds = new GridDataset( new NetcdfDataset(nf) );			
			double delta = 0.03;
			assertEquals(21.9823, gds.getBoundingBox().getLatMax()  , delta);
			assertEquals(19.0184, gds.getBoundingBox().getLatMin()  , delta);
			assertEquals(-154.5193, gds.getBoundingBox().getLonMax(), delta);
			assertEquals(-161.8306, gds.getBoundingBox().getLonMin(), delta);			
			assertEquals(2, gds.getGrids().size());
			assertNotNull(gds.findGridByName("temp"));
			assertNotNull(gds.findGridByName("salt"));
			
			
		}finally{
			nf.close();
		}		

		
	}	

}

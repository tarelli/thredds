package dap4.test.servlet;

import dap4.core.util.DapUtil;
import dap4.d4ts.D4TSServlet;

import javax.servlet.ServletConfig;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Define a subclass of D4TSServlet that overrides some
 * methods of HttpServlet to provide fake context information
 *
 * Implements servlet api 3.0
 */

public class FakeServlet extends D4TSServlet
    implements javax.servlet.ServletConfig,
    javax.servlet.ServletContext
{
    //////////////////////////////////////////////////
    // Constants

    static final String DEFAULTSERVLETNAME = "d4ts";

    //////////////////////////////////////////////////
    // Instance Variables

    String servletname = DEFAULTSERVLETNAME;

    // Define the prefix of the URL file that refers to the servlet
    String servletpath = "/" + DEFAULTSERVLETNAME;

    String datasetpath = null;   // absolute path to the directory containing datasets

    //////////////////////////////////////////////////
    // Constructor(s)

    public FakeServlet(String datasetpath)
    {
        datasetpath = DapUtil.canonicalpath(datasetpath,false);
        this.datasetpath = datasetpath;
    }

    //////////////////////////////////////////////////
    // Get/Set other than from the interfaces

    //////////////////////////////////////////////////
    // Overridden methods

    public javax.servlet.ServletConfig getServletConfig()
    {
        return (ServletConfig) this;
    }

    //////////////////////////////////////////////////
    // public interface ServletConfig

    public String getServletName()
    {
        return servletname;
    }

    public javax.servlet.ServletContext getServletContext()
    {
        return (javax.servlet.ServletContext) this;
    }

    public String getInitParameter(String s)
    {
        return null;
    }

    public Enumeration getInitParameterNames()
    {
        return null;
    }

    // end interface ServletConfig
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // public interface ServletContext

    public javax.servlet.ServletContext getContext(String s)
    {
        return this;
    }

    public int getMajorVersion()
    {
        return 3;
    }

    public int getMinorVersion()
    {
        return 0;
    }

    public String getMimeType(String s)
    {
        return null;
    }

    // Assume that suffix begins with "/WEB-INF"
    public Set getResourcePaths(String suffix)
    {
        // Create the directory from which to get
        // the entries
        suffix = DapUtil.canonicalpath(suffix,true);
        suffix = "/" + suffix; //guarantee leading /
        if(!suffix.startsWith("/WEB-INF"))
            return null;
        String suffix2 = suffix.substring("/WEB-INF".length(),suffix.length());
        if(!suffix2.startsWith("/")) suffix2 = "/" + suffix;
        // Assume it is relative to datasetpath
        String root = datasetpath + suffix2;
        File f = new File(root);
        if(!f.exists() || !f.isDirectory())
            return null;
        String[] contents =  f.list();
        Set contentset = new HashSet();
        for(String s: contents) {
            // Make s relative to suffix
            contentset.add(suffix+"/"+s);
        }
        return contentset;
    }

    public java.net.URL getResource(String suffix) throws java.net.MalformedURLException
    {
        return new URL("file://" + DapUtil.canonicalpath(getRealPath(suffix),false));
    }

    public InputStream getResourceAsStream(String s)
    {
        try {
            return getResource(s).openStream();
        } catch (IOException ioe) {
            return null;
        }
    }

    public javax.servlet.RequestDispatcher getRequestDispatcher(String s)
    {
        return null;
    }

    public javax.servlet.RequestDispatcher getNamedDispatcher(String s)
    {
        return null;
    }

    public void log(String s)
    {
        System.err.println(s);
    }

    public void log(String s, Throwable throwable)
    {
        System.err.println(s + ": " + throwable);
    }

    public String getRealPath(String path)
    {
        path = DapUtil.canonicalpath(path,true); // clean and make relative
        path = "/" + path;
        // Assume path starts with /WEB-INF
        if(!path.startsWith("/WEB-INF"))
            return null;
        path = path.substring("/WEB-INF".length(),path.length());
        if(!path.startsWith("/")) path = "/" + path;
        // Assume it is relative to datasetpath
        path = datasetpath + path;
        return path;
    }

    public String getServerInfo()
    {
        return null;
    }

    public Object getAttribute(String s)
    {
        return null;
    }

    public Enumeration getAttributeNames()
    {
        return null;
    }

    public void setAttribute(String s, Object o)
    {
        return;
    }

    public void removeAttribute(String s)
    {
        return;
    }

    public String getServletContextName()
    {
        return getServletName();
    }

    /**
     * @deprecated
     */
    public javax.servlet.Servlet getServlet(String s) throws javax.servlet.ServletException
    {
        return this;
    }

    @Deprecated
    public Enumeration getServlets()
    {
        return null;
    }

    @Deprecated
    public Enumeration getServletNames()
    {
        return null;
    }

    @Deprecated
    public void log(Exception e, String s)
    {
        log(s, e);
    }

    // servlet API 3.0 Additions

    public String getContextPath()
	{return null;}
    
    public int getEffectiveMajorVersion()
	{throw new UnsupportedOperationException();}
    
    public int getEffectiveMinorVersion()
	{throw new UnsupportedOperationException();}

    // Implemented in ServletConfig interface
    //     public String getInitParameter(String p0);
    
    // Implemented in ServletConfig interface
    //     public Enumeration<String> getInitParameterNames();
    
    public boolean setInitParameter(String p0, String p1)
	{throw new UnsupportedOperationException();}
    
    public javax.servlet.ServletRegistration.Dynamic addServlet(String p0, String p1)
	{throw new UnsupportedOperationException();}
    
    public javax.servlet.ServletRegistration.Dynamic addServlet(String p0, javax.servlet.Servlet p1)
	{throw new UnsupportedOperationException();}
    
    public javax.servlet.ServletRegistration.Dynamic addServlet(String p0, Class<? extends javax.servlet.Servlet> p1)
	{throw new UnsupportedOperationException();}
    
    public <T extends javax.servlet.Servlet> T createServlet(Class<T> p0) throws javax.servlet.ServletException
	{throw new UnsupportedOperationException();}
    
    public javax.servlet.ServletRegistration getServletRegistration(String p0)
	{throw new UnsupportedOperationException();}
    
    public Map<String,? extends javax.servlet.ServletRegistration> getServletRegistrations()
	{throw new UnsupportedOperationException();}
    
    public javax.servlet.FilterRegistration.Dynamic addFilter(String p0, String p1)
	{throw new UnsupportedOperationException();}
    
    public javax.servlet.FilterRegistration.Dynamic addFilter(String p0, javax.servlet.Filter p1)
	{throw new UnsupportedOperationException();}
    
    public javax.servlet.FilterRegistration.Dynamic addFilter(String p0, Class<? extends javax.servlet.Filter> p1)
	{throw new UnsupportedOperationException();}
    
    public <T extends javax.servlet.Filter> T createFilter(Class<T> p0) throws javax.servlet.ServletException
	{throw new UnsupportedOperationException();}
    
    public javax.servlet.FilterRegistration getFilterRegistration(String p0)
	{throw new UnsupportedOperationException();}
    
    public Map<String,? extends javax.servlet.FilterRegistration> getFilterRegistrations()
	{throw new UnsupportedOperationException();}
    
    public javax.servlet.SessionCookieConfig getSessionCookieConfig()
	{throw new UnsupportedOperationException();}
    
    public void setSessionTrackingModes(Set<javax.servlet.SessionTrackingMode> p0) throws IllegalStateException, IllegalArgumentException
	{throw new UnsupportedOperationException();}
    
    public Set<javax.servlet.SessionTrackingMode> getDefaultSessionTrackingModes()
	{throw new UnsupportedOperationException();}
    
    public Set<javax.servlet.SessionTrackingMode> getEffectiveSessionTrackingModes()
	{throw new UnsupportedOperationException();}
    
    public void addListener(Class<? extends EventListener> p0)
	{throw new UnsupportedOperationException();}
    
    public void addListener(String p0)
	{throw new UnsupportedOperationException();}
    
    public <T extends EventListener> void addListener(T p0)
	{throw new UnsupportedOperationException();}
    
    public <T extends EventListener> T createListener(Class<T> p0) throws javax.servlet.ServletException
	{throw new UnsupportedOperationException();}
    
    public void declareRoles(String... p0)
	{throw new UnsupportedOperationException();}
    
    public ClassLoader getClassLoader()
	{throw new UnsupportedOperationException();}
    
    public javax.servlet.descriptor.JspConfigDescriptor getJspConfigDescriptor()
	{throw new UnsupportedOperationException();}

    // end interface ServletContext
    //////////////////////////////////////////////////

}    



    

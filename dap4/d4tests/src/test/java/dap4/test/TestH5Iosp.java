package dap4.test;

import dap4.servlet.DMRPrint;
import dap4.test.util.UnitTestCommon;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class TestH5Iosp extends UnitTestCommon
{
    static protected final boolean DEBUG = false;

    static protected final boolean NCDUMP = true;

    static protected final Mode mode = Mode.BOTH;

    //////////////////////////////////////////////////
    // Constants

    static protected String DATADIR = "tests/src/test/data"; // relative to opuls root
    static protected String TESTDATADIR = DATADIR + "/resources/";
    static protected String BASELINEDIR = DATADIR + "/resources/TestIosp/baseline";
    static protected String TESTINPUTDIR = DATADIR + "/resources/testfiles";

    static protected final BigInteger MASK = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    //////////////////////////////////////////////////
    // Type Declarations

    static protected class H5IospTest
    {
        static String root = null;
        String title;
        String dataset;
        String testinputpath;
        String baselinepath;

        H5IospTest(String dataset)
        {
            this.title = dataset;
            this.dataset = dataset;
            this.testinputpath
                = root + "/" + TESTINPUTDIR + "/" + dataset;
            this.baselinepath
                = root + "/" + BASELINEDIR + "/" + dataset + ".hdf5";
        }

        public String toString()
        {
            return dataset;
        }
    }

    static protected enum Mode {DMR,DATA,BOTH;}

    //////////////////////////////////////////////////
    // Instance variables

    // System properties

    protected boolean prop_diff = true;
    protected boolean prop_baseline = false;
    protected boolean prop_visual = false;
    protected boolean prop_debug = DEBUG;
    protected boolean prop_generate = true;

    // Misc variables
    protected boolean isbigendian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    // Test cases

    protected List<H5IospTest> alltestcases = new ArrayList<H5IospTest>();

    protected List<H5IospTest> chosentests = new ArrayList<H5IospTest>();

    protected String datasetpath = null;

    protected String root = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public TestH5Iosp()
        throws Exception
    {
        this("TestServlet");
    }

    public TestH5Iosp(String name)
        throws Exception
    {
        this(name, null);
    }

    public TestH5Iosp(String name, String[] argv)
        throws Exception
    {
        super(name);
        setSystemProperties();
        this.root = getRoot();
        if(this.root == null)
            throw new Exception("Opuls root not found");
        File f = new File(root + "/" + BASELINEDIR);
        if(!f.exists()) f.mkdir();
        this.datasetpath = this.root + "/" + DATADIR;
        defineAllTestcases(this.root);
        chooseTestcases();
    }

    //////////////////////////////////////////////////
    // Define test cases

    void
    chooseTestcases()
    {
        if(false) {
            //chosentests = locate("test_atomic_types.nc");
            chosentests.add(new H5IospTest("test_test.nc"));
        } else {
            for(H5IospTest tc : alltestcases)
                chosentests.add(tc);
        }
    }

    void defineAllTestcases(String root)
    {
        H5IospTest.root = root;
        this.alltestcases.add(new H5IospTest("test_one_var.nc"));
        this.alltestcases.add(new H5IospTest("test_one_vararray.nc"));
        this.alltestcases.add(new H5IospTest("test_atomic_types.nc"));
        this.alltestcases.add(new H5IospTest("test_atomic_array.nc"));
        this.alltestcases.add(new H5IospTest("test_enum.nc"));
        this.alltestcases.add(new H5IospTest("test_enum_array.nc"));
        this.alltestcases.add(new H5IospTest("test_struct_type.nc"));
        this.alltestcases.add(new H5IospTest("test_struct_array.nc"));
        this.alltestcases.add(new H5IospTest("test_struct_nested.nc"));
        this.alltestcases.add(new H5IospTest("test_vlen1.nc"));
        this.alltestcases.add(new H5IospTest("test_vlen2.nc"));
        this.alltestcases.add(new H5IospTest("test_vlen3.nc"));
        this.alltestcases.add(new H5IospTest("test_vlen4.nc"));
        this.alltestcases.add(new H5IospTest("test_vlen5.nc"));
    }


    //////////////////////////////////////////////////
    // Junit test methods

    public void testH5Iosp()
        throws Exception
    {
            for(H5IospTest testcase : chosentests) {
                if(!doOneTest(testcase)) {
                    assertTrue(false);
                }
            }
    }

    //////////////////////////////////////////////////
    // Primary test method
    boolean
    doOneTest(H5IospTest testcase)
        throws Exception
    {
        boolean pass = true;

        System.out.println("Testcase: " + testcase.testinputpath);

        NetcdfDataset ncfile = openDataset(testcase.testinputpath);

        String metadata = null;
        String data = null;
        if(mode == Mode.DMR || mode == Mode.BOTH) {
            metadata = (NCDUMP ? ncdumpmetadata(ncfile)  : null);
            if(prop_visual)
                visual("Meta Data: ", metadata);
        }
        if(mode == Mode.DATA || mode == Mode.BOTH) {
            data = (NCDUMP ? ncdumpdata(ncfile) : null);
            if(prop_visual)
                visual("Data: ", data);
        }

        String baselinefile = String.format("%s", testcase.baselinepath);
        if(prop_baseline) {
            if(mode == Mode.DMR || mode == Mode.BOTH)
                writefile(baselinefile + ".dmr",metadata);
            if(mode == Mode.DATA || mode == Mode.BOTH)
                writefile(baselinefile + ".dap",data);
        } else if(prop_diff) { //compare with baseline
            String baselinecontent = null;
            if(mode == Mode.DMR || mode == Mode.BOTH) {
                // Read the baseline file(s)
                System.out.println("DMR Comparison:");
                baselinecontent = readfile(baselinefile + ".dmr");
                pass = pass && compare(baselinecontent, metadata);
                System.out.println(pass ? "Pass" : "Fail");
            }
            if(mode == Mode.DATA || mode == Mode.BOTH) {
                System.out.println("DATA Comparison:");
                baselinecontent = readfile(baselinefile + ".dap");
                pass = pass && compare(baselinecontent, data);
                System.out.println(pass ? "Pass" : "Fail");
            }
        }
        return pass;
            }

    //////////////////////////////////////////////////
    // Utility methods

    boolean
    report(String msg)
    {
        System.err.println(msg);
        prop_generate = false;
        return false;
    }

    /**
     * Try to get the system properties
     */
    void setSystemProperties()
    {
        if(System.getProperty("nodiff") != null)
            prop_diff = false;
        String value = System.getProperty("baseline");
        if(value != null) prop_baseline = true;
        value = System.getProperty("nogenerate");
        if(value != null) prop_generate = false;
        value = System.getProperty("debug");
        if(value != null) prop_debug = true;
        if(System.getProperty("visual") != null)
            prop_visual = true;
        if(prop_baseline && prop_diff)
            prop_diff = false;
    }

    // Locate the test cases with given prefix
    List<H5IospTest>
    locate(String prefix)
    {
        List<H5IospTest> results = new ArrayList<H5IospTest>();
        for(H5IospTest ct : this.alltestcases) {
            if(ct.dataset.startsWith(prefix))
                results.add(ct);
        }
        return results;
    }
    //////////////////////////////////////////////////
    // Stand alone

    static public void
    main(String[] argv)
    {
        try {
            new TestH5Iosp().testH5Iosp();
        } catch (Exception e) {
            System.err.println("*** FAIL");
            e.printStackTrace();
            System.exit(1);
        }
        System.err.println("*** PASS");
        System.exit(0);
    }// main

    //////////////////////////////////////////////////
    // Dump methods

    String ncdumpmetadata(NetcdfDataset ncfile)
    {
        boolean ok = false;
        String metadata = null;
        StringWriter sw = new StringWriter();

        // Print the meta-databuffer using these args to NcdumpW
        ok = false;
        try {
            ok = ucar.nc2.NCdumpW.print(ncfile, "-unsigned", sw, null);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ok = false;
        }
        try {
            sw.close();
        } catch (IOException e) {
        }
        if(!ok) {
            System.err.println("NcdumpW failed");
            System.exit(1);
        }
        return shortenFileName(sw.toString(),ncfile.getLocation());
    }

    String ncdumpdata(NetcdfDataset ncfile)
    {
        boolean ok = false;
        StringWriter sw = new StringWriter();

        // Dump the databuffer
        sw = new StringWriter();
        ok = false;
        try {
            ok = ucar.nc2.NCdumpW.print(ncfile, "-vall -unsigned", sw, null);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ok = false;
        }
        try {
            sw.close();
        } catch (IOException e) {
        }
        ;
        if(!ok) {
            System.err.println("NcdumpW failed");
            System.exit(1);
        }
        return shortenFileName(sw.toString(),ncfile.getLocation());
    }

}


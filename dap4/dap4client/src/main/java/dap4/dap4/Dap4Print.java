/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/
package dap4.dap4;

import dap4.core.util.*;
import dap4.core.dmr.*;
import dap4.core.data.*;

import dap4.dap4shared.*;
import org.apache.commons.cli.*;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Retrieve a DAP4 remote source and print in DMR format.
 * Also print the data in a nested format.
 * <p/>
 * WARNING: the printDMR code here is taken from dap4.servlet.DMRPrint;
 * so changes here should be propagated.
 *
 * @author Heimbigner
 *         (some of the code is from Caron's NCdumpW program)
 */


public class Dap4Print
{

    //////////////////////////////////////////////////
    // Constants

    static protected final int COLUMNS = 8;

    static protected final char LPAREN = '(';
    static protected final char RPAREN = ')';
    static protected final char LBRACE = '{';
    static protected final char RBRACE = '}';
    static protected final char LBRACKET = '[';
    static protected final char RBRACKET = ']';

    // Could use enumset, but it is so ugly,
    // so use good old OR'able flags
    static protected final int NILFLAGS = 0;
    static protected final int PERLINE = 1; // print xml attributes 1 per line
    static protected final int NONAME = 2; // do not print name xml attribute
    static protected final int NONNIL = 4; // print empty xml attributes

    //////////////////////////////////////////////////
    // Type declarations

    static class CommandlineOptions
    {
        // Local copies of the command line options
        String path = null;
        String outputfile = null;
        boolean allvars = false;
        List<String> vars = new ArrayList<String>();
    }

    //////////////////////////////////////////////////
    // Instance variables

    protected String originalurl = null;
    protected PrintWriter writer = null;
    protected IndentWriter printer = null;
    protected D4DSP dsp = null;
    protected DapDataset dmr = null;
    protected D4DataDataset data = null;

    //////////////////////////////////////////////////
    //Constructor(s)

    public Dap4Print(String url, PrintWriter writer)
    {
        this.writer = writer;
        this.printer = new IndentWriter(this.writer);
        this.originalurl = url;
    }

    //////////////////////////////////////////////////
    // Accessors

    //////////////////////////////////////////////////
    // Main API 

    public void
    print()
        throws DapException
    {
        // ask the server for the remote dataset
        this.dsp = new D4DSP().open(this.originalurl);
        this.dmr = dsp.getDMR();
        this.data = (D4DataDataset) dsp.getDataDataset();
        this.printer.setIndent(0);
        print(this.dmr);
        printer.eol();
        this.printer.setIndent(0);
        printData();
        printer.eol();
    }

    //////////////////////////////////////////////////

    /**
     * Print an arbitrary DapNode and its subnodes
     * as if it is being sent to a client with
     * optional constraint; inclusions are determined
     * by the view.
     * <p/>
     * Handling newlines is a bit tricky because they may be
     * embedded for e.g. groups, enums,
     * etc.  So the rule is that the
     * last newline is elided and left
     * for the caller to print.
     * Exceptions: printMetadata
     * printDimrefs, printMaps
     *
     * @param node - the node to rpint
     * @throws IOException
     */

    void
    print(DapNode node)
        throws DapException
    {
        if(node == null)
            return;

        DapSort sort = node.getSort();
        String dmrname = sort.getName();

        switch (sort) {
        case DATASET:// treat like group
        case GROUP:
            DapGroup group = (DapGroup) node;
            printer.marginPrint("<" + dmrname);
            int flags = (sort == DapSort.DATASET ? PERLINE : NILFLAGS);
            printXMLAttributes(node, flags);
            printer.println(">");
            printer.indent();
            // Make the output order conform to the spec
            if(group.getDimensions().size() > 0) {
                for(DapNode subnode : group.getDimensions()) {
                    print(subnode);
                    printer.eol();
                }
            }
            if(group.getEnums().size() > 0) {
                for(DapNode subnode : group.getEnums()) {
                    print(subnode);
                    printer.eol();
                }
            }
            if(group.getVariables().size() > 0)
                for(DapNode subnode : group.getVariables()) {
                    print(subnode);
                    printer.eol();
                }
            printMetadata(node);
            if(group.getGroups().size() > 0)
                for(DapNode subnode : group.getGroups()) {
                    print(subnode);
                    printer.eol();
                }
            printer.outdent();
            printer.marginPrint("</" + dmrname + ">");
            break;

        case DIMENSION:
            DapDimension dim = (DapDimension) node;
            if(!dim.isShared()) break; // ignore, here, anonymous dimensions
            printer.marginPrint("<" + dmrname);
            printXMLAttributes(node, NILFLAGS);
            if(hasMetadata(node)) {
                printer.println(">");
                printMetadata(node);
                printer.marginPrint("</" + dmrname + ">");
            } else {
                printer.print("/>");
            }
            break;

        case ENUMERATION:
            DapEnum en = (DapEnum) node;
            printer.marginPrint("<" + dmrname);
            printXMLAttributes(en, NILFLAGS);
            printer.println(">");
            printer.indent();
            List<String> econstnames = en.getNames();
            for(String econst : econstnames) {
                Long value = en.lookup(econst);
                assert (value != null);
                printer.marginPrintln(
                    String.format("<EnumConst name=\"%s\" value=\"%s\"/>",
                        Escape.entityEscape(econst), value.toString()));
            }
            printMetadata(node);
            printer.outdent();
            printer.marginPrint("</" + dmrname + ">");
            break;

        case STRUCTURE:
        case SEQUENCE:
            DapStructure struct = (DapStructure) node;
            printer.marginPrint("<" + dmrname);
            printXMLAttributes(node, NILFLAGS);
            printer.println(">");
            printer.indent();
            for(DapVariable field : struct.getFields()) {
                print(field);
                printer.eol();
            }
            printDimrefs(struct);
            printMetadata(node);
            printMaps(struct);
            printer.outdent();
            printer.marginPrint("</" + dmrname + ">");
            break;

        case ATOMICVARIABLE:
            DapAtomicVariable var = (DapAtomicVariable) node;
            // Get the type sort of the variable
            DapType basetype = var.getBaseType();
            printer.marginPrint("<" + basetype.getAtomicType().name());
            printXMLAttributes(node, NILFLAGS);
            if(hasMetadata(node) || hasDimensions(var) || hasMaps(var)) {
                printer.println(">");
                printer.indent();
                if(hasDimensions(var))
                    printDimrefs(var);
                if(hasMetadata(var))
                    printMetadata(var);
                if(hasMaps(var))
                    printMaps(var);
                printer.outdent();
                printer.marginPrint("</" + basetype.getAtomicType().name() + ">");
            } else
                printer.print("/>");
            break;

        default:
            assert (false) : "Unexpected sort: " + sort.name();
            break;
        }

    }

    void
    printXMLAttributes(DapNode node, int flags)
        throws DapException
    {
        if((flags & PERLINE) != 0)
            printer.indent(2);

        // Print name first, if non-null and !NONAME
        // Note that the short name needs to use
        // entity escaping (which is done by printXMLattribute),
        // but backslash escaping is not required.
        String name = node.getShortName();
        if(name != null && (flags & NONAME) == 0) {
            name = node.getShortName();
            printXMLAttribute("name", name, flags);
        }

        switch (node.getSort()) {
        case DATASET:
            DapDataset dataset = (DapDataset) node;
            printXMLAttribute("dapVersion", dataset.getDapVersion(), flags);
            printXMLAttribute("dmrVersion", dataset.getDMRVersion(), flags);
            // boilerplate
            printXMLAttribute("xmlns", "http://xml.opendap.org/ns/DAP/4.0#", flags);
            printXMLAttribute("xmlns:dap", "http://xml.opendap.org/ns/DAP/4.0#", flags);
            break;

        case DIMENSION:
            DapDimension orig = (DapDimension) node;
            long size = orig.getSize();
            printXMLAttribute("size", Long.toString(size), flags);
            break;

        case ENUMERATION:
            printXMLAttribute("basetype", ((DapEnum) node).getBaseType().getTypeName(), flags);
            break;

        case ATOMICVARIABLE:
            DapAtomicVariable atom = (DapAtomicVariable) node;
            DapType basetype = atom.getBaseType();
            if(basetype.isEnumType()) {
                printXMLAttribute("enum", basetype.getTypeName(), flags);
            }
            break;

        case ATTRIBUTE:
            DapAttribute attr = (DapAttribute) node;
            basetype = attr.getBaseType();
            printXMLAttribute("type", basetype.getTypeName(), flags);
            if(attr.getBaseType().isEnumType()) {
                printXMLAttribute("enum", basetype.getTypeName(), flags);
            }
            break;

        default:
            break; // node either has no attributes or name only
        } //switch

        if((flags & PERLINE) != 0) {
            printer.outdent(2);
        }
    }

    /**
     * PrintXMLAttributes helper function
     */
    void
    printXMLAttribute(String name, String value, int flags)
        throws DapException
    {
        if(name == null) return;
        if((flags & NONNIL) == 0
            && (value == null || value.length() == 0))
            return;
        if((flags & PERLINE) != 0) {
            printer.eol();
            printer.margin();
        }
        printer.print(" " + name + "=");
        printer.print("\"");

        if(value != null) {
            // add xml entity escaping
            value = Escape.entityEscape(value);
            printer.print(value);
        }
        printer.print("\"");
    }

    void
    printMetadata(DapNode node)
        throws DapException
    {

        Map<String, DapAttribute> attributes = node.getAttributes();
        if(attributes.size() == 0) {
            return;
        }
        for(String key : attributes.keySet()) {
            DapAttribute attr = attributes.get(key);
            assert (attr != null);
            switch (attr.getSort()) {
            case ATTRIBUTE:
                printAttribute(attr);
                break;
            case ATTRIBUTESET:
                printContainerAttribute(attr);
                break;
            case OTHERXML:
                printOtherXML(attr);
                break;
            }
        }
    }

    void
    printContainerAttribute(DapAttribute attr)
    {
    }

    void
    printOtherXML(DapAttribute attr)
    {
    }

    void
    printAttribute(DapAttribute attr)
        throws DapException
    {
        printer.marginPrint("<Attribute");
        printXMLAttributes(attr, NILFLAGS);
        List<Object> values = attr.getValues();
        printer.println(">");
        if(values == null)
            throw new DapException("Attribute with no values:" + attr.getFQN());
        printer.indent();
        if(values.size() == 1) {
            printer.marginPrintln(String.format("<Value value=\"%s\"/>", getPrintValue(values.get(0))));
        } else {
            printer.marginPrintln("<Value>");
            printer.indent();
            for(Object value : values) {
                printer.marginPrint(getPrintValue(value));
                printer.eol();
            }
            printer.outdent();
            printer.marginPrintln("</Value>");
        }
        printer.outdent();
        printer.marginPrintln("</Attribute>");
    }

    /**
     * Print the dimrefs for a variable's dimensions.
     * If the variable has a non-whole projection, then use size
     * else use the dimension name.
     *
     * @param var whole dimensions are to be printed
     * @throws DapException
     */
    void
    printDimrefs(DapVariable var)
        throws DapException
    {
        if(var.getRank() == 0) return;
        List<DapDimension> dimset = var.getDimensions();
        for(int i = 0;i < var.getRank();i++) {
            DapDimension dim = dimset.get(i);
            printer.marginPrint("<Dim");
            if(dim.isShared()) {
                String fqn = dim.getFQN();
                assert (fqn != null) : "Illegal Dimension reference";
                printXMLAttribute("name", fqn, NILFLAGS);
            } else {
                printXMLAttribute("size", Integer.toString((int) dim.getSize()), NILFLAGS);
            }
            printer.println("/>");
        }
    }

    void
    printMaps(DapVariable parent)
        throws DapException
    {
        List<DapMap> maps = parent.getMaps();
        if(maps.size() == 0) return;
        for(DapMap map : maps) {
            printer.marginPrint("<Map");
            // Separate out name attribute so we can use FQN.
            String name = map.getFQN();
            assert (name != null) : "Illegal <Map> reference";
            printXMLAttribute("name", name, NILFLAGS);
            printXMLAttributes(map, NONAME);
            if(hasMetadata(map)) {
                printer.println(">");
                printer.indent();
                printMetadata(map);
                printer.outdent();
                printer.marginPrintln("</Map>");
            } else
                printer.println("/>");
        }
    }

    //////////////////////////////////////////////////

    protected void
    printData()
        throws DataException
    {
        printer.setIndent(0);
        printer.marginPrintln("<data>");
        List<DataVariable> topvars = data.getTopVariables();
        for(int i = 0;i < topvars.size();i++) {
            printVariable(topvars.get(i));
        }
        printer.setIndent(0);
        printer.marginPrintln("</data>");
    }

    protected void
    printVariable(DataVariable datav)
        throws DataException
    {
        DapVariable dapv = (DapVariable) datav.getTemplate();
        DapType daptype = dapv.getBaseType();
        if(datav.getSort() == DataSort.ATOMIC) {
            D4DataAtomic atom = (D4DataAtomic) datav;
            DapAtomicVariable datom = (DapAtomicVariable) dapv;
            long nelems = atom.getCount();
            long nrows = (nelems > 0 ? (nelems + (COLUMNS - 1)) / nelems : 0);
            printer.marginPrintf("<%s name=\"%s\">",
                dapv.getBaseType().getAtomicType().name(),
                dapv.getShortName());
            printer.eol();
            printer.indent();
            for(int r = 0;r < nrows;r++) {
                for(int c = 0;c < COLUMNS;c++) {
                    if(c > 0) printer.print(", ");
                    try {
                        Object value = atom.read((nrows * COLUMNS) + c);
                        printer.print(valueString(value, daptype));
                    } catch (IOException ioe) {
                        throw new DataException(ioe);
                    }
                }
                printer.eol();
            }
            printer.outdent();
            printer.marginPrintf("</%s>", dapv.getBaseType().getAtomicType().name());
        } else if(datav.getSort() == DataSort.COMPOUNDARRAY) {
            try {
                D4DataCompoundArray cmpd = (D4DataCompoundArray) datav;
                DapStructure dstruct = (DapStructure) dapv;
                List<DapVariable> dfields = dstruct.getFields();
                long nelems = cmpd.getCount();
                Odometer odom = new Odometer(dstruct.getDimensions());
                while(odom.hasNext()) {
                    String sindices = indicesToString(odom.getIndices());
                    printer.marginPrintf("<Structure name=\"%s\" indices=\"%s\">",
                        dapv.getShortName(),
                        sindices);
                    printer.eol();
                    DataStructure datastruct = (DataStructure) cmpd.read(odom.index());
                    printVariable(datastruct);
                    printer.marginPrintln("</Structure>");
                }
            } catch (IOException ioe) {
                throw new DataException(ioe);
            }
        } else if(datav.getSort() == DataSort.STRUCTURE) {
            // Print <Structure> only if scalar
            D4DataStructure struct = (D4DataStructure) datav;
            DapStructure dstruct = (DapStructure) dapv;
            List<DapVariable> dfields = dstruct.getFields();
            if(dstruct.getRank() == 0)
                printer.marginPrintf("<Structure name=\"%s\">",
                    dapv.getShortName());
            printer.indent();
            for(int f = 0;f < dfields.size();f++) {
                DataVariable dv = struct.readfield(f);
                printVariable(dv);
            }
            printer.outdent();
            if(dstruct.getRank() == 0)
                printer.marginPrintln("</Structure>");
        } else
            throw new DataException("Attempt to treat non-variable as variable:" + dapv.getFQN());
    }

    protected String
    indicesToString(long[] indices)
        throws DapException
    {
        StringBuilder buf = new StringBuilder();
        if(indices != null && indices.length > 0) {
            for(int i = 0;i < indices.length;i++) {
                buf.append(i == 0 ? LBRACKET : ",");
                buf.append(String.format("%d", indices[i]));
            }
            buf.append(RBRACKET);
        }
        return buf.toString();
    }

    protected String
    valueString(Object value, DapType basetype)
        throws DataException
    {
        if(value == null) return "null";
        AtomicType atype = basetype.getAtomicType();
        boolean unsigned = atype.isUnsigned();
        switch (atype) {
        case Int8:
        case UInt8:
            long lvalue = ((Byte) value).longValue();
            if(unsigned) lvalue &= 0xFFL;
            return String.format("%d", lvalue);
        case Int16:
        case UInt16:
            lvalue = ((Short) value).longValue();
            if(unsigned) lvalue &= 0xFFFFL;
            return String.format("%d", lvalue);
        case Int32:
        case UInt32:
            lvalue = ((Integer) value).longValue();
            if(unsigned) lvalue &= 0xFFFFFFFFL;
            return String.format("%d", lvalue);
        case Int64:
        case UInt64:
            lvalue = ((Long) value).longValue();
            if(unsigned) {
                BigInteger b = BigInteger.valueOf(lvalue);
                b = b.and(DapUtil.BIG_UMASK64);
                return b.toString();
            } else
                return String.format("%d", lvalue);
        case Float32:
            return String.format("%f", ((Float) value).floatValue());
        case Float64:
            return String.format("%f", ((Double) value).doubleValue());
        case Char:
            return "'" + ((Character) value).toString() + "'";
        case String:
        case URL:
            return "\"" + ((String) value) + "\"";
        case Opaque:
            ByteBuffer opaque = (ByteBuffer) value;
            String s = "0x";
            for(int i = 0;i < opaque.limit();i++) {
                byte b = opaque.get(i);
                char c = hexchar((b >> 4) & 0xF);
                s += c;
                c = hexchar((b) & 0xF);
                s += c;
            }
            return s;
        case Enum:
            return valueString(value, ((DapEnum) basetype).getBaseType());
        default:
            break;
        }
        throw new DataException("Unknown type: " + basetype);
    }


    static char
    hexchar(int i)
    {
        return "0123456789ABCDEF".charAt((i & 0xF));
    }
 /*
    void
    printAtomicArray(Array data, Variable v)
        throws IOException
    {
        if(v.getRank() == 0) {//scalar
            printer.printf("%s = %s;\n",v.getFullName(),valueString(data.getObject(0),v));
        } else {
            IndexIterator odom = data.getIndexIterator();
            while(odom.hasNext()) {
                printer.print(v.getFullName());
                Object value = odom.next();
                // get and print the current set of indices
                int[] indices = odom.getCurrentCounter();
                printIndices(indices);
                printer.printf(" = %s ;\n",valueString(value, v));
            }
        }
    }
*/

    //////////////////////////////////////////////////

/*
    static Map<Variable, ParsedSectionSpec>
    computevarmap(List<String> vars, NetcdfDataset nc)
    {
        Map<Variable, ParsedSectionSpec> varmap = null;
        if(vars != null) {
            varmap = new HashMap<Variable, ParsedSectionSpec>();
            // Convert the specified vars to ParsedSectionSpec format
            for(String v : vars) {
                try {
                    ParsedSectionSpec spec
                        = ParsedSectionSpec.parseVariableSection(nc, v);
                    varmap.put(spec.v, spec);
                } catch (Exception e) {
                    System.err.printf("Invalid variable reference: %s (ignored)\n", v);
                }
            }
            if(varmap.size() == 0)
                usage("No valid variables specified");
            // Based on the included variables, compute the set of
            // structures that have at least one field in the varmap
            // and, if not already there, add them to the map.
            for(Variable v : varmap.keySet()) {
                // Get the transitive set of all parent structures
                List<Structure> parents = parentpath(v);
                // Add in those structure if not already there
                for(Structure s : parents) {
                    if(varmap.get(s) == null)
                        varmap.put(s, createspec(s));
                }
            }
        }
        return varmap;
    }
*/

    /**
     * Compute the transitive set of all parent structures
     * of a given variable
     * <p/>
     * param v the variable of interest
     *
     * @return a Set<Structure> containing the set of all parent structures of v
     *         <p/>
     *         <p/>
     *         static List<Structure>
     *         parentpath(Variable v)
     *         {
     *         List<Structure> path = new ArrayList<Structure>();
     *         Structure parent = v.getParentStructure();
     *         while(parent != null) {
     *         path.add(0, parent);
     *         parent = parent.getParentStructure();
     *         }
     *         return path;
     *         }
     */
    //////////////////////////////////////////////////
    // Main helper methods
    static void
    usage(String msg)
    {
        System.err.println(msg);
        System.err.println("usage: java -jar Dap4Print"
            + "-o <output-file>"
            + "<url-with-constraint>"
        );
        System.exit(1);
    }

    static CommandlineOptions
    getopts(String[] argv)
    {
        // Get command line options
        Options options = new Options();
        options.addOption("o", true, "send output to this file");

        CommandLineParser clparser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = clparser.parse(options, argv);
        } catch (ParseException pe) {
            usage("Command line parse failure: " + pe.getMessage());
        }

        CommandlineOptions cloptions = new CommandlineOptions();
        String[] remainder = cmd.getArgs();
        if(remainder.length > 0)
            cloptions.path = remainder[0];
        if(cmd.hasOption("o")) {
            cloptions.outputfile = cmd.getOptionValue("o");
        }
        return cloptions;
    }

    //////////////////////////////////////////////////
    // Main

    /**
     * Main program.
     * See usage() for command line arguments.
     * Default is to dump the header info only.
     *
     * @param argv command line arguments
     */
    static public void
    main(String[] argv)
    {
        try {

            if(argv.length == 0)
                usage("");

            CommandlineOptions options = getopts(argv);
            PrintWriter output = null;
            if(options.outputfile != null) {
                File f = new File(options.outputfile);
                if(!f.canWrite())
                    usage("Cannot write to output file: " + options.outputfile);
                output = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), DapUtil.UTF8));
            } else
                output = new PrintWriter(new OutputStreamWriter(System.out, DapUtil.UTF8));
            Dap4Print d4printer = new Dap4Print(options.path, output);
            d4printer.print();
            output.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    //////////////////////////////////////////////////
    // Misc. Static Utilities

    static protected String
    getPrintValue(Object value)
    {
        if(value instanceof String) {
            return Escape.entityEscape((String) value);
        } else
            return value.toString();
    }

    static protected boolean hasMetadata(DapNode node)
    {
        return node.getAttributes().size() > 0;
    }

    static protected boolean hasMaps(DapVariable var)
    {
        return var.getMaps().size() > 0;
    }

    static protected boolean hasDimensions(DapVariable var)
    {
        return var.getDimensions().size() > 0;
    }

}


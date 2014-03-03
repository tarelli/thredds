/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

import dap4.core.data.*;
import dap4.core.dmr.DapVariable;

abstract public class D4DataVariable extends AbstractData implements DataVariable
{
    //////////////////////////////////////////////////
    // Instance variables

    protected D4DSP dsp = null;
    protected DapVariable dapvar = null;

    //////////////////////////////////////////////////
    // Constructors

    public D4DataVariable(D4DSP dsp, DapVariable dv)
        throws DataException
    {
        super(dv);
        this.dsp = dsp;
        this.dapvar = dv;
    }

    //////////////////////////////////////////////////
    // Accessor(s)

    public D4DSP getDSP()
    {
        return this.dsp;
    }

}
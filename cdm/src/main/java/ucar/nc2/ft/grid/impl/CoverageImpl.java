/*
 * Copyright (c) 1998 - 2013. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.grid.impl;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.ft.grid.Coverage;
import ucar.nc2.ft.grid.CoverageCS;

import java.io.IOException;
import java.util.List;

/**
 * Coverage Implementation
 *
 * @author John
 * @since 12/25/12
 */
public class CoverageImpl implements Coverage {
  private NetcdfDataset ds;
  private CoverageCS ccs;
  private VariableEnhanced ve;
  private VariableDS vds;

  CoverageImpl(NetcdfDataset ds, CoverageCS ccs, VariableEnhanced ve) {
    this.ds = ds;
    this.ccs = ccs;
    this.ve = ve;
    if (ve instanceof VariableDS) vds = (VariableDS) ve;
  }

  @Override
  public String getName() {
    return ve.getShortName();
  }

  @Override
  public String getFullName() {
    return ve.getFullName();
  }

  @Override
  public String getShortName() {
    return ve.getShortName();
  }

  @Override
  public String getDescription() {
    return ve.getDescription();
  }

  @Override
  public String getUnitsString() {
    return ve.getUnitsString();
  }

  @Override
  public int getRank() {
    return ve.getRank();
  }

  @Override
  public int[] getShape() {
    return ve.getShape();     // LOOK - canonicalize ??
  }

  @Override
  public DataType getDataType() {
    return ve.getDataType();
  }

  @Override
  public List<Attribute> getAttributes() {
    return ve.getAttributes();
  }

  @Override
  public Attribute findAttributeIgnoreCase(String name) {
    return ve.findAttributeIgnoreCase(name);
  }

  @Override
  public String findAttValueIgnoreCase(String attName, String defaultValue) {
    return null; // ds.findAttValueIgnoreCase(ve, attName, defaultValue);
  }

  @Override
  public List<Dimension> getDimensions() {
    return ve.getDimensions();
  }

  @Override
  public CoverageCS getCoordinateSystem() {
    return ccs;
  }

  @Override
  public boolean hasMissing() {
    return (vds != null) && vds.hasMissing();
  }

  @Override
  public boolean isMissing(double val) {
    return (vds != null) && vds.isMissing(val);
  }

  @Override
  public String getInfo() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public int compareTo(VariableSimpleIF o) {
    return getShortName().compareTo(o.getShortName());
  }

  @Override
  public String toString() {
    return ve.toString();
  }

  /////////////////

  @Override
  public Array readData(CoverageCS.Subset subset) throws IOException, InvalidRangeException {
    CoverageCSImpl.SubsetImpl impl = (CoverageCSImpl.SubsetImpl) subset;
    return impl.readData(ve);
  }


}

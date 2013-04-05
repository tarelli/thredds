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

import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft.grid.FmrcCS;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;

/**
 * Description
 *
 * @author John
 * @since 12/25/12
 */
public class FmrcCSImpl extends CoverageCSImpl implements FmrcCS {
  private CoordinateAxis1DTime[] timeAxisForRun;

  protected FmrcCSImpl(NetcdfDataset ds, CoordinateSystem cs, CoverageCSFactory fac) {
    super(ds, cs, fac);
  }

  @Override
  public CoordinateAxis1DTime getRunTimeAxis() {
    return (CoordinateAxis1DTime) cs.findAxis(AxisType.RunTime);
  }

  @Override
  public CoordinateAxis1DTime getTimeAxisForRun(CalendarDate runTime) {
    CoordinateAxis1DTime runTimeAxis = getRunTimeAxis();
    int runIndex = runTimeAxis.findTimeIndexFromCalendarDate(runTime);

    int nruns = (int) runTimeAxis.getSize();
    if ((runIndex < 0) || (runIndex >= nruns))
      throw new IllegalArgumentException("getTimeAxisForRun index out of bounds= " + runIndex);

    if (timeAxisForRun == null)
      timeAxisForRun = new CoordinateAxis1DTime[nruns];

    if (timeAxisForRun[runIndex] == null)
      timeAxisForRun[runIndex] = makeTimeAxisForRun(runIndex);

    return timeAxisForRun[runIndex];
  }

  private CoordinateAxis1DTime makeTimeAxisForRun(int run_index) {
    CoordinateAxis tAxis = getTimeAxis();
    VariableDS section;
    try {
      section = (VariableDS) tAxis.slice(0, run_index);
      return CoordinateAxis1DTime.factory(ds, section, null);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

}

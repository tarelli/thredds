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
package ucar.nc2.ft.grid;

import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.time.CalendarDate;

import java.util.Date;

/**
 * Description
 *
 * @author John
 * @since 12/23/12
 */
public interface FmrcCS extends CoverageCS {

  /**
   * Get the RunTime axis. Must be 1 dimensional.
   * A runtime coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * Typical meaning is the date that a Forecast Model Run is made.
   *
   * @return RunTime CoordinateAxis, may be null.
   */
  public CoordinateAxis1DTime getRunTimeAxis();

  /**
   * This is the case of a 2D time axis, which depends on the run index.
   * A time coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   *
   * @param runTime which run?
   * @return 1D time axis for that run.
   */
  public CoordinateAxis1DTime getTimeAxisForRun(CalendarDate runTime);
}

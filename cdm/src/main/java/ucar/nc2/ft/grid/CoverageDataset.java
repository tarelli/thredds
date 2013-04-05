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

import java.util.List;

/**
 * A feature datasets containing coverages : grids, swaths, images.
 *
 * @author John
 * @since 12/25/12
 */
public interface CoverageDataset extends ucar.nc2.ft.FeatureDataset {

  /** get the list of Coverages  contained in this dataset.
   * @return  list of Coverages
   */
  public List<Coverage> getCoverages();

  /** find the named Coverage.
   * @param name full unescaped name
   * @return  the named Coverage, or null if not found
   */
  public Coverage findCoverage(String name);

  /**
   * Return Coverage objects grouped by Coordinate System.
   * @return CoverageSets
   */
  public List<CoverageSet> getCoverageSets();

  /**
   * A set of Coverage objects with the same Coordinate System.
   */
  public interface CoverageSet {

    /** Get list of Coverage objects with same Coordinate System
     * @return list of Coverages
     */
    public List<Coverage> getCoverages();

    /** all the Coverage in this CoverageSet use this CoverageCS
     * @return  the common CoverageCS
     */
    public ucar.nc2.ft.grid.CoverageCS getCoverageCS();
  }

}

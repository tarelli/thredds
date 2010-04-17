/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.fmrc;

import net.jcip.annotations.ThreadSafe;
import thredds.inventory.FeatureCollection;
import ucar.nc2.*;
import ucar.nc2.constants.CF;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.util.CancelTask;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.ma2.*;

import java.io.IOException;
import java.util.*;

/**
 * Helper class for Fmrc.
 * The various GridDatasets must be thread-safe.
 * <p/>
 * Time coordinate values come from the FmrcInv, so there is little I/O here.
 * Non-aggregation variables are either cached or have DatasetProxyReaders ser so the file is opened when the variable needs to be read.
 * <p/>
 * The prototype dataset is kept separate, since the common case is that just the time coordinates have changed.
 * <p/>
 * This replaces ucar.nc2.dt.fmrc.FmrcImpl
 *
 *
 * @author caron
 * @since Jan 19, 2010
 */
@ThreadSafe
class FmrcDataset {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FmrcDataset.class);
  static private final boolean debugEnhance = false, debugRead = false;

  private final FeatureCollection.Config config;
  //private List<String> protoList; // the list of datasets in the proto that have proxy reader, so these need to exist. not implemented yet

  // allow to build a new state while old state can still be safely used
  private class State {
    NetcdfDataset proto; // once built, the proto doesnt change until setInventory is called with forceProto = true
    FmrcInvLite lite; // lightweight version of the FmrcInv

    private State(NetcdfDataset proto, FmrcInvLite lite) {
      this.proto = proto;
      this.lite = lite;
    }
  }
  private State state;
  private Object lock= new Object();

  FmrcDataset(FeatureCollection.Config config) {
    this.config = config;
  }

  List<Date> getRunDates() {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return localState.lite.getRunDates();
  }

  List<Date> getForecastDates() {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return localState.lite.getForecastDates();
  }

  double[] getForecastOffsets() {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return localState.lite.getForecastOffsets();
  }

  /**
   * Make the 2D dataset
   * @param result     use this empty NetcdfDataset, may be null (used by NcML)
   * @return 2D dataset
   * @throws IOException on read error
   */
  GridDataset getNetcdfDataset2D(NetcdfDataset result) throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset2D(result,  localState.proto, localState.lite);
  }

  GridDataset getBest() throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset1D( localState.proto, localState.lite, localState.lite.makeBestDatasetInventory());
  }

  GridDataset getRunTimeDataset(Date run) throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset1D(  localState.proto, localState.lite, localState.lite.makeRunTimeDatasetInventory( run));
  }

  GridDataset getConstantForecastDataset(Date run) throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset1D(  localState.proto, localState.lite, localState.lite.getConstantForecastDataset( run));
  }

  GridDataset getConstantOffsetDataset(double offset) throws IOException {
    State localState;
    synchronized (lock) {
      localState = state;
    }
    return buildDataset1D(  localState.proto, localState.lite, localState.lite.getConstantOffsetDataset( offset));
  }

  /**
   * Set a new FmrcInv and optionally a proto dataset
   *
   * @param fmrcInv    based on this inventory
   * @param forceProto create a new proto, else use existing one
   * @throws IOException on read error
   */
  void setInventory(FmrcInv fmrcInv, boolean forceProto) throws IOException {
    NetcdfDataset protoLocal = null;

    if (state == null || forceProto) {
      protoLocal = buildProto(fmrcInv, config.protoConfig);
    }

    FmrcInvLite liteLocal = new FmrcInvLite(fmrcInv);

    synchronized (lock) {
      if (protoLocal == null) protoLocal = state.proto;
      state = new State(protoLocal, liteLocal);
    }
  }

   /////////////////////////////////////////////////////////////////////////////
  // the prototypical dataset

  private NetcdfDataset buildProto(FmrcInv fmrcInv, FeatureCollection.ProtoConfig protoConfig) throws IOException {
    NetcdfDataset result = new NetcdfDataset(); // empty

    // choose some run in the list
    List<FmrInv> list = fmrcInv.getFmrInv();
    int protoIdx = 0;
    switch (protoConfig.choice) {
      case First:
        protoIdx = 0;
        break;
      case Random:
        Random r = new Random(System.currentTimeMillis());
        protoIdx = r.nextInt(list.size() - 1);
        break;
      case Penultimate:
        protoIdx = Math.max(list.size() - 2, 0);
        break;
      case Latest:
        protoIdx = Math.max(list.size() - 1, 0);
        break;
    }
    FmrInv proto = list.get(protoIdx); // use this one

    HashMap<String, NetcdfDataset> openFilesProto = new HashMap<String, NetcdfDataset>();

    // create the union of all objects in that run
    // this covers the case where the variables are split across files
    Set<GridDatasetInv> files = proto.getFiles();
    if (logger.isDebugEnabled()) logger.debug("FmrcDataset: proto= " + proto.getName()+" "+ proto.getRunDate()+" collection= "+config.spec);
    for (GridDatasetInv file : files) {
      NetcdfDataset ncfile = open(file.getLocation(), openFilesProto);
      transferGroup(ncfile.getRootGroup(), result.getRootGroup(), result);
      if (logger.isDebugEnabled()) logger.debug("FmrcDataset: proto dataset= " + file.getLocation());
    }

    // some additional global attributes
    Group root = result.getRootGroup();
    root.addAttribute(new Attribute("Conventions", "CF-1.4, " + _Coordinate.Convention));
    root.addAttribute(new Attribute("cdm_data_type", FeatureType.GRID.toString()));
    root.addAttribute(new Attribute("CF:feature_type", FeatureType.GRID.toString()));

    // remove some attributes that can cause trouble
    root.remove(root.findAttribute(_Coordinate.ModelRunDate));
    root.remove(root.findAttribute("location"));

    // protoList = new ArrayList<String>();
    // these are the non-agg variables - store data or ProxyReader in proto
    for (Variable v : root.getVariables()) {
      // see if its a non-agg variable
      FmrcInv.UberGrid grid = fmrcInv.findUberGrid(v.getName());
      if (grid == null) { // only non-agg vars need to be cached
        Variable orgV = (Variable) v.getSPobject();

        // see if its a time coordinate
        if (orgV instanceof CoordinateAxis) {
          CoordinateAxis axis = (CoordinateAxis) orgV;
          AxisType type = axis.getAxisType();
          if (type != null && (type == AxisType.Time || type == AxisType.RunTime)) {
            root.removeVariable(v.getShortName());
            v.setSPobject(null); // clear the reference to orgV
            continue;
          }
        }
        
        v.setCachedData(orgV.read()); // read from original - store in proto

        // not implemented yet
        /* if (config.protoConfig.cacheAll || v.isCaching() || v.isCoordinateVariable()) { // want to cache
          v.setCachedData(orgV.read()); // read from original - store in proto
        } else {
          String location = orgV.getParentGroup().getNetcdfFile().getLocation(); // hmmmmm
          v.setProxyReader(new DatasetProxyReader(location));  // keep track of original file
          protoList.add(location);
        } */
      }

      v.setSPobject(null); // clear the reference to orgV for all of proto
      v.removeAttribute(_Coordinate.Axes); // remove more troublesome attributes
    }

    result.finish();

    // data is read and cached, can close fiels now
    closeAll(openFilesProto);

    return result;
  }

  // transfer the objects in src group to the target group, unless that name already exists
  // Dimensions, Variables, Groups are not transferred, but an equivilent object is created with same metadata
  // Attributes and EnumTypedef are transferred, these are immutable with no references to container

  private void transferGroup(Group srcGroup, Group targetGroup, NetcdfDataset target) {
    // group attributes
    DatasetConstructor.transferGroupAttributes(srcGroup, targetGroup);

    // dimensions
    for (Dimension d : srcGroup.getDimensions()) {
      if (null == targetGroup.findDimensionLocal(d.getName())) {
        Dimension newd = new Dimension(d.getName(), d.getLength(), d.isShared(), d.isUnlimited(), d.isVariableLength());
        targetGroup.addDimension(newd);
      }
    }

    // transfer variables - eliminate any references to component files
    for (Variable v : srcGroup.getVariables()) {
      Variable targetV = targetGroup.findVariable(v.getShortName());

      if (null == targetV) { // add it
        if (v instanceof Structure) {
          targetV = new StructureDS(target, targetGroup, null, v.getShortName(), v.getDimensionsString(), v.getUnitsString(), v.getDescription());
          //LOOK - not adding the members here - what to do ??

        } else {
          targetV = new VariableDS(target, targetGroup, null, v.getShortName(), v.getDataType(), v.getDimensionsString(), v.getUnitsString(), v.getDescription());
        }

        DatasetConstructor.transferVariableAttributes(v, targetV);
        targetV.setSPobject(v); //temporary, for non-agg variables when proto is made
        if (v.hasCachedData())
          targetV.setCachedData(v.getCachedData()); //
        targetGroup.addVariable(targetV);
      }
    }

    // nested groups - check if target already has it
    for (Group srcNested : srcGroup.getGroups()) {
      Group nested = targetGroup.findGroup(srcNested.getShortName());
      if (null == nested) {
        nested = new Group(target, targetGroup, srcNested.getShortName());
        targetGroup.addGroup(nested);
        for (EnumTypedef et : srcNested.getEnumTypedefs()) {
          targetGroup.addEnumeration(et);
        }
      }
      transferGroup(srcNested, nested, target);
    }
  }

  /////////////////////////////////////////////////////////
  // constructing the dataset

  private String getRunDimensionName() {
    return "run";
  }

  private String getCoordinateList(VariableDS aggVar, String timeCoordName) {
    String coords = "";
    Attribute att = aggVar.findAttribute(CF.COORDINATES);
    if (att != null)
      coords = att.getStringValue();

    return getRunDimensionName() + " " + "forecast_" + timeCoordName + " " + coords;
  }

  private void addAttributeInfo(NetcdfDataset result, String attName, String info) {
    Attribute att = result.findGlobalAttribute(attName);
    if (att == null)
      result.addAttribute(null, new Attribute(attName, info));
    else {
      String oldValue = att.getStringValue();
      result.addAttribute(null, new Attribute(attName, oldValue +" ;\n"+ info));
    }
  }

  /**
   * Build the 2D time dataset, make it immutable so it can be shared across threads
   *
   * @param result     place results in here, if null create a new one. must be threadsafe (immutable)
   * @param proto      current proto dataset
   * @param lite       current inventory
   * @return resulting GridDataset
   * @throws IOException on read error
   */
  private GridDataset buildDataset2D(NetcdfDataset result, NetcdfDataset proto, FmrcInvLite lite) throws IOException {
    // make a copy, so that this object can coexist with previous incarnations
    if (result == null) result = new NetcdfDataset();
    result.setLocation(lite.name);
    transferGroup(proto.getRootGroup(), result.getRootGroup(), result);
    result.finish();
    addAttributeInfo(result, "history", "FMRC 2D Dataset");

    // create runtime aggregation dimension
    double[] runOffset = lite.runOffset;
    String runtimeDimName = getRunDimensionName();
    int nruns = runOffset.length;
    Dimension runDim = new Dimension(runtimeDimName, nruns);
    result.removeDimension(null, runtimeDimName); // remove previous declaration, if any
    result.addDimension(null, runDim);

    // deal with promoteGlobalAttribute
    // promoteGlobalAttributes((AggregationOuterDimension.DatasetOuterDimension) typicalDataset);

    DateFormatter dateFormatter = new DateFormatter();
    ProxyReader2D proxyReader2D = new ProxyReader2D();

    // extract a copy of the runtimes for thread safety
    // List<Date> runTimes = new ArrayList<Date>(fmrcInv.getRunTimes());

    // create runtime aggregation coordinate variable
    DataType coordType = DataType.DOUBLE; // LOOK getCoordinateType();
    VariableDS runtimeCoordVar = new VariableDS(result, null, null, runtimeDimName, coordType, runtimeDimName, null, null);
    runtimeCoordVar.addAttribute(new Attribute("long_name", "Run time for ForecastModelRunCollection"));
    runtimeCoordVar.addAttribute(new ucar.nc2.Attribute("standard_name", "forecast_reference_time"));
    runtimeCoordVar.addAttribute(new ucar.nc2.Attribute("units", "hours since " + dateFormatter.toDateTimeStringISO(lite.base)));
    runtimeCoordVar.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));
    result.removeVariable(null, runtimeCoordVar.getShortName());
    result.addVariable(null, runtimeCoordVar);
    if (logger.isDebugEnabled()) logger.debug("FmrcDataset: added runtimeCoordVar " + runtimeCoordVar.getName());

    // make the runtime coordinates
    Array runCoordVals = ArrayDouble.factory(DataType.DOUBLE, new int[] {nruns}, runOffset);
    runtimeCoordVar.setCachedData(runCoordVals);

    // make the time coordinate(s) as 2D
    List<Variable> nonAggVars = result.getVariables();
    for (FmrcInvLite.Gridset gridset : lite.gridSets) {
      Group newGroup = result.getRootGroup(); // can it be different ??

      //int noffsets = runSeq.getNTimeOffsets();
      Dimension timeDim = new Dimension(gridset.name, gridset.noffsets);
      result.removeDimension(null, gridset.name); // remove previous declaration, if any
      result.addDimension(null, timeDim);

      DataType dtype = DataType.DOUBLE;
      String dims = getRunDimensionName() + " " + gridset.name;
      VariableDS timeVar = new VariableDS(result, newGroup, null, "forecast_" + gridset.name, dtype, dims, null, null); // LOOK could just make a CoordinateAxis1D
      timeVar.addAttribute(new Attribute("long_name", "Forecast time for ForecastModelRunCollection"));
      timeVar.addAttribute(new ucar.nc2.Attribute("standard_name", "time"));
      timeVar.addAttribute(new ucar.nc2.Attribute("units", "hours since " + dateFormatter.toDateTimeStringISO(lite.base)));
      timeVar.addAttribute(new ucar.nc2.Attribute("missing_value", Double.NaN));
      timeVar.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

      // remove the old one if any
      newGroup.removeVariable(gridset.name);
      newGroup.addVariable(timeVar);

      Array timeCoordVals = Array.factory(DataType.DOUBLE, timeVar.getShape(), gridset.timeOffset);
      timeVar.setCachedData(timeCoordVals);

      // promote all grid variables to agg variables
      for (FmrcInvLite.Gridset.Grid ugrid : gridset.grids) {
        VariableDS aggVar = (VariableDS) result.findVariable(ugrid.name);
        if (aggVar == null) {
          logger.error("cant find ugrid variable "+ugrid.name+" in collection "+config.spec);
          if (config.spec.contains("Alaska"))
            for (Variable v : result.getVariables())
              logger.debug(" proto variable = "+v.getName());

          continue; // skip
        }

        // create dimension list
        List<Dimension> dimList = aggVar.getDimensions();
        dimList = dimList.subList(1, dimList.size());  // LOOK assumes time is outer dimension
        dimList.add(0, timeDim);
        dimList.add(0, runDim);

        aggVar.setDimensions(dimList);
        aggVar.setProxyReader(proxyReader2D);
        aggVar.setSPobject(ugrid);
        nonAggVars.remove(aggVar);

        // we need to explicitly list the coordinate axes, because time coord is now 2D
        String coords = getCoordinateList(aggVar, gridset.name);
        aggVar.removeAttribute(_Coordinate.Axes);
        aggVar.addAttribute(new Attribute(CF.COORDINATES, coords)); // CF
      }
    }

    result.finish();  // this puts the new dimensions into the global structures

    CoordSysBuilderIF builder = result.enhance();
    if (debugEnhance) System.out.printf("parseInfo = %s%n", builder.getParseInfo());

    return new ucar.nc2.dt.grid.GridDataset(result);
  }

  /*
  private ArrayDouble.D2 makeTimeCoordinateData2D(Date baseDate, FmrcInv.RunSeq runSeq, VariableDS timeVar) { // throws IOException {
    int[] shape = timeVar.getShape();
    int nruntimes = shape[0];
    int noffsets = shape[1];

    ArrayDouble.D2 timeCoordVals = new ArrayDouble.D2(nruntimes, noffsets);
    Index ima = timeCoordVals.getIndex();
    MAMath.setDouble(timeCoordVals, Double.NaN);  // all default to missing

    int runIdx = 0;
    for (TimeCoord tc : runSeq.getTimes()) {
      double runOffset = FmrcInv.getOffsetInHours(baseDate, tc.getRunDate());
      double[] offset = tc.getOffsetHours();
      for (int j = 0; j < offset.length; j++)
        timeCoordVals.setDouble(ima.set(runIdx, j), offset[j] + runOffset); // offset reletive to base date
      runIdx++;
    }

    return timeCoordVals;
  }

  // keep track of the ugrid and 2D time coords for each variable, put in SPobject

  private class Vstate2D {
    String varName;
    ArrayObject.D2 location;
    ArrayInt.D2 invIndex;
    Date baseDate;

    private Vstate2D(FmrcInv.UberGrid ugrid, Date baseDate, ArrayDouble.D2 timeCoordVals, List<Date> runDates) {
      this.varName = ugrid.getName();
      this.baseDate = baseDate;

      int[] shape = timeCoordVals.getShape();
      int nruns = shape[0]; // this will always equal the complete set of runs
      int ntimes = shape[1];

      this.location = (ArrayObject.D2) Array.factory(DataType.STRING, shape);
      this.invIndex = (ArrayInt.D2) Array.factory(DataType.INT, shape);

      // loop over runDates
      int gridIdx = 0;
      List<FmrInv.GridVariable> grids = ugrid.getRuns(); // must be sorted by rundate

      for (int runIdx=0; runIdx<nruns; runIdx++ ) {
        Date runDate =  runDates.get(runIdx);

        // do we have a grid for this runDate?
        FmrInv.GridVariable grid = grids.get(gridIdx);
        if (!grid.getRunDate().equals(runDate)) continue;
        gridIdx++; // for next loop

        // loop over actual inventory
        for (GridDatasetInv.Grid inv : grid.getInventory()) {
          double invOffset = FmrcInv.getOffsetInHours(baseDate, inv.tc.getRunDate()); // offset reletive to inv
          double[] offsets = inv.tc.getOffsetHours();
          for (int i = 0; i < offsets.length; i++) {
            int timeIdx = findIndex(timeCoordVals, runIdx, ntimes, invOffset + offsets[i]);
            if (timeIdx >= 0) {
              location.set(runIdx, timeIdx, inv.getLocation());
              invIndex.set(runIdx, timeIdx, i);
            }
          }
        }
      }

    }

    // look in the runIdx row of coords to see if a value matches want, return index else -1
    private int findIndex(ArrayDouble.D2 coords, int runIdx, int ntimes, double want) {
      for (int j=0; j<ntimes; j++)
        if (Misc.closeEnough(coords.get(runIdx, j), want)) return j;
      return -1;
    }

    private TimeInstance findInventory(int runIdx, int timeIdx) {
      String loc = (String) location.get(runIdx, timeIdx);
      if (loc == null) return null;
      return new TimeInstance(loc, invIndex.get(runIdx, timeIdx));
    }

  }  */

  private class ProxyReader2D implements ProxyReader {

    @Override
    public Array reallyRead(Variable mainv, CancelTask cancelTask) throws IOException {
      try {
        return reallyRead(mainv, mainv.getShapeAsSection(), cancelTask);

      } catch (InvalidRangeException e) {
        throw new IOException(e);
      }
    }

    // here is where agg variables get read

    @Override
    public Array reallyRead(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      FmrcInvLite.Gridset.Grid gridLite = (FmrcInvLite.Gridset.Grid) mainv.getSPobject();

      // read the original type - if its been promoted to a new type, the conversion happens after this read
      DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();

      Array allData = Array.factory(dtype, section.getShape());
      int destPos = 0;

      // assumes the first two dimensions are runtime and time: LOOK: ensemble ??
      List<Range> ranges = section.getRanges();
      Range runRange = ranges.get(0);
      Range timeRange = ranges.get(1);
      List<Range> innerSection = ranges.subList(2, ranges.size());

      // keep track of open file - must be local variable for thread safety
      HashMap<String, NetcdfDataset> openFilesRead = new HashMap<String, NetcdfDataset>();

      // iterate over the desired runs
      Range.Iterator runIter = runRange.getIterator();
      while (runIter.hasNext()) {
        int runIdx = runIter.next();
        //Date runDate = vstate.runTimes.get(runIdx);

        // iterate over the desired forecast times
        Range.Iterator timeIter = timeRange.getIterator();
        while (timeIter.hasNext()) {
          int timeIdx = timeIter.next();
          Array result = null;

          // find the inventory for this grid, runtime, and hour
          TimeInventory.Instance timeInv =  gridLite.getInstance(runIdx, timeIdx);
          if (timeInv != null) {
            if (debugRead) System.out.printf("HIT %d %d ", runIdx, timeIdx);
            result = read(timeInv, gridLite.name, innerSection, openFilesRead); // may return null
            result = MAMath.convert(result, dtype); // just in case it need to be converted
          }

          // missing data
          if (result == null) {
            int[] shape = new Section(innerSection).getShape();
            result = ((VariableDS) mainv).getMissingDataArray(shape); // fill with missing values
            if (debugRead) System.out.printf("MISS %d %d ", runIdx, timeIdx);
          }

          if (debugRead)
            System.out.printf("%d %d reallyRead %s %d bytes start at %d total size is %d%n", runIdx, timeIdx, mainv.getName(), result.getSize(), destPos, allData.getSize());

          Array.arraycopy(result, 0, allData, destPos, (int) result.getSize());
          destPos += result.getSize();
        }
      }

      // close any files used during this operation
      closeAll(openFilesRead);
      return allData;
    }

  }

 /////////////////////////////////////////////////////////////////////////
  // 1D

  /**
   * Build a dataset with a 1D time coordinate.
   *
   * @param proto      current proto dataset
   * @param lite       current inventory
   * @param timeInv         use this to generate the time coordinates
   * @return resulting GridDataset
   * @throws IOException on read error
   */
  private GridDataset buildDataset1D(NetcdfDataset proto, FmrcInvLite lite, TimeInventory timeInv) throws IOException {
    NetcdfDataset result = new NetcdfDataset(); // make a copy, so that this object can coexist with previous incarnations
    result.setLocation(lite.name);
    transferGroup(proto.getRootGroup(), result.getRootGroup(), result);
    result.finish();
    addAttributeInfo(result, "history", "FMRC "+timeInv.getName()+" Dataset");

    DateFormatter dateFormatter = new DateFormatter();
    ProxyReader1D proxyReader1D = new ProxyReader1D();

    // make the time coordinate(s) for each runSeq
    List<Variable> nonAggVars = result.getVariables();
    for (FmrcInvLite.Gridset gridset : lite.gridSets) {

      Group group = result.getRootGroup(); // can it be different ??
      String timeDimName = gridset.name;

      // construct the dimension
      int ntimes = timeInv.getTimeLength(gridset);
      Dimension timeDim = new Dimension(timeDimName, ntimes);
      result.removeDimension(group, timeDimName); // remove previous declaration, if any
      result.addDimension(group, timeDim);

      // optional time coordinate
      double[] timeCoordValues = timeInv.getTimeCoords(gridset);
      if (timeCoordValues != null) {
        VariableDS timeCoord = makeTimeCoordinate(result, group, timeDimName, lite.base, timeCoordValues, dateFormatter);
        group.removeVariable(timeCoord.getShortName());
        group.addVariable(timeCoord);
      }

      // optional runtime coordinate
      double[] runtimeCoordValues = timeInv.getRunTimeCoords(gridset);
      if (runtimeCoordValues != null) {
        VariableDS runtimeCoord = makeRunTimeCoordinate(result, group, timeDimName, lite.base, runtimeCoordValues, dateFormatter);
        group.removeVariable(runtimeCoord.getShortName());
        group.addVariable(runtimeCoord);
      }

      // optional offset coordinate
      double[] offsetCoordValues = timeInv.getOffsetCoords(gridset);
      if (offsetCoordValues != null) {
        VariableDS offsetCoord = makeOffsetCoordinate(result, group, timeDimName, lite.base, offsetCoordValues, dateFormatter);
        group.removeVariable(offsetCoord.getShortName());
        group.addVariable(offsetCoord);
      }

      // promote all grid variables to agg variables
      for (FmrcInvLite.Gridset.Grid ugrid : gridset.grids) {
        //BestInventory bestInv = makeBestInventory(bestTimeCoord, ugrid);

        VariableDS aggVar = (VariableDS) result.findVariable(ugrid.name);
        if (aggVar == null) {
          logger.error("cant find ugrid variable "+ugrid.name+" in collection "+config.spec);
          continue; // skip
        }

        // create dimension list
        List<Dimension> dimList = aggVar.getDimensions();
        dimList = dimList.subList(1, dimList.size());  // LOOK assumes time is outer dimension
        dimList.add(0, timeDim);

        aggVar.setDimensions(dimList);
        aggVar.setProxyReader(proxyReader1D);
        aggVar.setSPobject( new Vstate1D(ugrid, timeInv));
        nonAggVars.remove(aggVar);

        // we need to explicitly list the coordinate axes
        String coords = getCoordinateList(aggVar, timeDimName);
        aggVar.removeAttribute(_Coordinate.Axes);
        aggVar.addAttribute(new Attribute(CF.COORDINATES, coords)); // CF

       // if (logger.isDebugEnabled()) logger.debug("FmrcDataset: added grid " + aggVar.getName());
      }
    }

    result.finish();  // this puts the new dimensions into the global structures

    // these are the non-agg variables - get data or ProxyReader from proto
    for (Variable v : nonAggVars) {
      Variable protoV = proto.findVariable(v.getName());
      if (protoV.hasCachedData()) {
        v.setCachedData(protoV.getCachedData()); // read from original
      } else {
        v.setProxyReader(protoV.getProxyReader());
      }
    }

    CoordSysBuilderIF builder = result.enhance();
    if (debugEnhance) System.out.printf("parseInfo = %s%n", builder.getParseInfo());

    return new ucar.nc2.dt.grid.GridDataset(result);
  }

  private VariableDS makeTimeCoordinate(NetcdfDataset result, Group group, String dimName, Date base, double[] values, DateFormatter dateFormatter) {
    DataType dtype = DataType.DOUBLE;
    VariableDS timeVar = new VariableDS(result, group, null, "forecast_" + dimName, dtype, dimName, null, null); // LOOK could just make a CoordinateAxis1D
    timeVar.addAttribute(new Attribute("long_name", "Forecast time for ForecastModelRunCollection"));
    timeVar.addAttribute(new ucar.nc2.Attribute("standard_name", "time"));
    timeVar.addAttribute(new ucar.nc2.Attribute("units", "hours since " + dateFormatter.toDateTimeStringISO( base)));
    timeVar.addAttribute(new ucar.nc2.Attribute("missing_value", Double.NaN));
    timeVar.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    // construct the values
    int ntimes = values.length;
    ArrayDouble.D1 timeCoordVals = (ArrayDouble.D1) Array.factory( DataType.DOUBLE, new int[] {ntimes}, values);
    timeVar.setCachedData(timeCoordVals);

    return timeVar;
  }

  private VariableDS makeRunTimeCoordinate(NetcdfDataset result, Group group, String dimName, Date base, double[] values, DateFormatter dateFormatter) {
    DataType dtype = DataType.DOUBLE;
    VariableDS timeVar = new VariableDS(result, group, null, "run_" + dimName, dtype, dimName, null, null); // LOOK could just make a CoordinateAxis1D
    timeVar.addAttribute(new Attribute("long_name", "run times for coordinate = forecast_" + dimName));
    timeVar.addAttribute(new ucar.nc2.Attribute("standard_name", "forecast_reference_time"));
    timeVar.addAttribute(new ucar.nc2.Attribute("units", "hours since " + dateFormatter.toDateTimeStringISO( base)));
    timeVar.addAttribute(new ucar.nc2.Attribute("missing_value", Double.NaN));
    timeVar.addAttribute(new ucar.nc2.Attribute(_Coordinate.AxisType, AxisType.RunTime.toString()));

    // construct the values
    int ntimes = values.length;
    ArrayDouble.D1 timeCoordVals = (ArrayDouble.D1) Array.factory( DataType.DOUBLE, new int[] {ntimes}, values);
    timeVar.setCachedData(timeCoordVals);

    return timeVar;
  }

  private VariableDS makeOffsetCoordinate(NetcdfDataset result, Group group, String dimName, Date base, double[] values, DateFormatter dateFormatter) {
    DataType dtype = DataType.DOUBLE;
    VariableDS timeVar = new VariableDS(result, group, null, "offset_" + dimName, dtype, dimName, null, null); // LOOK could just make a CoordinateAxis1D
    timeVar.addAttribute(new Attribute("long_name", "offset hour from start of run for coordinate = forecast_" + dimName));
    timeVar.addAttribute(new ucar.nc2.Attribute("standard_name", "forecast_period"));
    timeVar.addAttribute(new ucar.nc2.Attribute("units", "hours since " + dateFormatter.toDateTimeStringISO( base)));
    timeVar.addAttribute(new ucar.nc2.Attribute("missing_value", Double.NaN));

    // construct the values
    int ntimes = values.length;
    ArrayDouble.D1 timeCoordVals = (ArrayDouble.D1) Array.factory( DataType.DOUBLE, new int[] {ntimes}, values);
    timeVar.setCachedData(timeCoordVals);

    return timeVar;
  }

  private class Vstate1D {
    FmrcInvLite.Gridset.Grid gridLite;
    TimeInventory timeInv;

    private Vstate1D(FmrcInvLite.Gridset.Grid gridLite, TimeInventory timeInv) {
      this.gridLite = gridLite;
      this.timeInv = timeInv;
    }
  }

  private class ProxyReader1D implements ProxyReader {

    @Override
    public Array reallyRead(Variable mainv, CancelTask cancelTask) throws IOException {
      try {
        return reallyRead(mainv, mainv.getShapeAsSection(), cancelTask);

      } catch (InvalidRangeException e) {
        throw new IOException(e);
      }
    }

    // here is where 1D agg variables get read
    @Override
    public Array reallyRead(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      Vstate1D vstate = (Vstate1D) mainv.getSPobject();

      // read the original type - if its been promoted to a new type, the conversion happens after this read
      DataType dtype = (mainv instanceof VariableDS) ? ((VariableDS) mainv).getOriginalDataType() : mainv.getDataType();

      Array allData = Array.factory(dtype, section.getShape());
      int destPos = 0;

      // assumes the first dimension is time: LOOK: what about ensemble ??
      List<Range> ranges = section.getRanges();
      Range timeRange = ranges.get(0);
      List<Range> innerSection = ranges.subList(1, ranges.size());

      // keep track of open files - must be local variable for thread safety
      HashMap<String, NetcdfDataset> openFilesRead = new HashMap<String, NetcdfDataset>();

      // iterate over the desired forecast times
      Range.Iterator timeIter = timeRange.getIterator();
      while (timeIter.hasNext()) {
        int timeIdx = timeIter.next();
        Array result = null;

        // find the inventory for this grid, runtime, and hour
        TimeInventory.Instance timeInv =  vstate.timeInv.getInstance(vstate.gridLite, timeIdx);
        if (timeInv.getDatasetLocation() != null) {
          if (debugRead) System.out.printf("HIT %d ", timeIdx);
          result = read(timeInv, mainv.getName(), innerSection, openFilesRead); // may return null
          result = MAMath.convert(result, dtype); // just in case it need to be converted
        }

        // may have missing data
        if (result == null) {
          int[] shape = new Section(innerSection).getShape();
          result = ((VariableDS) mainv).getMissingDataArray(shape); // fill with missing values
          if (debugRead) System.out.printf("MISS %d ", timeIdx);                                                   
        }

        if (debugRead)
          System.out.printf("%d reallyRead %s %d bytes start at %d total size is %d%n", timeIdx, mainv.getName(), result.getSize(), destPos, allData.getSize());

        Array.arraycopy(result, 0, allData, destPos, (int) result.getSize());
        destPos += result.getSize();
      }

      // close any files used during this operation
      closeAll(openFilesRead);
      return allData;
    }

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  /* private BestInventory makeBestInventory(TimeCoord.TimeResult bestTimeCoord, FmrcInv.UberGrid ugrid) {
    BestInventory bestInv = new BestInventory(bestTimeCoord, ugrid);

    // each run
    for (FmrInv.GridVariable grid : ugrid.getRuns()) {

      // each file in the run
      for (GridDatasetInv.Grid inv : grid.getInventory()) {
        double invOffset = FmrcInv.getOffsetInHours(bestTimeCoord.base, inv.tc.getRunDate()); // offset reletive to inv

        // set of offsets are relative to inv.tc.getRunDate()
        double[] offsets = inv.tc.getOffsetHours();
        for (int i = 0; i < offsets.length; i++) {
          bestInv.setOffset(offsets[i] + invOffset, inv.getLocation(), i); // later ones override
        }
      }
    }

    return bestInv;
  }

  public static class BestInventory {
    String varName;
    double[] offsets;
    String[] location; // file location
    int[] invIndex; // the index in the file

    BestInventory(TimeCoord.TimeResult tc, FmrcInv.UberGrid ugrid) {
      this.varName = ugrid.getName();
      this.offsets = tc.offsets; // all the TimeCoords possible
      this.location = new String[tc.offsets.length];
      this.invIndex = new int[tc.offsets.length];
    }

    void setOffset(double offsetHour, String location, int invIndex) {
      int offsetIndex = findIndex(offsetHour);
      if (offsetIndex < 0)
        throw new IllegalStateException("BestInventory cant find hour " + offsetHour + " in " + varName);
      this.location[offsetIndex] = location;
      this.invIndex[offsetIndex] = invIndex;
    }

    // linear search - barf
     private int findIndex(double offsetHour) {
      for (int i = 0; i < offsets.length; i++)
        if (Misc.closeEnough(offsets[i], offsetHour))
          return i;
      return -1;
    }

  }  */

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  // the general case is to get only one time per read - probably not too inefficient, eg GRIB, except maybe for remote reads

  private Array read(TimeInventory.Instance timeInstance, String varName, List<Range> innerSection, HashMap<String, NetcdfDataset> openFilesRead) throws IOException, InvalidRangeException {
    NetcdfFile ncfile = open(timeInstance.getDatasetLocation(), openFilesRead);
    Variable v = ncfile.findVariable(varName);

    // v could be missing, return missing data i think
    if (v == null) return null;

    // assume time is first dimension LOOK: out of-order; ensemble; section different ??
    Range timeRange = new Range(timeInstance.getDatasetIndex(), timeInstance.getDatasetIndex());
    Section s = new Section(innerSection);
    s.insertRange(0, timeRange);
    return v.read(s);
  }

  // this is mutable
  // private HashMap<String, NetcdfDataset> openFilesRead = new HashMap<String, NetcdfDataset>();

  private NetcdfDataset open(String location, HashMap<String, NetcdfDataset> openFiles) throws IOException {
    NetcdfDataset ncfile = null;

    if (openFiles != null) {
      ncfile = openFiles.get(location);
      if (ncfile != null) return ncfile;
    }

    ncfile = NetcdfDataset.acquireDataset(location, null);  // default enhance
    if (openFiles != null) {
      openFiles.put(location, ncfile);
    }
    return ncfile;
  }

  private void closeAll(HashMap<String, NetcdfDataset> openFiles) throws IOException {
    for (NetcdfDataset ncfile : openFiles.values()) {
      ncfile.close();
    }
    openFiles.clear();
  }

  ////////////////////////////////////////////////////////////////////////////////////
  // all normal (non agg, non cache) variables must use a proxy to acquire the file
  // the openFiles map is null - should be ok since its a non-agg, so file gets opened (and closed) for each read.

  protected class DatasetProxyReader implements ProxyReader {
    String location;

    DatasetProxyReader(String location) {
      this.location = location;
    }

    public Array reallyRead(Variable client, CancelTask cancelTask) throws IOException {
      NetcdfFile ncfile = null;
      try {
        ncfile = open(location, null);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        Variable proxyV = findVariable(ncfile, client);
        return proxyV.read();
      } finally {
        ncfile.close();
      }
    }

    public Array reallyRead(Variable client, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      NetcdfFile ncfile = null;
      try {
        ncfile = open(location, null);
        Variable proxyV = findVariable(ncfile, client);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        return proxyV.read(section);
      } finally {
        ncfile.close();
      }
    }
  }

  protected Variable findVariable(NetcdfFile ncfile, Variable client) {
    Variable v = ncfile.findVariable(client.getName());
    if (v == null) {  // might be renamed
      VariableEnhanced ve = (VariableEnhanced) client;
      v = ncfile.findVariable(ve.getOriginalName());
    }
    return v;
  }

}
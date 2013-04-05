package ucar.nc2.constants;

/**
 * Enumerate the types of "cdm index" files, with extension ".ncx".
 * These index files serialize the information needed to create a CDM dataset,
 * using protobuf. They can only be decoded by the corresponding protobuf code.
 *
 * Each cdmIndex file must start with a "magic start" set of bytes, which hopefully dont
 * collide with other file formats (!).
 *
 * @author John
 * @since 3/22/13
 */
public enum CdmIndex {

  Grib1Collection,
  Grib2Collection,
  Grib1TimePartition,
  Grib2TimePartition,

  GridCollection

}

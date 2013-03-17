val imageClasses = "graffiti" :: "boat" :: HNil

val otherImages = 2 :: 3 :: 4 :: 5 :: 6 :: HNil

val fastDetector = BoundedPairDetector(
  BoundedDetector(OpenCVDetector.FAST, 5000),
  200)
val detectors = fastDetector :: HNil

val extractor = new contrib.NCCLogPolarExtractor(
  2,
  32,
  16,
  32,
  3)
val extractors = extractor :: HNil

val matcher = new contrib.NCCLogPolarMatcher(8)
val matchers = matcher :: HNil

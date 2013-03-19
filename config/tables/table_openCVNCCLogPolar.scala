val imageClasses = 
  "graffiti" :: 
  "trees" :: 
  "jpeg" :: 
  "boat" :: 
  "bark" :: 
  "bikes" :: 
  "light" ::
  "wall" :: HNil

//val otherImages = 2 :: 3 :: 4 :: 5 :: 6 :: HNil
val otherImages = 2 :: 4 :: 6 :: HNil

val fastDetector = BoundedPairDetector(
  BoundedDetector(OpenCVDetector.FAST, 5000),
  200)
val siftDetector = BoundedPairDetector(
  BoundedDetector(OpenCVDetector.SIFT, 5000),
  200)
//val detectors = fastDetector :: siftDetector :: HNil
val detectors = fastDetector :: HNil

val extractor = new contrib.NCCLogPolarExtractor(
  2,
  32,
  32,
  32,
  3)
val extractors = extractor :: OpenCVExtractor.SIFT :: OpenCVExtractor.BRIEF :: OpenCVExtractor.BRISK :: HNil

val matcher = new contrib.NCCLogPolarMatcher(8)
val matchers = matcher :: VectorMatcher.L2 :: VectorMatcher.L0 :: HNil

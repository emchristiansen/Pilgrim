val imageClasses = 
  "graffiti" :: 
  "trees" :: 
  "jpeg" :: 
  "boat" :: 
  "bark" :: 
  "bikes" :: 
  "light" ::
  "wall" :: HNil

val otherImages = 2 :: 3 :: 4 :: 5 :: 6 :: HNil
//val otherImages = 2 :: HNil

val fastDetector = BoundedPairDetector(
  BoundedDetector(OpenCVDetector.FAST, 5000),
  100)
val siftDetector = BoundedPairDetector(
  BoundedDetector(OpenCVDetector.SIFT, 5000),
  100)
val briskDetector = BoundedPairDetector(
  BoundedDetector(OpenCVDetector.BRISK, 5000),
  100)
val detectors = fastDetector :: siftDetector :: briskDetector :: HNil
//val detectors = fastDetector :: HNil

val extractor = new contrib.NCCLogPolarExtractor(
  4,
  32,
  8,
  16,
  0.8)
val extractors = extractor :: OpenCVExtractor.SIFT :: OpenCVExtractor.BRIEF :: OpenCVExtractor.BRISK :: HNil

val matcher = new contrib.NCCLogPolarMatcher(4)
val matchers = matcher :: VectorMatcher.L2 :: VectorMatcher.L0 :: VectorMatcher.L1 :: HNil
//val matchers = matcher :: VectorMatcher.L2 :: HNil

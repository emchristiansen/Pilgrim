import nebula._

val imageClasses = Seq(
//  "graffiti", 
//  "trees", 
  "jpeg"
//  "boat", 
//  "bark", 
//  "bikes", 
//  "light", 
//  "wall"
).sorted

val otherImages = Seq(2)

//  FASTDetector(maxKeyPoints = 100),
val detectors = Seq(
  BRISKDetector(maxKeyPoints = 1))

val extractors = Seq(
  BRISKExtractor(true, true),
  BRIEFExtractor(true, true),
  ORBExtractor(true, true)
//  BRISKOrderExtractor(true, true)
)

val matchers = Seq(
  L0Matcher()
//  L1Matcher()
  // L1IntervalMatcher(),
  // L2Matcher()
) 


val extractorsAndMatchers = Seq(
  // (BRISKExtractor(true, true),
  //   L0Matcher()),
  (BRISKRawExtractor(true, true),
    L2Matcher())
  // (BRISKRankExtractor(true, true),
  //   L1Matcher()),
  // (BRISKRankExtractor(true, true),
  //   L2Matcher()),
  // (BRISKOrderExtractor(true, true),
  //   CayleyMatcher())
)

for (
  detector <- detectors;
  (extractor, matcher) <- extractorsAndMatchers
  // extractor <- extractors;
  // matcher <- matchers
) yield for (
  imageClass <- imageClasses;
  otherImage <- otherImages
) yield CorrespondenceExperiment(
  imageClass, 
  otherImage,
  detector, 
  extractor, 
  matcher)


import nebula._

val imageClasses = Seq(
  "jpeg").sorted

val otherImages = Seq(4)

//  FASTDetector(maxKeyPoints = 100),
val detectors = Seq(
  BRISKDetector(maxKeyPoints = 500))

val extractors = Seq(
  // BRISKExtractor(true, true),
  // FREAKExtractor(false, false),
  // SortExtractor(false, false, 16, 5, "Gray"),
  ELUCIDExtractor(false, false, 16, 3, 4, 5, "Gray"),
  // ELUCIDExtractor(false, true, 16, 3, 4, 5, "Gray"),
  ELUCIDExtractor(true, false, 16, 3, 4, 5, "Gray")
//  ELUCIDExtractor(true, true, 16, 3, 4, 5, "Gray")
)

val matchers = Seq(
  L0Matcher())

for (
  detector <- detectors;
  extractor <- extractors;
  matcher <- matchers
) yield for (
  imageClass <- imageClasses;
  otherImage <- otherImages
) yield CorrespondenceExperiment(
  imageClass, 
  otherImage,
  detector, 
  extractor, 
  matcher)


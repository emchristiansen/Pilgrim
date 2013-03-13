import nebula._

val imageClasses = Seq(
  "graffiti", 
  "trees", 
  "jpeg", 
  "boat", 
  "bark", 
  "bikes", 
  "light", 
  "wall").sorted

val otherImages = Seq(2, 4, 6)

//  FASTDetector(maxKeyPoints = 100),
val detectors = Seq(
  BRISKDetector(maxKeyPoints = 100))

val extractors = Seq(
  BRISKExtractor(true, true),
  FREAKExtractor(false, false),
  SortExtractor(false, false, 16, 5, "Gray"),
  ELUCIDExtractor(false, false, 16, 3, 4, 5, "Gray"),
  ELUCIDExtractor(false, true, 16, 3, 4, 5, "Gray"),
  ELUCIDExtractor(true, false, 16, 3, 4, 5, "Gray"),
  ELUCIDExtractor(true, true, 16, 3, 4, 5, "Gray")
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


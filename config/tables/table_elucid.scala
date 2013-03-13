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
  ELUCIDExtractor(false, false, 16, 3, 3, 5, "Gray"),
  ELUCIDExtractor(false, false, 16, 3, 4, 5, "Gray"),
  ELUCIDExtractor(false, false, 16, 3, 5, 5, "Gray"),
  ELUCIDExtractor(false, false, 16, 3, 6, 5, "Gray"),
  ELUCIDExtractor(false, false, 8, 3, 4, 5, "Gray"),
  ELUCIDExtractor(false, false, 32, 3, 4, 5, "Gray")
)

val matchers = Seq(
  L0Matcher(),
  CayleyMatcher()
)

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


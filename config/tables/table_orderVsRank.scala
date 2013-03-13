import nebula._
import nebula.SortDescriptor._

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

val detectors = Seq(
  FASTDetector(maxKeyPoints = 100)
)

val extractors = Seq(
  NormalizeExtractor(
    false,
    false,
    25,
    5,
    "Gray"),
  RankExtractor(
    false,
    false,
    25,
    5,
    "Gray"))

val matchers = Seq(
  L0Matcher(),
  L1Matcher(),
  L1IntervalMatcher(),
  L2Matcher()
)

// val matchers = Seq(
//   L0Matcher()
// )

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
  matcher
)


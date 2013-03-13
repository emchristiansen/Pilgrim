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
  OpenCVDetector(OpenCVDetectorType.BRISK, Some(100))
)

val extractors = for (
  patchWidth <- Seq(8, 16, 24)
) yield Seq(
  PatchExtractor(
    PatchExtractorType.NCC,
    false,
    false,
    25,
    5,
    "Gray"),
  PatchExtractor(
    PatchExtractorType.NormalizeRange,
    false,
    false,
    25,
    5,
    "Gray"),
  PatchExtractor(
    PatchExtractorType.Rank,
    false,
    false,
    25,
    5,
    "Gray"))

val matchers = Seq(
  MatcherType.L1,
  MatcherType.L2
)

// val matchers = Seq(
//   L0Matcher()
// )

for (
  detector <- detectors;
  extractor <- extractors.flatten;
  matcher <- matchers
) yield for (
  imageClass <- imageClasses;
  otherImage <- otherImages
) yield implicitly[WideBaselineExperiment => Experiment].apply(
  WideBaselineExperiment(
    imageClass, 
    otherImage,
    detector, 
    extractor, 
    matcher))


val imageClasses = Seq(
  "graffiti", 
  "trees", 
  "jpeg", 
  "boat", 
  "bark", 
  "bikes", 
  "light", 
  "wall").sorted

// val imageClasses = Seq(
//   "graffiti", 
//   "boat", 
//   "bikes", 
//   "light").sorted

val otherImages = Seq(2, 4, 6)

val detectors = Seq(
  OpenCVDetector(OpenCVDetectorType.FAST, Some(100))
)

val extractors: Seq[Extractor] = Seq(
  OpenCVExtractor(OpenCVExtractorType.SIFT),
  OpenCVExtractor(OpenCVExtractorType.SURF),
  OpenCVExtractor(OpenCVExtractorType.BRISK),
  OpenCVExtractor(OpenCVExtractorType.BRIEF),
  OpenCVExtractor(OpenCVExtractorType.ORB),
  PatchExtractor(
    PatchExtractorType.Rank,
    false,
    false,
    24,
    5,
    "Gray"),
  PatchExtractor(
    PatchExtractorType.Order,
    false,
    false,
    24,
    5,
    "Gray"),
  PatchExtractor(
    PatchExtractorType.NCC,
    false,
    false,
    24,
    5,
    "Gray"),
  PatchExtractor(
    PatchExtractorType.Rank,
    false,
    false,
    49,
    5,
    "Gray"),
  PatchExtractor(
    PatchExtractorType.Order,
    false,
    false,
    49,
    5,
    "Gray")
)

val matchers: Seq[Matcher] = Seq(
  MatcherType.L0,
  MatcherType.L1,
  MatcherType.L2
)

val extractorsAndMatchers = for (
  extractor <- extractors;
  matcher <- matchers
) yield (extractor, matcher)

val extra: Seq[Tuple2[Extractor, Matcher]] = Seq(
  // (PatchExtractor(
  //   PatchExtractorType.Order,
  //   false,
  //   false,
  //   49,
  //   5,
  //   "Gray"),
  // MatcherType.KendallTau),
  // (PatchExtractor(
  //   PatchExtractorType.Rank,
  //   false,
  //   false,
  //   49,
  //   5,
  //   "Gray"),
  // MatcherType.KendallTau),
  (PatchExtractor(
    PatchExtractorType.Rank,
    false,
    false,
    24,
    5,
    "Gray"),
  MatcherType.KendallTau),
  (PatchExtractor(
    PatchExtractorType.Order,
    false,
    false,
    24,
    5,
    "Gray"),
  MatcherType.KendallTau)
)

for (
  detector <- detectors;
  (extractor, matcher) <- extractorsAndMatchers ++ extra
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

val imageClasses = Seq(
  "bikes").sorted

val otherImages = Seq(4)

val detectors = Seq(
  OpenCVDetector(OpenCVDetectorType.FAST, Some(200))
)

val extractorsAndMatchers: Seq[Tuple2[Extractor, Matcher]] = Seq(
  (PatchExtractor(
    PatchExtractorType.Rank,
    false,
    false,
    24,
    5,
    "sRGB"),
  MatcherType.KendallTau),
  (PatchExtractor(
    PatchExtractorType.Order,
    false,
    false,
    24,
    5,
    "sRGB"),
  MatcherType.L0),
  (PatchExtractor(
    PatchExtractorType.Order,
    false,
    false,
    24,
    5,
    "sRGB"),
  MatcherType.Cayley))

for (
  detector <- detectors;
  (extractor, matcher) <- extractorsAndMatchers;
  imageClass <- imageClasses;
  otherImage <- otherImages
) yield implicitly[WideBaselineExperiment => Experiment].apply(
  WideBaselineExperiment(
    imageClass, 
    otherImage,
    detector, 
    extractor, 
    matcher))

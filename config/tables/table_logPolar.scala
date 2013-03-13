val imageClasses = Seq(
  "graffiti", 
  "trees", 
  "jpeg", 
  "boat", 
  "bark", 
  "bikes", 
  "light", 
  "wall").sorted

val otherImages = Seq(2, 3, 4, 5, 6)

val detectors = 
  BoundedPairDetector(
    BoundedDetector(OpenCVDetector.FAST, 5000),
    200
  ) :: HNil

val logPolarExtractor0 = LogPolarExtractor(
  false,
  2,
  16,
  16,
  16,
  3,
  "Gray")

val logPolarExtractor1 = LogPolarExtractor(
  false,
  2,
  24,
  16,
  16,
  3,
  "Gray")

val logPolarExtractor2 = LogPolarExtractor(
  false,
  2,
  30,
  32,
  32,
  3,
  "Gray")

val logPolarExtractor3 = LogPolarExtractor(
  false,
  2,
  48,
  32,
  32,
  3,
  "Gray")

//val extractors0 = logPolarExtractor0 :: logPolarExtractor1 :: logPolarExtractor2 :: HNil
//val extractors0 = logPolarExtractor0 :: logPolarExtractor1 :: HNil
val extractors0 = logPolarExtractor0 :: logPolarExtractor1 :: logPolarExtractor2 :: logPolarExtractor3 :: HNil
val extractors1 = OpenCVExtractor.SIFT :: HNil
val extractors2 = OpenCVExtractor.BRIEF :: HNil

val logPolarMatcher0 = LogPolarMatcher(
  PatchNormalizer.NCC,
  Matcher.L1,
  true,
  true,
  8)

val logPolarMatcher1 = LogPolarMatcher(
  PatchNormalizer.NCC,
  Matcher.L2,
  true,
  true,
  8)

val logPolarMatcher2 = LogPolarMatcher(
  PatchNormalizer.Rank,
  Matcher.L1,
  true,
  true,
  8)

val logPolarMatcher3 = LogPolarMatcher(
  PatchNormalizer.Rank,
  Matcher.L2,
  true,
  true,
  8)

val matchers0 = logPolarMatcher0 :: logPolarMatcher1 :: logPolarMatcher2 :: logPolarMatcher3 :: HNil
val matchers1 = Matcher.L2 :: HNil
val matchers2 = Matcher.L0 :: HNil 

val tuples = 
  HListUtil.mkTuple3(detectors, extractors0, matchers0) ++ 
  HListUtil.mkTuple3(detectors, extractors1, matchers1) ++ 
  HListUtil.mkTuple3(detectors, extractors2, matchers2)

val transposed = for (
  imageClass <- imageClasses;
  otherImage <- otherImages
) yield {
  object constructExperiment extends Poly1 {
    implicit def default[D <% PairDetector, E <% Extractor[F], M <% Matcher[F], F] = at[(D, E, M)] {
      case (detector, extractor, matcher) => {
        WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
      }
    }
  }

  // This lifting, combined with flatMap, filters out types that can't be used
  // to construct experiments.
  object constructExperimentLifted extends Lift1(constructExperiment)
  
  val experiments = tuples flatMap constructExperimentLifted

  println(experiments)

  object constructCapstone extends Poly1 {
    implicit def default[E <% RuntimeConfig => ExperimentRunner[R] <% RuntimeConfig => StorageInfo[R]: JsonFormat: TypeTag, R <% RuntimeConfig => ExperimentSummary: TypeTag] = at[E] {
      experiment => unsafeCapstone(experiment)
    }
  }

  object getJson extends Poly1 {
    implicit def default[E: JsonFormat] = at[E] { experiment =>
      {
        experiment.toJson
      }
    }
  }

  val capstones = experiments map constructCapstone
  val jsons = experiments map getJson
  capstones.toList zip jsons.toList
}

transposed.transpose

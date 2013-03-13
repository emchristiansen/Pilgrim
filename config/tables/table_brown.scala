val datasets = Seq(
  "liberty", 
  "liberty_harris", 
  "notredame",
  "notredame_harris",
  "yosemite",
  "yosemite_harris")

val numMatchess = Seq(1000)

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

val extractors = logPolarExtractor0 :: logPolarExtractor1 :: logPolarExtractor2 :: HNil
//val extractors = logPolarExtractorFast :: logPolarExtractorGood :: OpenCVExtractor.SIFT :: OpenCVExtractor.SURF :: HNil
//val extractors = logPolarExtractor :: HNil

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

val matchers = logPolarMatcher0 :: logPolarMatcher1 :: logPolarMatcher2 :: logPolarMatcher3 :: Matcher.L2 :: HNil

val transposed = for (
  dataset <- datasets;
  numMatches <- numMatchess
) yield {
  val tuples = HListUtil.mkTuple2(extractors, matchers)

  object constructExperiment extends Poly1 {
    implicit def default[E <% Extractor[F], M <% Matcher[F], F] = at[(E, M)] {
      case (extractor, matcher) => {
        BrownExperiment(dataset, numMatches, extractor, matcher)
      }
    }
  }

  // This lifting, combined with flatMap, filters out types that can't be used                                                                                                                                                                                                             
  // to construct experiments.                                                                                                                                                                                                                                                              
  object constructExperimentLifted extends Lift1(constructExperiment)

  val experiments = tuples flatMap constructExperimentLifted

  object constructCapstone extends Poly1 {
    implicit def default[E <% RuntimeConfig => ExperimentRunner[R] <% RuntimeConfig => StorageInfo[R]: JsonFormat: TypeTag, R <% RuntimeConfig => ExperimentSummary: TypeTag] = at[E] {
      experiment => Distributed.unsafeCapstone(experiment)
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

import reflect.runtime.universe._

val imageClasses = Seq(
  "graffiti", 
  // "trees", 
  // "jpeg", 
  // "boat", 
  // "bark", 
  // "bikes", 
  // "light", 
  "wall").sorted

// val otherImages = Seq(2, 3, 4, 5, 6)
val otherImages = Seq(2)

val detectors = 
  BoundedPairDetector(
    BoundedDetector(OpenCVDetector.FAST, 1000),
    100
  ) :: HNil


val logPolarExtractor = LogPolarExtractor(
  false,
  2,
  32,
  8,
  8,
  3,
  "Gray")

val extractors = logPolarExtractor :: OpenCVExtractor.SIFT :: OpenCVExtractor.SURF :: HNil

val logPolarMatcher = LogPolarMatcher(
  PatchNormalizer.Raw,
  Matcher.L1,
  false,
  false,
  2)

val matchers = logPolarMatcher :: Matcher.L1 :: Matcher.L2 :: HNil

val transposed = for (
  imageClass <- imageClasses;
  otherImage <- otherImages
) yield {
  object detectorMapper extends Poly1 {
    implicit def default[D] = at[D] { detector =>
      {
        object extractorMapper extends Poly1 {
          implicit def default[E] = at[E] { extractor =>
            {
              object matcherMapper extends Poly1 {
                implicit def default[M] = at[M] { matcher =>
                  (detector, extractor, matcher)
                }
              }
              matchers map matcherMapper
            }
          }
        }
        extractors flatMap extractorMapper
      }
    }
  }

  val tuples = detectors flatMap detectorMapper
  println(tuples)

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

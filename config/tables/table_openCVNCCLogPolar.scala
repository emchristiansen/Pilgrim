loadOpenCV

val imageClasses = Seq(
  "graffiti",
  "trees",
  "jpeg",
  "boat",
  "bark",
  "bikes",
  "light",
  "wall").sorted

val otherImages = Seq(2)

val fastDetector = BoundedPairDetector(
  BoundedDetector(OpenCVDetector.FAST, 5000),
  200)
val detectors = fastDetector :: HNil

val extractor = new contrib.NCCLogPolarExtractor(
  2,
  32,
  16,
  32,
  3)
val extractors = extractor :: HNil

val matcher = new contrib.NCCLogPolarMatcher(8)
val matchers = matcher :: HNil

//////////////////////////////

object verifyPairDetector extends Poly1 {
  implicit def default[D <% PairDetector] = at[D] { identity }
}

detectors map verifyPairDetector

/////////////////////////////////

object verifyExtractor extends Poly1 {
  implicit def default[E <% Extractor[F], F] = at[E] { identity }
}

extractors map verifyExtractor

///////////////////////////////////

object verifyMatcher extends Poly1 {
  implicit def default[M <% Matcher[F], F] = at[M] { identity }
}

matchers map verifyMatcher

/////////////////////////////////////

object verifyJson extends Poly1 {
  implicit def default[A: JsonFormat] = at[A] { identity }
}

detectors map verifyJson
extractors map verifyJson
matchers map verifyJson

/////////////////////////////////////////

val tuples = HListUtil.mkTuple3(detectors, extractors, matchers)

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

  ///////////////////////////////////////
  
  object printHList extends Poly1 {
    implicit def default[A] = 
      at[A] { println }
  }

  experiments map printHList    

  ///////////////////////////////////////
  
  object verifyExperimentRunner extends Poly1 {
    implicit def default[E <% RuntimeConfig => ExperimentRunner[R], R] = 
      at[E] { identity }
  }

  experiments map verifyExperimentRunner
  
  ///////////////////////////////////////
  
  object verifyJson extends Poly1 {
    implicit def default[E <% RuntimeConfig => ExperimentRunner[R]: JsonFormat: TypeTag, R: JsonFormat: TypeTag] = 
      at[E] { identity }
  }

  experiments map verifyJson    
  
  ///////////////////////////////////////
  
  object verifyStorageInfo extends Poly1 {
    implicit def default[E <% RuntimeConfig => StorageInfo[R], R] = 
      at[E] { identity }
  }

  experiments map verifyStorageInfo
  
  ///////////////////////////////////////
  
  object verifyExperimentSummary extends Poly1 {
    implicit def default[E <% RuntimeConfig => ExperimentRunner[R], R <% RuntimeConfig => ExperimentSummary] = 
      at[E] { identity }
  }

  experiments map verifyExperimentSummary    
  
  /////////////////////////////////////////
  
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

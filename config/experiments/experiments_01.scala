import nebula._

val detector = FASTDetector(maxKeyPoints = 50)

val extractor = SortExtractor(
  normalizeRotation = false,
  normalizeScale = false,
  patchWidth = 8,
  blurWidth = 5,
  color = true
)

// val extractor = BRISKExtractor(
//   normalizeRotation = false,
//   normalizeScale = false)

val matcher = L0Matcher()

val experiment = CorrespondenceExperiment(
  imageClass = "bikes",
  otherImage = 4,
  detector = detector,
  extractor = extractor,
  matcher = matcher)

Seq(experiment)

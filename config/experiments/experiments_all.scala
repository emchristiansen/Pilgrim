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

val detectors = Seq(
  FASTDetector(maxKeyPoints = 100)
)

val rawExtractor = RawExtractor(
  normalizeRotation = false,
  normalizeScale = false,
  patchWidth = 16,
  blurWidth = 5,
  color = true)  

val sortExtractor = SortExtractor(
  normalizeRotation = false,
  normalizeScale = false,
  patchWidth = 16,
  blurWidth = 5,
  color = true)

val l0Matcher = L0Matcher()
val l1Matcher = L1Matcher()
val l2Matcher = L2Matcher()
val kendallTauMatcher = KendallTauMatcher()
val cayleyMatcher = CayleyMatcher()

// Only certain combinations are legal.
val extractorsAndMatchers = Seq(
  (rawExtractor, l0Matcher),
  (rawExtractor, l1Matcher),
  (rawExtractor, l2Matcher),
  (sortExtractor, l0Matcher),
  (sortExtractor, l1Matcher),
  (sortExtractor, l2Matcher),
  (sortExtractor, kendallTauMatcher),
  (sortExtractor, cayleyMatcher))

for (
  imageClass <- imageClasses;
  otherImage <- otherImages;
  detector <- detectors;
  (extractor, matcher) <- extractorsAndMatchers
) yield CorrespondenceExperiment(
  imageClass, 
  otherImage,
  detector, 
  extractor, 
  matcher)

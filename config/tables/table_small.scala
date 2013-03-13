val imageClasses = Seq(
  "Dimetrodon",
  "Grove2",
  "Grove3",
  "Hydrangea",
  "RubberWhale",
  "Urban2",
  "Urban3",
  "Venus"
).sorted

val searchRadii = Seq(4)

val rankExtractors = for (
  patchWidth <- Seq(16);
  color <- Seq("Gray", "sRGB")
) yield PatchExtractor(
  PatchExtractorType.Rank,
  normalizeRotation = false,
  normalizeScale = false,
  patchWidth = patchWidth,
  blurWidth = 5,
  color = color)

val nccExtractors = for (
  patchWidth <- Seq(16);
  color <- Seq("Gray", "sRGB")
) yield PatchExtractor(
  PatchExtractorType.NCC,
  normalizeRotation = false,
  normalizeScale = false,
  patchWidth = patchWidth,
  blurWidth = 5,
  color = color)

val matchers = Seq(
  MatcherType.L1,
  MatcherType.L2)

for (
  searchRadius <- searchRadii;
  extractor <- rankExtractors ++ nccExtractors;
  matcher <- matchers
) yield for (
  imageClass <- imageClasses
) yield implicitly[SmallBaselineExperiment => Experiment].apply(
  SmallBaselineExperiment(
    searchRadius,
    imageClass,
    extractor, 
    matcher))

// Seq(Seq(
//   {
//     val experiment: Experiment = SmallBaselineExperiment(
//       4,
//       "Dimetrodon",
//       RankExtractor(
// 	false,
// 	false,
// 	8,
// 	5,
// 	"Gray"),
//       L1Matcher())
//     experiment
//   }
// ))



// Seq(Seq(
//   {
//     val experiment: Experiment = SmallBaselineExperiment(
//       4,
//       "Dimetrodon",
//       NCCExtractor(
// 	false,
// 	false,
// 	8,
// 	5,
// 	"Gray"),
//       L2Matcher())
//     experiment
//   }
// ))

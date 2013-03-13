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

val detector =
  BoundedPairDetector(
    BoundedDetector(OpenCVDetector.FAST, 5000),
    200)

val transposed = for (
  imageClass <- imageClasses;
  otherImage <- otherImages
) yield {
  val experiments = (for (
    minRadius <- Seq(1, 2, 3, 4, 5);
    maxRadius <- Seq(4, 8, 16, 24, 32, 40, 48, 56, 64);
    numScales <- Seq(1, 2, 4, 8, 16, 32, 64);
    numAngles <- Seq(1, 2, 4, 8, 16, 32, 64);
    blurWidth <- Seq(1, 2, 3, 4, 6, 8, 10);
    color <- Seq("Gray")
  ) yield {
    val extractor = LogPolarExtractor(
      false,
      minRadius,
      maxRadius,
      numScales,
      numAngles,
      blurWidth,
      color)

    for (scaleSearchRadiusFactor <- Seq(0, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7)) yield {
      val scaleSearchRadius = (scaleSearchRadiusFactor * numScales).round.toInt

      val matcher = LogPolarMatcher(
        PatchNormalizer.NCC,
        Matcher.L2,
        true,
        true,
        scaleSearchRadius)

      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    }
  }).flatten

  val capstones = experiments map (e => Distributed.unsafeCapstone(e))
  val jsons = experiments map (_.toJson)
  capstones.toList zip jsons.toList
}

transposed.transpose

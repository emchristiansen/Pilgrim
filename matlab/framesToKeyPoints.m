function matOfKeyPoint = framesToKeyPoints(frames)
assert(size(frames, 1) == 4);

keyPoints = java.util.ArrayList;

for index = 1 : size(frames, 2)
  frame = single(frames(:, index));
  keyPoint = org.opencv.features2d.KeyPoint(frame(1), frame(2), frame(3), frame(4), 0, 0, 0);
  keyPoints.add(keyPoint);
end

matOfKeyPoint = org.opencv.core.MatOfKeyPoint;
matOfKeyPoint.fromList(keyPoints);
end


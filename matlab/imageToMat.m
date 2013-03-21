function mat = imageToMat(image)
tempFile = java.io.File.createTempFile('matlab_imageToMat', '.png');
imwrite(image, char(tempFile.toString));

mat = org.opencv.highgui.Highgui.imread(tempFile.toString);
% mat = org.opencv.core.Mat;
% org.opencv.imgproc.Imgproc.cvtColor(rgbMat, mat, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY);
end


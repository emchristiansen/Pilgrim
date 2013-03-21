function vectorNCCBlock = matrixNCCBlockToOpenCV(matrixNCCBlock)
vectorNCCBlock = org.opencv.contrib.VectorNCCBlock;

for index = 1 : length(matrixNCCBlock)
  nccBlock = matrixNCCBlock(index);
  vectorNCCBlock.pushBack(nccBlock)
end
end


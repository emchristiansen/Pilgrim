function matrix = matToMatrix(mat)
assert(mat.channels == 1);

matrix = zeros(mat.rows, mat.cols);

% This takes around 90s, and I have no idea why. Is it just how long it
% takes to go through JNI?
for row = 1 : size(matrix, 1)
  for column = 1 : size(matrix, 2)
    % This is supposed to be double[] but is somehow actually just double.
    entryArray = mat.get(row - 1, column - 1);
    matrix(row, column) = entryArray;
  end
end
end


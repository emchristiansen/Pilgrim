function descriptorBenchmark(category)

% --------------------------------------------------------------------
% PART 3: Detector matching score
% --------------------------------------------------------------------

% The matching score is similar to the repeatability score, but
% involves computing a descriptor. Detectors like SIFT bundle a
% descriptor as well. However, most of them (e.g. MSER) do not have an
% associated descriptor (e.g. MSER). In this case we can bind one of
% our choice by using the DescriptorAdapter class.
%
% In this particular example, the object encapsulating the SIFT
% detector is used as descriptor form MSER.

import localFeatures.*;
import datasets.*;
import benchmarks.*;

dataset = VggAffineDataset('category',category);
% dataset = VggAffineDataset;

siftDetector = VlFeatSift();
mserDetector = VlFeatMser('MinDiversity',0.5);

detectors{1} = siftDetector ;
detectors{2} = DescriptorAdapter(mserDetector,siftDetector) ;

% As an additional example, we show how to use a different descriptor
% for the SIFT detector. In this case, we bind the descriptor
% implemented by the ExampleLocalFeatureExtractor() class which simply
% computes the mean, standard deviation, and median of a patch.
%
% Note that in this manner the SIFT descriptor is replaced by the new
% descriptor.

nccLogPolarExtractor = NCCLogPolarFeatureExtractor();
detectors{3} = DescriptorAdapter(siftDetector,nccLogPolarExtractor);

% We create a benchmark object and run the tests as before, but in
% this case we request that descriptor-based matched should be tested.

matchBenchmark = RepeatabilityBenchmark('Mode','MatchingScore');

matchScore = [] ;
numMatches = [] ;
for d = 1:numel(detectors)
  for i = 2:dataset.NumImages
    [matchScore(d,i) numMatches(d,i)] = ...
      matchBenchmark.testFeatureExtractor(detectors{d}, ...
                                dataset.getTransformation(i), ...
                                dataset.getImagePath(1), ...
                                dataset.getImagePath(i)) ;
  end
end

% Print and plot the results

detectorNames = {'SIFT', ...
                 'MSER + Sim. inv. SIFT desc.', ...
                 'NCCLogPolar' };

printScores(detectorNames, matchScore*100, 'Match Score');
printScores(detectorNames, numMatches, 'Number of matches') ;

% figure(4); clf;
% subplot(1,2,1);
% plotScores(detectorNames, dataset, matchScore*100,'Matching Score');
% subplot(1,2,2);
% plotScores(detectorNames, dataset, numMatches,'Number of matches');

% Same as with the correspondences, we can plot the matches based on
% feature frame descriptors. The code is nearly identical.

% imageBIdx = 4 ;
% 
% [drop drop siftMatches siftReprojFrames] = ...
%   repBenchmark.testFeatureExtractor(siftDetector, ...
%                             dataset.getTransformation(imageBIdx), ...
%                             dataset.getImagePath(1), ...
%                             dataset.getImagePath(imageBIdx)) ;
% 
% [drop drop mvmMatches mvmReprojFrames] = ...
%     repBenchmark.testFeatureExtractor(meanVarMedianDescExtractor, ...
%                               dataset.getTransformation(imageBIdx), ...
%                               dataset.getImagePath(1), ...
%                               dataset.getImagePath(imageBIdx)) ;
% 
% figure(5); clf;
% image = imread(dataset.getImagePath(imageBIdx));
% subplot(1,2,1); imshow(image);
% 
% benchmarks.helpers.plotFrameMatches(siftMatches, siftReprojFrames,...
%                                     'IsReferenceImage',false);
% title(sprintf('SIFT Matches with %d image (%s dataset).',...
%               imageBIdx,dataset.DatasetName));
% 
% subplot(1,2,2); imshow(image);
% benchmarks.helpers.plotFrameMatches(mvmMatches, mvmReprojFrames,...
%   'IsReferenceImage',false);
% title(sprintf('Matches using mean-variance-median descriptor with %d image (%s dataset).',...
%               imageBIdx,dataset.DatasetName));

% --------------------------------------------------------------------
% Helper functions
% --------------------------------------------------------------------

function printScores(detectorNames, scores, name)
  numDetectors = numel(detectorNames);
  maxNameLen = length('Method name');
  for k = 1:numDetectors
    maxNameLen = max(maxNameLen,length(detectorNames{k}));
  end
  fprintf(['\n', name,':\n']);
  formatString = ['%' sprintf('%d',maxNameLen) 's:'];
  fprintf(formatString,'Method name');
  for k = 2:size(scores,2)
    fprintf('\tImg#%02d',k);
  end
  fprintf('\n');
  for k = 1:numDetectors
    fprintf(formatString,detectorNames{k});
    for l = 2:size(scores,2)
      fprintf('\t%6s',sprintf('%.2f',scores(k,l)));
    end
    fprintf('\n');
  end
end

function plotScores(detectorNames, dataset, score, titleText)
  xstart = max([find(sum(score,1) == 0, 1) + 1 1]);
  xend = size(score,2);
  xLabel = dataset.ImageNamesLabel;
  xTicks = dataset.ImageNames;
  plot(xstart:xend,score(:,xstart:xend)','+-','linewidth', 2); hold on ;
  ylabel(titleText) ;
  xlabel(xLabel);
  set(gca,'XTick',xstart:1:xend);
  set(gca,'XTickLabel',xTicks);
  title(titleText);
  set(gca,'xtick',1:size(score,2));
  maxScore = max([max(max(score)) 1]);
  meanEndValue = mean(score(:,xend));
  legendLocation = 'SouthEast';
  if meanEndValue < maxScore/2
    legendLocation = 'NorthEast';
  end
  legend(detectorNames,'Location',legendLocation);
  grid on ;
  axis([xstart xend 0 maxScore]);
end

end
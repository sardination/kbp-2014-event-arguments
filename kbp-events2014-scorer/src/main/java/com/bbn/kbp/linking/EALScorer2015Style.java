package com.bbn.kbp.linking;

import com.bbn.bue.common.annotations.MoveToBUECommon;
import com.bbn.bue.common.collections.CollectionUtils;
import com.bbn.bue.common.evaluation.FMeasureCounts;
import com.bbn.kbp.events2014.AnswerKey;
import com.bbn.kbp.events2014.EventArgScoringAlignment;
import com.bbn.kbp.events2014.EventArgumentLinking;
import com.bbn.kbp.events2014.KBPRealis;
import com.bbn.kbp.events2014.Response;
import com.bbn.kbp.events2014.ResponseLinking;
import com.bbn.kbp.events2014.SystemOutput;
import com.bbn.kbp.events2014.TypeRoleFillerRealis;
import com.bbn.kbp.events2014.linking.EventArgumentLinkingAligner;
import com.bbn.kbp.events2014.linking.ExactMatchEventArgumentLinkingAligner;
import com.bbn.kbp.events2014.scorer.LinkingScore;
import com.bbn.kbp.events2014.scorer.StandardScoringAligner;
import com.bbn.kbp.events2014.scorer.bin.Preprocessor;
import com.bbn.kbp.events2014.scorer.bin.PreprocessorKBP2014;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.compose;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;

@Beta
public final class EALScorer2015Style {
  private static final Logger log = LoggerFactory.getLogger(EALScorer2015Style.class);

  private final Preprocessor preprocessor;
  private final EventArgumentLinkingAligner aligner =
      ExactMatchEventArgumentLinkingAligner.create();
  private final LinkF1 linkF1 = LinkF1.create();
  private final double beta;
  private final double lambda;

  /* pacakge-private */ EALScorer2015Style(Preprocessor preprocessor, final double beta, final double lambda) {
    checkArgument(beta >= 0.0);
    checkArgument(lambda >= 0.0);

    this.preprocessor = checkNotNull(preprocessor);
    this.beta = beta;
    this.lambda = lambda;
  }

  public static EALScorer2015Style create() {
    return new EALScorer2015Style(PreprocessorKBP2014.createKeepingRealis(), 0.25, 0.5);
  }

  public final class Result {
    private final EventArgScoringAlignment<TypeRoleFillerRealis> argScoringAlignment;
    private final LinkingScore linkingScore;

    private Result(
        final EventArgScoringAlignment<TypeRoleFillerRealis> argScoringAlignment,
        final LinkingScore linkingScore) {
      this.argScoringAlignment = checkNotNull(argScoringAlignment);
      this.linkingScore = checkNotNull(linkingScore);
    }

    public double scaledArgumentScore() {
      final int numGoldArgumentECs = Sets.union(argScoringAlignment.truePositiveEquivalenceClasses(),
          argScoringAlignment.falseNegativeEquivalenceClasses()).size();
      return unscaledArgumentScore() / numGoldArgumentECs;
    }

    public double unscaledArgumentScore() {
      final int truePositiveArgumentECs = argScoringAlignment.truePositiveEquivalenceClasses().size();
      final int falsePositiveArgumentECs = argScoringAlignment.falsePositiveEquivalenceClasses().size();
      return truePositiveArgumentECs - beta * falsePositiveArgumentECs;
    }

    public double unscaledLinkingScore() {
      return linkingScore.F1()*linkingScore.referenceArgumentLinking().allLinkedEquivalenceClasses().size();
    }

    public double scaledLinkingScore() {
      return linkingScore.F1();
    }

    public double scaledScore() {
      return (1.0-lambda)*scaledArgumentScore()+lambda*scaledLinkingScore();
    }

    public EventArgScoringAlignment<TypeRoleFillerRealis> argumentScoringAlignment() {
      return argScoringAlignment;
    }

    public LinkingScore linkingScore() {
      return linkingScore;
    }

  }

  private static final Predicate<TypeRoleFillerRealis> REALIS_IS_NOT_GENERIC =
      compose(not(equalTo(KBPRealis.Generic)), TypeRoleFillerRealis.realisFunction());

  public Result score(AnswerKey referenceArguments, ResponseLinking referenceLinking,
      SystemOutput systemOutput, ResponseLinking systemLinking) {

    final EventArgScoringAlignment<TypeRoleFillerRealis> scoringAlignment =
        scoreEventArguments(referenceArguments, systemOutput);

    final ImmutableSet<TypeRoleFillerRealis> linkableEquivalenceClasses =
        FluentIterable.from(scoringAlignment.truePositiveEquivalenceClasses())
          .append(scoringAlignment.falseNegativeEquivalenceClasses())
          .filter(REALIS_IS_NOT_GENERIC)
        .toSet();

    final LinkingScore linkingScore = scoreLinking(referenceArguments, linkableEquivalenceClasses,
        referenceLinking, systemLinking);

    return new Result(scoringAlignment, linkingScore);
  }

  private EventArgScoringAlignment<TypeRoleFillerRealis> scoreEventArguments(
      final AnswerKey referenceArguments, final SystemOutput systemOutput) {
    final Preprocessor.Result preprocessorResult = preprocessor.preprocess(systemOutput,
        referenceArguments);
    final Function<Response, TypeRoleFillerRealis> equivalenceClassFunction =
        TypeRoleFillerRealis.extractFromSystemResponse(preprocessorResult.normalizer());

    final StandardScoringAligner<TypeRoleFillerRealis> scoringAligner =
        StandardScoringAligner.forEquivalenceClassFunction(equivalenceClassFunction);
    return scoringAligner.align(preprocessorResult.answerKey(), preprocessorResult.systemOutput());
  }

  public LinkingScore scoreLinking(AnswerKey answerKey, Set<TypeRoleFillerRealis> linkableEquivalenceClasses,
      ResponseLinking referenceLinking, ResponseLinking systemLinking) {
    checkArgument(answerKey.docId() == systemLinking.docID(), "System output has doc ID %s " +
        "but answer key has doc ID %s", systemLinking.docID(), answerKey.docId());
    checkArgument(answerKey.docId() == referenceLinking.docID(),
        "Answer key docID %s does not match "
            + "reference linking doc ID %s", answerKey.docId(), referenceLinking.docID());
    checkArgument(systemLinking.docID() == systemLinking.docID(), "System output docID %s does "
        + "not match system linking docID %s", systemLinking.docID(), systemLinking.docID());

    checkArgument(referenceLinking.incompleteResponses().isEmpty(),
        "Reference key for %s has %s responses whose linking has not been annotated",
        referenceLinking.docID(), referenceLinking.incompleteResponses().size());

    checkArgument(systemLinking.incompleteResponses().isEmpty(),
        "System linking for %s has incomplete responses", systemLinking.docID());

    final EventArgumentLinking referenceArgumentLinking =
        aligner.align(referenceLinking, answerKey);
    final EventArgumentLinking systemArgumentLinking =
        aligner.align(systemLinking, answerKey);

    final EventArgumentLinking filteredReferenceArgumentLinking = referenceArgumentLinking
        .filteredCopy(in(linkableEquivalenceClasses));

    checkState(referenceArgumentLinking.equals(filteredReferenceArgumentLinking), "Filtering the "
        + "reference linking should have no effect");

    final EventArgumentLinking filteredSystemArgumentLinking = systemArgumentLinking
        .filteredCopy(in(linkableEquivalenceClasses));

    return LinkingScore.from(referenceLinking, referenceArgumentLinking, systemLinking, systemArgumentLinking,
        linkF1.score(filteredSystemArgumentLinking.linkedAsSetOfSets(),
            filteredReferenceArgumentLinking.linkedAsSetOfSets()));
  }
}

class LinkF1 {
  private LinkF1() {
  }

  public static LinkF1 create() {
    return new LinkF1();
  }

  public <T> ExplicitFMeasureInfo score(final Iterable<Set<T>> predicted,
      final Iterable<Set<T>> gold) {
    double linkF1Sum = 0.0;
    double linkPrecisionSum = 0.0;
    double linkRecallSum = 0.0;

    final Multimap<T, Set<T>> predictedItemToGroup =
        CollectionUtils.makeSetElementsToContainersMultimap(predicted);
    final Multimap<T, Set<T>> goldItemToGroup =
        CollectionUtils.makeSetElementsToContainersMultimap(gold);

    final ImmutableSet<T> keyItems = ImmutableSet.copyOf(concat(gold));
    if (keyItems.isEmpty()) {
      if (predictedItemToGroup.isEmpty()) {
        return new ExplicitFMeasureInfo(1.0, 1.0, 1.0);
      } else {
        return new ExplicitFMeasureInfo(0.0, 0.0, 0.0);
      }
    }

    for (final T keyItem : keyItems) {
      // withouts are to ensure an element is not counted as its own neighbor
      final Set<T> predictedNeighbors = ImmutableSet.copyOf(
          without(concat(predictedItemToGroup.get(keyItem)),
              keyItem));
      final Set<T> goldNeighbors = ImmutableSet.copyOf(without(
          concat(goldItemToGroup.get(keyItem)),
          keyItem));

      final boolean predictedAndGoldAreSingleton = predictedNeighbors.isEmpty() && goldNeighbors.isEmpty();

      if (!predictedAndGoldAreSingleton) {
        int truePositiveLinks = Sets.intersection(predictedNeighbors, goldNeighbors).size();
        int falsePositiveLinks = Sets.difference(predictedNeighbors, goldNeighbors).size();
        int falseNegativeLinks = Sets.difference(goldNeighbors, predictedNeighbors).size();

        final FMeasureCounts fMeasureCounts =
            FMeasureCounts.from(truePositiveLinks, falsePositiveLinks, falseNegativeLinks);
        linkF1Sum += fMeasureCounts.F1();
        linkPrecisionSum += fMeasureCounts.precision();
        linkRecallSum += fMeasureCounts.recall();
      } else {
        // arguments which are correctly linked to nothing (singletons)
        // count as having perfect links
        linkF1Sum += 1.0;
        linkPrecisionSum += 1.0;
        linkRecallSum += 1.0;
      }
    }
    return new ExplicitFMeasureInfo(linkPrecisionSum/keyItems.size(),
        linkRecallSum/keyItems.size(), linkF1Sum/keyItems.size());
  }

  @MoveToBUECommon
  private static <T, S extends T> Iterable<T> without(Iterable<T> input, S itemToExclude) {
    return filter(input, not(Predicates.<T>equalTo(itemToExclude)));
  }

  @MoveToBUECommon
  private static <T> Iterable<T> withoutAnyOf(Iterable<T> input,
      Collection<? extends T> itemsToExclude) {
    return filter(input, not(in(itemsToExclude)));
  }
}


package com.bbn.kbp.events2014;

import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a span of char offsets.  An optional string is provided for debugging only and is not
 * considered in equality and hashcode computations.
 *
 * CharOffsetSpans are ordered lexicographically by their starting offset and then their ending
 * offset.
 *
 * @author rgabbard
 */
public class CharOffsetSpan implements Comparable<CharOffsetSpan> {

  public CharOffsetSpan(final int start, final int end,
      // Nullable
      final String string) {
    checkArgument(start >= 0, "Character offset span must have non-negative start index, got %d",
        start);
    checkArgument(end >= 0, "Character offset span must have non-negative end index, got %d", end);
    checkArgument(end >= start,
        "End of character offset span must be greater than or equal to start, but got start=%d, end=%s",
        start, end);
    this.start = start;
    this.end = end;
    this.string = string;
  }

  public static CharOffsetSpan fromOffsetsAndDebugString(final int startInclusive,
      final int endInclusive, final String s) {
    return new CharOffsetSpan(startInclusive, endInclusive, checkNotNull(s));
  }

  public static CharOffsetSpan fromOffsetsOnly(final int startInclusive, final int endInclusive) {
    return new CharOffsetSpan(startInclusive, endInclusive, null);
  }

  public int startInclusive() {
    return start;
  }

  public int endInclusive() {
    return end;
  }

  public Optional<String> string() {
    return Optional.fromNullable(string);
  }

  public boolean contains(CharOffsetSpan other) {
    return startInclusive() <= other.startInclusive()
        && endInclusive() >= other.endInclusive();
  }

  final int start;
  final int end;
  private final String string;

  public OffsetRange<CharOffset> asCharOffsetRange() {
    return OffsetRange.charOffsetRange(startInclusive(), endInclusive());
  }

  @Override
  public String toString() {
    return String.format("%d-%d", start, end);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(start, end);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final CharOffsetSpan other = (CharOffsetSpan) obj;
    return start == other.start
        && end == other.end;

  }

  @Override
  public int compareTo(final CharOffsetSpan other) {
    checkNotNull(other);
    return ComparisonChain.start().compare(start, other.start)
        .compare(end, other.endInclusive())
        .result();
  }

}

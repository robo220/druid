/*
 * Druid - a distributed column store.
 * Copyright (C) 2012, 2013  Metamarkets Group Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.metamx.druid.query.metadata;

import com.google.common.collect.Lists;
import com.metamx.common.guava.Sequences;
import com.metamx.druid.index.v1.TestIndex;
import com.metamx.druid.query.QueryRunnerTestHelper;
import io.druid.query.QueryRunner;
import io.druid.query.QueryRunnerFactory;
import io.druid.query.metadata.metadata.ColumnAnalysis;
import io.druid.query.metadata.metadata.SegmentAnalysis;
import io.druid.query.metadata.metadata.SegmentMetadataQuery;
import io.druid.query.spec.QuerySegmentSpecs;
import io.druid.segment.IncrementalIndexSegment;
import io.druid.segment.QueryableIndexSegment;
import io.druid.segment.Segment;
import io.druid.segment.column.ValueType;
import junit.framework.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 */
public class SegmentAnalyzerTest
{
  @Test
  public void testIncrementalDoesNotWork() throws Exception
  {
    final List<SegmentAnalysis> results = getSegmentAnalysises(
        new IncrementalIndexSegment(TestIndex.getIncrementalTestIndex())
    );

    Assert.assertEquals(0, results.size());
  }

  @Test
  public void testMappedWorks() throws Exception
  {
    final List<SegmentAnalysis> results = getSegmentAnalysises(
        new QueryableIndexSegment("test_1", TestIndex.getMMappedTestIndex())
    );

    Assert.assertEquals(1, results.size());

    final SegmentAnalysis analysis = results.get(0);
    Assert.assertEquals("test_1", analysis.getId());

    final Map<String,ColumnAnalysis> columns = analysis.getColumns();
    Assert.assertEquals(TestIndex.COLUMNS.length, columns.size()); // All columns including time

    for (String dimension : TestIndex.DIMENSIONS) {
      final ColumnAnalysis columnAnalysis = columns.get(dimension.toLowerCase());

      Assert.assertEquals(dimension, ValueType.STRING.name(), columnAnalysis.getType());
      Assert.assertTrue(dimension, columnAnalysis.getSize() > 0);
      Assert.assertTrue(dimension, columnAnalysis.getCardinality() > 0);
    }

    for (String metric : TestIndex.METRICS) {
      final ColumnAnalysis columnAnalysis = columns.get(metric.toLowerCase());

      Assert.assertEquals(metric, ValueType.FLOAT.name(), columnAnalysis.getType());
      Assert.assertTrue(metric, columnAnalysis.getSize() > 0);
      Assert.assertNull(metric, columnAnalysis.getCardinality());
    }
  }

  /**
   * *Awesome* method name auto-generated by IntelliJ!  I love IntelliJ!
   *
   * @param index
   * @return
   */
  private List<SegmentAnalysis> getSegmentAnalysises(Segment index)
  {
    final QueryRunner runner = QueryRunnerTestHelper.makeQueryRunner(
        (QueryRunnerFactory) new SegmentMetadataQueryRunnerFactory(), index
    );

    final SegmentMetadataQuery query = new SegmentMetadataQuery(
        "test", QuerySegmentSpecs.create("2011/2012"), null, null, null
    );
    return Sequences.toList(query.run(runner), Lists.<SegmentAnalysis>newArrayList());
  }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.List;
import org.apache.beam.sdk.transforms.CombineWithContext.CombineFnWithContext;
import org.apache.beam.sdk.transforms.CombineWithContext.Context;
import org.apache.beam.sdk.transforms.CombineWithContext.KeyedCombineFnWithContext;
import org.apache.beam.sdk.transforms.Sum;
import org.apache.beam.sdk.util.state.StateContexts;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link CombineFnUtil}.
 */
@RunWith(JUnit4.class)
public class CombineFnUtilTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  KeyedCombineFnWithContext<Integer, Integer, Integer, Integer> mockCombineFn;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    mockCombineFn = mock(KeyedCombineFnWithContext.class, withSettings().serializable());
  }

  @Test
  public void testNonSerializable() throws Exception {
    expectedException.expect(NotSerializableException.class);
    expectedException.expectMessage(
        "Cannot serialize the CombineFn resulting from CombineFnUtil.bindContext.");

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(buffer);
    oos.writeObject(CombineFnUtil.bindContext(mockCombineFn, StateContexts.nullContext()));
  }

  @Test
  public void testToFnWithContextIdempotent() throws Exception {
    CombineFnWithContext<Integer, int[], Integer> fnWithContext =
        CombineFnUtil.toFnWithContext(Sum.ofIntegers());
    assertTrue(fnWithContext == CombineFnUtil.toFnWithContext(fnWithContext));

    KeyedCombineFnWithContext<Object, Integer, int[], Integer> keyedFnWithContext =
        CombineFnUtil.toFnWithContext(Sum.ofIntegers().asKeyedFn());
    assertTrue(keyedFnWithContext == CombineFnUtil.toFnWithContext(keyedFnWithContext));
  }

  @Test
  public void testToFnWithContext() throws Exception {
    CombineFnWithContext<Integer, int[], Integer> fnWithContext =
        CombineFnUtil.toFnWithContext(Sum.ofIntegers());
    List<Integer> inputs = ImmutableList.of(1, 2, 3, 4);
    Context nullContext = CombineContextFactory.nullContext();
    int[] accum = fnWithContext.createAccumulator(nullContext);
    for (Integer i : inputs) {
      accum = fnWithContext.addInput(accum, i, nullContext);
    }
    assertEquals(10, fnWithContext.extractOutput(accum, nullContext).intValue());

    KeyedCombineFnWithContext<String, Integer, int[], Integer> keyedFnWithContext =
        CombineFnUtil.toFnWithContext(Sum.ofIntegers().<String>asKeyedFn());
    String key = "key";
    accum = keyedFnWithContext.createAccumulator(key, nullContext);
    for (Integer i : inputs) {
      accum = keyedFnWithContext.addInput(key, accum, i, nullContext);
    }
    assertEquals(10, keyedFnWithContext.extractOutput(key, accum, nullContext).intValue());
  }
}

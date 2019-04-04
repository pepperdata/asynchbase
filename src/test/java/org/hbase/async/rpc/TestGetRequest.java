/*
 * Copyright (C) 2015  The Async HBase Authors.  All rights reserved.
 * This file is part of Async HBase.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the StumbleUpon nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.hbase.async.rpc;

import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertSame;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.hbase.async.BaseTestHBaseClient;
import org.hbase.async.HBaseClient;
import org.hbase.async.KeyValue;
import org.hbase.async.RegionClient;
import org.hbase.async.stats.Counter;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.xml.*",
  "ch.qos.*", "org.slf4j.*",
  "com.sum.*", "org.xml.*"})
@PrepareForTest({ HBaseClient.class, RegionClient.class })
final class TestGetRequest extends BaseTestHBaseClient {

  /**
   * Test a simple get request with table and key
   * @throws Exception
   */
  @Test
  public void simpleGet() throws Exception {
    final GetRequest get = new GetRequest(TABLE, KEY);
    final ArrayList<KeyValue> row = new ArrayList<KeyValue>(1);
    row.add(KV);

    when(regionclient.isAlive()).thenReturn(true);
    doAnswer(new Answer<Object>() {
      public Object answer(final InvocationOnMock invocation) {
        get.getDeferred().callback(row);
        return null;
      }
    }).when(regionclient).sendRpc(get);

    assertSame(row, client.get(get).joinUninterruptibly());
  }

  /**
   * Test a get that queries by family byte
   * @throws Exception
   */
  @Test
  public void getWithFamily() throws Exception {
    final GetRequest get = new GetRequest(TABLE, KEY, FAMILY);
    final ArrayList<KeyValue> row = new ArrayList<KeyValue>(1);
    row.add(KV);
    when(regionclient.isAlive()).thenReturn(true);
    doAnswer(new Answer<Object>() {
      public Object answer(final InvocationOnMock invocation) {
        get.getDeferred().callback(row);
        return null;
      }
    }).when(regionclient).sendRpc(get);
    assertSame(row, client.get(get).joinUninterruptibly());
  }
  
  /**
   * Test a get that queries by family byte and qualifier byte
   * @throws Exception
   */
  @Test
  public void getWithQualifier() throws Exception {
    final GetRequest get = new GetRequest(TABLE, KEY, FAMILY, QUALIFIER);
    final ArrayList<KeyValue> row = new ArrayList<KeyValue>(1);
    row.add(KV);
    when(regionclient.isAlive()).thenReturn(true);
    doAnswer(new Answer<Object>() {
      public Object answer(final InvocationOnMock invocation) {
        get.getDeferred().callback(row);
        return null;
      }
    }).when(regionclient).sendRpc(get);
    assertSame(row, client.get(get).joinUninterruptibly());
  }
  
  /**
   * Test a get that queries by family string
   * @throws Exception
   */
  @Test
  public void getWithFamilyString() throws Exception{
    final GetRequest get = new GetRequest(new String(TABLE), new String(KEY), 
        new String(FAMILY));
    final ArrayList<KeyValue> row = new ArrayList<KeyValue>(1);
    row.add(KV);
    when(regionclient.isAlive()).thenReturn(true);
    doAnswer(new Answer<Object>() {
      public Object answer(final InvocationOnMock invocation) {
        get.getDeferred().callback(row);
        return null;
      }
    }).when(regionclient).sendRpc(get);
    assertSame(row, client.get(get).joinUninterruptibly()); 
  }
  
  /**
   * Test a get that queries by family string and qualifier string
   * @throws Exception
   */
  @Test
  public void getWithQualifierString() throws Exception {
    final GetRequest get = new GetRequest(new String(TABLE), new String(KEY), 
        new String(FAMILY), new String(QUALIFIER));
    final ArrayList<KeyValue> row = new ArrayList<KeyValue>(1);
    row.add(KV);
    when(regionclient.isAlive()).thenReturn(true);
    doAnswer(new Answer<Object>() {
      public Object answer(final InvocationOnMock invocation) {
        get.getDeferred().callback(row);
        return null;
      }
    }).when(regionclient).sendRpc(get);
    assertSame(row, client.get(get).joinUninterruptibly()); 
  }

}

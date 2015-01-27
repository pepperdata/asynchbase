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
package org.hbase.async;

import org.jboss.netty.buffer.ChannelBuffer;

import org.hbase.async.generated.FilterPB;

/**
 * Filter which returns only the key component of each KV. The value is
 * rewritten as empty. It can be used to grab all the keys without
 * also having to grab all the values.
 */
public final class KeyOnlyFilter extends ScanFilter {

  private static final byte[] NAME = Bytes.ISO88591("org.apache.hadoop"
      + ".hbase.filter.KeyOnlyFilter");
  private boolean len_as_val;

  /**
   * Constructor
   * Equivalent to {@link #KeyOnlyFilter(boolean)KeyOnlyFilter}{@code (false)}
   */
  public KeyOnlyFilter() {
    this(false);
  }

  /**
   * Constructor
   * @param len_as_val If {@code true}, return the length of the stored value
   * instead of empty.
   */
  public KeyOnlyFilter(boolean len_as_val) {
    this.len_as_val = len_as_val;
  }

  @Override
  byte[] serialize() {
    final FilterPB.KeyOnlyFilter.Builder filter =
        FilterPB.KeyOnlyFilter.newBuilder();
    filter.setLenAsVal(len_as_val);
    return filter.build().toByteArray();
  }

  @Override
  byte[] name() {
    return NAME;
  }

  @Override
  int predictSerializedSize() {
    return 1 + NAME.length + 1;
  }

  @Override
  void serializeOld(final ChannelBuffer buf) {
    buf.writeByte((byte) NAME.length);     // 1
    buf.writeBytes(NAME);                  // 44
    buf.writeByte(len_as_val ? 1 : 0);
  }

  public String toString() {
    return "KeyOnlyFilter(" + len_as_val + ")";
  }
}
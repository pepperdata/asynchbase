package org.hbase.async.rpc;

import java.util.HashMap;

import org.jboss.netty.buffer.ChannelBuffer;

import org.hbase.async.generated.HBasePB;
import org.hbase.async.generated.RPCPB;

import static org.hbase.async.util.Buffers.*;

public class ExceptionHandler {
  private ExceptionHandler() {} // prevent instances

  /** Maps remote exception types to our corresponding types.  */
  private static final HashMap<String, HBaseException> REMOTE_EXCEPTION_TYPES;
  static {
    REMOTE_EXCEPTION_TYPES = new HashMap<String, HBaseException>();
    REMOTE_EXCEPTION_TYPES.put(NoSuchColumnFamilyException.REMOTE_CLASS,
                               new NoSuchColumnFamilyException(null, null));
    REMOTE_EXCEPTION_TYPES.put(NotServingRegionException.REMOTE_CLASS,
                               new NotServingRegionException(null, null));
    REMOTE_EXCEPTION_TYPES.put(RegionMovedException.REMOTE_CLASS,
                               new RegionMovedException(null, null));
    REMOTE_EXCEPTION_TYPES.put(RegionOpeningException.REMOTE_CLASS,
                               new RegionOpeningException(null, null));
    REMOTE_EXCEPTION_TYPES.put(RegionServerAbortedException.REMOTE_CLASS,
                               new RegionServerAbortedException(null, null));
    REMOTE_EXCEPTION_TYPES.put(RegionServerStoppedException.REMOTE_CLASS,
                               new RegionServerStoppedException(null, null));
    REMOTE_EXCEPTION_TYPES.put(RegionTooBusyException.REMOTE_CLASS,
                               new RegionTooBusyException(null, null));
    REMOTE_EXCEPTION_TYPES.put(ServerNotRunningYetException.REMOTE_CLASS,
                               new ServerNotRunningYetException(null, null));
    REMOTE_EXCEPTION_TYPES.put(UnknownScannerException.REMOTE_CLASS,
                               new UnknownScannerException(null, null));
    REMOTE_EXCEPTION_TYPES.put(UnknownRowLockException.REMOTE_CLASS,
                               new UnknownRowLockException(null, null));
    REMOTE_EXCEPTION_TYPES.put(VersionMismatchException.REMOTE_CLASS,
                               new VersionMismatchException(null, null));
    REMOTE_EXCEPTION_TYPES.put(CallQueueTooBigException.REMOTE_CLASS,
                               new CallQueueTooBigException(null, null));
    REMOTE_EXCEPTION_TYPES.put(UnknownProtocolException.REMOTE_CLASS,
                               new UnknownProtocolException(null, null));
  }

  /**
   * De-serializes an exception from HBase 0.94 and before.
   * @param buf The buffer to read from.
   * @param request The RPC that caused this exception.
   */
  public static HBaseException deserializeException(
    final HBaseRpc request,
    final ChannelBuffer buf
  ) {
    // In case of failures, the rest of the response is just 2
    // Hadoop-encoded strings.  The first is the class name of the
    // exception, the 2nd is the message and stack trace.
    final String type = readHadoopString(buf);
    final String msg = readHadoopString(buf);
    return makeException(request, type, msg);
  }

  /**
   * Creates an appropriate {@link HBaseException} for the given type.
   * When we de-serialize an exception from the wire, we're given a string as
   * a class name, which we use here to map to an appropriate subclass of
   * {@link HBaseException}, for which we create an instance that we return.
   * @param request The RPC in response of which the exception was received.
   * @param type The fully qualified class name of the exception type from
   * HBase's own code.
   * @param msg Some arbitrary additional string that accompanies the
   * exception, typically carrying a stringified stack trace.
   */
  public static final HBaseException makeException(
    final HBaseRpc request,
    final String type,
    final String msg
  ) {
    final HBaseException exc = REMOTE_EXCEPTION_TYPES.get(type);
    if (exc != null) {
      return exc.make(msg, request);
    } else {
      return new RemoteException(type, msg);
    }
  }

  /**
   * Decodes an exception from HBase 0.95 and up.
   * @param e the exception protobuf obtained from the RPC response header
   * containing the exception.
   */
  public static HBaseException decodeException(
    final HBaseRpc request,
    final RPCPB.ExceptionResponse e
  ) {
    final String type;
    if (e.hasExceptionClassName()) {
      type = e.getExceptionClassName();
    } else {
      type = "(missing exception type)";  // Shouldn't happen.
    }
    return makeException(request, type, e.getStackTrace());
  }

  /**
   * Decodes an exception from HBase 0.95 and up {@link HBasePB.NameBytesPair}.
   * @param pair A pair whose name is the exception type, and whose value is
   * the stringified stack trace.
   */
  public static HBaseException decodeExceptionPair(
    final HBaseRpc request,
    final HBasePB.NameBytesPair pair
  ) {
    final String stacktrace;
    if (pair.hasValue()) {
      stacktrace = pair.getValue().toStringUtf8();
    } else {
      stacktrace = "(missing server-side stack trace)";  // Shouldn't happen.
    }
    return makeException(request, pair.getName(), stacktrace);
  }
}

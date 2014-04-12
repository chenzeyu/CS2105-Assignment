/**
 * RDTReceiver : Encapsulate a reliable data receiver that runs
 * over a unreliable channel that may drop packets (but is
 * corruption-free and delivers in order).
 *
 * Ooi Wei Tsang
 * CS2105, National University of Singapore
 * 1 March 2011
 */

import java.io.*;
import java.util.*;

/**
 * RDTReceiver receives a data packet from "below" and pass
 * the byte array contained it it to the "above".
 */
class RDTReceiver {
  UDTReceiver udt;
  int seqNumber;

  RDTReceiver(int port) throws IOException
  {
    udt = new UDTReceiver(port);
    seqNumber = 0;
  }

  boolean checkPacket(DataPacket p, int ack){
    return !p.isCorrupted && p.seq == ack;
  }

  void toggleSeq(){
    if (seqNumber == 0) seqNumber = 1;
    else seqNumber = 0;
  }

  /**
   * recv() reads the next in-order, uncorrupted data packet
   * from the layer below and returns the byte array contains
   * the data.  It returns null if when there is no more data
   * to read (i.e., the transmission is complete).
   */
  byte[] recv() throws IOException, ClassNotFoundException
  {
    DataPacket p = udt.recv();

    // send ACK
    while(!checkPacket(p, seqNumber)){
      AckPacket ack = new AckPacket((seqNumber+1)%2);
      udt.send(ack);
      p = udt.recv();
    }
    AckPacket ack = new AckPacket(p.seq);
    udt.send(ack);
    toggleSeq();
    // deliver data
    if (p.length > 0) {
      byte [] copy = new byte[p.length];
      System.arraycopy(p.data, 0, copy, 0, p.length);
      return copy;
    } else {
      return null;
    }
  }

  void close() throws IOException
  {
    udt.close();
  }
}

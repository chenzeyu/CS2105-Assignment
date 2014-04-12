/**
 * RDTSender : Encapsulate a reliable data sender that runs
 * over a unreliable channel that may drop and corrupt packets
 * (but always delivers in order).
 *
 * Ooi Wei Tsang
 * CS2105, National University of Singapore
 * 12 March 2013
 */
import java.io.*;
import java.util.*;

/**
 * RDTSender receives a byte array from "above", construct a
 * data packet, and send it via UDT.  It also receives
 * ack packets from UDT.
 */
class RDTSender {
  UDTSender udt;
  int seqNumber;

  class RDTTimer extends TimerTask {
    UDTSender udt;
    DataPacket p;

    public RDTTimer(UDTSender udt, DataPacket p) {
      this.udt = udt;
      this.p = p;
    }
    public void run() {
      try{
        udt.send(p);
      }  catch(IOException e){
        e.printStackTrace();
      }
    }
  }

  RDTSender(String hostname, int port) throws IOException
  {
    udt = new UDTSender(hostname, port);
    seqNumber = 0;
  }

  boolean checkAck(AckPacket ack, int seq){
    return seq == ack.ack && !ack.isCorrupted;
  }

  void toggleSeq(){
    seqNumber= (seqNumber==1) ? 0 : 1;
  }

  /**
   * send() delivers the given array of bytes reliably and should
   * not return until it is sure that the packet has been
   * delivered.
   */
  void send(byte[] data, int length) throws IOException, ClassNotFoundException
  {
    DataPacket p = new DataPacket(data, length, seqNumber);
    Timer timer = new Timer();
    udt.send(p);
    timer.schedule(new RDTTimer(udt, p), 50, 50);

    AckPacket ack = udt.recv();

    while(!checkAck(ack, seqNumber)) {
      System.out.println("send failed");
      ack = udt.recv();
    }
    System.out.println("sent successfully");
    timer.cancel();
    timer = null;
    toggleSeq();
  }

  /**
   * close() is called when there is no more data to send.
   * This method creates an empty packet with 0 bytes and
   * send it to the receiver, to indicate that there is no
   * more data.
   *
   * This method should not return until it is sure that
   * the empty packet has been delivered correctly.  It
   * catches any EOFException (which signals the receiver
   * has closed the connection) and close its own connection.
   */
  void close() throws IOException, ClassNotFoundException
  {
    DataPacket p = new DataPacket(null, 0, seqNumber);
    udt.send(p);
    Timer timer = new Timer();
    timer.schedule(new RDTTimer(udt, p), 0, 50);
    try {
      AckPacket ack = udt.recv();
      while(!checkAck(ack, seqNumber)){
        ack = udt.recv();
      }
      timer.cancel();
    } catch (EOFException e) {
      //receiver closed connection
      udt.close();
      timer.cancel();
      return;
    }
    finally {
      udt.close();
      return;
    }
  }
}

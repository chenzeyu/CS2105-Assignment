import java.io.*;
import java.util.*;
import java.net.*;

class TCPServer {
  public static void main (String args[]) throws Exception{
    ServerSocket serverSocket = new ServerSocket(9000);
    while(true){
      System.out.println("waiting for new connection");
      Socket s = serverSocket.accept();
      System.out.println("connection accept");

      InputStream is = s.getInputStream();
      BufferedReader br = new BufferedReader(
                            new InputStreamReader(is));
      OutputStream os = s.getOutputStream();
      DataOutputStream output = new DataOutputStream(os);

      String inputString = br.readLine();
      while(inputString.compareTo("") != 0){
        output.writeBytes(inputString.toUpperCase() + "\n");
        inputString = br.readLine();
      }

      s.close();
      System.out.println("connection closed");
    }
  }
}



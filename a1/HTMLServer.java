import java.io.*;
import java.util.*;
import java.net.*;

class HTMLServer {
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

      // read in and parse the HTTP requests
      String inputString = br.readLine();
      String fields[] = inputString.split(" ");
      if (fields[0].equals("GET")){
        String filename = fields[1];
        filename = "/Users/chenzeyu/Desktop/CS2105" + filename;
        while(inputString.compareTo("") != 0){
          inputString = br.readLine();
        }

        File f = new File(filename);
        if (f.canRead()){
          //send file back
          int size = (int)f.length();
          byte buffer[] = new byte[size];
          FileInputStream fis = new FileInputStream(filename);
          fis.read(buffer);

          output.writeBytes("HTTP/1.0 200 OK\r\n");
          output.writeBytes("Content-Type: text/html\r\n");
          output.writeBytes("\r\n");
          output.write(buffer,0,size);
        } else {
          output.writeBytes("HTTP/1.0 404 Not Found\r\n");
          output.writeBytes("\r\n");
        }
      } else {
        while(inputString.compareTo("") != 0){
          inputString = br.readLine();
        }
      }
      s.close();
      System.out.println("connection closed");
    }
  }
}



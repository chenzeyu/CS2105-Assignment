import java.util.*;
import java.io.*;
import java.net.*;

public class WebServer {
  static final String perlPath = "/usr/bin/perl";
  static final String envPath = "/usr/bin/env";
  static final String REQUEST_METHOD = "REQUEST_METHOD";
  static final String QUERY_STRING = "QUERY_STRING";
  static final String CONTENT_TYPE = "CONTENT_TYPE";
  static final String CONTENT_LENGTH = "CONTENT_LENGTH";
  static final String HEADER_END = "\r\n";
  static final int BUFFER_SIZE = 4048;


  private static class URL{
    private String path;
    private String query;
    private String address;

    public URL(String url){
      address = url;
      int queryIndex = url.indexOf("?");
      if(queryIndex == -1){
        path = url;
        query = "";
      } else {
        query = url.substring(queryIndex+1);
        path = url.substring(0, queryIndex);
      }
    }
  }

  private static Process exec(String cmd) throws Exception{
    return Runtime.getRuntime().exec(cmd);
  }

  private static Hashtable<String,String> getHeaders(BufferedReader reader) throws Exception{
    Hashtable<String,String> headers = new Hashtable<String,String>();
    String line;
    while((line = reader.readLine()) != null && line.length() != 0){
      //System.out.println(line);
      String[] contents = line.split(": ");
      headers.put(contents[0], contents[1]);
    }
    return headers;
  }

  private static ServerSocket setUpServer(String inputPort) throws Exception{
    int port = Integer.parseInt(inputPort);
    ServerSocket theSocket = new ServerSocket(port);
    return theSocket;
  }

  private static void executeCGI(Hashtable<String,String> env, String fileName, DataOutputStream dos, BufferedReader br) throws Exception{
    String envArgs = "";
    Iterator<Map.Entry<String, String>> itr = env.entrySet().iterator();
    while(itr.hasNext()){
      Map.Entry<String, String> entry = itr.next();
      envArgs += " ";
      envArgs += entry.getKey();
      envArgs += "=";
      envArgs += entry.getValue();
    }
    String command = envPath + envArgs+" "+perlPath+" " + fileName;
    Process pro = exec(command);
    System.out.println("exec: " + command);

    //post
    if(br != null){
      DataOutputStream newdos = new DataOutputStream(pro.getOutputStream());
      int contentLength = Integer.parseInt(env.get(CONTENT_LENGTH));
      char[] content = new char[contentLength];
      br.read(content, 0, contentLength);
      String toWrite = new String(content);
      newdos.writeBytes(toWrite);
      newdos.flush();
    }

    // write output
    byte[] buffer = new byte[BUFFER_SIZE];
    int count;
    DataInputStream dis = new DataInputStream(pro.getInputStream());
    dos.writeBytes("HTTP/1.0 200 OK"+HEADER_END);
    while ((count = dis.read(buffer)) != -1) {
      dos.write(buffer, 0, count);
      System.out.println("buffffer " + dis.read(buffer));
    }
  }

  private static void runServer(ServerSocket svrSocket, String execPath) throws Exception{

    while(true){
      Socket s = svrSocket.accept();
      //set up BufferedReader from svrSocket
      InputStream is = s.getInputStream();
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      //set up the DataOutPutSteam from svrSocket
      OutputStream os = s.getOutputStream();
      DataOutputStream dos = new DataOutputStream(os);

      Hashtable<String,String> enVariables = new Hashtable<String,String>();
      String line = br.readLine();

      String contents[] = line.split(" ");
      enVariables.put(REQUEST_METHOD, contents[0]);
      URL url = new URL(contents[1]);
      enVariables.put(QUERY_STRING, url.query);

      Hashtable<String, String> headers = getHeaders(br);
      String filePath = url.path;
      if(url.path.startsWith("/")){
        filePath = filePath.substring(1);
        filePath = execPath + filePath;
      }

      if (enVariables.get(REQUEST_METHOD).equals("POST")) {
        enVariables.put(CONTENT_TYPE, headers.get("Content-Type"));
        enVariables.put(CONTENT_LENGTH, headers.get("Content-Length"));
      }

      //set up the file, if requests a CGI then execute it, otherwise write to dos.
      File requestedFile = new File(filePath);
      System.out.println(filePath);
      respond(s, br, dos, enVariables, filePath, requestedFile);
    }
  }

  private static void respond(Socket s, BufferedReader br,
      DataOutputStream dos, Hashtable<String, String> enVariables,
      String filePath, File requestedFile) throws Exception,
          FileNotFoundException, IOException {
            if (requestedFile.canRead()) {
              if(filePath.endsWith(".pl")){//execute the CGI
                if(enVariables.get(REQUEST_METHOD).equals("POST")){
                  executeCGI(enVariables, filePath, dos, br);
                }
                else{
                  executeCGI(enVariables, filePath, dos, null);

                }
              } else {//GET other resources
                //System.out.println("!!!!!!!!!!!!!!!:" + contents[0]);
                byte[] buffer = new byte[(int)requestedFile.length()];
                FileInputStream fis = new FileInputStream(filePath);
                fis.read(buffer);
                dos.writeBytes("HTTP/1.0 200 OK"+HEADER_END);
                if (filePath.endsWith("html")) {
                  dos.writeBytes("Content-type: text/html"+HEADER_END);
                } else if (filePath.endsWith("jpg")) {
                  dos.writeBytes("Content-type: image/jpeg"+HEADER_END);
                } else if (filePath.endsWith("css")) {
                  dos.writeBytes("Content-type: text/css"+HEADER_END);
                }
                dos.writeBytes(HEADER_END);
                dos.write(buffer,  0, (int)requestedFile.length());
                fis.close();
              }
              s.close();
            }
          }

  public static void main (String args[]) throws Exception {
    String execPath = System.getProperty("user.home") + "/a1/";
    ServerSocket svrSocket  = setUpServer(args[0]);
    runServer(svrSocket, execPath);
  }
}

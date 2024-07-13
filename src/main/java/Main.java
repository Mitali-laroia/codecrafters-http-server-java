import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {
  private static String directory;

  public static void main(String[] args) {
    if (args.length > 1 && args[0].equals("--directory")) {
      directory = args[1];
      System.out.println(directory);
    }
    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage
    //
    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);
      while (true) {
        clientSocket = serverSocket.accept();
        new Thread(new ClientCall(clientSocket, directory)).start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      if (clientSocket != null) {
        try {
          clientSocket.close();
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        }
      }
    }
  }
}

class ClientCall implements Runnable {
  private Socket clientSocket;
  private String directory;

  public ClientCall(Socket clientSocket, String directory) {
    this.clientSocket = clientSocket;
    this.directory = directory;
  }

  @Override
  public void run() {
    try {
      InputStream inputStream = clientSocket.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      String line = reader.readLine();
      System.out.println(line);
      String[] httpPath = line.split(" ", 0);
      String requestType = httpPath[0];
      String path = httpPath[1];
      OutputStream output = clientSocket.getOutputStream();
      if (httpPath[1].matches("^/echo/(.+)$")) {
        String str = httpPath[1].substring(6);
        String compressionTech = "";
        while (!(line = reader.readLine()).isEmpty()) {
          if (line.startsWith("Accept-Encoding:")) {
              compressionTech = line.substring("Accept-Encoding:".length()).trim();
          }
        }
        Set<String> encodingTypeSet = new HashSet<>(Arrays.asList(compressionTech.split(", ", 0)));
        String httpResponse;
        if(encodingTypeSet.contains("gzip")){
            httpResponse = String.format("HTTP/1.1 200 OK\r\n" + "Content-Encoding: gzip\r\n" + "Content-Type: text/plain\r\n"  
            + "Content-Length: %d\r\n"+ "\r\n" + "%s", str.length(), str);
        }
        else {
          httpResponse = String.format("HTTP/1.1 200 OK\r\n" + "Content-Type: text/plain\r\n"
          + "Content-Length: %d\r\n" + "\r\n" + "%s", str.length(), str);
        }
        output.write(httpResponse.getBytes());
      } else if (requestType.equals("POST") && path.startsWith("/files")) {
        String fileName = path.substring(7);
        int contentLength = 0;
        while (!(line = reader.readLine()).isEmpty()) {
          if (line.startsWith("Content-Length:")) {
              contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
          }
        } 
        char[] body = new char[contentLength];
        reader.read(body,0,contentLength);
        String bodyContent = new String(body);
        File file = new File(directory, fileName);
        try (OutputStreamWriter writer = new OutputStreamWriter(new java.io.FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(bodyContent);
        }
        
        // Send response
        String response = "HTTP/1.1 201 Created\r\n\r\n";
        output.write(response.getBytes());

      } else if (path.startsWith("/files/")) {
        String fileName = path.substring(7);
        File file = new File(directory, fileName);
        if (file.exists()) {
          byte[] fileBytes = Files.readAllBytes(file.toPath());
          String response = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
              fileBytes.length + "\r\n\r\n";
          output.write(response.getBytes());
          output.write(fileBytes);
        } else {
          output.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        }
      } else if (httpPath[1].equals("/user-agent")) {
        reader.readLine();
        // reader.readLine();
        String userAgent = reader.readLine().split("\\s+")[1];
        String httpResponse = String.format("HTTP/1.1 200 OK\r\n" + "Content-Type: text/plain\r\n"
            + "Content-Length: %d\r\n" + "\r\n" + "%s", userAgent.length(), userAgent);
        output.write(httpResponse.getBytes());
      } else if (httpPath[1].equals("/")) {
        output.write(("HTTP/1.1 200 OK\r\n\r\n").getBytes());
      } else {
        output.write(("HTTP/1.1 404 Not Found\r\n\r\n").getBytes());
      }
      System.out.println("accepted new connection");
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      if (clientSocket != null) {
        try {
          clientSocket.close();
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        }
      }
    }
  }
}

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
import java.util.List;
import java.util.stream.Collectors;

public class Main {
  private static String directory;
  public static void main(String[] args) {
    if (args.length > 1 && args[0].equals("--directory")) {
      directory = args[1];
      System.out.println(directory);
    }
    // You can use print statements as follows for debugging, they'll be visible when running tests.
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
      OutputStream output = clientSocket.getOutputStream();
      if (httpPath[1].matches("^/echo/(.+)$")) {
        String str = httpPath[1].substring(6);
        String httpResponse = String.format("HTTP/1.1 200 OK\r\n" + "Content-Type: text/plain\r\n"
            + "Content-Length: %d\r\n" + "\r\n" + "%s", str.length(), str);
        output.write(httpResponse.getBytes());
      } 
      else if (httpPath[1].matches("^/files/(.+)$")) {
        // String filePath = httpPath[1].substring(7);
        System.out.println(directory);
        Boolean fileExists = new File(directory).isFile();
        if(fileExists){
          Path path = Path.of(directory);
          File file = new File(directory, directory);
          String content =  Files.readString(path);
          String httpResponse = String.format("HTTP/1.1 200 OK\r\n" + "Content-Type: application/octet-stream\r\n"
            + "Content-Length: %d\r\n" + "\r\n" + "%s", file.length(), content);
          output.write(httpResponse.getBytes());
        }
        else {
          output.write(("HTTP/1.1 404 Not Found\r\n\r\n").getBytes());
        }
      } 
      else if (httpPath[1].equals("/user-agent")) {
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


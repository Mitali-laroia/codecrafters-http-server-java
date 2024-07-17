
# HTTP Server in Java

Presenting you a http server from scratch in Java. In version one, I have added basic functionalities like make a get request, post request with file upload and apply compression to the file. This TCP server listens on port 4221.

### Routes Info:
- Get request: ``` curl -v http://localhost:4221 ``` 
    
    Expected response: ``` HTTP/1.1 200 OK\r\n\r\n ```

- Get request with response body : ```  curl -v http://localhost:4221/echo/abc ``` 
    
    Expected response: ``` HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 3\r\n\r\nabc ```

- Get request to read headers : ```   curl -v --header "User-Agent: foobar/1.2.3" http://localhost:4221/user-agent``` 
    
    Expected response: ``` HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 12\r\n\r\nfoobar/1.2.3 ```

- Get request to check files in a directory: ```  curl -i http://localhost:4221/files/foo ``` 
    
    Expected response: ``` HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: 14\r\n\r\nHello, World! ```

- Post request to upload files in a directory: ```   curl -v --data "12345" -H "Content-Type: application/octet-stream" http://localhost:4221/files/file_123 ``` 
    
    Expected response: ```HTTP/1.1 201 Created\r\n\r\n ```

- Get request to compress contents of a file using gzip compression: ```   curl -v -H "Accept-Encoding: gzip" http://localhost:4221/echo/abc | hexdump -C ``` 
    
    Expected response: ```HTTP/1.1 200 OK
    Content-Encoding: gzip
    Content-Type: text/plain
    Content-Length: 23 ```

    ``` 1F 8B 08 00 00 00 00 00 ``` 

    ```    00 03 4B 4C 4A 06 00 C2 ```

    ```    41 24 35 03 00 00 00 ```

This server supports taking concurrent requests.


package thread;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class ServerThread implements Runnable {

    private ServerSocket serverSocket;
    private StringBuilder htmlBuilder;
    private StringBuilder responseBuilder;

    private Socket socket;
    private BufferedReader bufferedReader;
    private OutputStream outputStream;

    public ServerThread(ServerSocket serverSocket) throws IOException {
        this.serverSocket = serverSocket;
        this.htmlBuilder = new StringBuilder();
        this.responseBuilder = new StringBuilder();
    }

    public void allClear() throws IOException {
        this.socket.close();
        this.bufferedReader.close();
        this.outputStream.close();

        this.htmlBuilder.delete(0, htmlBuilder.length());
        this.responseBuilder.delete(0, responseBuilder.length());
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Init
                socket = serverSocket.accept();
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream = socket.getOutputStream();

                // First Line of Request
                String firstLine = bufferedReader.readLine();
                StringTokenizer tokenizer = new StringTokenizer(firstLine);

                // Method & Filepath
                String method = tokenizer.nextToken();
                String filepath = tokenizer.nextToken();

                // Current File
                File file = new File("." + filepath);

                if (method.equals("GET")) {
                    // Html
                    makeHtml("GET", file);

                    if (filepath.equals("/")) {
                        makeOkResponse(filepath, method);
                    } else {
                        if (file.exists()) {
                            if (file.canRead()) {
                                makeOkResponse(filepath, method);
                            } else {
                                makeErrResponse("403 Forbidden");
                            }
                        } else {
                            String parentName = getParentName();
                            if (parentName.equals(filepath.substring(filepath.lastIndexOf("/") + 1))) {
                                makeErrResponse("403 Forbidden");
                            } else {
                                makeErrResponse("404 Not Found");
                            }
                        }
                    }
                }

                if (method.equals("POST")) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.contains("Content-Disposition:")) {
                            break;
                        }
                    }
                    String filename = line.substring(line.indexOf("filename=") + 9).replace("\"", "");

                    if (file.exists() && file.isDirectory()) {
                        if (file.canWrite()) {
                            File[] fileList = file.listFiles();
                            if (Arrays.asList(fileList).stream().map(f -> f.getName()).collect(Collectors.toList()).contains(filename)) {
                                makeErrResponse("409 Conflict");
                            } else if (line.contains("form-data") == true) {
                                makeFileWriter(line, filename);
                                makeHtml("POST", new File(".\\" + filename));
                                makeOkResponse(filepath, method);
                            } else {
                                makeErrResponse("405 Method Not Allowed");
                            }
                        } else {
                            makeErrResponse("403 Forbidden");
                        }
                    }
                }

                if (method.equals("DELETE")) {
                    if (file.exists() && file.isFile()) {
                        if (file.delete()) {
                            makeErrResponse("204 No Content");
                        } else {
                            makeErrResponse("403 Forbidden");
                        }
                    } else {
                        makeErrResponse("204 No Content");
                    }
                }

                // write responsse
                outputStream.write(responseBuilder.toString().getBytes());

                allClear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void makeHtml(String method, File currentFile) throws IOException {
        htmlBuilder.append("<html><head><title>" + method + "</title></head><body>");

        if (currentFile.isDirectory()) {
            htmlBuilder.append("<h1>Internal List of " + currentFile.getName() + " folders</h1>");

            File[] fileList = currentFile.listFiles();
            for (File file: fileList)
                htmlBuilder.append("<p>" + file.getName() + "</p>");
        }
        if (currentFile.isFile()) {
            FileReader fileReader = new FileReader(currentFile);
            htmlBuilder.append("<h1>" + currentFile.getName() + "</h1><p>");

            int p = 0;
            while ((p = fileReader.read()) != -1)
                htmlBuilder.append((char)p);
            htmlBuilder.append("</p>");
        }

        htmlBuilder.append("</body></html>");
    }

    public void makeOkResponse(String filepath, String method) {
        responseBuilder.append("HTTP/1.1 200 OK\n");
        responseBuilder.append("Host: localhost:8080\n");
        if (!method.equals("GET"))
            responseBuilder.append(getFileType(filepath));
        responseBuilder.append("Content-Length: " + htmlBuilder.length() + "\n");
        responseBuilder.append("\n" + htmlBuilder + "\n\n");
    }

    public void makeErrResponse(String err) {
        responseBuilder.append("HTTP/1.1 " + err + "\n");
        responseBuilder.append("Host: localhost:" + serverSocket.getLocalPort() + "\n");
        responseBuilder.append("\n" + htmlBuilder + "\n\n");
    }

    public void makeFileWriter(String line, String filename) throws IOException {
        File newFile = new File(".\\" + filename);
        newFile.createNewFile();

        boolean check = false;
        FileWriter fileWriter = new FileWriter(newFile);

        while ((line = bufferedReader.readLine()) != null) {
            if (line.equals(""))                        check = true;
            if (line.contains("------------------"))    break;
            if (check == true)                          fileWriter.append(line + "\n");
        }
        fileWriter.flush();

    }

    public String getParentName() {
        File file = new File(".");
        String parentPath = file.getAbsolutePath().replace("\\.", "");

        return parentPath.substring(parentPath.lastIndexOf("\\") + 1);
    }

    public String getFileType(String filePath) {
        String type = filePath.substring(filePath.lastIndexOf(".") + 1);
        if (type.equals("txt")) {
            return "Content-Type: text/plain\n";
        } else if (type.equals("png") || type.equals("jpeg") || type.equals("gif") || type.equals("webp")) {
            return "Content-Type: image/" + type + "\n";
        } else if (type.equals("js")) {
            return "Content-Type: text/javascript\n";
        }
        return "Content-Type: multipart/form-data\n";
    }
}
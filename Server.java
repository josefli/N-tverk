import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;


public class Server {
   static ArrayList<ClientHandler> clientList = new ArrayList<>();

    public static void main(String args[]) throws IOException {
        int session_id = 1;
        ServerSocket serverSocket = new ServerSocket(80);
        System.out.println("Server is running");

        while (true) {
            Socket socket = serverSocket.accept();
            int cookie = sessionChecker(socket);
            if(cookie!= -1){
                //Sök i listan
                for(ClientHandler client : clientList){
                    if(client.getCookie() == cookie){

                        client.setSocket(socket);

                    }
                }
            } else {
                ClientHandler clientSocket = new ClientHandler(socket, session_id);
                System.out.println("New client");
                new Thread(clientSocket).start();
                clientList.add(clientSocket);
                //Lägg till i listan
            }  
        }
        // serverSocket.close();

    }

    private static int sessionChecker(Socket clientSocket) throws IOException{
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String line = "";
        char[] buffer = new char[1000];
        int charsRead = 0;
        while ((charsRead = inFromClient.read(buffer)) != -1) {
            if (charsRead < 1000) {
                line += String.valueOf(buffer).substring(0, charsRead);
                break;
            } else {
                line += String.valueOf(buffer);
            }
        } 
        return extractCookie(line);
    }
    private static int extractCookie(String httpResponse) {
        try {
            String[] arr = httpResponse.split("user-cookie=", 2);
            System.out.println(arr[1] + " " + arr[1].length());
            return (int) Integer.parseInt(arr[1]);
        } catch(PatternSyntaxException error){
            return -1;
        }  catch(IndexOutOfBoundsException error){
            return -1;
        } 
    }
}



class ClientHandler implements Runnable {

    String startPage = "<html>" + "\n"
            + "<head>" + "\n"
            + "<title>Number guessing game</title>" + "\n"
            + "</head>" + "\n"
            + "<body>" + "\n"
            + "<h1>Number guessing game</h1>" + "\n"
            + "<form name=\"guessform\" method =\"POST\">" + "\n"
            + "<input type =\"text\" id=\"guess\" name=\"guess\">" + "\n"
            + "<input type=\"submit\" value=\"Guess\">" + "\n"
            + "</form>" + "\n"
            + "</body>" + "\n"
            + "</html>";

    final String CRLF = "\r\n";
    private int cookie;
    private GameLogic game = new GameLogic(cookie);
    private Socket clientSocket;


    public ClientHandler(Socket socket, int cookie) {
        clientSocket = socket;
        this.cookie = cookie;
    }

    public int getCookie(){
        return cookie;
    }

    public void setSocket(Socket socket){
        clientSocket = socket;
    }

    @Override
    public void run() {

        try {
            PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream());
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line = "";
            char[] buffer = new char[1000];
            int charsRead = 0;
            while ((charsRead = inFromClient.read(buffer)) != -1) {
                if (charsRead < 1000) {
                    line += String.valueOf(buffer).substring(0, charsRead);
                    break;
                } else {
                    line += String.valueOf(buffer);
                }
            }

            System.out.println(line);
            if (checkIfPOST(line)) {
                String resp = game.giveFeedbackOnGuess(extractNumber(line));
                outToClient.print(createHTTPResponse(generateGuessedPage(resp)));
                outToClient.flush();
            }  else {
                outToClient.print(createHTTPResponse(startPage));
                outToClient.flush();
            }
        } catch (IOException e) {

        }
    }

    private String generateGuessedPage(String result){
        String guessedPage = "<html>" + "\n"
            + "<head>" + "\n"
            + "<title>Number guessing game</title>" + "\n"
            + "</head>" + "\n"
            + "<body>" + "\n"
            + "<h1>Number guessing game</h1>" + "\n"
            + "<p>" + result + "</p>\n" 
            + "<form name=\"guessform\" method =\"POST\">" + "\n"
            + "<input type =\"text\" id=\"guess\" name=\"guess\">" + "\n"
            + "<input type=\"submit\" value=\"Guess\">" + "\n"
            + "</form>" + "\n"
            + "</body>" + "\n"
            + "</html>";

            return guessedPage;
    }

    private String createHTTPResponse(String body) {
        String response = "HTTP/1.1 200 OK" + CRLF +
                "Content-Length: " + body.getBytes().length + CRLF + "Content-Type: text/html" + CRLF + "Connection: keep-alive" + CRLF + "Set-Cookie: user-cookie=" + cookie + CRLF +
                CRLF +
                body + CRLF + CRLF;
        return response;
    }

    private boolean checkIfPOST(String httpResponse) {
        return httpResponse.contains("POST");
    }

    private double extractNumber(String httpResponse) {
        String[] arr = httpResponse.split("guess=", 2);
        System.out.println(arr[1] + " " + arr[1].length());
        return Integer.parseInt(arr[1]);
    }
}


class SessionChecker implements Runnable {
    private Socket clientSocket;
    public SessionChecker(Socket socket) {
        clientSocket = socket;  
    }

    @Override
    public void run() {
        try {
            PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream());
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line = "";
            char[] buffer = new char[1000];
            int charsRead = 0;
            while ((charsRead = inFromClient.read(buffer)) != -1) {
                if (charsRead < 1000) {
                    line += String.valueOf(buffer).substring(0, charsRead);
                    break;
                } else {
                    line += String.valueOf(buffer);
                }
            } 
            int cookie = extractCookie(line);
            if(cookie != -1){


            } else {

            }


        } catch (IOException e) {

        }
    }
    private int extractCookie(String httpResponse) {
        try {
            String[] arr = httpResponse.split("user-cookie=", 2);
            System.out.println(arr[1] + " " + arr[1].length());
            return (int) Integer.parseInt(arr[1]);
        } catch(PatternSyntaxException error){
            return -1;
        } 
    }
}


class GameLogic {
    private int session_id;
    private int correctNumber;
    private int numberOfGuesses;

    public GameLogic(int session_id) {
        this.session_id = session_id;
        this.correctNumber = (int) (Math.random() * 100);
        numberOfGuesses = 0;
        System.out.println("New game object");
    }

    public int getCorrectNumber() {
        return correctNumber;
    }
    public int getNumberOfGuesses() {
        return numberOfGuesses;
    }

    private int checkNumber(double number) {

        if (number < correctNumber) {
            return -1;
        } else if (number > correctNumber) {
            return 1;
        } else {
            return 0;
        }
    }

    public String giveFeedbackOnGuess(double userGuess) {
        System.out.println("User guess: " + userGuess);
        System.out.println("Correct number: " + correctNumber);
        int result = checkNumber(userGuess);
        String output = "";
        switch (result) {
            case -1:
                output = "Guess higher!";
                break;
            case 1:
                output = "Guess lower!";
                break;
            case 0:
                output = "You guessed it!";
                break;
        }
        return output;
    }

}

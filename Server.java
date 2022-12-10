import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;



public class Server {
   static Semaphore semaphore = new Semaphore(1, true);
   static ArrayList<GameLogic> gameSessionList = new ArrayList<>();
   static int game_id = 1;
   static int thread = 0;

    public static void main(String args[]) throws IOException {
        game_id=1;
        ServerSocket serverSocket = new ServerSocket(80);
        System.out.println("Server is running");

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler clientSocket = new ClientHandler(socket, semaphore, thread);
            thread++;
            System.out.println("New client");
            new Thread(clientSocket).start();              
        }
        // serverSocket.close();

    }
    
}



class ClientHandler implements Runnable {
    private int name;
    private static final int NOT_CHECKED = -2;
    private Socket clientSocket;
    PrintWriter outToClient;
    BufferedReader inFromClient;  
    final String CRLF = "\r\n";
    private int cookie = NOT_CHECKED;
    private GameLogic game; 
    private Semaphore sem;
 

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


    public ClientHandler(Socket socket, Semaphore sem, int name) {
        clientSocket = socket;
        this.sem = sem;
        this.name = name;
    }

    public int getCookie(){
        return cookie;
    }

    @Override
    public void run() {
        try {

           PrintWriter outToClient1 = new PrintWriter(clientSocket.getOutputStream());
           BufferedReader inFromClient1 = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line = "";
            char[] buffer = new char[1000];
            int charsRead = 0;
           
            while ((charsRead = inFromClient1.read(buffer)) != -1) {
                if (charsRead < 1000) {
                    line += String.valueOf(buffer).substring(0, charsRead);
                    break;
                } else {   
                    line += String.valueOf(buffer);
                }
            }
            
          
            if(!isFaviconRequest(line)){
                if(cookie == NOT_CHECKED){
                    cookie = extractCookie(line);
                }

                if(cookie != -1){
                    sem.acquire();
                    System.out.println("We are here");
                    System.out.println("Cookie: " + cookie);
                    System.out.println("Semaphore acquired for thread" + name); 
                    System.out.println("List length: " + Server.gameSessionList.size());
                    for(GameLogic g : Server.gameSessionList){
                        System.out.println(g.getGameID());
                        if(g.getGameID() == cookie){
                            System.out.println("Found game");
                            this.game = g;
                           
                        }
                    }
                } else {
                    this.game = new GameLogic(Server.game_id);
                    Server.gameSessionList.add(game);
                    Server.game_id++;
                    System.out.println("New game added: "+ game + " Game id:" + game.getGameID()); 
                    sem.release();
                    System.out.println("Semaphore released"); 
                }
    
                if (checkIfPOST(line)) {
                    String resp = game.giveFeedbackOnGuess(extractNumber(line));
                    outToClient1.print(createHTTPResponse(generateGuessedPage(resp), game.getGameID()));
                    outToClient1.flush();
                }  else {
                    outToClient1.print(createHTTPResponse((startPage), game.getGameID()));
                    outToClient1.flush();
                }

            }
            
            
        } catch (IOException e) {

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Semaphore error");
        }
    }

    private static int extractCookie(String httpResponse) {
        if(httpResponse.contains("user-cookie=")){
            String[] arr = httpResponse.split("user-cookie=", 2);
            System.out.println("Cookie found: " + arr[1].charAt(0));
            return ((int) arr[1].charAt(0)) - 48;  
        }
        return -1;
}

    private boolean isFaviconRequest(String httpRequest){
        return httpRequest.contains("GET /favicon");
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

    private String createHTTPResponse(String body, int cookie_id) {
        String response = "HTTP/1.1 200 OK" + CRLF +
                "Content-Length: " + body.getBytes().length + CRLF + "Content-Type: text/html" + CRLF + "Connection: keep-alive" + CRLF + "Set-Cookie: user-cookie=" + cookie_id + CRLF +
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


class GameLogic {
    private int game_id;
    private int correctNumber;
    private String latestGuess;
    private int numberOfGuesses;

    public GameLogic(int game_id) {
        this.game_id = game_id;
        this.correctNumber = (int) (Math.random() * 100);
        numberOfGuesses = 0;
        System.out.println("New game object");
    }
    
    public int getGameID(){
        return game_id;
    }
    public int getCorrectNumber() {
        return correctNumber;
    }
    public int getNumberOfGuesses() {
        return numberOfGuesses;
    }

    public String getLatestGuess() {
        return latestGuess;
    }

    private int checkNumber(double number) {
        numberOfGuesses++;
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
        latestGuess = output;
        return output;
    }

}

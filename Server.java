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

    public static void main(String args[]) throws IOException {
        game_id=1;
        ServerSocket serverSocket = new ServerSocket(80);
        System.out.println("Server is running");

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler clientSocket = new ClientHandler(socket, semaphore);
            System.out.println("New client");
            new Thread(clientSocket).start();              
        }
        // serverSocket.close();

    }
    
}



class ClientHandler implements Runnable {
    private static final int NOT_CHECKED = -2;
    private boolean cookie_exists = false;
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


    public ClientHandler(Socket socket, Semaphore sem) {
        clientSocket = socket;
        this.sem = sem;
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
                    System.out.println("Request is " + charsRead + "chars.");
                    line += String.valueOf(buffer).substring(0, charsRead);
                    break;
                } else {   
                    System.out.println("Request is 1000 chars.");
                    line += String.valueOf(buffer);
                }
            }
            
            if(!isFaviconRequest(line)){
                if(cookie == NOT_CHECKED){
                    cookie = extractCookie(line);
                    if(cookie != -1){
                        cookie_exists = true;
                    }
                }
                sem.acquire();
                if(cookie_exists){                    
                    for(GameLogic g : Server.gameSessionList){
                        if(g.getGameID() == cookie){
                            this.game = g; 
                            System.out.println("Existing game: " + g.getGameID());
                        }
                    }
                } else {
                    this.game = new GameLogic(Server.game_id);
                    System.out.println("New game: " + game.getGameID());
                    Server.gameSessionList.add(game);
                    Server.game_id++;
                }
                sem.release();
    
                if (checkIfPOST(line)) {
                    System.out.println("Request is POST");
                    String resp = game.giveFeedbackOnGuess(extractNumber(line));
                    outToClient1.print(createHTTPResponse(generateGuessedPage(resp, game.getNumberOfGuesses()), game.getGameID()));
                    outToClient1.flush();
                }  else if(cookie_exists){
                    System.out.println("New tab, existing game");
                    outToClient1.print(createHTTPResponse(generateGuessedPage(game.getLatestGuess(), game.getNumberOfGuesses()), game.getGameID()));
                    outToClient1.flush();
                } else {
                    System.out.println("Request is GET");
                    outToClient1.print(createHTTPResponse((startPage), game.getGameID()));
                    outToClient1.flush();
                }
            }
            
            
        } catch (IOException e) {

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.err.println("Semaphore error");
        }
    }

    private static int extractCookie(String httpResponse) {
        if(httpResponse.contains("user-cookie=")){
            String[] arr = httpResponse.split("user-cookie=", 2);
            //Funkar enbart om cookie är 0-9
            return ((int) arr[1].charAt(0)) - 48;  
        }
        return -1;
}

    private boolean isFaviconRequest(String httpRequest){
        return httpRequest.contains("GET /favicon");
    }

    private String generateGuessedPage(String result, int numberOfGuesses){
        String guessedPage = "<html>" + "\n"
            + "<head>" + "\n"
            + "<title>Number guessing game</title>" + "\n"
            + "</head>" + "\n"
            + "<body>" + "\n"
            + "<h1>Number guessing game</h1>" + "\n"
            + "<p>" + result + "</p>\n"
            + "<p>You have guessed" + numberOfGuesses + "times.</p>\n"  
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
        return Integer.parseInt(arr[1]);
    }

    public int getCookie(){
        return cookie;
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

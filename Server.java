import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;



public class Server {
    

    
    public static void main(String args[]) throws IOException{
        int session_id = 1;
        ServerSocket serverSocket = new ServerSocket(80);
        System.out.println("Server is running");
        
        while(true){
            Socket socket = serverSocket.accept();  
            ClientHandler clientSocket = new ClientHandler(socket, session_id);
            session_id++;
            new Thread(clientSocket).start(); 
        }
       // serverSocket.close();
        
    }
}



 class ClientHandler implements Runnable {
    String html = "<html>" + "\n" 
    + "<head>" + "\n" 
    +"<title>Number guessing game</title>" + "\n" 
    +"</head>" + "\n" 
    +"<body>" + "\n" 
    +"<h1>Number guessing game</h1>" + "\n" 
    +"<form name=\"guessform\" method =\"POST\">" + "\n" 
    + "<input type =\"text\" id=\"guess\" value=\"guess!!!!\" name=\"guess\">" + "\n" 
    +"<input type=\"submit\" value=\"Guess\">" + "\n" 
    +"</form>" + "\n" 
    +"</body>" + "\n" 
    +"</html>";
  



    final String CRLF =  "\r\n";
    private int session_id;
    private GameLogic game = new GameLogic(session_id);
    private Socket clientSocket;   
    
    public ClientHandler(Socket socket, int session_id){
        clientSocket = socket;
        this.session_id = session_id;
    }

    private String createHTTPResponse(String body) {
        String response = "HTTP/1.1 200 OK" + CRLF +
    	"Content-Length: " + body.getBytes().length + CRLF +  "Content-Type: text/html" + CRLF +
    								CRLF +
    								body + CRLF +CRLF;
        return response;
    }

    @Override
    public void run() {
    
            
            try {
                PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream());
               
                System.out.println(game.getCorrectNumber());
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String line;
                char[] buffer = new char[500];
                
                
                 while ((inFromClient.read(buffer)) != -1) {
                    System.out.println(buffer);
                    
                  // String response = game.giveFeedbackOnGuess(Integer.parseInt(line));
                 // System.out.println("To browser");
                 // outToClient.print(createHTTPResponse(html));
                   
                   //outToClient.flush();    
                         
                            
                }
              /*  System.out.println("HEJSAN!!!!!!!!!!!!!");      
                System.out.println("Sending to browser \n");
                System.out.println(createHTTPResponse("HEJ"));
                String resp =createHTTPResponse("");
               // System.out.println(resp);
                outToClient.println(createHTTPResponse("<body>HEJ</body>" + CRLF));
                outToClient.flush();  */     
            } catch (IOException e) {
                
            }
        }
    }


    class GameLogic {
        private int session_id;
        private int correctNumber;

        public GameLogic(int session_id){
            this.session_id = session_id;
            this.correctNumber = (int) (Math.random() * 100);
        }

        public int getCorrectNumber(){
            return correctNumber;
        }

        private int checkNumber (int number) {
           
            if(number < correctNumber){
                return -1;
            } else if (number > correctNumber) {
                return 1;
            } else {
                return 0;
            }
        }

        public String giveFeedbackOnGuess (int userGuess) {
            int result = checkNumber(userGuess);
            String output="";
            switch(result){
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


    
    

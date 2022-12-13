import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class EmailClient {
    private static String host = "webmail.kth.se";
    private static int port = 993;
    private static String tag = "A0";
    private static int tagNr = 1;
    private static String lineEnding = "\r\n";
    private static Scanner sc = new Scanner(System.in);
    private static String username;
    private static String password;


    public static void main(String[] args) throws UnknownHostException, IOException{
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);
        PrintWriter writer = new PrintWriter(sslSocket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

        System.out.println("\n\n-- To exit, type q --\n\n");

        read(reader);
        getUserInfo();
        toServer(writer, createCommand("LOGIN " + username + " " + password));
        read(reader);
        
        while(true){
            incTag();
            fromCommandLine(writer);
            read(reader);
        }
    }

    private static void read(BufferedReader reader) throws IOException{
        String line;
        System.out.print("S: ");
        while((line = reader.readLine()) != null){
            System.out.println(line);
            if(line.contains(tag + tagNr) || line.contains("* OK The Microsoft Exchange IMAP4 service is ready.")){
                break;
            }
            if(line.contains("BAD")){
                System.out.println(line);
                break;
            }
        }
    }

    private static void fromCommandLine(PrintWriter writer){
        String msg;
        String l = sc.nextLine();

        if(isQ(l)){
            quit();
        }

        msg = tag + tagNr + " " + l + lineEnding;
        System.out.println("C: " + msg);
        toServer(writer, msg);
    }

    private static void toServer(PrintWriter writer, String msg){
        writer.print(msg);
        writer.flush();
    }

    private static void getUserInfo(){
        System.out.print("Enter your username: ");
        username = sc.nextLine();
        if(isQ(username)){
            quit();
        }
        System.out.print("Enter your password: ");
        password = String.valueOf(System.console().readPassword());
        if(isQ(password)){
            quit();
        }
        System.out.println();
    }

    private static String createCommand(String command){
        return tag + tagNr + " " + command + lineEnding;
    }

    private static boolean isQ(String s){
        return s.equals("q");
    }

    private static void quit(){
        System.out.println("Exiting program");
        System.exit(0);
    }

    private static void incTag(){
        tagNr++;
    }
}

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.io.File;

public class OS {
    private static char[]R = new char[4];
    private static char[]IR = new char[4];
    private static char[]IC = new char[2];
    private static char[]C = new char[1];
    private static char[][] memory = new char[100][4];

    public static void doFinal(String path){
        File file = new File(path);
        Scanner sc = new Scanner(path);
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            boolean prevLineDTA = false;
            // Read the file line by line until the end is reached
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
                if(line.startsWith("$AMJ")){
                    init();
                }

                else if(line.startsWith("$DTA")){
                    execute();
                    prevLineDTA = true;
                }

                else if(line.startsWith("$END")){
                    continue;
                }

                else if(!prevLineDTA){
                    load(line);
                    System.out.println("Current line: " + line + "\n");
                    System.out.println("Memory:");
                    printMemory();
                    prevLineDTA = false;
                }
            }

        } catch (IOException e) {
            // Handle any potential IO exceptions, such as file not found
            e.printStackTrace();
        }

    }

    private static void init(){
        Arrays.fill(R, '\u0000');
        Arrays.fill(IR, '\u0000');
        Arrays.fill(IC, '\u0000');
        Arrays.fill(C, '\u0000');
    }

    private static void load(String line){
//        char[] buff = new char[4];
//        for(int i=0; i<line.length(); i++){
//            buff = line.getChars();
//        }
        int charIndex = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < memory[0].length; j++) {
                // Check if there are still characters in the string
                if (charIndex < line.length()) {
                    memory[i][j] = line.charAt(charIndex);
                    charIndex++;
                } else {
                    // If the end of the string is reached, you may want to handle this case
                    // For now, let's fill the remaining elements with a default character, say ' '
                    while(j< memory[0].length){
                        memory[i][j] = '\u0000';
                        j++;
                    }
                    return;
                }
            }
        }
    }

    private static void execute(){

    }

    private static void printMemory(){
        for (int i = 0; i < memory.length; i++) {
            for (int j = 0; j < memory[0].length; j++) {
                if(memory[i][j]=='\u0000') return;
                System.out.print(memory[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        OS.doFinal("D:/Advay/JavaProjects/GeneralProblems/jobs.txt");
    }
}

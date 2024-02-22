import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.ListResourceBundle;
import java.util.Scanner;
import java.io.File;

public class OSExtended
{
    private static char[]R = new char[4];
    private static char[]IR = new char[4];
    private static char[]IC = new char[2];
    private static boolean[]C = new boolean[1];          // can we make it boolean??
    private static char[][] memory = new char[100][4];
    private static BufferedReader reader;
    private static char[][]buffer=new char[10][4]; // To store DTA word here from file(you can remove this ,you can apply your own method)
    public static void doFinal(String path)
    {
        File file = new File(path);
        Scanner sc = new Scanner(path);
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            OSExtended.reader = reader;
            String line,nextLine="";
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
                    prevLineDTA = false;
                    continue;
                }

                else if(!prevLineDTA){
                    load(line);
                    char[] temp = {'0', '0'};
                    IC = temp;
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

    private static void init()
    {
        Arrays.fill(R, '\u0000');
        Arrays.fill(IR, '\u0000');
        Arrays.fill(IC, '\u0000');
        Arrays.fill(C, false);
    }

    private static void load(String line)
    {
        int charIndex = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < memory[0].length; j++) {
                // Check if there are still characters in the string
                if (charIndex < line.length()) {
                    memory[i][j] = line.charAt(charIndex);
                    charIndex++;
                    if(line.charAt(charIndex-1)=='H'){
                        break;
                    }
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

    private static void execute()
    {
        int address = 0;
        while(address<10)
        {
            //get the address from IC in numeric form
            //System.out.println("IC "+ Arrays.toString(IC));
            address = Character.getNumericValue(IC[0])*10 + Character.getNumericValue(IC[1]);

            //increment IC
            int numericValue = Character.getNumericValue(IC[1]);
            if (numericValue >= 0) {
                IC[1] = Character.forDigit(numericValue + 1, 10);
            } else {
                // Handle the case where IC[1] is not a digit
                System.out.println("IC[1] is not a valid digit.");
                return;
            }

            //load instruction in IR
           OSExtended.IR = OSExtended.memory[address];

           //break if the instruction is null
            if(IR[0]=='\u0000') break;
            int j=0;

                String instruction="" + IR[0] + IR[1];
                int operand = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);

                j++;
                switch(instruction)
                {

                    case "GD":

                        getdata(operand);
                        break;

                        //store in memory from file(identify DTA command)

                    case "PD":

                        printdata(operand);
                        break;


                    case "LR":

                        if(loadregister(operand)==-1)
                        {
                            System.out.println("LR"+ operand+"Memory in IR already in use! can't allocate");
                        }
                            j++;
                        break;

                    case "SR":

                        storetoLocation(operand);
                        j++;
                        break;

                    case "CR":
                        compRegister(operand);
                        j++;
//                        System.out.println(Arrays.toString(C));
                        break;

                    case "BT":
                       branch(address, operand);
                        //load memory to IR
                        j++;
                        break;

                    default:
                        if(instruction.contains("H"))
                        {
                            System.out.println("Current job ended");
                            return;
                        }
                        else System.out.println("Unkown instruction Encountered  :"+instruction);
                        break;
                }
        }
    }

    private static void getdata(int address) {
        if (address < 10 || address > 90) return;
        try {
            String line = reader.readLine();
            int charIndex = 0;
            for (int i = address; i <address+ 10; i++) {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    private static  void  printdata(int address)
    {
        System.out.println("\n\nOutput:");
        for (int i = address; i <address+ 10; i++) {
            for (int j = 0; j < memory[0].length; j++) {
                if(memory[i][j]=='\u0000'){
                    System.out.println();
                    return;
                }
                System.out.print(memory[i][j]);
            }
        }
    }
    private  static int loadregister(int address)
    {
        if (address > 10 && address < 100)
        {
            for (int i=0;i<4;i++)
            {
                R[i] = memory[address][i];
            }

            return 0;
        }
        else
        {
            return -1;
        }
    }

    private static void storetoLocation(int address)
    {
        if (address > 10 && address < 100)
        {
            for(int i=0;i<4;i++)
            {
                memory[address][i]=R[i];
            }
        }
    }

    private static void compRegister(int address)
    {
        for(int i=0;i<4;i++)
        {
            if(R[i]!=memory[address][i])
            {
                C[0] = false;
                return;
            }
        }
        C[0] = true;
    }

    private static void branch(int currAddress, int branchAddress){
        if(branchAddress<10 || branchAddress>99) return;    //throw exception

        if(!C[0]) return;

        int diff = branchAddress/10 - currAddress/10;
        if(diff>0){
            String line = "";
            //load the other set of 10 instructions
            for(int i = 0; i<diff; i++){
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            load(line);
        }
        IC[0] = '0';
        IC[1] = Character.forDigit(branchAddress%10, 10);
    }
    private static void printMemory()
    {
        for (int i = 0; i < memory.length; i++) {
            for (int j = 0; j < memory[0].length; j++) {
                if(memory[i][j]=='\u0000') return;
                System.out.print(memory[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args)
    {
        OSExtended.doFinal("D:/Advay/JavaProjects/GeneralProblems/jobs.txt");
        //OS.doFinal("C:/Users/asita/OneDrive/Desktop/OS.txt");
    }
}

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.ListResourceBundle;
import java.util.Scanner;
import java.io.File;

public class OS
{
    private static char[]R = new char[4];
    private static char[]IR = new char[4];
    private static char[]IC = new char[2];
    private static char[]C = new char[1];          // can we make it boolean??
    private static char[][] memory = new char[100][4];

    private static char[][]buffer=new char[10][4]; // To store DTA word here from file(you can remove this ,you can apply your own method)
    public static void doFinal(String path)
    {
        File file = new File(path);
        Scanner sc = new Scanner(path);
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line,nextLine="";
            boolean prevLineDTA = false;
            // Read the file line by line until the end is reached
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
                if(line.startsWith("$AMJ")){
                    init();
                }

                else if(line.startsWith("$DTA")){

                    //my code inserted
                    char dummy='1';
                    if(dummy=='1')
                    {
                        nextLine = reader.readLine();
                        dummy='2';
                    }
                    int ind=0;
                    for(int i=0;i<10;i++)
                    {    for(int j=0;j<4;j++)
                        {
                            if (ind< nextLine.length())
                            {
                                buffer[i][j] = nextLine.charAt(ind);
                                ind++;
                            }
                            else
                            break;
                        }
                    }
                    //the code would read the next line from $DTA (only 1 line ) store in buffer and then execute() would be called
                    //till here

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

    private static void init()
    {
        Arrays.fill(R, '\u0000');
        Arrays.fill(IR, '\u0000');
        Arrays.fill(IC, '\u0000');
        Arrays.fill(C, '\u0000');
    }

    private static void load(String line)
    {
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

    private static void execute()
    {
        for(int i=0;i<10;i++)
        {
           String indexString=String.valueOf(i);
           while(indexString.length()<2)
           {
             indexString='0'+indexString;

           }
           IC[0]=indexString.charAt(0);   // IC will store the block  index(keep track of current instruction block)
           IC[1]=indexString.charAt(1);

            int j=0;

                String instruction=String.valueOf(memory[i][j])+String.valueOf(memory[i][j+1]);
                j+=2;
                int address=Integer.parseInt(String.valueOf(memory[i][j])+String.valueOf(memory[i][j+1]));

                j++;
                switch(instruction)
                {

                    case "GD":

                        getdata(address);

                        break;
                        //store in memory from file(identify DTA command)

                    case "PD":

                        printdata(address);
                        break;


                    case "LR":

                        if(loadregister(address)==-1)
                        {
                            System.out.println("Command:"+i+"LR"+ address+"Memory in IR already in use! can't allocate");
                        }
                            j++;

                    case "SR":

                        storetoLocation(address);
                        j++;

                    case "CR":
                        compRegister(address);
                        j++;

                    case "BT":

//                        Branch(btaddress);
                        //load memory to IR
                        j++;

                    default:
                        if(instruction.contains("H"))
                        {
                            System.out.println("Halting the program.");
                            System.exit(0);
                        }
                        System.out.println("Unkown instruction Encountered  :"+instruction);



                }

        }
    }

    private static void getdata(int address)
    {

        for(int i=0;i<10;i++)
        {
            if (address > 10 && address < 100)
            {
                for (int j = 0; j < 4; j++)
                {
                    memory[address][j] = buffer[i][j];
                }
                address++;
            }
            else
                break;
        }
    }
    private static  void  printdata(int address)
    {

        for(int j=0;j<10;j++)
        {
            if (address > 10 && address < 100)
            {
                for (int i = 0; i < 4; i++)
                {
                    System.out.print(memory[address][i]);
                }
            }
            else
            {
                break;
            }
            address++;
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

    private static int compRegister(int address)
    {
        int flag =0;
        for(int i=0;i<4;i++)
        {
            if(R[i]==memory[address][i])
            {
                flag=0;
            }
            else
            {
                flag=-1;
                break;
            }
        }
        return flag;
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

        OS.doFinal("D:/Advay/JavaProjects/GeneralProblems/jobs.txt");

    }
}

import java.io.*;
import java.util.Arrays;
import java.io.*;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.ListResourceBundle;
import java.util.Scanner;


class PCB{
    int jobId, TTL, TLL, TC, LC;
    public PCB(int jobId, int TTL, int TLL){
        this.jobId = jobId;
        this.TTL = TTL;
        this.TLL = TLL;
        this.TC = 0;
        this.LC = 0;
    }
}
public class OSPhase2
{

    private static int blockLen = 10;
    private static char[]R = new char[4];
    private static char[]IR = new char[4];
    private static char[]IC = new char[2];
    private static boolean[]C = new boolean[1];          // can we make it boolean??
    private static char[][] memory = new char[300][4];
    private static char SI;
    private static char[] PTR = new char[2];
    private static PCB pcb;
    private static boolean[] isAllocated = new boolean[memory.length];
    private static BufferedReader reader;
    private static int charArrayToInt(char[] arr){
        int ans = 0;
        for(int i = arr.length-1; i>=0; i--){
            ans+= (int) (Math.pow(10, arr.length-1-i)*Character.valueOf(arr[i]));
        }
        return ans;
    }

    private static void intToCharArray(int val, char[] arr){
        if(val>Math.pow(10, arr.length)-1) return;

    }
    public static void doFinal(String path)
    {
        //clear the contents of the output file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
            writer.write("");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // Handle exception
            e.printStackTrace();
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            OSPhase2.reader = reader;
            String line;
            boolean prevLineDTA = false;
            // Read the file line by line until the end is reached
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
                if(line.startsWith("$AMJ")){
                    init(Integer.parseInt(line.substring(3, 8)), Integer.parseInt(line.substring(8, 13)), Integer.parseInt(line.substring(13, 18)));
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

    private static void init(int jobID, int TTL, int TLL)
    {
        Arrays.fill(R, '\u0000');
        Arrays.fill(IR, '\u0000');
        Arrays.fill(IC, '\u0000');
        Arrays.fill(C, false);
        Arrays.fill(isAllocated, false);
        int pageTableAddress = (int)(Math.random()*30);
        isAllocated[pageTableAddress] = true;
        PTR[0] = Character.forDigit(pageTableAddress/10, 10);
        PTR[1] = Character.forDigit(pageTableAddress%10, 10);
        pcb = new PCB(jobID, TTL, TLL);
    }

    private static int addressMap(int virtualAddress){
        int row = Character.getNumericValue(PTR[0])*10 + Character.getNumericValue(PTR[1]) + virtualAddress/10;
        int realAddress = charArrayToInt(memory[row]) + virtualAddress%10;
        return realAddress;
    }

    private static void load(String line)
    {
        //generate a page frame
        int block;
        boolean allAllocated = true;
        for(boolean val: isAllocated){
            if(!val){
                allAllocated = false;
                break;
            }
        }
        if(allAllocated){
            System.out.println("Memory full.");
            //do something to end the job
            terminate();
        }
        do{
            block = (int) (Math.random()*30);
        }
        while(isAllocated[block]);


        //make the entry into the page table
        for(int i = 0; i<blockLen; i++){
            int row = charArrayToInt(PTR)*10 + i;
            if(memory[row][0] == '\u0000'){
                intToCharArray(block, memory[row]);
                //complete the above function
            }
        }

        int charIndex = 0;
        for (int i = block*10; i < block*10 + 10; i++) {
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
        while(true)
        {
            //get the address from IC in numeric form
            //System.out.println("IC "+ Arrays.toString(IC));
            address = Character.getNumericValue(IC[0])*10 + Character.getNumericValue(IC[1]);

            //increment IC
            int newAddress = address+1;
            IC[0] = Character.forDigit(newAddress/10, 10);
            IC[1] = Character.forDigit(newAddress%10, 10);


            //load instruction in IR
            OSPhase2.IR = OSPhase2.memory[address];

            //break if the instruction is null
            if(IR[0]=='\u0000') break;

            String instruction="" + IR[0] + IR[1];
            int operand = Character.getNumericValue(IR[2])*10 + Character.getNumericValue(IR[3]);

            switch(instruction)
            {

                case "GD":
                    IR[3] = 0;
                    operand = operand - operand%10;
                    SI = '1';
                    MOS(SI, operand);
                    break;

                case "PD":
                    SI = '2';
                    MOS(SI, operand);
                    break;


                case "LR":
                    loadregister(operand);
                    break;

                case "SR":
                    storetoLocation(operand);
                    break;

                case "CR":
                    compRegister(operand);
//                        System.out.println(Arrays.toString(C));
                    break;

                case "BT":
                    branch(address, operand);
                    //load memory to IR
                    break;

                case "AD":
                    add(operand);
                    break;

                case "SB":
                    Subtract(operand);
                    break;

                case "ML":
                    Multiple(operand);
                    break;

                case "DV":
                    Divide(operand);
                    break;

                default:
                    if(instruction.contains("H"))
                    {
                        SI = '3';
                        MOS(SI, operand);
                        System.out.println("Current job ended");
                        return;
                    }
                    else System.out.println("Unkown instruction Encountered  :"+instruction);
                    break;
            }
        }
    }


    public static void MOS(char SI, int operand) {

        switch (SI) {

            case '1':
                getdata(operand);
                break;

            case '2':
                printdata(operand);
                break;

            case '3':
                terminate();
                break;

            default:
                break;
        }
    }

    public static void terminate()
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt", true))) {
            // Write your content here
            writer.write("\n\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // Handle exception
            e.printStackTrace();
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
        StringBuilder sb = new StringBuilder();
        System.out.println("\n\nOutput:");
        boolean breakFlag = false;
        for (int i = address; i <address+ 10; i++) {
            for (int j = 0; j < memory[0].length; j++) {
                if(memory[i][j]=='\u0000'){
                    System.out.println();
                    breakFlag = true;
                    break;
                }
                System.out.print(memory[i][j]);
                sb.append(memory[i][j]);
            }
            if(breakFlag) break;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt", true))) {
            // Write your content here
            writer.write(sb.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // Handle exception
            e.printStackTrace();
        }
    }
    private  static void loadregister(int address)
    {
        if (address < 10 || address > 100) return;
        for (int i=0;i<4;i++)
        {
            R[i] = memory[address][i];
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
        if(branchAddress>99) return;    //throw exception
        if(!C[0]) return;

        IC[0] = Character.forDigit(branchAddress /10, 10);
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

    private static void add(int address)
    {
        int r=0;
        int r2=0;
        for (int i=0;i<4;i++)
        {
            if (R[i]!='\0')
            {
                int operand1 = Character.getNumericValue(R[i]);
                r=(r*10)+operand1;
            }

        }

        for (int i=0;i<4;i++)
        {
            if (memory[address][i]!='\0')
            {
                int operand2 = Character.getNumericValue(memory[address][i]);
                r2=(r2*10)+operand2;
            }

        }
        int result = r + r2;

        for (int i=3;i>=0;i--)
        {
            if(result!=0)
            {
                int rem=result%10;
                char cr=Character.forDigit(rem,10);
                R[i]=cr;
                result=result/10;
            }

        }

    }
    private static void Multiple(int address)
    {
        int r=0;
        int r2=0;
        for (int i=0;i<4;i++)
        {
            if (R[i]!='\0')
            {
                int operand1 = Character.getNumericValue(R[i]);
                r=(r*10)+operand1;
            }

        }

        for (int i=0;i<4;i++)
        {
            if (memory[address][i]!='\0')
            {
                int operand2 = Character.getNumericValue(memory[address][i]);
                r2=(r2*10)+operand2;
            }

        }
        int result = r * r2;

        for (int i=3;i>=0;i--)
        {
            if(result!=0)
            {
                int rem=result%10;
                char cr=Character.forDigit(rem,10);
                R[i]=cr;
                result=result/10;
            }

        }

    }

    private static void Subtract(int address)
    {
        int r=0;
        int r2=0;
        for (int i=0;i<4;i++)
        {
            if (R[i]!='\0')
            {
                int operand1 = Character.getNumericValue(R[i]);
                r=(r*10)+operand1;
            }

        }

        for (int i=0;i<4;i++)
        {
            if (memory[address][i]!='\0')
            {
                int operand2 = Character.getNumericValue(memory[address][i]);
                r2=(r2*10)+operand2;
            }

        }
        int result = r - r2;

        for (int i=3;i>=0;i--)
        {
            if(result!=0)
            {
                int rem=result%10;
                char cr=Character.forDigit(rem,10);
                R[i]=cr;
                result=result/10;
            }

        }

    }

    private static void Divide(int address)
    {
        int r=0;
        int r2=0;
        for (int i=0;i<4;i++)
        {
            if (R[i]!='\0')
            {
                int operand1 = Character.getNumericValue(R[i]);
                r=(r*10)+operand1;
            }

        }

        for (int i=0;i<4;i++)
        {
            if (memory[address][i]!='\0')
            {
                int operand2 = Character.getNumericValue(memory[address][i]);
                r2=(r2*10)+operand2;
            }

        }
        int result = r / r2;

        for (int i=3;i>=0;i--)
        {
            if(result!=0)
            {
                int rem=result%10;
                char cr=Character.forDigit(rem,10);
                R[i]=cr;
                result=result/10;
            }

        }

    }

    public static void main(String[] args)
    {
        OSPhase2.doFinal("D:/Advay/JavaProjects/GeneralProblems/arithematic_jobs.txt");
        //OS.doFinal("C:/Users/asita/OneDrive/Desktop/OS.txt");
    }
}

import java.io.*;
import java.util.Arrays;


class PCB{
    int jobId, TTL, TLL, TC, LC, DC, NGD;
    public PCB(int jobId, int TTL, int TLL){
        this.jobId = jobId;
        this.TTL = TTL;
        this.TLL = TLL;
        this.TC = 0;
        this.LC = 0;
        DC = 0;
    }
}
public class OSPhase2Updated
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
        return Integer.parseInt(new String(arr));
    }

    public static int countSubstringOccurrences(String str, String substr) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substr, index)) != -1) {
            count++;
            index += substr.length();
        }
        return count;
    }

    private static void intToCharArray(int val, char[] arr){
        if(val>Math.pow(10, arr.length)-1) return;

        String strNum = Integer.toString(val);
        int blankSpaces = arr.length-strNum.length();
        for(int i = 0; i<blankSpaces; i++){
            arr[i] = '0';
        }
        for(int i = blankSpaces; i<arr.length; i++)
            arr[i] = strNum.charAt(i-blankSpaces);
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
            OSPhase2Updated.reader = reader;
            String line;
            boolean prevLineDTA = false;
            // Read the file line by line until the end is reached
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
                if(line.startsWith("$AMJ")){
                    init(Integer.parseInt(line.substring(4, 8)), Integer.parseInt(line.substring(8, 12)), Integer.parseInt(line.substring(12, 16)));
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
                    pcb.NGD+=countSubstringOccurrences(line, "GD");
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
        System.out.println("PTR: " + Arrays.toString(PTR));
        System.out.println("PCB: " + pcb.jobId + " " + pcb.TTL + " " + pcb.TLL);
    }

    private static int addressMap(int virtualAddress){
        int row = charArrayToInt(PTR)*10 + virtualAddress/10;
        return charArrayToInt(memory[row])*10 + virtualAddress%10;
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
            terminate();
        }
        do{
            block = (int) (Math.random()*30);
        }
        while(isAllocated[block]);
//        System.out.println("Block: " + block);
//        System.out.println("PTR: " + PTR);
//        System.out.println("Int conversion: "+ charArrayToInt(PTR));
        //make the entry into the page table
        for(int i = 0; i<blockLen; i++){
            int row = charArrayToInt(PTR)*10 + i;
            if(memory[row][0] == '\u0000'){
                intToCharArray(block, memory[row]);
//                System.out.println("Row: " + row + " contents: "+ Arrays.toString(memory[row]));
                break;
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

    private static void load(String line, int address){
        if(isAllocated[address/10]){
            System.out.println("Memory specified by GD already occupied.");
            terminate();
        }
        //====================raise the necessary interrupt====================

        for(int i = 0; i<blockLen; i++){
            int row = charArrayToInt(PTR)*10 + i;
            if(memory[row][0] == '\u0000'){
                intToCharArray(address/10, memory[row]);
                break;
            }
        }

        int charIndex = 0;
        for (int i = address; i < address*10 + 10; i++) {
            for (int j = 0; j < memory[0].length; j++) {
                // Check if there are still characters in the string
                if (charIndex < line.length()) {
                    memory[i][j] = line.charAt(charIndex);
                    charIndex++;
                }
                else {
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

        int virtualAddress = 0;
        while(true)
        {
            //get the address from IC in numeric form
            //System.out.println("IC "+ Arrays.toString(IC));

            //=====================handle the case where memory is empty====================
            int realAddress = addressMap(charArrayToInt(IC));

            //increment IC
            virtualAddress+=1;
            IC[0] = Character.forDigit(virtualAddress/10, 10);
            IC[1] = Character.forDigit(virtualAddress%10, 10);

            //load instruction in IR
            IR = memory[realAddress];
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
                    pcb.TC += 2;
                    pcb.DC++;
                    if(pcb.TC>pcb.TTL)
                    {
                        EM(3);
                    }
                    break;

                case "PD":
                    SI = '2';
                    MOS(SI, operand);

                    pcb.TC=+2;
                    if(pcb.TC>pcb.TTL)
                    {
                        EM(3);
                    }
                    break;


                case "LR":
                    loadregister(operand);
                    pcb.TC++;
                    if(pcb.TC>pcb.TTL)
                    {
                        EM(3);
                    }
                    break;

                case "SR":
                    storetoLocation(operand);

                    pcb.TC++;
                    if(pcb.TC>pcb.TTL)
                    {
                        EM(3);
                    }
                    break;

                case "CR":
                    compRegister(operand);

                    pcb.TC++;
                    if(pcb.TC>pcb.TTL)
                    {
                        EM(3);
                    }

                    break;

                case "BT":
                    branch(realAddress, operand);

                    pcb.TC++;
                    if(pcb.TC>pcb.TTL)
                    {
                        EM(3);
                    }
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
                    else EM(4);
                    break;
            }
        }
    }


    public static void MOS(char SI, int operand) {

        switch (SI) {

            case '1':
                getdata(operand);
                pcb.DC++;
                if(pcb.DC>pcb.NGD)
                {
                    EM(1);
                }

                break;

            case '2':
                printdata(operand);
                pcb.LC++;
                if(pcb.LC>pcb.TLL)
                {
                    EM(2);
                    terminate();
                }
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
        if (address > 290) {
            System.out.println("Address exceeds memory.");
            terminate();
            //==========================handle this as expected==========================
        }
        //load the line with the load function
        try {
            String line = reader.readLine();
            if(line.charAt(0)=='$') EM(1);
            System.out.println("Line: "+line);
            load(line, address);
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
    static void EM(int i) {
        //=================== terminate the program after encountering these errors ==========================
        switch (i) {
            case 1:
                System.out.println("Out of Data Error");
                break;
            case 2:
                System.out.println("Line Limit Exceeded Error");
                break;
            case 3:
                System.out.println("Time Limit Exceeded Error");
                break;

            case 4:
                System.out.println("Operation Code Error");
                break;

            case 5:
                System.out.println("Operand Error");
                break;

            case 6:
                System.out.println("Invalid Page Fault ");
                break;
        }
    }

    public static void main(String[] args)
    {
        doFinal("D:/Advay/JavaProjects/GeneralProblems/testJobs.txt");
    }
}
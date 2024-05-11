import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

class Channel1 extends Thread {
    private BufferQueues bufferQueues;
    private String filepath;
    private Interrupts interrupts;
    int time;
    public void setMembers(BufferQueues bufferQueues, String filePath, Interrupts interrupts) {
        this.bufferQueues = bufferQueues;
        this.filepath = filePath;
        this.interrupts = interrupts;
    }

    @Override
    public void run() {
        Buffer buffer = bufferQueues.empty.poll();
        if(buffer==null) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            // Read the file line by line until the end is reached
            System.out.println("Inside channel 1.");
            while (!(line = reader.readLine()).startsWith("$END")) {
                if(line.length()>buffer.remainingSize){

                    buffer.printContents();
                    System.out.println();
                    bufferQueues.inputful.offer(buffer);
                    //====================check if the pointer issue exists
                    if(bufferQueues.empty.isEmpty()){
                        System.out.println("Empty buffers are insufficient.");
                        System.exit(-1);
                    }
                    buffer = bufferQueues.empty.poll();
                }
                buffer.load(line);
            }
            buffer.load(line);
            buffer.printContents();
            System.out.println();
            bufferQueues.inputful.offer(buffer);
        } catch (IOException e) {
            // Handle any potential IO exceptions, such as file not found
            e.printStackTrace();
        }
        System.out.println("Outside channel 1.");
        interrupts.IOI = '1';
    }
}

class Channel2 extends Thread {
    private BufferQueues bufferQueues;
    private BufferedWriter bufferedWriter;
    private Interrupts interrupts;

    public void setMembers(BufferQueues bufferQueues, BufferedWriter bufferedWriter, Interrupts interrupts) {
        this.bufferQueues = bufferQueues;
        this.bufferedWriter = bufferedWriter;
        this.interrupts = interrupts;
    }

    @Override
    public void run() {
        Buffer buffer = bufferQueues.outputful.poll();
        if(buffer!=null){
            Printer.writeToFile(bufferedWriter, buffer);
            buffer.clear();
            bufferQueues.empty.add(buffer);
            interrupts.IOI = '2';
        }

    }
}

class Channel3 extends Thread {
    private BufferQueues bufferQueues;
    private Drum drum;
    String op;
    Interrupts interrupts;

    public void setMembers(BufferQueues bufferQueues, Drum drum, Interrupts interrupts, String op) {
        this.bufferQueues = bufferQueues;
        this.drum = drum;
        this.interrupts = interrupts;
        this.op = op;
    }

    @Override
    public void run() {
        Buffer buffer;
        switch (op){
            case "IS":
                buffer = bufferQueues.inputful.poll();
                if(buffer!=null){
                    drum.load(buffer);
                    buffer.clear();
                    bufferQueues.empty.offer(buffer);
                }
                else System.out.println("No input buffer available for spooling.");
                break;

            case "OS":
                buffer = bufferQueues.empty.poll();
                if(buffer!=null){
                    drum.storeToBuffer(buffer);
                    bufferQueues.outputful.offer(buffer);
                }
                else System.out.println("No input buffer available for spooling.");
                break;

            case "LD":


        }
    }
}

class Channels{
    Channel1 channel1;
    Channel2 channel2;
    Channel3 channel3;
    boolean[] isBusy = new boolean[3];
    int[] time = new int[3];
    public Channels(){
        channel1 = new Channel1();
        channel2 = new Channel2();
        channel3 = new Channel3();
    }
}

class Printer{
    BufferedWriter bufferedWriter;
    Buffer buffer;

    public Printer(BufferedWriter bufferedWriter, Buffer buffer){
        this.bufferedWriter = bufferedWriter;
        this.buffer = buffer;
    }
    static void writeToFile(BufferedWriter bufferedWriter, Buffer buffer){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<buffer.buffer.length; i++){
            for(int j = 0; j<buffer.buffer[0].length; j++){
                if(buffer.buffer[i][j]=='\u0000') break;
                sb.append(buffer.buffer[i][j]);
            }
        }
        try {
            bufferedWriter.write(sb.toString());
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
class Interrupts{
    char SI, PI, TI, IOI;
    public Interrupts(){
        SI = PI = TI = IOI = '0';
    }
}
class Buffer{
    char[][] buffer;
    int size, wordLength, ptr, remainingSize;
    public Buffer(int capacity, int wordLength){
        buffer = new char[capacity][wordLength];
        this.size = capacity;
        this.wordLength = wordLength;
        ptr = 0;
        remainingSize = capacity*wordLength;
    }
    public void load(String line){
        int charIndex = 0;
        for (int i = ptr; i < buffer.length; i++) {
            for (int j = 0; j < buffer[0].length; j++) {
                if (charIndex < line.length()) {
                    buffer[i][j] = line.charAt(charIndex);
                    charIndex++;
                    remainingSize--;
                    if(line.charAt(charIndex-1)=='H'){
                        if(j!=0) ptr = i+1;
                        else ptr = i;
                        break;
                    }
                } else {
                    if(j!=0) ptr = i+1;
                    else ptr = i;
                    remainingSize = (size - ptr)*wordLength;
                    while(j< buffer[0].length){
                        buffer[i][j] = '\u0000';
                        j++;
                    }
                    return;
                }
            }
        }
        ptr = size;
    }

    public void printContents(){
        if(buffer[0][0]=='\u0000') {
            System.out.println("The buffer is empty.");
            return;
        }
        System.out.println(ptr);
        for (int i = 0; i < ptr; i++) {
            for (int j = 0; j < buffer[0].length; j++) {
                System.out.print(buffer[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    public void clear(){
        for (int i = 0; i < buffer.length; i++) {
            for (int j = 0; j < buffer[0].length; j++) {
                if(buffer[i][j]=='\u0000') return;
                buffer[i][j] = '\u0000';
            }
        }
    }
}
class Drum{
    char[][] drum;
    int ptr;
    int size, wordLength;
    public Drum(int size, int wordLength){
        drum = new char[size][wordLength];
        this.size = size;
        this.wordLength = wordLength;
        ptr = 0;
    }

    public void load(Buffer buffer){
        if((size - ptr - 1)<buffer.size){
            System.out.println("Drum is full.");
            return;
        }
        if(wordLength != buffer.wordLength){
            System.out.println("Drum and buffer must have the same word length.");
            return;
        }
        int i;
        boolean breakFlag = false;
        for(i = ptr; i< buffer.size; i++){
            for(int j = 0; j<wordLength; j++){
                if(buffer.buffer[i - ptr][j]=='\u0000'){
                    breakFlag = true;
                    break;
                }
                drum[i][j] = buffer.buffer[i - ptr][j];
            }
            if(breakFlag) break;
        }
        ptr = ptr + 10;
    }

    void printDrum(){
        for(int i = 0; i< ptr; i++){
            for(int j = 0; j<wordLength; j++){
                if(drum[i][j]=='\u0000') break;
                System.out.print(drum[i][j] + " ");
            }
            System.out.println();
        }
    }

    public void storeToBuffer(Buffer buffer){
        int i;
        boolean breakFlag = false;
        if(ptr==0){
            System.out.println("Drum is empty.");
            return;
        }
        for(i = ptr - 10; i< buffer.size; i++){
            for(int j = 0; j<wordLength; j++){
                if(drum[i][j]=='\u0000'){
                    breakFlag = true;
                    break;
                }
                buffer.buffer[i - ptr + 10][j] = drum[i][j];
            }
            if(breakFlag) break;
        }
    }
}

class BufferQueues{
    Queue<Buffer> empty;
    Queue<Buffer> inputful;
    Queue<Buffer> outputful;

    public BufferQueues(int numBuffers, int capacity, int wordLength){
       empty = new LinkedList<>();
       for(int i = 0; i<numBuffers; i++){
           empty.add(new Buffer(capacity, wordLength));
       }
       inputful = new LinkedList<>();
       outputful = new LinkedList<>();
    }
}

class PCBQueues{
    private static Queue<PCB> readyQueue;
    private static Queue<PCB> loadQueue;
    private static Queue<PCB> IOQueue;
    private static Queue<PCB> terminateQueue;
    public PCBQueues(){
        readyQueue = new LinkedList<>();
        loadQueue = new LinkedList<>();
        IOQueue = new LinkedList<>();
        terminateQueue = new LinkedList<>();
    }
}

class Registers{
    char[]R;
    char[]IR;
    char[]IC;
    char[] PTR;
    boolean[]C;
    public Registers(){
        R = new char[4];
        IR = new char[4];
        IC = new char[2];
        PTR = new char[2];
        C = new boolean[1];
    }
}
public class OSPhase3
{

    private static int blockLen = 10;
    private static int numBuffers = 10;
    private static int bufferRows = 10;
    private static int wordLength = 4;
    private static Registers registers = new Registers();
    private static char[][] memory = new char[300][4];
    private static Buffer[] supervisoryMemory = new Buffer[10];
    private static char[][] drum = new char[500][4];
    private static PCB pcb;
    private static Interrupts interrupts = new Interrupts();
    private static boolean[] isAllocated = new boolean[memory.length];
    private static BufferedReader reader;
    private static String output;
    private static PCBQueues pcbQueues = new PCBQueues();
    private static BufferQueues bufferQueues = new BufferQueues(numBuffers, bufferRows, wordLength);
    private static Channels channels = new Channels();
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
            OSPhase3.reader = reader;
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
                    registers.IC = temp;
//                    System.out.println("Current line: " + line + "\n");
//                    System.out.println("Memory:");
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
        Arrays.fill(registers.R, '\u0000');
        Arrays.fill(registers.IR, '\u0000');
        Arrays.fill(registers.IC, '\u0000');
        Arrays.fill(registers.C, false);
        Arrays.fill(isAllocated, false);
        interrupts.SI = interrupts.TI = interrupts.PI = '0';
        output = "";
        int pageTableAddress;
        do{
            pageTableAddress = (int)(Math.random()*30);
        }while (pageTableAddress<10);
        isAllocated[pageTableAddress] = true;
        registers.PTR[0] = Character.forDigit(pageTableAddress/10, 10);
        registers.PTR[1] = Character.forDigit(pageTableAddress%10, 10);
        pcb = new PCB(jobID, TTL, TLL);
        System.out.println("registers.PTR: " + Arrays.toString(registers.PTR));
        System.out.println("PCB: " + pcb.jobId + " " + pcb.TTL + " " + pcb.TLL);
    }

    private static int addressMap(int virtualAddress){
        int row = charArrayToInt(registers.PTR)*10 + virtualAddress/10;
        System.out.println("Page table entry: " + Arrays.toString(memory[row]));
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
            EM(7);
        }
        do{
            block = (int) (Math.random()*30);
        }
        while(isAllocated[block] || block<10);
//        System.out.println("Block: " + block);
//        System.out.println("registers.PTR: " + registers.PTR);
//        System.out.println("Int conversion: "+ charArrayToInt(registers.PTR));
        //make the entry into the page table
        for(int i = 0; i<blockLen; i++){
            int row = charArrayToInt(registers.PTR)*10 + i;
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
            EM(8);
        }

        for(int i = 0; i<blockLen; i++){
            int row = charArrayToInt(registers.PTR)*10 + i;
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
            if(interrupts.SI=='3') break;
            int realAddress = addressMap(charArrayToInt(registers.IC));

            //increment registers.IC
            virtualAddress+=1;
            registers.IC[0] = Character.forDigit(virtualAddress/10, 10);
            registers.IC[1] = Character.forDigit(virtualAddress%10, 10);

            //load instruction in registers.IR
            registers.IR = memory[realAddress];
            //break if the instruction is null
            if(registers.IR[0]=='\u0000') break;

            String instruction="" + registers.IR[0] + registers.IR[1];
            int operand = Character.getNumericValue(registers.IR[2])*10 + Character.getNumericValue(registers.IR[3]);

            switch(instruction)
            {

                case "GD":
                    registers.IR[3] = 0;
                    operand = operand - operand%10;
                    interrupts.SI = '1';
                    MOS(operand);
                    pcb.TC += 2;
                    if(pcb.TC>pcb.TTL)
                    {
                        interrupts.SI = '3';
                        EM(3);
                    }
                    break;

                case "PD":
                    interrupts.SI = '2';
                    MOS(operand);

                    pcb.LC++;
                    System.out.println("PCB TLL: " + pcb.TLL + "Line Count: " + pcb.LC);
                    if(pcb.LC>pcb.TLL)
                    {
                        EM(2);
                    }
                    pcb.TC++;
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
                    //load memory to registers.IR
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
                        interrupts.SI = '3';
                        MOS(operand);
                        System.out.println("Current job ended");
                        return;
                    }
                    else EM(4);
                    break;
            }
        }
    }

    public static void startChannel(int i){
        if(i==3)i++;
        if(Character.getNumericValue(interrupts.IOI) >= i)
            interrupts.IOI = Character.forDigit(Character.getNumericValue(interrupts.IOI)-i, 10);
        if(i==4)i--;
        channels.time[i-1] = 0;
        channels.isBusy[i-1] = true;
    }
    public static void IR1(){
        String line = "";
        try {
            line = reader.readLine();
        } catch (IOException e) {
            EM(1);
        }
        if(bufferQueues.empty.isEmpty()) return;
        startChannel(1);
        channels.channel1.setMembers(bufferQueues, line, interrupts);
        channels.channel1.start();
        try {
            channels.channel1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void MOS(int operand) {

        switch (interrupts.SI) {

            case '1':
                getdata(operand);
                break;

            case '2':
                printdata(operand);
                if(pcb.LC>pcb.TLL)
                {
                    EM(2);
                    terminate(2);
                }
                break;

            case '3':
                terminate(0);
                break;

            default:
                break;
        }

        switch (interrupts.IOI){
            case '1':
                IR1();
        }
    }

    public static void terminate(int exitCode)
    {
        //clear the memory to avoid garbage in the next job
        int pageTableAddress = charArrayToInt(registers.PTR)*10;
        for(int i = pageTableAddress; i<pageTableAddress+10; i++){
            if(memory[i][0]=='\u0000') break;
            int blockAddress = charArrayToInt(memory[i])*10;
            for(int j = blockAddress; j<blockAddress+10; j++){
                if(memory[j][0]=='\u0000') break;
                Arrays.fill(memory[j], '\u0000');
            }
        }

        //write to the output file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt", true))) {
            String exitLine = "Job finished with exit code " + exitCode + "\n";
            int numIns = charArrayToInt(registers.IC)-1;
            String lastIns;
            if(registers.IR[0]=='H') lastIns = "H";
            else lastIns = new String(Arrays.copyOfRange(registers.IR, 0, 2));
            String status = "Last executed ins: " + lastIns + "\tRun time: "
                    + pcb.TC + "\tLines printed: " + pcb.LC + "\tNo. of executed ins: " +  numIns;

            if(!output.isEmpty()) writer.write(output + "\n");
            writer.write(exitLine);
            writer.write(status);
            writer.write("\n\n\n");
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getdata(int address) {
        if (address > 290) {
            EM(5);
        }
        //load the line with the load function
        try {
            String line = reader.readLine();
            if(line.charAt(0)=='$') EM(1);
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
        output = sb.toString();
    }
    private  static void loadregister(int address)
    {
        if (address < 10 || address > 100) return;
        for (int i=0;i<4;i++)
        {
            registers.R[i] = memory[address][i];
        }

    }

    private static void storetoLocation(int address)
    {
        if (address > 10 && address < 100)
        {
            for(int i=0;i<4;i++)
            {
                memory[address][i]=registers.R[i];
            }
        }
    }

    private static void compRegister(int address)
    {
        for(int i=0;i<4;i++)
        {
            if(registers.R[i]!=memory[address][i])
            {
                registers.C[0] = false;
                return;
            }
        }
        registers.C[0] = true;
    }

    private static void branch(int currAddress, int branchAddress){
        if(branchAddress>99) return;    //throw exception
        if(!registers.C[0]) return;

        registers.IC[0] = Character.forDigit(branchAddress /10, 10);
        registers.IC[1] = Character.forDigit(branchAddress%10, 10);
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
            if (registers.R[i]!='\0')
            {
                int operand1 = Character.getNumericValue(registers.R[i]);
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
                registers.R[i]=cr;
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
            if (registers.R[i]!='\0')
            {
                int operand1 = Character.getNumericValue(registers.R[i]);
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
                registers.R[i]=cr;
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
            if (registers.R[i]!='\0')
            {
                int operand1 = Character.getNumericValue(registers.R[i]);
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
                registers.R[i]=cr;
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
            if (registers.R[i]!='\0')
            {
                int operand1 = Character.getNumericValue(registers.R[i]);
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
                registers.R[i]=cr;
                result=result/10;
            }

        }

    }
    static void EM(int i) {
        switch (i) {
            case 1:
                System.out.println("Out of Data Error");
                terminate(1);
                break;
            case 2:
                System.out.println("Line Limit Exceeded Error");
                terminate(2);
                break;
            case 3:
                System.out.println("Time Limit Exceeded Error");
                terminate(3);
                break;

            case 4:
                System.out.println("Operation Code Error");
                terminate(4);
                break;

            case 5:
                System.out.println("Operand Error");
                terminate(5);
                break;

            case 6:
                System.out.println("Invalid Page Fault");
                terminate(6);
                break;

            case 7:
                System.out.println("Memory full");
                terminate(7);

            case 8:
                System.out.println("Trying to store data in allocated memory for the program");
                terminate(8);
        }
    }

    public static void main(String[] args)
    {
//        doFinal("D:/Advay/JavaProjects/GeneralProblems/jobs.txt");
//        char[] IOI = new char[1];
        channels.channel1.setMembers(bufferQueues, "D:\\Advay\\JavaProjects\\GeneralProblems\\jobs.txt", interrupts);
        channels.channel1.start();
        try {
            channels.channel1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(Buffer item: bufferQueues.empty) item.printContents();
        for(Buffer item: bufferQueues.inputful){
            item.printContents();
            System.out.println();
        }

        Drum drum1 = new Drum(500, 4);
        channels.channel3.setMembers(bufferQueues, drum1, interrupts, "IS");
        channels.channel3.start();
        try {
            channels.channel3.join();
            drum1.printDrum();
            System.out.println("Size of empty queue: " + bufferQueues.empty.size());
            System.out.println("Size of input queue: " + bufferQueues.inputful.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        channels.channel3 = new Channel3();
        channels.channel3.setMembers(bufferQueues, drum1, interrupts, "OS");
        channels.channel3.start();
        try {
            channels.channel3.join();
            System.out.println("Size of empty queue: " + bufferQueues.empty.size());
            System.out.println("Size of output queue: " + bufferQueues.outputful.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter("output.txt", true));
            channels.channel2.setMembers(bufferQueues, writer, interrupts);
            channels.channel2.start();
            try {
                channels.channel2.join();
                System.out.println("Size of empty queue: " + bufferQueues.empty.size());
                System.out.println("Size of output queue: " + bufferQueues.outputful.size());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            // Handle exception
            e.printStackTrace();
        }

//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt", true))) {
////            if(!bufferQueues.inputful.isEmpty()) Printer.writeToFile(writer, bufferQueues.inputful.peek());
//            channels.channel2.setMembers(bufferQueues, writer, interrupts);
//            channels.channel2.start();
//            try {
//                channels.channel2.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

}

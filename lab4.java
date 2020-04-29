import java.util.*;
import java.io.*;

public class lab4 {
    static int M; // machine size
    static int P; // page size
    static int S; // Size of each process
    static int J; // Job mix
    static int N; // # of references for each process
    static String R; // replacement algo

    static final int q = 3; // quantum;
    static ArrayList<Process> processes; // list of processes
    static ArrayList<Frame> frame_table; // frame table

    static Scanner scanner; // random number scanner

    public static void main(String[] args) throws FileNotFoundException {
        // Input check
        if (args.length < 6) {
            System.out.println("Wrong input");
            System.exit(1);
        }
        
        // Initialize scanner
        File file = new File("random-numbers.txt");
        scanner = new Scanner(file);

        // Read input
        M = Integer.parseInt(args[0]);
        P = Integer.parseInt(args[1]);
        S = Integer.parseInt(args[2]);
        J = Integer.parseInt(args[3]);
        N = Integer.parseInt(args[4]);
        R = args[5];

        // Initialize process list
        processes = new ArrayList<>();
        // Initialize frame table
        frame_table = new ArrayList<>();

        // Process J
        switch (J) {
            case 1:
                // 1 process with A = 1, B = C = 0
                processes.add(new Process(0, 1.0, 0.0, 0.0, P, S, N));
                break;
            case 2:
                // Four processes, each with A=1 and B=C=0
                for (int i = 0; i < 4; i++) {
                    processes.add(new Process(i, 1.0, 0.0, 0.0, P, S, N));
                }
                break;
            case 3:
                // Four processes, each with A=B=C=0 (fully random references)
                for (int i = 0; i < 4; i++) {
                    processes.add(new Process(i, 0.0, 0.0, 0.0, P, S, N));
                }
                break;
            case 4:
                // Four Processes. The first process has A=.75, B=.25 and C=0; 
                // the second process has A=.75, B=0, and C=.25;
                // the third process has A=.75, B=.125 and C=.125;
                // and the fourth process has A=.5, B=.125 and C=.125.
                processes.add(new Process(0, 0.75, 0.25, 0.0, P, S, N));
                processes.add(new Process(1, 0.75, 0.0, 0.25, P, S, N));
                processes.add(new Process(2, 0.75, 0.125, 0.125, P, S, N));
                processes.add(new Process(3, 0.5, 0.125, 0.125, P, S, N));
                break;
            default:
                break;
        }

        // Populate frame table: note that frames have same # as pages
        for (int i = 0; i < (M / P); i++) {
            frame_table.add(new Frame(-1, -1, -1));
        }

        // Driver
        int time = 1; // clock, starts at 1
        int terminated = 0; // # of terminated processes

        while (terminated < processes.size()) {
            for (Process p : processes) {
                for (int i = 0; i < q; i++) {
                    // Skip process if already terminated
                    if (p.finished == true) {
                        break;
                    }

                    // All references fulfilled, terminate
                    if (p.ref_count == N) {
                        terminated++;
                        p.finished = true;
                        break;
                    }

                    // Calculate page number
                    int page_num = p.current_ref / P;
                    // Check whether current reference is in frame table
                    int frame_index = check_ref(p, page_num);

                    if (frame_index == -1) {
                        // page fault
                        // First, check if frame table is full
                        // If full, return -1, else return index of highest number frame
                        int free_index = check_full();

                        if (free_index == -1) {
                            // no free frame
                            // use replacement algo 'R' to replace frame
                            int replace_index = -1; // index to replace

                            switch (R) {
                                case "fifo":
                                    // FIFO
                                    replace_index = fifo();
                                    break;
                                case "lru":
                                    // Least Recently Used
                                    replace_index = lru();
                                    break;
                                case "random":
                                    // Random
                                    replace_index = random();
                                    break;
                                default:
                                    break;
                            }

                            // Replace and update
                            Frame f = frame_table.get(replace_index);
                            // Update eviction count
                            processes.get(f.id).evic_count++;
                            // Update residency time
                            processes.get(f.id).residency_time += time - f.initial_time;

                            // Update frame with new page
                            f.id = p.id;
                            f.initial_time = time;
                            f.last_time = time;
                            f.page_number = page_num;
                            f.occupied = true;
                            // Increment page fault count
                            p.fault_count++;
                        } else {
                            // free frame found
                            Frame f = frame_table.get(free_index);
                            // update frame with page
                            f.id = p.id;
                            f.page_number = page_num;
                            f.occupied = true;
                            f.initial_time = time;
                            f.last_time = time;
                            // increment page fault
                            p.fault_count++;
                        }
                    } else {
                        // hit
                        Frame f = frame_table.get(frame_index);
                        // update last access time
                        f.last_time = time;
                    }

                    // Update next reference
                    next_ref(p);
                    // Increment # of references completed
                    p.ref_count++;
                    // Increment time
                    time++;
                }
            }
        }

        // Print output
        print_output();
    }

    // Check whether current reference is in frame table
    // Returns index of hit in frame table, or -1
    static int check_ref(Process p, int page_num) {
        for (int i = 0; i < frame_table.size(); i++) {
            if (p.id == frame_table.get(i).id && page_num == frame_table.get(i).page_number && frame_table.get(i).occupied == true) {
                return i;
            }
        }
        return -1;
    }

    // Check if frame table is full
    static int check_full() {
        // Iterate backwards to find highest numbered free frame
        for (int i = frame_table.size() - 1; i >= 0; i--) {
            if (frame_table.get(i).occupied == false)
                return i;
        }
        return -1;
    }

    // FIFO replacement algo
    static int fifo() {
        int min = Integer.MAX_VALUE;
        int index = -1;

        for (int i = 0; i < frame_table.size(); i++) {
            if (frame_table.get(i).initial_time < min) {
                min = frame_table.get(i).initial_time;
                index = i;
            }
        }
        return index;
    }

    // LRU replacement algo
    static int lru() {
        int min = Integer.MAX_VALUE;
        int index = -1;

        for (int i = 0; i < frame_table.size(); i++) {
            if (frame_table.get(i).last_time < min) {
                min = frame_table.get(i).last_time;
                index = i;
            }
        }
        return index;
    }

    // Random num replacement algo
    static int random() {
        int random = scanner.nextInt();
        int index = random % frame_table.size();
        return index;
    }

    // Set process's next reference
    static void next_ref(Process p) {
        int r = scanner.nextInt();

        double y = r / (Integer.MAX_VALUE + 1d);

        int next_ref = -1;

        if (y < p.A) {
            next_ref = (p.current_ref + 1) % p.S;
        } else if (y < (p.A + p.B)) {
            next_ref = (p.current_ref + p.S - 5) % p.S;
        } else if (y < (p.A + p.B + p.C)) {
            next_ref = (p.current_ref + 4) % p.S;
        } else {
            next_ref = scanner.nextInt() % p.S;
        }

        p.current_ref = next_ref;
    }

    // Print output and echo input
    static void print_output() {
        System.out.println("The machine size is " + M + ".");
        System.out.println("The page size is " + P + ".");
        System.out.println("The process size is " + S + ".");
        System.out.println("The job mix number is " + J + ".");
        System.out.println("The number of references per process is " + N + ".");
        System.out.println("The replacement algorithm is " + R + ".");
        System.out.println();

        int total_faults = 0;
        int total_residency = 0;
        int total_evictions = 0;

        for (Process p : processes) {
            total_faults += p.fault_count;
            total_residency += p.residency_time;
            total_evictions += p.evic_count;

            if (p.evic_count == 0) {
                // No evictions
                System.out.println("Process " + (p.id + 1) + " had " + p.fault_count + " faults.");
                System.out.println("\tWith no evictions, the average residence is undefined.");
            } else {
                double avg_residency = (double) p.residency_time / (double) p.evic_count;
                System.out.println("Process " + (p.id + 1) + " had " + p.fault_count + " faults and " + avg_residency + " average residency.");
            }
        }

        // New line
        System.out.println();

        // Print total faults & total avg residency
        if (total_residency == 0) {
            System.out.println("The total number of faults is " + total_faults + ".");
            System.out.println("\tWith no evictions, the overall average residence is undefined.");
        } else {
            double total_avg_residency = (double) total_residency / (double) total_evictions;
            System.out.println("The total number of faults is " + total_faults + " and the overall average residency is " + total_avg_residency + ".");
        }
    }
}

// Class for individual process
class Process {
    public int id; // index

    public double A;
    public double B;
    public double C;

    public int P;
    public int S;
    public int N;

    public int current_ref; // Current reference
    public int ref_count; // # of references finished
    public int fault_count; // # of page faults
    public int evic_count; // # of evictions
    public int residency_time; // total residency time

    public boolean finished; // process termination

    // Constructor
    public Process(int id, double A, double B, double C, int P, int S, int N) {
        this.id = id;
        this.A = A;
        this.B = B;
        this.C = C;

        this.P = P;
        this.S = S;
        this.N = N;

        // Initial reference
        this.current_ref = (111 * (id + 1)) % S; // increment 1 to id since index

        this.ref_count = 0;
        this.fault_count = 0;
        this.evic_count = 0;
        this.residency_time = 0;
        this.finished = false;
    }
}

// Class for individual frame
class Frame {
    public int id; // Which process is using this frame
    public int page_number; // Which page is using this frame?
    public boolean occupied; // frame occupied or not?
    public int initial_time; // Initial time of reference
    public int last_time; // Last time of reference

    // Constructor
    public Frame(int id, int page_number, int initial_time) {
        this.id = id;
        this.page_number = page_number;
        this.initial_time = initial_time;
        this.occupied = false;
        this.last_time = 0;
    }

}

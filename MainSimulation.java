package org.iquantum.examples.quantum.SmartCity;

import java.io.FileWriter;
import java.io.PrintWriter;

public class MainSimulation {

    public static void main(String[] args) throws Exception {

        // =========================================
        // FIXED CONFIGURATION (AS PER SUPERVISOR)
        // =========================================
        int[] taskCases = {100, 200, 300, 500, 800, 1000, 1500};
        int N = 20; // FIXED NUMBER OF NODES

        // =========================================
        // CSV OUTPUT
        // =========================================
        PrintWriter pw = new PrintWriter(new FileWriter("comparison.csv"));

        pw.println("Algorithm,Nodes,Tasks,AvgEnergy,AvgLatency,LoadBalance,ExecTime");

        // =========================================
        // LOOP OVER TASK COUNTS ONLY
        // =========================================
        for (int T : taskCases) {

            System.out.println("\n=================================");
            System.out.println("Running Simulation");
            System.out.println("Nodes = " + N + " | Tasks = " + T);
            System.out.println("=================================");

            // =========================================
            // HETEROGENEOUS TASK GENERATION
            // =========================================
            SmartCityTask[] tasks = new SmartCityTask[T];

            for (int i = 0; i < T; i++) {

                double lengthMI;
                double dataKB;

                if (i % 3 == 0) {
                    lengthMI = 500;
                    dataKB = 100;
                } else if (i % 3 == 1) {
                    lengthMI = 3000;
                    dataKB = 400;
                } else {
                    lengthMI = 8000;
                    dataKB = 900;
                }

                tasks[i] = new SmartCityTask(i, lengthMI, dataKB);
            }

            // =====================================================
            // QITS
            // =====================================================
            EdgeNode[] qitsNodes = createNodes(N);
            QITScheduler qits = new QITScheduler();

            long qitsStart = System.currentTimeMillis();
            int[] qitsMap = qits.schedule(tasks, qitsNodes);
            long qitsEnd = System.currentTimeMillis();

            double qitsExecTime = (qitsEnd - qitsStart) / 1000.0;

            double[] qitsResult = runSimulation(tasks, qitsNodes, qitsMap);

            pw.println("QITS," + N + "," + T + "," +
                    qitsResult[0] + "," +
                    qitsResult[1] + "," +
                    qitsResult[2] + "," +
                    qitsExecTime);

            // =====================================================
            // FCFS
            // =====================================================
            EdgeNode[] fcfsNodes = createNodes(N);
            FCFSScheduler fcfs = new FCFSScheduler();

            long fcfsStart = System.currentTimeMillis();
            int[] fcfsMap = fcfs.schedule(tasks, fcfsNodes);
            long fcfsEnd = System.currentTimeMillis();

            double fcfsExecTime = (fcfsEnd - fcfsStart) / 1000.0;

            double[] fcfsResult = runSimulation(tasks, fcfsNodes, fcfsMap);

            pw.println("FCFS," + N + "," + T + "," +
                    fcfsResult[0] + "," +
                    fcfsResult[1] + "," +
                    fcfsResult[2] + "," +
                    fcfsExecTime);

            // =====================================================
            // SJF
            // =====================================================
            EdgeNode[] sjfNodes = createNodes(N);
            SJFScheduler sjf = new SJFScheduler();

            long sjfStart = System.currentTimeMillis();
            int[] sjfMap = sjf.schedule(tasks, sjfNodes);
            long sjfEnd = System.currentTimeMillis();

            double sjfExecTime = (sjfEnd - sjfStart) / 1000.0;

            double[] sjfResult = runSimulation(tasks, sjfNodes, sjfMap);

            pw.println("SJF," + N + "," + T + "," +
                    sjfResult[0] + "," +
                    sjfResult[1] + "," +
                    sjfResult[2] + "," +
                    sjfExecTime);

            // =====================================================
            // ROUND ROBIN
            // =====================================================
            EdgeNode[] rrNodes = createNodes(N);
            RoundRobinScheduler rr = new RoundRobinScheduler();

            long rrStart = System.currentTimeMillis();
            int[] rrMap = rr.schedule(tasks, rrNodes);
            long rrEnd = System.currentTimeMillis();

            double rrExecTime = (rrEnd - rrStart) / 1000.0;

            double[] rrResult = runSimulation(tasks, rrNodes, rrMap);

            pw.println("RR," + N + "," + T + "," +
                    rrResult[0] + "," +
                    rrResult[1] + "," +
                    rrResult[2] + "," +
                    rrExecTime);

            System.out.println("Completed -> Nodes: " + N + " Tasks: " + T);
        }

        pw.close();
        System.out.println("\ncomparison.csv generated successfully");
    }

    // =====================================================
    // CREATE HETEROGENEOUS EDGE NODES
    // =====================================================
    private static EdgeNode[] createNodes(int count) {

        EdgeNode[] nodes = new EdgeNode[count];

        for (int i = 0; i < count; i++) {

            double cpu, energy, latency;

            if (i % 3 == 0) {
                cpu = 800;
                energy = 1.5;
                latency = 25;
            } else if (i % 3 == 1) {
                cpu = 2000;
                energy = 0.7;
                latency = 10;
            } else {
                cpu = 3500;
                energy = 0.4;
                latency = 5;
            }

            nodes[i] = new EdgeNode(i, cpu, energy, latency);
        }

        return nodes;
    }

    // =====================================================
    // PERFORMANCE SIMULATION
    // =====================================================
    public static double[] runSimulation(
            SmartCityTask[] tasks,
            EdgeNode[] nodes,
            int[] mapping) {

        double totalEnergy = 0;
        double totalLatency = 0;
        double[] nodeLoads = new double[nodes.length];

        for (int i = 0; i < tasks.length; i++) {

            EdgeNode node = nodes[mapping[i]];

            EdgeNode.Result result = node.simulateExecution(tasks[i]);

            totalEnergy += result.energyJ;
            totalLatency += result.latencySec;

            nodeLoads[mapping[i]] += tasks[i].getLengthMI();
        }

        double mean = 0;
        for (double load : nodeLoads) mean += load;
        mean /= nodes.length;

        double variance = 0;
        for (double load : nodeLoads)
            variance += Math.pow(load - mean, 2);

        variance /= nodes.length;

        double loadBalance = 1 / (1 + Math.sqrt(variance));

        // =========================================
        // RETURN: AvgEnergy, AvgLatency, LoadBalance
        // =========================================
        return new double[] {
                totalEnergy / tasks.length,
                totalLatency / tasks.length,
                loadBalance
        };
    }
}

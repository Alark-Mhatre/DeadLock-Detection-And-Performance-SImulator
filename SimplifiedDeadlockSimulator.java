import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class SimplifiedDeadlockSimulator extends JFrame {

    // GUI Components
    private JTextArea wfgInput, wfgOutput;
    private JTextField availInput;
    private JTextArea allocInput, reqInput, matrixOutput;
    private JTable compareTable;

    public SimplifiedDeadlockSimulator() {
        setTitle("Deadlock Detection & Performance Simulator");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Wait-For Graph (Single Instance)", createWFGPanel());
        tabs.addTab("Matrix Algorithm (Multi Instance)", createMatrixPanel());
        tabs.addTab("Performance Comparison", createComparisonPanel());

        add(tabs);
    }

    // =========================================================================
    // POINT 7: IMPLEMENTATION OF TRADITIONAL ALGORITHMS (CORE LOGIC)
    // =========================================================================

    // --- Algorithm 1: Wait-For Graph (DFS Cycle Detection) ---
    private Map<String, Object> runWFGAlgorithm(int[][] matrix) {
        long start = System.nanoTime();
        int n = matrix.length;
        int[] visited = new int[n]; // 0=unvisited, 1=visiting, 2=visited
        Set<Integer> deadlocked = new TreeSet<>();

        for (int i = 0; i < n; i++) {
            if (visited[i] == 0) dfs(i, matrix, visited, new ArrayList<>(), deadlocked);
        }

        double timeMs = (System.nanoTime() - start) / 1_000_000.0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("deadlocked", !deadlocked.isEmpty());
        result.put("processes", deadlocked.toString());
        result.put("timeMs", String.format("%.4f", timeMs));
        result.put("timeComp", "O(V + E)");
        result.put("spaceComp", "O(V)");
        return result;
    }

    private void dfs(int node, int[][] matrix, int[] visited, List<Integer> path, Set<Integer> deadlocked) {
        visited[node] = 1;
        path.add(node);

        for (int neighbor = 0; neighbor < matrix.length; neighbor++) {
            if (matrix[node][neighbor] == 1) {
                if (visited[neighbor] == 1) { // Cycle detected
                    for (int i = path.indexOf(neighbor); i < path.size(); i++) {
                        deadlocked.add(path.get(i));
                    }
                } else if (visited[neighbor] == 0) {
                    dfs(neighbor, matrix, visited, path, deadlocked);
                }
            }
        }
        path.remove(path.size() - 1);
        visited[node] = 2;
    }

    // --- Algorithm 2: Matrix Vector Detection (Banker's Variant) ---
    private Map<String, Object> runMatrixAlgorithm(int[] avail, int[][] alloc, int[][] req) {
        long start = System.nanoTime();
        int n = alloc.length, m = avail.length;
        int[] work = Arrays.copyOf(avail, m);
        boolean[] finish = new boolean[n];

        // Process with 0 requests and 0 allocation finishes automatically
        for (int i = 0; i < n; i++) {
            boolean empty = true;
            for (int j = 0; j < m; j++) {
                if (alloc[i][j] != 0 || req[i][j] != 0) { empty = false; break; }
            }
            if (empty) finish[i] = true;
        }

        List<String> safeSeq = new ArrayList<>();
        while (true) {
            boolean progress = false;
            for (int i = 0; i < n; i++) {
                if (!finish[i]) {
                    boolean canGrant = true;
                    for (int j = 0; j < m; j++) {
                        if (req[i][j] > work[j]) { canGrant = false; break; }
                    }

                    if (canGrant) {
                        for (int j = 0; j < m; j++) work[j] += alloc[i][j];
                        finish[i] = true;
                        safeSeq.add("P" + i);
                        progress = true;
                        break;
                    }
                }
            }
            if (!progress) break;
        }

        List<Integer> deadlocked = new ArrayList<>();
        for (int i = 0; i < n; i++) if (!finish[i]) deadlocked.add(i);

        double timeMs = (System.nanoTime() - start) / 1_000_000.0;

        Map<String, Object> result = new HashMap<>();
        result.put("deadlocked", !deadlocked.isEmpty());
        result.put("processes", deadlocked.toString());
        result.put("safeSeq", safeSeq.toString());
        result.put("timeMs", String.format("%.4f", timeMs));
        result.put("timeComp", "O(m * n^2)");
        result.put("spaceComp", "O(m * n)");
        return result;
    }

    // =========================================================================
    // POINT 8: GUI DESIGN AND LAYOUT PANELS
    // =========================================================================

    // --- Tab 1: Wait-For Graph Interface ---
    private JPanel createWFGPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Input: Adjacency Matrix (0/1)"));
        wfgInput = new JTextArea("0 1 0 0\n0 0 1 0\n1 0 0 1\n0 0 0 0");
        JButton runBtn = new JButton("Execute Algorithm 1");
        runBtn.addActionListener(e -> handleWFGExecution());
        left.add(new JScrollPane(wfgInput), BorderLayout.CENTER);
        left.add(runBtn, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Output Results"));
        wfgOutput = new JTextArea();
        wfgOutput.setEditable(false);
        right.add(new JScrollPane(wfgOutput), BorderLayout.CENTER);

        panel.add(left); panel.add(right);
        return panel;
    }

    // --- Tab 2: Matrix Algorithm Interface ---
    private JPanel createMatrixPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        JPanel inputs = new JPanel(new GridLayout(4, 2, 5, 5));
        inputs.setBorder(BorderFactory.createTitledBorder("Input: System Vectors & Matrices"));
        
        inputs.add(new JLabel("Available Vector:"));
        availInput = new JTextField("0 0 0");
        inputs.add(availInput);

        inputs.add(new JLabel("Allocation Matrix:"));
        allocInput = new JTextArea("0 1 0\n2 0 0\n3 0 3");
        inputs.add(new JScrollPane(allocInput));

        inputs.add(new JLabel("Request Matrix:"));
        reqInput = new JTextArea("0 0 0\n2 0 2\n0 0 0");
        inputs.add(new JScrollPane(reqInput));

        JButton runBtn = new JButton("Execute Algorithm 2");
        runBtn.addActionListener(e -> handleMatrixExecution());
        inputs.add(runBtn);

        matrixOutput = new JTextArea();
        matrixOutput.setEditable(false);

        panel.add(inputs, BorderLayout.NORTH);
        panel.add(new JScrollPane(matrixOutput), BorderLayout.CENTER);
        return panel;
    }

    // --- Tab 3: Performance Comparison Interface ---
    private JPanel createComparisonPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JButton benchmarkBtn = new JButton("Compare Performance of Both Algorithms");
        benchmarkBtn.addActionListener(e -> handleComparison());

        String[] cols = {"Algorithm Name", "Time Complexity", "Space Complexity", "Resource Support", "Execution Time"};
        compareTable = new JTable(new DefaultTableModel(cols, 0));

        panel.add(benchmarkBtn, BorderLayout.NORTH);
        panel.add(new JScrollPane(compareTable), BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // EVENT HANDLERS & INPUT/OUTPUT VALIDATION
    // =========================================================================

    private void handleWFGExecution() {
        try {
            String[] lines = wfgInput.getText().trim().split("\n");
            int n = lines.length;
            int[][] matrix = new int[n][n];

            for (int i = 0; i < n; i++) {
                String[] tokens = lines[i].trim().split("\\s+");
                if (tokens.length != n) throw new IllegalArgumentException("Row " + i + " must have " + n + " columns.");
                for (int j = 0; j < n; j++) matrix[i][j] = Integer.parseInt(tokens[j]);
            }

            Map<String, Object> res = runWFGAlgorithm(matrix);
            wfgOutput.setText("--- ALGORITHM 1: WAIT-FOR GRAPH ---\n" +
                "Deadlock Status  : " + ((boolean)res.get("deadlocked") ? "DEADLOCK DETECTED!" : "NO DEADLOCK") + "\n" +
                "Deadlocked Nodes : " + res.get("processes") + "\n" +
                "Execution Time   : " + res.get("timeMs") + " ms\n" +
                "Time Complexity  : " + res.get("timeComp") + "\n" +
                "Space Complexity : " + res.get("spaceComp"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid Input: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleMatrixExecution() {
        try {
            String[] availStr = availInput.getText().trim().split("\\s+");
            int m = availStr.length;
            int[] avail = new int[m];
            for (int j = 0; j < m; j++) avail[j] = Integer.parseInt(availStr[j]);

            String[] allocLines = allocInput.getText().trim().split("\n");
            String[] reqLines = reqInput.getText().trim().split("\n");
            int n = allocLines.length;

            int[][] alloc = new int[n][m];
            int[][] req = new int[n][m];

            for (int i = 0; i < n; i++) {
                String[] aTokens = allocLines[i].trim().split("\\s+");
                String[] rTokens = reqLines[i].trim().split("\\s+");
                for (int j = 0; j < m; j++) {
                    alloc[i][j] = Integer.parseInt(aTokens[j]);
                    req[i][j] = Integer.parseInt(rTokens[j]);
                }
            }

            Map<String, Object> res = runMatrixAlgorithm(avail, alloc, req);
            matrixOutput.setText("--- ALGORITHM 2: MATRIX DETECTION ---\n" +
                "Deadlock Status  : " + ((boolean)res.get("deadlocked") ? "DEADLOCK DETECTED!" : "NO DEADLOCK") + "\n" +
                "Deadlocked Units : " + res.get("processes") + "\n" +
                "Safe Path        : " + res.get("safeSeq") + "\n" +
                "Execution Time   : " + res.get("timeMs") + " ms\n" +
                "Time Complexity  : " + res.get("timeComp") + "\n" +
                "Space Complexity : " + res.get("spaceComp"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid Matrix Setup: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleComparison() {
        // Sample benchmark execution on 10 processes
        int[][] wfgMat = new int[10][10];
        wfgMat[0][1] = 1; wfgMat[1][2] = 1; wfgMat[2][0] = 1; 
        Map<String, Object> res1 = runWFGAlgorithm(wfgMat);

        int[] avail = {1, 1};
        int[][] alloc = new int[10][2];
        int[][] req = new int[10][2];
        Map<String, Object> res2 = runMatrixAlgorithm(avail, alloc, req);

        DefaultTableModel model = (DefaultTableModel) compareTable.getModel();
        model.setRowCount(0);
        model.addRow(new Object[]{"Wait-For Graph (DFS)", res1.get("timeComp"), res1.get("spaceComp"), "Single Instance", res1.get("timeMs") + " ms"});
        model.addRow(new Object[]{"Matrix Vector Detection", res2.get("timeComp"), res2.get("spaceComp"), "Multiple Instances", res2.get("timeMs") + " ms"});
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimplifiedDeadlockSimulator().setVisible(true));
    }
}
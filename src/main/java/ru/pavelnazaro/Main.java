package ru.pavelnazaro;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class Main {

    private static final Pattern VALID_LINE_PATTERN = Pattern.compile("^(\"[0-9]*\"(;\"[0-9]*\")*)$");
    private static final String OUTPUT_TXT = "output.txt";
    private static final String JAVA_RUN_SAMPLE = "Usage: java -jar Main.jar input.txt -Xmx1G";
    private static final String FILE_S_NOT_FOUND = "File %s not found%n";
    private static final String SPLIT_REGEX = ";";
    private static final String REMOVE_UNNECESSARY_SYMBOL = "\"";
    private static final String REMOVE_UNNECESSARY_SYMBOL_REPLACEMENT = "";
    private static final String TIME_S_MS = "Time: %s ms%n";
    private static final String COUNT_GROUPS_WITH_MORE_THAN_ONE_ELEMENTS = "Count groups with more than one elements: ";
    private static final String GROUP = "Group ";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println(JAVA_RUN_SAMPLE);
            return;
        }

        String inputFilePath = args[0];
        File file = new File(inputFilePath);
        if (!file.exists()) {
            System.out.printf(FILE_S_NOT_FOUND, inputFilePath);
            return;
        }

        long startTime = System.currentTimeMillis();

        Set<String[]> uniqueLines = readAndFilterLines(inputFilePath);
        List<Set<String[]>> groups = groupLines(uniqueLines);
        writeGroupsToFile(groups);

        long endTime = System.currentTimeMillis();
        System.out.printf(TIME_S_MS, (endTime - startTime));
        long multiElementGroupsCount = groups.stream().filter(g -> g.size() > 1).count();
        System.out.println(COUNT_GROUPS_WITH_MORE_THAN_ONE_ELEMENTS + multiElementGroupsCount);
    }

    private static Set<String[]> readAndFilterLines(String filePath) {
        Set<String[]> uniqueLines = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isValidLine(line)) {
                    uniqueLines.add(line.replace(REMOVE_UNNECESSARY_SYMBOL, REMOVE_UNNECESSARY_SYMBOL_REPLACEMENT).split(SPLIT_REGEX));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uniqueLines;
    }

    private static boolean isValidLine(String line) {
        return VALID_LINE_PATTERN.matcher(line).matches();
    }

    private static List<Set<String[]>> groupLines(Set<String[]> uniqueLines) {
        List<Set<String[]>> groups = new ArrayList<>();
        Set<String[]> usedLines = new HashSet<>();
        Map<Integer, Map<String, Set<String[]>>> columnIndex = new HashMap<>();

        for (String[] line : uniqueLines) {
            for (int i = 0; i < line.length; i++) {
                if (!line[i].isEmpty()) {
                    columnIndex
                            .computeIfAbsent(i, k -> new HashMap<>())
                            .computeIfAbsent(line[i], k -> new HashSet<>())
                            .add(line);
                }
            }
        }

        for (String[] line : uniqueLines) {
            if (usedLines.contains(line)) {
                continue;
            }

            Set<String[]> currentGroup = new HashSet<>();
            Queue<String[]> queue = new LinkedList<>();
            queue.add(line);
            usedLines.add(line);

            while (!queue.isEmpty()) {
                String[] currentLine = queue.poll();
                currentGroup.add(currentLine);

                for (int i = 0; i < currentLine.length; i++) {
                    if (!currentLine[i].isEmpty()) {
                        Set<String[]> matches = columnIndex.get(i).get(currentLine[i]);
                        if (matches != null) {
                            for (String[] match : matches) {
                                if (!usedLines.contains(match) && hasColumnMatch(currentLine, match)) {
                                    queue.add(match);
                                    usedLines.add(match);
                                }
                            }
                        }
                    }
                }
            }

            if (currentGroup.size() > 1) {
                groups.add(currentGroup);
            }
        }

        groups.sort(Comparator.comparingInt((Set<String[]> group) ->
                group.stream().mapToInt(arr -> arr.length).sum()).reversed());

        return groups;
    }

    private static boolean hasColumnMatch(String[] line1, String[] line2) {
        int minCount = Math.min(line1.length, line2.length);
        for (int i = 0; i < minCount; i++) {
            if (!line1[i].isEmpty() && line1[i].equals(line2[i])) {
                return true;
            }
        }
        return false;
    }

    private static void writeGroupsToFile(List<Set<String[]>> groups) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_TXT))) {
            int groupNumber = 1;

            for (Set<String[]> group : groups) {
                writer.write(GROUP + groupNumber + System.lineSeparator());
                for (String[] line : group) {
                    writer.write(String.join(SPLIT_REGEX, line) + System.lineSeparator());
                }
                writer.write(System.lineSeparator());
                groupNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
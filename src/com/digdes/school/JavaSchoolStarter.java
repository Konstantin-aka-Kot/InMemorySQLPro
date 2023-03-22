package com.digdes.school;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavaSchoolStarter {

    public static void main(String... args){
        JavaSchoolStarter starter = new JavaSchoolStarter();
        try {
            //Вставка строки в коллекцию
            List<Map<String,Object>> result1 = starter.execute("INSERT VALUES 'lastName' = 'Федоров' , 'id'=3, 'age'=40, 'active'=true");
            System.out.println("INSERT: " + result1);
            //Изменение значения которое выше записывали
            List<Map<String,Object>> result2 = starter.execute("UPDATE VALUES 'active'=false, 'cost'=10.1 where 'id'=3");
            System.out.println("UPDATE: " + result2);
            //Получение всех данных из коллекции (т.е. в данном примере вернется 1 запись)
            List<Map<String,Object>> result3 = starter.execute("SELECT");
            System.out.println("SELECT: " + result3);

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
    private List<Map<String, Object>> data;

    public JavaSchoolStarter() {
        data = new ArrayList<>();
    }

    public List<Map<String, Object>> execute(String command) {
        String[] tokens = command.trim().split(" ", 2);
        String operation = tokens[0].toUpperCase(Locale.ROOT);

        switch (operation) {
            case "INSERT":
                return insert(tokens[1]);
            case "UPDATE":
                return update(tokens[1]);
            case "DELETE":
                return delete(tokens[1]);
            case "SELECT":
                return select(tokens.length > 1 ? tokens[1] : "");
            default:
                throw new IllegalArgumentException("Invalid operation: " + operation);
        }
    }

    private List<Map<String, Object>> insert(String values) {
        Map<String, Object> newRow = parseValues(values);
        data.add(newRow);
        return Collections.singletonList(newRow);
    }

    private List<Map<String, Object>> update(String conditions) {
        String[] parts = conditions.split("(?i) WHERE ", 2);
        Map<String, Object> newValues = parseValues(parts[0]);
        List<Map<String, Object>> updatedRows = new ArrayList<>();
        if (parts.length > 1) {
            String condition = parts[1];
            for (Map<String, Object> row : data) {
                if (applyCondition(row, condition)) {
                    row.putAll(newValues);
                    updatedRows.add(row);
                }
            }
        } else {
            data.forEach(row -> row.putAll(newValues));
            updatedRows.addAll(data);
        }
        return updatedRows;
    }

    private List<Map<String, Object>> delete(String conditions) {
        String[] parts = conditions.split("(?i) WHERE ", 2);
        List<Map<String, Object>> removedRows = new ArrayList<>();
        if (parts.length > 1) {
            String condition = parts[1];
            Iterator<Map<String, Object>> iterator = data.iterator();
            while (iterator.hasNext()) {
                Map<String, Object> row = iterator.next();
                if (applyCondition(row, condition)) {
                    iterator.remove();
                    removedRows.add(row);
                }
            }
        } else {
            removedRows.addAll(data);
            data.clear();
        }
        return removedRows;
    }

    private List<Map<String, Object>> select(String conditions) {
        String[] parts = conditions.split("(?i) WHERE ", 2);
        if (parts.length > 1) {
            String condition = parts[1];
            return data.stream()
                    .filter(row -> applyCondition(row, condition))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>(data);
        }
    }

    private Map<String, Object> parseValues(String valuesString) {
        String[] keyValuePairs = valuesString.split(",");
        Map<String, Object> values = new LinkedHashMap<>();
        for (String keyValuePair : keyValuePairs) {
            String[] parts = keyValuePair.split("=", 2);
            String key = parts[0].trim().toLowerCase(Locale.ROOT);
            if (key.startsWith("'") && key.endsWith("'")) {
                key = key.substring(1, key.length() - 1);
            }
            Object value = parseValue(parts[1].trim());
            values.put(key, value);
        }
        return values;
    }

    private Object parseValue(String value) {
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                throw new IllegalArgumentException("Invalid value: " + value);
            }
        }
    }

    private boolean applyCondition(Map<String, Object> row, String condition) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        Pattern pattern = Pattern.compile("('?\\w+'?)\\s*(=|!=|>|<|>=|<=)\\s*(.*)");
        Matcher matcher = pattern.matcher(condition);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid condition: " + condition);
        }

        String columnName = matcher.group(1);
        if (columnName.startsWith("'") && columnName.endsWith("'")) {
            columnName = columnName.substring(1, columnName.length() - 1);
        }

        String operator = matcher.group(2);
        Object value = parseValue(matcher.group(3));

        if (!row.containsKey(columnName)) {
            throw new IllegalArgumentException("Invalid column name: " + columnName);
        }

        return compare(row.get(columnName), value, operator);
    }

    private boolean compare(Object columnValue, Object value, String operator) {
        if (columnValue == null) {
            return !operator.equals("=");
        }

        if (value == null) {
            return !operator.equals("!=");
        }

        int comparisonResult;
        if (columnValue instanceof Comparable && value instanceof Comparable) {
            comparisonResult = ((Comparable) columnValue).compareTo(value);
        } else {
            throw new IllegalArgumentException("Invalid comparison between " + columnValue + " and " + value);
        }

        switch (operator) {
            case "=":
                return comparisonResult == 0;
            case "!=":
                return comparisonResult != 0;
            case ">":
                return comparisonResult > 0;
            case "<":
                return comparisonResult < 0;
            case ">=":
                return comparisonResult >= 0;
            case "<=":
                return comparisonResult <= 0;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }
}
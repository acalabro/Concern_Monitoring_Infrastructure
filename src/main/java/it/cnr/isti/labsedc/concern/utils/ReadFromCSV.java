package it.cnr.isti.labsedc.concern.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ReadFromCSV implements Iterator<String>, AutoCloseable {

    private BufferedReader reader;
    private int columnIndex;
    private String nextValue;   

    public ReadFromCSV(String csvFile, String columnName) {
        try {
			reader = new BufferedReader(new FileReader(csvFile));
        String headerLine;
			headerLine = reader.readLine();

        if (headerLine == null) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        String[] headers = headerLine.split(",");
        columnIndex = -1;

        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equals(columnName)) {
                columnIndex = i;
                break;
            }
        }

        if (columnIndex == -1) {
            throw new IllegalArgumentException("Column not found: " + columnName);
        }

        advance();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void advance() {
        String line;
        nextValue = null;

        try {
			while ((line = reader.readLine()) != null) {
			    String[] fields = line.split(",");
			    if (fields.length > columnIndex) {
			        nextValue = fields[columnIndex].trim();
			        return;
			    }
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    @Override
    public boolean hasNext() {
        return nextValue != null;
    }

    @Override
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        String value = nextValue;
        advance();
        return value;
    }

    @Override
    public void close() {
        try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void main(String[] args) {
        try (ReadFromCSV reader =
                 new ReadFromCSV("/home/acalabro/Desktop/Dataset/GNB_MacScheduler_ordinato.csv", "ULSCH_Round_1")) {

            while (reader.hasNext()) {
                String value = reader.next();
                System.out.println(value);
            }
        }
    }
}
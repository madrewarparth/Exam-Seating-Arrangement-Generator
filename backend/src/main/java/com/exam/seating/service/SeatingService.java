package com.exam.seating.service;

import com.exam.seating.model.SeatingPlan;
import com.exam.seating.model.Student;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeatingService {

    @Value("${room.capacity:30}")
    private int roomCapacity;

    @Value("${pdf.output.dir:/tmp/seating}")
    private String pdfOutputDir;

    private List<Student> students = new ArrayList<>();
    private List<SeatingPlan> currentPlan = new ArrayList<>();

    public List<Student> processCsv(MultipartFile file) throws Exception {
        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            CsvToBean<Student> csvToBean = new CsvToBeanBuilder<Student>(reader)
                    .withType(Student.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            this.students = csvToBean.parse();
            return this.students;
        }
    }

    public List<SeatingPlan> generatePlan(String examDate) {
        if (students.isEmpty()) {
            throw new IllegalStateException("No students uploaded.");
        }

        // Sort students by branch and then by roll number
        Map<String, List<Student>> studentsByBranch = students.stream()
                .sorted(Comparator.comparing(Student::getRollNo))
                .collect(Collectors.groupingBy(Student::getBranch));

        List<String> branches = new ArrayList<>(studentsByBranch.keySet());
        
        List<SeatingPlan> plan = new ArrayList<>();
        int roomIndex = 1;
        int benchInRoom = 1;

        boolean studentsRemaining = true;
        
        // Iterators for each branch
        Map<String, Iterator<Student>> branchIterators = new HashMap<>();
        for (String branch : branches) {
            branchIterators.put(branch, studentsByBranch.get(branch).iterator());
        }

        while (studentsRemaining) {
            studentsRemaining = false;

            for (String branch : branches) {
                Iterator<Student> iterator = branchIterators.get(branch);
                if (iterator.hasNext()) {
                    studentsRemaining = true;
                    Student student = iterator.next();
                    
                    String roomNumber = "Room-" + roomIndex;
                    plan.add(new SeatingPlan(student, roomNumber, benchInRoom));
                    
                    benchInRoom++;
                    if (benchInRoom > roomCapacity) {
                        benchInRoom = 1;
                        roomIndex++;
                    }
                }
            }
        }

        this.currentPlan = plan;
        return plan;
    }

    public File exportPdf(String examDate) throws Exception {
        if (currentPlan.isEmpty()) {
            throw new IllegalStateException("Seating plan not generated yet.");
        }

        File dir = new File(pdfOutputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File pdfFile = new File(dir, "seating-plan-" + examDate + ".pdf");
        PdfWriter writer = new PdfWriter(pdfFile.getAbsolutePath());
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        Paragraph header = new Paragraph("Exam Seating Plan")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20);
        document.add(header);

        Paragraph dateP = new Paragraph("Date: " + examDate)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(14);
        document.add(dateP);
        document.add(new Paragraph("\n"));

        // Group by room
        Map<String, List<SeatingPlan>> planByRoom = currentPlan.stream()
                .collect(Collectors.groupingBy(SeatingPlan::getRoomNumber));

        List<String> sortedRooms = new ArrayList<>(planByRoom.keySet());
        Collections.sort(sortedRooms);

        for (String room : sortedRooms) {
            document.add(new Paragraph("Room: " + room).setFontSize(16).setBold());
            
            float[] columnWidths = {50F, 100F, 150F, 100F};
            Table table = new Table(columnWidths);
            
            table.addHeaderCell("Bench");
            table.addHeaderCell("Roll No");
            table.addHeaderCell("Name");
            table.addHeaderCell("Branch");

            List<SeatingPlan> roomPlan = planByRoom.get(room);
            roomPlan.sort(Comparator.comparing(SeatingPlan::getBenchNumber));

            for (SeatingPlan sp : roomPlan) {
                table.addCell(String.valueOf(sp.getBenchNumber()));
                table.addCell(sp.getStudent().getRollNo());
                table.addCell(sp.getStudent().getName());
                table.addCell(sp.getStudent().getBranch());
            }

            document.add(table);
            document.add(new Paragraph("\n"));
        }

        document.close();
        return pdfFile;
    }

    public List<String> getRooms() {
        return currentPlan.stream()
                .map(SeatingPlan::getRoomNumber)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
